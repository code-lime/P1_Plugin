package org.lime.gp.block.component.display;

import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BlockSkullShapeInfo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.lime.Position;
import org.lime.core;
import org.lime.display.Models;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.invokable.BlockDirtyInvokable;
import org.lime.gp.block.component.display.invokable.BlockUpdateInvokable;
import org.lime.gp.block.component.list.DisplayComponent;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.ProxyMap;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.module.TimeoutData;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class DisplayInstance extends BlockInstance implements CustomTileMetadata.Shapeable, CustomTileMetadata.Tickable, CustomTileMetadata.AsyncTickable, CustomTileMetadata.FirstTickable, CustomTileMetadata.Removeable {
    private static int TIMEOUT_TICKS = 20;
    public static core.element create() {
        return core.element.create(DisplayInstance.class)
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("display.timeout")
                        .withDefault(new JsonPrimitive(20))
                        .withInvoke(_v -> TIMEOUT_TICKS = _v.getAsInt())
                );
    }

    private final system.LockToast1<Long> variableIndex = system.toast(1L).lock();
    private final ConcurrentHashMap<String, String> variables = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, DisplayPartial.Partial> partials = new ConcurrentHashMap<>();

    @Override public DisplayComponent component() { return (DisplayComponent)super.component(); }

    private void variableDirty() {
        variableIndex.edit0(v -> v + 1);
        saveData();
    }

    public DisplayInstance(DisplayComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }
    public void set(String key, String value) {
        if (Objects.equals(variables.put(key, value), value)) return;
        variableDirty();
    }
    public void set(Map<String, String> variables) {
        this.variables.putAll(variables);
        variableDirty();
    }
    public void modify(system.Func1<Map<String, String>, Boolean> modifyFunc) {
        if (!modifyFunc.invoke(variables)) return;
        variableDirty();
    }
    public Optional<String> get(String key) { return Optional.ofNullable(variables.get(key)); }
    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(variables);
    }
    public long variableIndex() { return variableIndex.get0(); }

    public Optional<InfoComponent.Rotation.Value> getRotation() {
        return get("rotation").flatMap(ExtMethods::parseInt).map(InfoComponent.Rotation.Value::ofAngle);
    }

    public Optional<DisplayPartial.Partial> getPartial(double distanceSquared, Map<String, String> variables) {
        for (DisplayPartial.Partial partial : component().partials) {
            if (partial.distanceSquared <= distanceSquared)
                return Optional.of(partial.partial(variables));
        }
        return Optional.empty();
    }

    public static final class DisplayMap extends TimeoutData.ITimeout {
        public final Map<UUID, ItemFrameDisplayObject> frameMap = new HashMap<>();
        public final Map<BlockModelDisplay.BlockModelKey, ModelDisplayObject> modelMap = new HashMap<>();
        public DisplayMap(Map<UUID, ItemFrameDisplayObject> itemFrame, Map<BlockModelDisplay.BlockModelKey, ModelDisplayObject> models) {
            super(TIMEOUT_TICKS);
            this.frameMap.putAll(itemFrame);
            this.modelMap.putAll(models);
        }
    }

    public record ItemFrameDisplayObject(Location location, ItemStack item, InfoComponent.Rotation.Value rotation, UUID index) {
        public static ItemFrameDisplayObject of(Location location, ItemStack item, InfoComponent.Rotation.Value rotation, UUID index) {
            return new ItemFrameDisplayObject(location, item, rotation, index);
        }
    }
    public record ModelDisplayObject(Location location, Set<UUID> viewers, Models.Model model, Map<String, Object> data) {
        public boolean hasViewer(UUID uuid) { return viewers.contains(uuid); }
        public static ModelDisplayObject of(Location location, Models.Model model, Map<String, Object> data) {
            return new ModelDisplayObject(location, ConcurrentHashMap.newKeySet(), model, data);
        }
    }

    @Override public void read(JsonObjectOptional json) {
        variables.clear();
        json.forEach((key, value) -> value.getAsString()
                .or(() -> value.getAsNumber().map(Object::toString))
                .or(() -> value.getAsBoolean().map(Object::toString))
                .ifPresent(v -> variables.put(key, v)));
        variableDirty();
    }
    @Override public system.json.builder.object write() { return system.json.object().add(variables, k -> k, v -> v); }

    private DisplayPartial.Partial orSync(CustomTileMetadata metadata, UUID playerUUID, Player player, Map<String, String> variable, DisplayPartial.Partial _new, DisplayPartial.Partial _old) {
        if (Objects.equals(_new, _old)) return _new;
        TileEntityLimeSkull skull = metadata.skull;
        WorldServer world = (WorldServer)skull.getLevel();
        UUID worldUUID = world.uuid;
        BlockPosition position = skull.getBlockPos();
        if (_new instanceof DisplayPartial.BlockPartial block) cacheDisplay.put(playerUUID, block.getDynamicDisplay(position, variable));
        else cacheDisplay.remove(playerUUID);
        lime.invokable(new BlockUpdateInvokable(player, worldUUID, position, 1));
        lime.invokable(new BlockUpdateInvokable(player, worldUUID, position, 20));
        return _new;
    }

    public static boolean sendBlock(Player player, Position position) {
        BlockPosition pos = new BlockPosition(position.x, position.y, position.z);
        UUID worldUUID = position.world.getUID();
        lime.invokable(new BlockUpdateInvokable(player, worldUUID, pos, 1));
        lime.invokable(new BlockUpdateInvokable(player, worldUUID, pos, 20));
        return true;
    }

    public static boolean markDirtyBlock(net.minecraft.world.level.World world, BlockPosition position) {
        if (!(world instanceof WorldServer worldServer)) return false;
        lime.invokable(new BlockDirtyInvokable(worldServer, position, 1));
        lime.invokable(new BlockDirtyInvokable(worldServer, position, 20));
        return true;
    }
    public static boolean markDirtyBlock(Position position) {
        return markDirtyBlock(((CraftWorld)position.world).getHandle(), new BlockPosition(position.x, position.y, position.z));
    }

    private final ConcurrentHashMap<UUID, BlockDisplay.IBlock> cacheDisplay = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Object> animationData = new ConcurrentHashMap<>();
    @Override public void onFirstTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        CacheBlockDisplay.trySetCacheBlock(metadata.skull, CacheBlockDisplay.ICacheInfo.of(cacheDisplay));
        markDirtyBlock(event.getWorld(), event.getPos());
        lime.once(() -> markDirtyBlock(event.getWorld(), event.getPos()), 0.5);
    }
    private static final int TOTAL_UPDATE_TICKS = 20 * 2 * 60;
    private int update_ticks = -1;
    private final system.LockToast1<IBlockData> last_state = system.toast(Blocks.AIR.defaultBlockState()).lock();

    public static class TickTimeInfo {
        public int count = 1;

        public int calls = 0;
        public long preinit_ns = 0;
        public long remove_init_ns = 0;
        public long cache_init_ns = 0;
        public long foreach_ns = 0;
        public long remove_ns = 0;
        public long display_ns = 0;
        public long apply_ns = 0;

        private Map<String, Long> nanoMap() {
            return system.map.<String, Long>of()
                    .add("preinit", -preinit_ns/count)
                    .add("remove_init", -remove_init_ns/count)
                    .add("cache_init", -cache_init_ns/count)
                    .add("foreach", -foreach_ns/count)
                    .add("remove", -remove_ns/count)
                    .add("display", -display_ns/count)
                    .add("apply_ns", -apply_ns/count)
                    .build();
        }

        public Component toComponent() {
            Map<String, Long> nanoMap = nanoMap();
            long total_ns = nanoMap.values().stream().mapToLong(v -> v).sum();
            List<Component> components = new ArrayList<>();
            components.add(Component.text("calls: " + (calls / count) + "*" + count));
            nanoMap.forEach((name, ns) -> components.add(Component.empty()
                    .append(Component.text("[" + name.charAt(0) + "")
                            .append(Component.text(":").color(NamedTextColor.WHITE))
                            .append(Component.text((ns * 100 / total_ns) + "%").color(NamedTextColor.AQUA))
                            .append(Component.text("]"))
                            .color(NamedTextColor.GREEN)
                            .hoverEvent(HoverEvent.showText(Component.text(String.join("\n",
                                    "Name: " + name,
                                    "Time: " + ns + " ns ("+TimeUnit.NANOSECONDS.toMillis(ns)+" ms)",
                                    "Percent: " + (ns * 100 / total_ns) + "%"
                            ))))
                    )));
            return Component.join(JoinConfiguration.separator(Component.text(" ")), components);
        }

        public void append(TickTimeInfo info) {
            this.count += info.count;
            this.calls += info.calls;
            this.preinit_ns += info.preinit_ns;
            this.remove_init_ns += info.remove_init_ns;
            this.cache_init_ns += info.cache_init_ns;
            this.foreach_ns += info.foreach_ns;
            this.remove_ns += info.remove_ns;
            this.display_ns += info.display_ns;
            this.apply_ns += info.apply_ns;
        }
    }

    @Override public void onAsyncTick(CustomTileMetadata metadata) {
        TickTimeInfo tickTimeInfo = org.lime.gp.block.Blocks.deltaTime.get0();
        tickTimeInfo.calls++;

        long time_ns = System.nanoTime();

        TileEntityLimeSkull skull = metadata.skull;
        BlockPosition block_position = skull.getBlockPos();
        Location block_location = metadata.location(0.5,0.5,0.5);
        World world = block_location.getWorld();
        net.minecraft.world.level.World handle_world = ((CraftWorld)world).getHandle();
        Vector pos = block_location.toVector();
        IBlockData state = last_state.get0();

        Map<String, String> variables = getAll();
        int angle = getRotation().map(v -> v.angle).orElse(0);

        ConcurrentHashMap<Player, Double> shows = new ConcurrentHashMap<>();
        ConcurrentHashMap<UUID, ItemFrameDisplayObject> frameMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<BlockModelDisplay.BlockModelKey, ModelDisplayObject> modelMap = new ConcurrentHashMap<>();

        time_ns -= System.nanoTime();
        tickTimeInfo.preinit_ns += time_ns;
        time_ns = System.nanoTime();

        HashSet<UUID> removeList = new HashSet<>(partials.keySet());

        time_ns -= System.nanoTime();
        tickTimeInfo.remove_init_ns += time_ns;
        time_ns = System.nanoTime();

        removeList.addAll(cacheDisplay.keySet());

        time_ns -= System.nanoTime();
        tickTimeInfo.cache_init_ns += time_ns;
        time_ns = System.nanoTime();

        EntityPosition.playerLocations.forEach((player, location) -> {
            UUID playerUUID = player.getUniqueId();
            removeList.remove(playerUUID);
            system.Toast1<Double> distance = system.toast(null);
            Optional.ofNullable(partials.compute(playerUUID, (k, v) -> world == location.getWorld()
                            ? getPartial(distance.val0 = location.toVector().distanceSquared(pos), variables).map(partial -> orSync(metadata, playerUUID, player, variables, partial, v)).orElse(v)
                            : orSync(metadata, playerUUID, player, variables, null, v))
                    )
                    .ifPresent(partial -> {
                        if (partial instanceof DisplayPartial.FramePartial frame && frame.show) frameMap.put(playerUUID, ItemFrameDisplayObject.of(pos.toLocation(world), frame.nms(variables), frame.rotation, frame.uuid));
                        if (partial instanceof DisplayPartial.ModelPartial model) model.model().ifPresent(_model -> modelMap.computeIfAbsent(
                                new BlockModelDisplay.BlockModelKey(metadata.key.uuid(), metadata.position(), _model.unique, unique()),
                                k -> ModelDisplayObject.of(pos.toLocation(world, angle, 0), _model, animationData)
                        ).viewers.add(playerUUID));
                    });
            if (distance.val0 == null) return;
            shows.put(player, distance.val0);
        });

        time_ns -= System.nanoTime();
        tickTimeInfo.foreach_ns += time_ns;
        time_ns = System.nanoTime();

        removeList.forEach(partials::remove);
        removeList.forEach(cacheDisplay::remove);

        time_ns -= System.nanoTime();
        tickTimeInfo.remove_ns += time_ns;
        time_ns = System.nanoTime();

        metadata.list(BlockDisplay.Displayable.class)
                .forEach(displayable -> shows.forEach((player, distanceSquared) -> displayable.onDisplayAsync(player, handle_world, block_position, state)
                        .filter(v -> distanceSquared <= v.distanceSquared())
                        .flatMap(BlockDisplay.IModelBlock::model)
                        .ifPresent(_model -> modelMap.computeIfAbsent(
                                new BlockModelDisplay.BlockModelKey(metadata.key.uuid(), metadata.position(), _model.unique, displayable.unique()),
                                k -> ModelDisplayObject.of(pos.toLocation(world, angle, 0), _model, animationData)
                        ).viewers.add(player.getUniqueId()))
                ));

        time_ns -= System.nanoTime();
        tickTimeInfo.display_ns += time_ns;
        time_ns = System.nanoTime();

        if (frameMap.size() == 0 && modelMap.size() == 0) TimeoutData.remove(unique(), DisplayMap.class);
        else TimeoutData.put(unique(), DisplayMap.class, new DisplayMap(frameMap, modelMap));

        time_ns -= System.nanoTime();
        tickTimeInfo.apply_ns += time_ns;
    }
    private final ProxyMap<String, String> proxyVariables = ProxyMap.of(this.variables);
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        update_ticks = (update_ticks + 1) % TOTAL_UPDATE_TICKS;
        if (update_ticks == 0) markDirtyBlock(event.getWorld(), event.getPos());
        last_state.set0(event.getState());
        component().animationTick(getAll(), this.proxyVariables, animationData);
        if (proxyVariables.checkDirty(true)) variableDirty();
    }
    public void reshow() {
        TimeoutData.remove(unique(), DisplayMap.class);
        TileEntityLimeSkull skull = metadata().skull;
        UUID block_uuid = metadata().key.uuid();
        BlockDisplay.MODEL_MANAGER.getDisplays()
                .values()
                .forEach(display -> {
                    if (block_uuid.equals(display.key.block_uuid()))
                        display.hideAll();
                });
        BlockDisplay.ITEM_FRAME_MANAGER.getDisplays()
                .values()
                .forEach(display -> {
                    if (block_uuid.equals(display.block_uuid))
                        display.hideAll();
                });
        markDirtyBlock(skull.getLevel(), skull.getBlockPos());
    }
    @Override public @Nullable VoxelShape onShape(CustomTileMetadata metadata, BlockSkullShapeInfo event) {
        Map<String, String> variables = new HashMap<>(getAll());
        variables.put("shape", "this");
        return getPartial(0, variables)
                .map(v -> v instanceof CustomTileMetadata.Shapeable shapeable ? shapeable : null)
                .map(shapeable -> shapeable.onShape(metadata, event))
                .orElse(null);
    }
    /*@Override public Optional<BlockDisplay.IBlock> onDisplayAsync(Player player, net.minecraft.world.level.World world, BlockPosition position, IBlockData data) {
        Map<String, String> variables = getAll();
        return getPartial(player.getLocation().toVector().distanceSquared(new Vector(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5)), variables)
                .map(v -> v instanceof DisplayPartial.BlockPartial blockPartial ? blockPartial : null)
                .flatMap(blockPartial -> blockPartial.getDynamicDisplay(player, world, position, data, variables));
    }*/
    @Override public void onRemove(CustomTileMetadata metadata, TileEntitySkullEventRemove event) {
        TimeoutData.remove(unique(), DisplayMap.class);
        cacheDisplay.clear();
    }
}



























