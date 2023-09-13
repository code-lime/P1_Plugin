package org.lime.gp.block;

import com.google.gson.JsonObject;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.lime.Position;
import org.lime.docs.IGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.data.MultiBlockInstance;
import org.lime.gp.block.component.list.MultiBlockComponent;
import org.lime.gp.docs.IDocsLink;
import org.lime.system;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class BlockInfo {
    public final LinkedHashMap<String, ComponentStatic<?>> components = new LinkedHashMap<>();

    private final String _key;
    public String getKey() { return _key; }

    private final long loadIndex = System.currentTimeMillis();
    public long getLoadIndex() { return loadIndex; }

    public BlockInfo(String key) { this._key = key; }
    public BlockInfo(String key, JsonObject json) {
        this(key);
        if (json.has("components")) json.get("components").getAsJsonObject().entrySet().forEach(kv -> {
            ComponentStatic<?> setting = ComponentStatic.parse(kv.getKey(), this, kv.getValue());
            components.put(setting.name(), setting);
        });
    }

    public BlockInfo add(ComponentStatic<?> component) {
        this.components.put(component.name(), component);
        return this;
    }
    public BlockInfo add(system.Func1<BlockInfo, ComponentStatic<?>> component) {
        return add(component.invoke(this));
    }
    @SuppressWarnings("unchecked")
    public <T extends ComponentStatic<?>> Optional<T> component(Class<T> tClass) {
        for (ComponentStatic<?> component : components.values()) {
            if (tClass.isInstance(component))
                return Optional.of((T) component);
        }
        return Optional.empty();
    }
    public <T extends ComponentStatic<?>> Stream<T> components(Class<T> tClass) {
        return components.values().stream().filter(tClass::isInstance).map(tClass::cast);
    }

    public TileEntityLimeSkull setBlock(Position position) {
        return setBlock(position, Collections.emptyMap());
    }
    public TileEntityLimeSkull setBlock(Position position, InfoComponent.Rotation.Value rotation) {
        return setBlock(position, Collections.singletonMap("display", system.json.object().add("rotation", rotation.angle + "").build()));
    }
    public TileEntityLimeSkull setBlock(Position position, Map<String, JsonObject> data) {
        return Blocks.setBlock(position, this, data);
    }

    public List<TileEntityLimeSkull> setMultiBlock(Player player, Position position, InfoComponent.Rotation.Value rotation) {
        return setMultiBlock(player, position, Collections.emptyMap(), rotation);
    }
    public List<TileEntityLimeSkull> setMultiBlock(Player player, Position position, Map<String, JsonObject> data, InfoComponent.Rotation.Value rotation) {
        data = new HashMap<>(data);
        data.compute("display", (k,v) -> (v == null ? system.json.object() : system.json.object().add(v)).add("rotation", rotation.angle + "").build());
        List<TileEntityLimeSkull> skulls = new ArrayList<>();
        TileEntityLimeSkull skull = setBlock(position, data);
        skulls.add(skull);
        skull.customKey().flatMap(key -> component(MultiBlockComponent.class)
                .map(v -> v.blocks.entrySet()
                        .stream()
                        .map(_v -> _v.getValue().set(player, key.uuid(), key.type(), position, rotation, _v.getKey()))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList())
                )
                .map(v -> {
                    v.forEach(_child -> _child.forEach(child -> child.customUUID()
                            .ifPresent(child_uuid -> Blocks.customOf(skull)
                                    .ifPresent(_v -> _v.list(MultiBlockInstance.class)
                                            .forEach(instance -> instance.child(child_uuid, child.getBlockPos()))
                                    )
                            )
                    ));
                    return v;
                })
        ).ifPresent(v -> v.forEach(_v -> skulls.addAll(_v)));
        return skulls;
    }

    public interface Replacer<T> {
        BlockInfo info();
        Optional<T> read(Block block);
        default Map<String, JsonObject> variable(T value) { return Collections.emptyMap(); }
        default void edit(T value, CustomTileMetadata metadata) { }

        default boolean replace(Block block) {
            return read(block).flatMap(val -> Blocks.customOf(info().setBlock(new Position(block.getLocation()), variable(val)))
                    .map(metadata -> {
                        edit(val, metadata);
                        return true;
                    })
            ).orElse(false);
        }
    }

    public final HashMap<Material, List<BlockInfo.Replacer<?>>> replaces = new HashMap<>();

    public BlockInfo addReplace(Material material, Replacer<?> replacer) {
        replaces.computeIfAbsent(material, v -> new ArrayList<>()).add(replacer);
        return this;
    }
    public BlockInfo addReplace(Material material) {
        BlockInfo _this = this;
        return addReplace(material, new Replacer<Integer>() {
            @Override public BlockInfo info() { return _this; }
            @Override public Optional<Integer> read(Block block) { return Optional.of(0); }
        });
    }

    public static <T extends Comparable<T>> IBlockData setValue(IBlockData data, IBlockState<T> state, String value) {
        return state.getValue(value).map(v -> data.setValue(state, v)).orElse(data);
    }
    public static <T extends Comparable<T>>String getValue(IBlockData data, IBlockState<T> state) {
        return state.getName(data.getValue(state));
    }

    @Override public String toString() { return "BlockInfo[" + Optional.ofNullable(getKey()).orElse("NULLABLE") + "]"; }

    public static IGroup docs(String title, IDocsLink docs) {
        return JsonGroup.of(title, JObject.of(
                JProperty.optional(IName.raw("components"), IJElement.anyObject(
                        JProperty.require(IName.raw("COMPONENT_NAME"), IJElement.link(docs.component()))
                ), IComment.text("Указывает список компонентов для конкретного блока"))
        ));
    }
}