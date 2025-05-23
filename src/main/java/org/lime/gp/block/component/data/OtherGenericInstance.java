package org.lime.gp.block.component.data;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullDestroyInfo;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.OtherGenericComponent;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.json.JsonObjectOptional;
import org.lime.system.execute.Func2;
import org.lime.system.json;
import org.lime.system.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class OtherGenericInstance extends BlockInstance implements CustomTileMetadata.Tickable, MultiBlockInstance.OwnerVariableModifiable, CustomTileMetadata.Interactable, CustomTileMetadata.Destroyable, CustomTileMetadata.Damageable {
    public BlockPosition offset;
    public UUID owner;
    public String owner_type;

    public final boolean interactOwner;

    public OtherGenericInstance(OtherGenericComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        interactOwner = component.interactOwner;
    }

    private static BlockPosition parse(String text) {
        var pos = MathUtils.getPosToast(text);
        return new BlockPosition(pos.val0, pos.val1, pos.val2);
    }

    @Override public void read(JsonObjectOptional json) {
        this.offset = json.getAsString("offset")
                .map(OtherGenericInstance::parse)
                .or(() -> json.getAsString("position")
                        .map(OtherGenericInstance::parse)
                        .map(v -> v.subtract(metadata().skull.getBlockPos())))
                .orElse(null);
        this.owner = json.getAsString("owner").map(UUID::fromString).orElse(null);
        this.owner_type = json.getAsString("owner_type").orElse(null);
    }
    @Override public json.builder.object write() {
        return json.object()
                .add("offset", this.offset == null ? null : (offset.getX() +" " + offset.getY() + " " + offset.getZ()))
                .add("owner", this.owner)
                .add("owner_type", this.owner_type);
    }
    public static JsonObject mapBlockUuids(JsonObject json, BlockPosition position, Func2<BlockPosition, UUID, UUID> mapper) {
        if (json.has("owner") && json.has("offset")) {
            json = json.deepCopy();
            BlockPosition offset = parse(json.get("offset").getAsString());

            json.addProperty("owner", mapper.invoke(position.offset(offset), UUID.fromString(json.get("owner").getAsString())).toString());
        }
        return json;
    }

    public static Optional<String> tryGetOwnerType(PersistentDataContainer container) {
        return Optional.ofNullable(container.get(Blocks.ofKey("other.generic"), LimePersistentDataType.JSON_OBJECT))
                .map(JsonObjectOptional::of)
                .flatMap(v -> v.getAsString("owner_type"));
    }

    public Optional<TileEntityLimeSkull> owner() {
        TileEntityLimeSkull base = metadata().skull;
        return Optional.of(base.getLevel())
                .flatMap(v -> Optional.ofNullable(offset).map(_v -> base.getBlockPos().offset(_v)).flatMap(_v -> v.getBlockEntity(_v, TileEntityTypes.SKULL)))
                .map(v -> v instanceof TileEntityLimeSkull skull ? skull : null)
                .filter(v -> v.customUUID().filter(uuid -> uuid.equals(owner)).isPresent());
    }

    private int ticker = 0;
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if ((ticker++ % 40) == 0) owner().ifPresentOrElse(skull -> {}, metadata::setAir);
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        if (interactOwner) return owner().flatMap(Blocks::of).map(v -> v.onInteract(event)).orElse(EnumInteractionResult.PASS);
        return EnumInteractionResult.PASS;
    }
    @Override public void onDamage(CustomTileMetadata metadata, BlockDamageEvent event) {
        if (interactOwner) owner().flatMap(Blocks::of).ifPresent(v -> v.onDamage(event));
    }
    @Override public void onDestroy(CustomTileMetadata metadata, BlockSkullDestroyInfo event) {
        World world = event.world();
        owner()
                .flatMap(Blocks::customOf)
                .filter(owner -> owner.list(MultiBlockInstance.class).findFirst().map(_v -> !_v.inDestroyMethod).orElse(true))
                .ifPresent(owner -> event.player()
                        .map(v -> v instanceof EntityPlayer p ? p : null)
                        .ifPresentOrElse(player -> {
                            List<EntityItem> captureDrops = world.captureDrops;
                            player.gameMode.destroyBlock(owner.skull.getBlockPos());
                            world.captureDrops = captureDrops;
                        }, () -> world.destroyBlock(owner.skull.getBlockPos(), false))
                );
    }

    /*
    @Override public Map<String, String> onDisplayVariable() {
        Map<String, String> map = new HashMap<>();
        owner()
                .flatMap(Blocks::customOf)
                .stream()
                .flatMap(v -> v.list(DisplayInstance.class))
                .flatMap(v -> v.getAll().entrySet().stream())
                .forEach(kv -> map.put("owner." + kv.getKey(), kv.getValue()));
        return map;
    }
    */
    @Override public void onOwnerVariableModify(CustomTileMetadata metadata, Map<String, String> variables) {
        metadata.list(DisplayInstance.class)
                .findAny()
                .ifPresent(display -> display.modify(map -> {
                    variables.forEach((key, value) -> map.put("owner." + key, value));
                    map.keySet().removeIf(k -> k.startsWith("owner.") && !variables.containsKey(k.substring(6)));
                    return true;
                }));
    }

    public Stream<TileEntityLimeSkull> getStructureBlocks() {
        return owner()
                .flatMap(Blocks::customOf)
                .stream()
                .flatMap(owner -> owner.list(MultiBlockInstance.class).flatMap(MultiBlockInstance::getStructureBlocks));
    }
}




















