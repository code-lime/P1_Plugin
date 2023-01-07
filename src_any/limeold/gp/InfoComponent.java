package org.lime.gp.block.component;

import com.google.gson.*;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.block.entity.TileEntitySkullEventDestroy;
import net.minecraft.world.level.block.BlockSkullEventInteract;
import net.minecraft.world.level.block.BlockSkullEventShape;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.block.BlocksOld;
import org.lime.gp.lime;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.system;
import org.lime.gp.block.BlockMap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InfoComponent {
    public interface IShape {
        boolean asyncShape(TileEntitySkull state, BlockSkullEventShape e);
    }
    public interface IDestroy {
        void onDestroy(TileEntitySkull state, TileEntitySkullEventDestroy e);
    }
    public interface ILoot {
        List<ItemStack> populate(TileEntitySkull state, PopulateLootEvent e);
    }
    public interface ISetBlock {
        void onSet(TileEntitySkull state, BlockPlaceEvent e);
    }
    @Retention(RetentionPolicy.RUNTIME) public @interface Component { String name(); }
    public interface IReplace {
        final class Result {
            public Result setEmptyCompound(NBTTagCompound compound) {
                return this.compound == null ? new Result(compound, data) : this;
            }

            public Result combine(Result result) {
                if (result == null) return this;
                return result.setEmptyCompound(this.compound);
            }

            public record TileInfo(MinecraftKey minecraftkey, TileEntityTypes<?> tileTypeIndex) {
                public static Optional<TileInfo> of(TileEntityTypes<?> tile) {
                    return Optional.ofNullable(IRegistry.BLOCK_ENTITY_TYPE.getKey(tile)).map(v -> new TileInfo(v, tile));
                }

                public NBTTagCompound init(BlockPosition position, NBTTagCompound nbt) {
                    nbt = nbt.copy();
                    nbt.putString("id", minecraftkey.toString());
                    nbt.putInt("x", position.getX());
                    nbt.putInt("y", position.getY());
                    nbt.putInt("z", position.getZ());
                    return nbt;
                }
            }

            public static final HashMap<Material, TileInfo> material_to_tile = new HashMap<>();
            public static final HashMap<Integer, TileInfo> tile_to_ = new HashMap<>();
            public final NBTTagCompound compound;
            public final IBlockData data;

            public Result(NBTTagCompound compound, IBlockData data) {
                this.compound = compound;
                this.data = data;
            }
            public Result(IBlockData data) {
                this(null, data);
            }

            static {
                IRegistry.BLOCK_ENTITY_TYPE.forEach(tileCreator -> TileInfo.of(tileCreator)
                        .ifPresent(info -> tileCreator.validBlocks.forEach(block -> material_to_tile.put(block.defaultBlockState().getBukkitMaterial(), info))));
            }
            public Optional<PacketPlayOutTileEntityData> packet(BlockPosition position) {
                return Optional.ofNullable(compound)
                        .map(v -> data == null ? null : material_to_tile.getOrDefault(data.getBukkitMaterial(), null))
                        .map(v -> ReflectionAccess.init_PacketPlayOutTileEntityData.newInstance(position, v.tileTypeIndex, v.init(position, compound)));
            }
            public Optional<NBTTagCompound> nbt(BlockPosition position) {
                return Optional.ofNullable(compound)
                        .map(v -> data == null ? null : material_to_tile.getOrDefault(data.getBukkitMaterial(), null))
                        .map(v -> v.init(position, compound));
            }
        }

        final class Input {
            public final Player player;
            public final BlockPosition position;
            public final IBlockData data;

            public Optional<BlocksOld.Info> tryGetInfo() {
                return BlockMap.byPosition(new Position(player.getWorld(), position.getX(), position.getY(), position.getZ()));
            }

            public Input(Player player, BlockPosition position, IBlockData data) {
                this.player = player;
                this.position = position;
                this.data = data;
            }
            private static BlockPosition toPos(Location location) {
                return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            }
            public Input(Player player, Block block) {
                this(player, toPos(block.getLocation()), ((CraftBlock)block).getNMS());
            }

            public BlockPosition position() { return position; }
            public World world() { return player.getWorld(); }

            public Result toResult() { return new Result(null, data); }
            public Result toResult(NBTTagCompound compound) { return new Result(compound, data); }
            public Result toResult(IBlockData data) { return new Result(null, data); }
        }

        Result replace(Input input);

        static IReplace combine(Collection<? extends IReplace> replaces) {
            return (input) -> replace(input, replaces);
        }
        static Result replace(Input input, Collection<? extends IReplace> replaces) {
            Result result = input.toResult();
            for (IReplace replace : replaces) result = result.combine(replace.replace(new Input(input.player, input.position, result.data)));
            return result;
        }
    }
    public interface IComponent extends IReplace {
        BlocksOld.InfoCreator creator();
        String name();
        default boolean tick(TileEntitySkull skull) { return true; }
        default void interact(TileEntitySkull state, BlockSkullEventInteract event) { }
    }
    public static abstract class StaticInfoComponent<T extends JsonElement> implements IComponent {
        private final BlocksOld.InfoCreator _creator;
        public BlocksOld.InfoCreator creator() { return this._creator; }

        private String _name;
        public String name() { return _name; }
        @Override public IReplace.Result replace(IReplace.Input input) { return input.toResult(); }

        private static final Map<Class<?>, String> componentKeys;
        private static final Map<String, system.Func2<BlocksOld.InfoCreator, JsonElement, StaticInfoComponent<?>>> components;
        private static Optional<Constructor<?>> constructor(Class<?> tClass, Class<?>... args) {
            try { return Optional.of(org.lime.reflection.access(tClass.getDeclaredConstructor(args))); }
            catch (Exception e) { return Optional.empty(); }
        }
        static {
            try {
                componentKeys = new HashMap<>();
                components = new HashMap<>();
                Stream.of(Components.class.getDeclaredClasses())
                        .filter(StaticInfoComponent.class::isAssignableFrom)
                        .map(v -> constructor(v, BlocksOld.InfoCreator.class, JsonElement.class)
                                .or(() -> constructor(v, BlocksOld.InfoCreator.class, JsonArray.class))
                                .or(() -> constructor(v, BlocksOld.InfoCreator.class, JsonObject.class))
                                .or(() -> constructor(v, BlocksOld.InfoCreator.class, JsonPrimitive.class))
                                .or(() -> constructor(v, BlocksOld.InfoCreator.class, JsonNull.class))
                                .or(() -> constructor(v, BlocksOld.InfoCreator.class))
                                .map(c -> system.toast(v.getAnnotation(Component.class), c))
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .filter(kv -> kv.val0 != null)
                        .forEach(kv -> {
                            components.put(kv.val0.name(), (creator, json) -> {
                                try {
                                    return (StaticInfoComponent<?>)(kv.val1.getParameterCount() == 2 ? kv.val1.newInstance(creator, json) : kv.val1.newInstance(creator));
                                }
                                catch (InvocationTargetException e) {
                                    lime.logStackTrace(e.getCause());
                                    throw new IllegalArgumentException(e.getCause());
                                }
                                catch (Exception e) {
                                    lime.logStackTrace(e);
                                    throw new IllegalArgumentException(e);
                                }
                            });
                            componentKeys.put(kv.val1.getDeclaringClass(), kv.val0.name());
                        });
            } catch (Exception e) {
                lime.logStackTrace(e);
                throw e;
            }
            components.keySet().forEach(k -> lime.logOP("Component: " + k));
        }
        public StaticInfoComponent(BlocksOld.InfoCreator creator) {
            this._creator = creator;
            this._name = componentKeys.getOrDefault(this.getClass(), null);
        }
        public StaticInfoComponent(BlocksOld.InfoCreator creator, T json) { this(creator); }
        public static StaticInfoComponent<?> parse(String key, BlocksOld.InfoCreator creator, JsonElement json) {
            if (key.equals("other#generic")) return GenericDynamicComponent.other(creator, json.getAsJsonObject());
            return components.get(key).invoke(creator, json);
        }
    }
    public static abstract class DynamicInfoComponent<T extends JsonElement, I extends BlocksOld.InfoInstance> extends StaticInfoComponent<T> {
        public DynamicInfoComponent(BlocksOld.InfoCreator creator) { super(creator); }
        public DynamicInfoComponent(BlocksOld.InfoCreator creator, T json) { super(creator, json); }

        public abstract I createInstance(BlocksOld.Info info);
    }

    public static class Rotation {
        public enum Value {
            ANGLE_0,
            ANGLE_45,
            ANGLE_90,
            ANGLE_135,
            ANGLE_180,
            ANGLE_225,
            ANGLE_270,
            ANGLE_315;

            public final int angle;
            public final org.bukkit.util.Vector direction;

            Value() {
                this.angle = 45 * ordinal();
                this.direction = new org.bukkit.util.Vector(0,0,1).rotateAroundY(Math.toRadians(180-angle));
            }

            public static Value ofAngle(int angle) {
                return valueOf("ANGLE_" + angle);
            }

            public system.Toast3<Integer, Integer, Integer> rotate(system.Toast3<Integer, Integer, Integer> pos) {
                return switch (this) {
                    case ANGLE_0, ANGLE_45 -> system.toast(pos.val0, pos.val1, pos.val2);
                    case ANGLE_90, ANGLE_135 -> system.toast(-pos.val2, pos.val1, pos.val0);
                    case ANGLE_180, ANGLE_225 -> system.toast(-pos.val0, pos.val1, -pos.val2);
                    case ANGLE_270, ANGLE_315 -> system.toast(pos.val2, pos.val1, -pos.val0);
                };
            }
        }

        private static double getMod(double x, double y, double z) { return Math.sqrt(x * x + y * y + z * z); }
        private static double getAngle(org.bukkit.util.Vector a, org.bukkit.util.Vector b) {
            double ab = a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
            double _a = getMod(a.getX(), a.getY(), a.getZ());
            double _b = getMod(b.getX(), b.getY(), b.getZ());
            return Math.acos(ab / (_a * _b));
        }
        public static Value of(Vector direction, Collection<Value> rotations) {
            double min_angle = 0;
            Value min_value = null;
            for (Value rotation : rotations) {
                double angle = getAngle(direction, rotation.direction);
                if (min_value == null || min_angle > angle) {
                    min_angle = angle;
                    min_value = rotation;
                }
            }
            return min_value == null ? Value.ANGLE_0 : min_value;
        }
    }
    public static final class GenericDynamicComponent<T extends BlocksOld.InfoInstance> extends DynamicInfoComponent<JsonObject, T> {
        private final system.Func1<BlocksOld.Info, T> createInstance;
        private final String name;
        public GenericDynamicComponent(String name, BlocksOld.InfoCreator creator, system.Func1<BlocksOld.Info, T> createInstance) {
            super(creator);
            this.createInstance = createInstance;
            this.name = name;
        }
        @Override public T createInstance(BlocksOld.Info info) {
            return createInstance.invoke(info);
        }
        @Override public String name() { return getName(name); }

        public static final class Other extends BlocksOld.InfoInstance implements IDestroy, IShape, ILoot {
            public Material material;
            public IBlockData blockData;
            public BlockPosition position;
            public Rotation.Value rotation;
            public UUID owner;

            private final boolean replace;
            private final boolean force_interact;

            private Other(BlocksOld.Info info) {
                super(info);
                this.replace = true;
                this.force_interact = true;
            }

            public Other(BlocksOld.Info info, JsonObject args) {
                super(info);
                this.replace = !args.has("replace") || args.get("replace").getAsBoolean();
                this.force_interact = !args.has("force_interact") || args.get("force_interact").getAsBoolean();
            }

            private static BlockPosition parse(String text) {
                var pos = system.getPosToast(text);
                return new BlockPosition(pos.val0, pos.val1, pos.val2);
            }

            @Override public Result replace(Input input) {
                return replace ? input.toResult(blockData) : input.toResult();
            }
            @Override public JsonObject load(JsonObject json) {
                this.material = json.has("material") ? Material.valueOf(json.get("material").getAsString()) : Material.AIR;
                this.position = json.has("position") ? parse(json.get("position").getAsString()) : null;
                this.rotation = json.has("rotation") ? Rotation.Value.valueOf(json.get("rotation").getAsString()) : null;
                this.owner = json.has("owner") ? UUID.fromString(json.get("owner").getAsString()) : null;
                IBlockData blockData = CraftMagicNumbers
                        .getBlock(material)
                        .defaultBlockState();
                if (json.has("states")) {
                    HashMap<String, IBlockState<?>> states = system.map.<String, IBlockState<?>>of()
                            .add(blockData.getProperties(), IBlockState::getName, v -> v)
                            .build();
                    for (Map.Entry<String, JsonElement> kv : json.get("states").getAsJsonObject().entrySet()) {
                        IBlockState<?> state = states.getOrDefault(kv.getKey(), null);
                        if (state == null) continue;
                        blockData = BlocksOld.setValue(blockData, state, kv.getValue().getAsString());
                    }
                }
                this.blockData = blockData;
                return json;
            }
            @Override public void save() {
                setSaved(system.json.object()
                        .add("material", this.material)
                        .add("rotation", this.rotation.toString())
                        .add("position", this.position == null ? null : (position.getZ() +" " + position.getY() + " " + position.getZ()))
                        .add("owner", this.owner == null ? null : owner.toString())
                        .addObject("states", v -> v.add(this.blockData.getProperties(), IBlockState::getName, state -> BlocksOld.getValue(this.blockData, state)))
                        .build());
            }

            @Override public boolean tick(TileEntitySkull skull) {
                net.minecraft.world.level.World world = skull.getLevel();
                if (world == null || BlockMap.getForceInfo(world, position).filter(v -> v.uuid.equals(owner)).isPresent()) return true;
                BlockPosition self = skull.getBlockPos();
                world.getWorld().getBlockAt(self.getX(), self.getY(), self.getZ()).setType(Material.AIR);
                return true;
            }
            @Override public List<ItemStack> populate(TileEntitySkull state, PopulateLootEvent e) {
                if (position == null) return Collections.emptyList();
                return BlockMap.getForceInfo(e.getWorld(), position)
                        .filter(v -> !v.isLootGenerated)
                        .map(v -> {
                            v.isLootGenerated = true;
                            return v;
                        })
                        .map(v -> v.getAll(ILoot.class)
                                .flatMap(_v -> _v.populate(state, e).stream())
                                .collect(Collectors.toList())
                        )
                        .orElseGet(Collections::emptyList);
            }
            @Override public void interact(TileEntitySkull state, BlockSkullEventInteract event) {
                if (force_interact) {
                    super.interact(state, event);
                    return;
                }
                net.minecraft.world.level.World world = state.getLevel();
                if (world != null) BlockMap.getForceTileInfo(world, position).ifPresent(kv -> kv.val0.interact(kv.val1, event));
            }
            @Override public void onDestroy(TileEntitySkull state, TileEntitySkullEventDestroy e) {
                if (position == null) return;
                net.minecraft.world.level.World world = state.getLevel();
                if (world == null || BlockMap.getForceInfo(world, position).filter(v -> v.uuid.equals(owner)).isEmpty()) return;
                world.getWorld().getBlockAt(position.getX(), position.getY(), position.getZ()).setType(Material.AIR);
            }
            @Override public boolean asyncShape(TileEntitySkull state, BlockSkullEventShape e) {
                if (!replace) return false;
                e.setResult(blockData.getShape(e.getWorld(), e.getPos(), e.getContext()));
                return true;
            }
        }
        public static <T extends BlocksOld.InfoInstance>GenericDynamicComponent<T> of(String name, BlocksOld.InfoCreator creator, system.Func1<BlocksOld.Info, T> createInstance) {
            return new GenericDynamicComponent<>(name, creator, createInstance);
        }
        public static GenericDynamicComponent<Other> other(BlocksOld.InfoCreator creator) {
            return of("other", creator, Other::new);
        }
        public static GenericDynamicComponent<Other> other(BlocksOld.InfoCreator creator, JsonObject args) {
            return of("other", creator, info -> new Other(info, args));
        }
        public static String getName(String name) {
            return name + "#generic";
        }
    }
    public static class VarDynamicComponent extends BlocksOld.InfoInstance {
        public VarDynamicComponent(BlocksOld.Info info) {
            super(info);
        }
        @Override public IReplace.Result replace(IReplace.Input input) { return input.toResult(); }
        @Override public JsonObject load(JsonObject json) { return json; }
    }
}
