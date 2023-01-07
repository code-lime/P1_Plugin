package p1.blocks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Vector3f;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.monster.EntityZombie;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.*;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.core;
import org.lime.packetwrapper.WrapperPlayServerBlockChange;
import org.lime.packetwrapper.WrapperPlayServerMultiBlockChange;
import org.lime.packetwrapper.WrapperPlayServerTileEntityData;
import org.lime.system;
import p1.*;
import PopulateLootEvent;
import metadata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class LimeBlockData implements Listener {
    public static core.element create() {
        return core.element.create(LimeBlockData.class)
                .disable()
                .withInstance()
                .withInit(LimeBlockData::init)
                .withUninit(LimeBlockData::uninit)
                .<JsonObject>addConfig("blocks", v -> v.withInvoke(LimeBlockData::config).withDefault(new JsonObject()));
    }

    public static final ConcurrentHashMap<String, Info> blocks = new ConcurrentHashMap<>();

    public static class Info {
        public static Info NONE;
        private Info() {
            this.key = "STATIC.NONE";
            this.model = null;
            this.states = Collections.emptyMap();
            this.material = Material.AIR;
            this.display = DisplayType.ITEM_FRAME;
            this.rotation = RotationRange.ANGLE_90;
            this.values = Collections.emptyMap();
            this.meta = Collections.emptyMap();
        }

        public final String key;
        public final String model;
        public final Map<String, String> states;
        public final Material material;
        public enum DisplayType {
            ITEM_FRAME((location) -> {
                EntityItemFrame frame = new EntityItemFrame(
                        ((CraftWorld)location.getWorld()).getHandle(),
                        new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                        EnumDirection.byName(BlockFace.UP.name()));
                frame.setInvisible(true);
                frame.setInvulnerable(true);
                return frame;
            }, (entity, rotation, item) -> {
                entity.setItem(item, true, false);
                entity.setRotation(rotation.ordinal());
            }, (a0, a1) -> {}),
            ZOMBIE((location) -> {
                EntityZombie zombie = new EntityZombie(((CraftWorld)location.getWorld()).getHandle());
                zombie.moveTo(location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5, 0, 0);
                zombie.setBaby(true);
                zombie.setInvisible(true);
                zombie.setInvulnerable(true);
                return zombie;
            }, (entity, rotation, item) -> {
                entity.setItemSlot(EnumItemSlot.HEAD, item);
                entity.setYRot(rotation.angle);
            }, (entity, player) -> {
                PacketManager.sendPackets(player,
                        new PacketPlayOutEntityTeleport(entity),
                        new PacketPlayOutEntityHeadRotation(entity, (byte)entity.getYRot()),
                        new PacketPlayOutEntityEquipment(entity.getId(), Collections.singletonList(Pair.of(EnumItemSlot.HEAD, entity.getItemBySlot(EnumItemSlot.HEAD))))
                );
            }),
            ARMOR_STAND((location) -> {
                EntityArmorStand stand = new EntityArmorStand(
                        EntityTypes.ARMOR_STAND,
                        ((CraftWorld)location.getWorld()).getHandle()
                );
                stand.moveTo(location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5, 0, 0);
                stand.setHeadPose(new Vector3f(0, 0, 0));
                stand.setSmall(true);
                stand.setMarker(true);
                stand.setInvisible(true);
                stand.setNoBasePlate(true);
                stand.setInvulnerable(true);
                return stand;
            }, (entity, rotation, item) -> {
                entity.setItemSlot(EnumItemSlot.HEAD, item);
                entity.setYRot(rotation.angle);
            }, (entity, player) -> {
                PacketManager.sendPackets(player,
                        new PacketPlayOutEntityTeleport(entity),
                        new PacketPlayOutEntityHeadRotation(entity, (byte)entity.getYRot()),
                        new PacketPlayOutEntityEquipment(entity.getId(), Collections.singletonList(Pair.of(EnumItemSlot.HEAD, entity.getItemBySlot(EnumItemSlot.HEAD))))
                );
            });

            private final system.Func1<Location, Entity> create;
            private final system.Action3<Entity, Info.Rotation, net.minecraft.world.item.ItemStack> update;
            private final system.Action2<Entity, Player> sendData;

            <T extends Entity>DisplayType(system.Func1<Location, T> create, system.Action3<T, Info.Rotation, net.minecraft.world.item.ItemStack> update, system.Action2<T, Player> sendData) {
                this.create = create::Invoke;
                this.sendData = (a0, a1) -> sendData.invoke((T)a0, a1);
                this.update = (a0, a1, a2) -> update.invoke((T)a0, a1, a2);
            }

            public Entity create(Location location) { return this.create.invoke(location); }
            public void sendData(Entity entity, Player player) { this.sendData.invoke(entity, player); }
            public void update(Entity entity, Info.Rotation rotation, net.minecraft.world.item.ItemStack item) { this.update.invoke(entity, rotation, item); }
        }
        public enum RotationRange {
            NONE(360),
            ANGLE_45(45),
            ANGLE_90(90);

            public final ImmutableList<Rotation> rotations;
            public final int angle;

            RotationRange(int angle) {
                this.angle = angle;
                List<Rotation> rotations = new ArrayList<>();
                Rotation now = Rotation.ANGLE_0;
                while (!rotations.contains(now)) {
                    rotations.add(now);
                    now = now.next(angle);
                }
                this.rotations = ImmutableList.copyOf(rotations);
            }
            public Rotation of(double angle) {
                double _index = angle / this.angle;
                return rotations.get((int)(Math.round(_index) % rotations.size()));
            }
            private static double getMod(double x, double y, double z) { return Math.sqrt(x * x + y * y + z * z); }
            private static double getAngle(org.bukkit.util.Vector a, org.bukkit.util.Vector b) {
                double ab = a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
                double _a = getMod(a.getX(), a.getY(), a.getZ());
                double _b = getMod(b.getX(), b.getY(), b.getZ());
                return Math.acos(ab / (_a * _b));
            }
            public Rotation of(org.bukkit.util.Vector direction) {
                system.Toast2<Double, Rotation> min = null;
                for (Rotation rotation : rotations) {
                    double angle = getAngle(direction, rotation.direction);
                    if (min == null || min.val0 > angle)
                        min = new system.Toast2<>(angle, rotation);
                }
                return min.val1;
            }
        }
        public enum Rotation {
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

            Rotation() {
                this.angle = 45 * ordinal();
                this.direction = new Vector(0,0,1).rotateAroundY(Math.toRadians(180-angle));
            }
            public Rotation next(int angle) {
                return of(this.angle + angle);
            }
            public static Rotation of(double angle) {
                double _index = angle / 45;
                return values()[(int)(Math.round(_index) % 8)];
            }
        }

        public final DisplayType display;
        public final RotationRange rotation;
        public final Map<String, JsonPrimitive> values;
        public final Map<String, JsonElement> meta;
        private final Set<system.Toast3<Integer, Integer, Integer>> multi = new LinkedHashSet<>();
        public Info(String key, JsonObject json) {
            this.key = key;
            this.model = json.has("model") ? json.get("model").getAsString() : null;
            this.states = json.has("states")
                    ? system.map.<String, String>of().add(json.get("states").getAsJsonObject().entrySet(), Map.Entry::getKey, kv->kv.getValue().getAsString()).build()
                    : Collections.emptyMap();
            this.material = Material.valueOf(json.get("material").getAsString());
            this.display = json.has("display") ? DisplayType.valueOf(json.get("display").getAsString()) : DisplayType.ITEM_FRAME;
            this.rotation = json.has("rotation") ? RotationRange.valueOf(json.get("rotation").getAsString()) : RotationRange.ANGLE_90;
            this.values = json.has("values")
                    ? system.map.<String, JsonPrimitive>of().add(json.get("values").getAsJsonObject().entrySet(), Map.Entry::getKey, kv->kv.getValue().getAsJsonPrimitive()).build()
                    : Collections.emptyMap();
            this.meta = json.has("values")
                    ? system.map.<String, JsonElement>of().add(json.get("values").getAsJsonObject().entrySet(), Map.Entry::getKey, Map.Entry::getValue).build()
                    : Collections.emptyMap();
            if (json.has("multi")) json.get("multi").getAsJsonArray().forEach(item -> multi.add(system.getPosToast(item.getAsString())));
            multi.remove(system.toast(0, 0, 0));
        }
        public boolean isCan(Block block) {
            for (system.Toast3<Integer, Integer, Integer> p : multi) {
                if (!block.getRelative(p.val0, p.val1, p.val2).getType().isAir())
                    return false;
            }
            return true;
        }

        public Optional<ItemStack> getBlockModelItem() { return Optional.ofNullable(model).map(ItemManager::getItemCreator).map(ItemManager.IItemCreator::createItem); }


        /*public static abstract class IBlock<T extends JsonElement> extends CustomMeta.IBlockMeta<JsonObject> {
            private final List<Position> others = new ArrayList<>();

            public String getState() {
                return ItemManager.MenuBlockSetting.isOpen(getLoaded().getPosition()) ? "open" : "default";
            }

            public int block_item;
            public ItemManager.BlockSetting.Rotation block_rotation;
            public Optional<Material> getBlockMaterial() { return getSettingsCreator().map(v -> v.material); }
            public Optional<String> getBlockModel() { return getSettingsCreator().map(v -> v.states.getOrDefault(getState(), v.model)); }
            public Optional<ItemManager.ItemCreator> getCreator() { return creators.getOrDefault(block_item, null) instanceof ItemManager.ItemCreator creator ? Optional.of(creator) : Optional.empty(); }
            public Optional<ItemManager.BlockSetting> getSettingsCreator() { return getCreator().map(v -> v.getOrNull(ItemManager.BlockSetting.class)); }
            public Optional<ItemStack> getBlockModelItem() { return getBlockModel().map(v -> creatorIDs.getOrDefault(v, null)).map(ItemManager.IItemCreator::createItem); }
            public boolean hasBlockModel() { return getBlockModel().orElse(null) != null; }
            public boolean display() { return true; }
            public ItemManager.BlockSetting.DisplayType getDisplay() { return getSettingsCreator().map(v -> v.display).orElse(ItemManager.BlockSetting.DisplayType.ITEM_FRAME); }

            @Override public void read(JsonObject json) {
                block_item = json.get("item").getAsInt();
                block_rotation = json.has("rotation") ? ItemManager.BlockSetting.Rotation.valueOf(json.get("rotation").getAsString()) : ItemManager.BlockSetting.Rotation.ANGLE_0;
                if (json.has("others")) json.get("others").getAsJsonArray().forEach(item -> {
                    system.Toast3<Integer, Integer, Integer> pos = system.getPosToast(item.getAsString());
                    others.add(Position.of(getWorld(), pos.val0, pos.val1, pos.val2));
                });
                readBlock((T)json.get("block"));
            }
            public abstract void readBlock(T json);
            @Override public JsonObject write() {
                system.json.builder.object json = system.json.object()
                        .add("item", block_item)
                        .add("rotation", (block_rotation == null ? ItemManager.BlockSetting.Rotation.ANGLE_0 : block_rotation).name())
                        .add("block", writeBlock());
                if (others.size() > 0) json = json.addArray("others", v -> v.add(others, Position::toSave));
                return json.build();
            }
            public abstract T writeBlock();
            @Override public void destroy() { others.forEach(p -> p.getBlock().setType(Material.AIR)); }
            public abstract void load();
            public boolean filter() { return getBlockMaterial().map(v -> v == getLoaded().getBlock().getType()).orElse(false); }
            public void apply(ItemManager.BlockSetting setting) {
                ItemManager.ItemCreator creator = setting.creator();
                this.block_item = creator.getID();
                load();
            }

            public abstract ItemManager.BlockSetting.IBlock<?> owner();
        }
        public static class NoneBlock extends ItemManager.BlockSetting.IBlock<JsonObject> {
            @Override public void readBlock(JsonObject json) { }
            @Override public JsonObject writeBlock() { return new JsonObject(); }
            @Override public void create() { }
            @Override public void load() { }
            @Override public void destroy() {
                getCreator().ifPresent(v -> ItemManager.dropBlockItem(getLocation(), v.createItem(1)));
                super.destroy();
            }
            @Override public void populate(PopulateLootEvent e) { e.setCancelled(true); }
            @Override public ItemManager.BlockSetting.IBlock<?> owner() { return this; }
        }
        public static class OtherBlock extends ItemManager.BlockSetting.IBlock<JsonObject> {
            private Position owner = null;

            @Override public boolean display() { return false; }
            @Override public void readBlock(JsonObject json) {
                if (!json.has("owner")) return;
                JsonObject _owner = json.get("owner").getAsJsonObject();
                owner = Position.of(getLocation().getWorld(), _owner.get("x").getAsInt(), _owner.get("y").getAsInt(), _owner.get("z").getAsInt());
            }
            @Override public JsonObject writeBlock() {
                return system.json.object()
                        .addObject("owner", v -> v
                                .add("x", owner.x)
                                .add("y", owner.y)
                                .add("z", owner.z)
                        )
                        .build();
            }
            @Override public void create() { }
            @Override public void load() { }

            @Override public void destroy() {
                owner.getBlock().setType(Material.AIR);
                super.destroy();
            }

            @Override public void populate(PopulateLootEvent e) { e.setCancelled(true); }

            @Override public ItemManager.BlockSetting.IBlock<?> owner() {
                for (CustomMeta.IBlockMeta<?> meta : CustomMeta.LoadedBlock.getReadOnlyAll(owner)) {
                    if (meta instanceof ItemManager.BlockSetting.IBlock<?> block) {
                        return block.owner();
                    }
                }
                return null;
            }
        }*/
    }

    private static final BlockDisplayManager manager = new BlockDisplayManager();
    private static boolean inited = false;
    public static void config(JsonObject json) {
        Displays.uninitDisplay(manager);
        if (inited) uninit();

        HashMap<String, Info> blocks = new HashMap<>();
        Info.NONE = new Info();
        blocks.put(Info.NONE.key, Info.NONE);
        json.entrySet().forEach(kv -> {
            String key = kv.getKey();
            blocks.put(key, new Info(key, kv.getValue().getAsJsonObject()));
        });
        Info.NONE = blocks.get(Info.NONE.key);
        LimeBlockData.blocks.clear();
        LimeBlockData.blocks.putAll(blocks);
        Displays.initDisplay(manager);
        inited = true;
    }
    public static void init() {
        AnyEvent.AddEvent("setblock", AnyEvent.type.owner, builder -> builder.createParam(blocks::get, blocks::keySet), (player, info) -> {
            set(player.getLocation().getBlock(), info);
        });
        AnyEvent.AddEvent("tmp.check", AnyEvent.type.owner, builder -> builder
                .createParam(Integer::parseInt, "x")
                .createParam(Integer::parseInt, "y")
                .createParam(Integer::parseInt, "z"), (player, x, y, z) -> {
            Block block = player.getWorld().getBlockAt(x,y,z);
            lime.logOP(metadata.get(block, metadata.class, "lime.block").map(metadata::asString).orElse("EMPTY"));
        });
        lime.repeat(LimeBlockData::sync, 1);
        PacketManager.adapter()
                .add(PacketType.Play.Server.BLOCK_CHANGE, event -> {
                    WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event.getPacket());
                    WrappedBlockData data = packet.getBlockData();
                    if (data.getType() != Material.BEACON) return;
                    BlockData block = sync_blocks.getOrDefault(new Position(packet.getBukkitLocation(event.getPlayer().getWorld())), null);
                    if (block == null) return;
                    data.setType(Material.RED_SHULKER_BOX);
                    packet.setBlockData(data);
                })
                .add(PacketType.Play.Server.TILE_ENTITY_DATA, event -> {
                    WrapperPlayServerTileEntityData packet = new WrapperPlayServerTileEntityData(event.getPacket());
                    if (packet.getAction() != 3) return;
                    event.setCancelled(true);
                })
                .add(PacketType.Play.Server.MULTI_BLOCK_CHANGE, event -> {
                    WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event.getPacket());
                    Map<Short, WrappedBlockData> records = packet.getChangeData();
                    boolean edited = false;
                    World world = event.getPlayer().getWorld();
                    WrapperPlayServerMultiBlockChange.SectionPosition section = packet.getSection();
                    for (Map.Entry<Short, WrappedBlockData> kv : records.entrySet()) {
                        WrappedBlockData data = kv.getValue();
                        if (data.getType() != Material.BEACON) return;
                        Location location = section.getPacked(kv.getKey()).toLocation(world);
                        BlockData block = sync_blocks.getOrDefault(new Position(location), null);
                        if (block == null) return;
                        data.setType(Material.RED_SHULKER_BOX);
                        edited = true;
                    }
                    if (edited) packet.setChangeData(records);
                })
                .listen();
    }
    public static void uninit() {
        LimeBlockData.sync();
        sync_blocks.clear();
        Bukkit.getWorlds().forEach(world -> metadata.removeAll(((CraftWorld)world).getBlockMetadata(), BlockData.class));
    }
    public static final class BlockData extends metadata.base {
        public final Position position;
        public final UUID uuid;
        public final Info info;
        public final ConcurrentLinkedQueue<IMeta<?>> meta = new ConcurrentLinkedQueue<>();
        public final HashMap<String, JsonElement> metaDefault = new HashMap<>();

        public UUID getUniqueId() { return uuid; }
        public World getWorld() { return position.world; }
        public Location getLocation() { return position.getLocation(); }
        public Location getLocation(double x, double y, double z) { return position.getLocation(x, y, z); }
        public Location getCenterLocation(double x, double y, double z) { return getLocation(x + 0.5, y + 0.5,z + 0.5); }
        public Location getCenterLocation() { return getCenterLocation(0, 0, 0); }

        private BlockData(Position position, Info info, UUID uuid, PersistentDataContainer container) {
            this.position = position;
            this.uuid = uuid;
            this.info = info;
            JsonObject json = JManager.get(JsonObject.class, container, LIME_META_KEY, new JsonObject());
            //json.entrySet().forEach(kv -> IMeta.parse(kv.getKey()).map(value -> value.apply(this, kv.getValue())).ifPresent(this.meta::add));
            //info.meta.forEach((key, meta) -> IMeta.parse(key).map(value -> value.apply(this, meta)).ifPresent(this.meta::add));
            this.meta.forEach(IMeta::init);
        }
        /*public static BlockData tryCreate(Position position, Beacon beacon) {
            PersistentDataContainer container = beacon.getPersistentDataContainer();
            JsonPrimitive primitive = JManager.FromContainer(JsonPrimitive.class, container, "lime.block", null);
            if (primitive == null) return null;
            return new BlockData(position, UUID.fromString(primitive.getAsString()));
        }
        public static BlockData tryCreate(Position position) {
            return position.getBlock().getState() instanceof Beacon beacon ? tryCreate(position, beacon) : null;
        }*/

        /*private static final ConcurrentHashMap<String, Optional<Class<IMeta<?>>>> classes = new ConcurrentHashMap<>();
        private static Optional<IMeta<?>> instanceClass(String class_name) {
            return classes
                    .computeIfAbsent(class_name, (k) -> { try { return Optional.of((Class<IMeta<?>>)Class.forName(k)); } catch (Exception e) { return Optional.empty(); } })
                    .map(v -> { try { return v.newInstance(); } catch (Exception e) { return null; } });
        }

        /*public void setMeta(IMeta<?> meta) {
            PersistentDataContainer container = beacon.getPersistentDataContainer();
            JsonObject json = JManager.FromContainer(JsonObject.class, container, "meta", new JsonObject());
            json.add(meta.getClass().getName(), meta.write());
            JManager.ToContainer(container, "meta", json);
            beacon.update();
        }
        public void delMeta(IMeta<?> meta) {
            PersistentDataContainer container = beacon.getPersistentDataContainer();
            JsonObject json = JManager.FromContainer(JsonObject.class, container, "meta", new JsonObject());
            json.remove(meta.getClass().getName());
            JManager.ToContainer(container, "meta", json);
            beacon.update();
        }
        public List<IMeta<?>> getAllMeta() {
            JsonObject json = JManager.FromContainer(JsonObject.class, beacon.getPersistentDataContainer(), "meta", new JsonObject());
            List<IMeta<?>> list = new ArrayList<>();
            json.entrySet().forEach(kv -> instanceClass(kv.getKey()).map(v -> {
                v.readAny(kv.getValue());
                return v;
            }).ifPresent(list::add));
            return list;
        }*/
        /*public void setMeta(IMeta<?> meta) {
            List<MetadataValue> values = beacon.getMetadata("lime.meta");
            PersistentDataContainer container = beacon.getPersistentDataContainer();
            JsonObject json = JManager.FromContainer(JsonObject.class, container, "meta", new JsonObject());
            json.add(meta.getClass().getName(), meta.write());
            JManager.ToContainer(container, "meta", json);
            beacon.update();
        }
        public void delMeta(IMeta<?> meta) {
            PersistentDataContainer container = beacon.getPersistentDataContainer();
            JsonObject json = JManager.FromContainer(JsonObject.class, container, "meta", new JsonObject());
            json.remove(meta.getClass().getName());
            JManager.ToContainer(container, "meta", json);
            beacon.update();
        }
        public List<IMeta<?>> getAllMeta() {
            JsonObject json = JManager.FromContainer(JsonObject.class, beacon.getPersistentDataContainer(), "meta", new JsonObject());
            List<IMeta<?>> list = new ArrayList<>();
            json.entrySet().forEach(kv -> instanceClass(kv.getKey()).map(v -> {
                v.readAny(kv.getValue());
                return v;
            }).ifPresent(list::add));
            return list;
        }*/

        /*private static final ConcurrentHashMap<String, Optional<Class<IMeta<?>>>> classes = new ConcurrentHashMap<>();
        private static Optional<IMeta<?>> instanceClass(String class_name) {
            return classes
                    .computeIfAbsent(class_name, (k) -> { try { return Optional.of((Class<IMeta<?>>)Class.forName(k)); } catch (Exception e) { return Optional.empty(); } })
                    .map(v -> { try { return v.newInstance(); } catch (Exception e) { return null; } });
        }*/

        public void save(PersistentDataContainer container) {
            JsonObject json = JManager.get(JsonObject.class, container, LIME_META_KEY, new JsonObject());
            meta.forEach(value -> json.add(value.name(), value.write()));
            JManager.set(container, LIME_META_KEY, json);
        }
    }

    @Retention(RetentionPolicy.RUNTIME) public @interface Meta { String name(); }
    public static abstract class IMeta<T extends JsonElement> {
        private String _name;
        public String name() { return _name; }

        private static final Map<String, system.Func0<IMeta<?>>> metas;
        private static final Map<Class<?>, String> metaKeys;
        private static Optional<Constructor<?>> constructor(Class<?> tClass, Class<?>... args) {
            try { return Optional.of(org.lime.reflection.access(tClass.getDeclaredConstructor(args))); }
            catch (Exception e) { return Optional.empty(); }
        }
        static {
            try {
                metas = new HashMap<>();
                metaKeys = new HashMap<>();
                Stream.of(LimeBlockData.class.getDeclaredClasses())
                        .filter(LimeBlockData.IMeta.class::isAssignableFrom)
                        .map(v -> constructor(v)
                                .map(c -> system.toast(v.getAnnotation(ItemManager.Setting.class), c))
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .filter(kv -> kv.val0 != null)
                        .forEach(kv -> {
                            Constructor<?> constructor = kv.val1;
                            String name = kv.val0.name();
                            metas.put(name, () -> {
                                try {
                                    return (IMeta<?>)constructor.newInstance();
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
                            metaKeys.put(constructor.getDeclaringClass(), name);
                        });
            } catch (Exception e) {
                lime.logStackTrace(e);
                throw e;
            }
            metas.keySet().forEach(k -> lime.logOP("Meta: " + k));
        }
        public static Optional<IMeta<?>> parse(String key, BlockData data) {
            return Optional.ofNullable(metas.getOrDefault(key, null)).map(v -> {
                IMeta<?> meta = v.invoke();
                meta._name = key;
                meta.data = data;
                return meta;
            });
        }
        public static <T extends IMeta<?>>Optional<T> parse(Class<T> tClass, BlockData data) {
            String key = metaKeys.getOrDefault(tClass, null);
            return parse(key, data).map(v -> (T)v);
        }

        private BlockData data;
        private final system.Func1<JsonElement, T> convert;

        public IMeta(system.Func1<JsonElement, T> convert) { this.convert = convert; }

        //public T getDefault() { return getBlockData().metaDefault; }
        public BlockData getBlockData() { return data; }
        public T convert(JsonElement json) { return convert.invoke(json); }
        public World getWorld() { return getBlockData().getWorld(); }
        public Location getLocation() { return getBlockData().getLocation(); }
        public Location getLocation(double x, double y, double z) { return getBlockData().getLocation(x,y,z); }
        public Location getCenterLocation(double x, double y, double z) { return getBlockData().getCenterLocation(x,y,z); }
        public Location getCenterLocation() { return getBlockData().getCenterLocation(); }

        public String getKey() { return getBlockData().getUniqueId().toString(); }
        public String getKey(String prefix) { return prefix + ":" + getKey(); }

        public abstract void init();
        public abstract void destroy();
        public abstract void read(T json);
        public abstract T write();
        public void populate(PopulateLootEvent e) { }

        private IMeta<T> apply(BlockData data, JsonElement json) {
            this.data = data;
            read(convert(json));
            return this;
        }

        public static void showParticle(Location location, Particle particle) { location.getWorld().spawnParticle(particle, location, 0, 0, 0, 0); }
        public static void showParticle(Location location) { showParticle(location, Particle.FLAME); }

        public static void showParticle(Player player, Vector position, Particle particle) { player.spawnParticle(particle, position.getX(), position.getY(), position.getZ(), 0, 0, 0, 0); }
        public static void showParticle(Player player, Vector position) { showParticle(player, position, Particle.FLAME); }
    }
    public static abstract class IMetaObject extends IMeta<JsonObject> { public IMetaObject() { super(JsonElement::getAsJsonObject); }}

    @Meta(name = "display") public static class DisplayMeta extends IMetaObject {
        private Info.Rotation rotation = Info.Rotation.ANGLE_0;
        private Optional<ItemStack> model = Optional.empty();

        @Override public void init() {
            model = getBlockData().info.getBlockModelItem();
        }
        @Override public void destroy() { }
        @Override public void read(JsonObject json) {
            rotation = system.json.getter(json)
                    .of("rotation")
                    .other(v -> Info.Rotation.valueOf(v.getAsString()), Info.Rotation.ANGLE_0);
        }
        @Override public JsonObject write() {
            return system.json.object()
                    .add("rotation", rotation.name())
                    .build();
        }

        public Optional<ItemStack> model() { return model; }
        public Info.Rotation rotation() { return rotation; }
        public Info.DisplayType display() { return getBlockData().info.display; }
    }
    @Meta(name = "other") public static class OtherMeta extends IMetaObject {
        private final List<Position> others = new ArrayList<>();

        @Override public void init() { }
        @Override public void destroy() { others.forEach(p -> p.getBlock().setType(Material.AIR)); }
        @Override public void read(JsonObject json) {
            system.json.getter(json).of("others").otherFunc(JsonElement::getAsJsonArray, JsonArray::new).getAsJsonArray().forEach(item -> {
                system.Toast3<Integer, Integer, Integer> pos = system.getPosToast(item.getAsString());
                others.add(Position.of(getWorld(), pos.val0, pos.val1, pos.val2));
            });
        }
        @Override public JsonObject write() {
            return system.json.object().addArray("others", v -> v.add(others, Position::toSave)).build();
        }
    }
    @Meta(name = "other.marker") public static class OtherMarkerMeta extends IMetaObject {
        private Position owner = null;

        @Override public void init() { }
        @Override public void destroy() { owner.getBlock().setType(Material.AIR); }
        @Override public void read(JsonObject json) {
            if (!json.has("owner")) return;
            JsonObject _owner = json.get("owner").getAsJsonObject();
            owner = Position.of(getLocation().getWorld(), _owner.get("x").getAsInt(), _owner.get("y").getAsInt(), _owner.get("z").getAsInt());
        }
        @Override public JsonObject write() {
            return system.json.object()
                    .addObject("owner", v -> v
                            .add("x", owner.x)
                            .add("y", owner.y)
                            .add("z", owner.z)
                    )
                    .build();
        }
    }

    @EventHandler public static void on(PopulateLootEvent e) {
        Vec3D pos = e.getOrDefault(PopulateLootEvent.Parameters.Origin, null);
        if (pos == null || !e.has(PopulateLootEvent.Parameters.BlockState)) return;
        of(Position.of(e.getCraftWorld(), new Vector(pos.x, pos.y, pos.z)).getBlock())
                .ifPresent(data -> data.meta.forEach(meta -> meta.populate(e)));
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST) public static void on(PlayerInteractEvent e) {
        Block _block = e.getClickedBlock();
        if (_block == null) return;
        for (CustomMeta.IBlockMeta<?> meta : CustomMeta.LoadedBlock.getReadOnlyAll(_block)) {
            if (meta instanceof ItemManager.BlockSetting.IBlock<?> block) {
                ItemManager.MenuBlockSetting setting = block.getCreator().map(v -> v.getOrNull(ItemManager.MenuBlockSetting.class)).orElse(null);
                if (setting == null) continue;
                if (setting.tryOpen(block.owner(), e)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    private static final String LIME_META_KEY = "lime.meta";
    private static final String LIME_DATA_KEY = "lime.data";
    private static final NamespacedKey LIME_BLOCK_INFO = new NamespacedKey(lime._plugin, "lime.block.info");
    private static final NamespacedKey LIME_BLOCK_KEY = new NamespacedKey(lime._plugin, "lime.block");
    public static Optional<BlockData> of(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof Beacon beacon)) return Optional.empty();
        PersistentDataContainer container = beacon.getPersistentDataContainer();

        String key = container.getOrDefault(LIME_BLOCK_KEY, PersistentDataType.STRING, "");
        if (key.isEmpty()) return Optional.empty();
        String _info = container.getOrDefault(LIME_BLOCK_INFO, PersistentDataType.STRING, "");
        if (_info.isEmpty()) return Optional.empty();
        Info info = blocks.getOrDefault(_info, null);

        UUID uuid = UUID.fromString(key);
        Optional<BlockData> data = metadata.get(block, BlockData.class, LIME_DATA_KEY);
        switch (data.map(v -> uuid.equals(v.uuid) ? 0 : 1).orElse(2)) {
            case 0: return data;
            case 1: metadata.remove(block, LIME_DATA_KEY);
            default: return Optional.of(metadata.set(block, LIME_DATA_KEY, new BlockData(new Position(block), info, uuid, container)));
        }
    }
    public static BlockData set(Block block, Info info) {
        return set(block, info, new Vector(1, 0, 0));
    }
    public static BlockData set(Block block, Info info, Vector direction) {
        block.setType(Material.BEACON);
        Beacon beacon = (Beacon)block.getState();
        PersistentDataContainer container = beacon.getPersistentDataContainer();
        UUID uuid = UUID.randomUUID();
        container.set(LIME_BLOCK_KEY, PersistentDataType.STRING, uuid.toString());
        container.set(LIME_BLOCK_INFO, PersistentDataType.STRING, info.key);
        beacon.update();
        BlockData data = of(block).get();
        List<Position> others = new ArrayList<>();
        Info.Rotation rotation = info.rotation.of(direction);
        Position pos = Position.of(block);
        info.multi.forEach(p -> {
            Position other_pos = Position.of(block).add(p.val0, p.val1, p.val2);
            others.add(other_pos);
            Block other_block = other_pos.getBlock();
            other_block.setType(Material.BEACON);
            Beacon other_beacon = (Beacon)other_block.getState();

            PersistentDataContainer other_container = other_beacon.getPersistentDataContainer();

            other_container.set(LIME_BLOCK_KEY, PersistentDataType.STRING, uuid.toString());
            other_container.set(LIME_BLOCK_INFO, PersistentDataType.STRING, Info.NONE.key);
            other_beacon.update();
            BlockData other_data = of(other_block).get();
            OtherMarkerMeta marker = IMeta.parse(OtherMarkerMeta.class, other_data).get();
            marker.owner = pos;
            other_data.meta.add(marker);
            other_data.save(other_container);
        });
        if (others.size() > 0) {
            OtherMeta meta = IMeta.parse(OtherMeta.class, data).get();
            meta.others.addAll(others);
            data.meta.add(meta);
        }
        DisplayMeta display = IMeta.parse(DisplayMeta.class, data).get();
        display.rotation = rotation;
        data.meta.add(display);
        data.save(container);
        return data;
    }

    private static final ConcurrentHashMap<Position, BlockData> sync_blocks = new ConcurrentHashMap<>();
    public static void sync() {
        List<Position> positions = new ArrayList<>();
        List<Long> chunks = new ArrayList<>();
        Bukkit.getWorlds().forEach(world -> {
            for (Chunk chunk : world.getLoadedChunks()) {
                chunks.add(chunk.getChunkKey());
                for (BlockState tile : chunk.getTileEntities())
                    if (tile instanceof Beacon beacon) {
                        Block block = tile.getBlock();
                        Position pos = new Position(tile.getLocation());
                        of(block).ifPresent(data -> {
                            sync_blocks.put(pos, data);
                            data.save(beacon.getPersistentDataContainer());
                            positions.add(pos);
                            tile.update();
                        });
                    }
            }
        });
        LimeBlockData.sync_blocks.entrySet().removeIf(kv -> {
            Position position = kv.getKey();
            boolean isExist = positions.contains(position);
            if (isExist) return false;
            long chunk = Chunk.getChunkKey(position.getLocation());
            if (chunks.contains(chunk)) kv.getValue().meta.forEach(IMeta::destroy);
            return true;
        });
    }

    private static class BlockDisplay extends Displays.ObjectDisplay<DisplayMeta, Entity> {
        private DisplayMeta meta;
        private ItemStack item;
        private final Info.DisplayType display;

        private Info.Rotation block_rotation;

        private BlockDisplay(DisplayMeta meta) {
            super(meta.getCenterLocation());
            this.meta = meta;
            this.display = meta.display();
            this.block_rotation = meta.rotation();
            postInit();
        }

        @Override protected Entity createEntity(Location location) { return display.create(location); }
        @Override protected void sendData(Player player, boolean child) {
            display.sendData(entity, player);
            super.sendData(player, child);
        }
        @Override public void update(DisplayMeta meta, double delta) {
            this.meta = meta;
            ItemStack item = this.meta.model().orElse(null);
            if (item == null) {
                hideAll();
                return;
            }
            boolean similar = item.isSimilar(this.item);
            if (!similar || block_rotation != meta.rotation()) {
                block_rotation = meta.rotation();
                display.update(entity, meta.rotation(), CraftItemStack.asNMSCopy(this.item = item));
            }
            super.update(meta, delta);
            if (!similar) invokeAll(this::sendDataWatcher);
        }
    }
    private static class BlockDisplayManager extends Displays.DisplayManager<UUID, DisplayMeta, BlockDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }

        @Override public Map<UUID, DisplayMeta> getData() {
            Map<UUID, DisplayMeta> displays = new HashMap<>();
            sync_blocks.forEach((pos, data) -> data.meta.forEach(meta -> {
                if (meta instanceof DisplayMeta display)
                    displays.put(data.uuid, display);
            }));
            return displays;
        }
        @Override public BlockDisplay create(UUID uuid, DisplayMeta meta) { return new BlockDisplay(meta); }
    }


    /*@EventHandler public static void on(PopulateLootEvent e) {
        Vec3D pos = e.getOrDefault(PopulateLootEvent.Parameters.Origin, null);
        if (pos == null || !e.has(PopulateLootEvent.Parameters.BlockState)) return;
        BlockData data = getData(Position.of(e.getWorld(), new Vector(pos.getX(), pos.getY(), pos.getZ())));

    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void OnBlockPhysic(BlockPhysicsEvent e) {
        Block block = e.getBlock();
        if (CustomMeta.LoadedBlock.getReadOnly(block) != null) e.setCancelled(true);
        tryInit(block);
    }
    @EventHandler public static void on(BlockPlaceEvent e) {
        e.getBlock().getMetadata()
        if (CustomMeta.LoadedBlock.getReadOnly(e.getBlockPlaced()) == null) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerInteractEvent e) {
        Block _block = e.getClickedBlock();
        if (_block == null) return;
        for (CustomMeta.IBlockMeta<?> meta : CustomMeta.LoadedBlock.getReadOnlyAll(_block)) {
            if (meta instanceof ItemManager.BlockSetting.IBlock<?> block) {
                ItemManager.MenuBlockSetting setting = block.getCreator().map(v -> v.getOrNull(ItemManager.MenuBlockSetting.class)).orElse(null);
                if (setting == null) continue;
                if (setting.tryOpen(block.owner(), e)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }*/

    /*public interface IIMeta<T extends JsonElement> {
        BlockData getBlockData();
        T convert(JsonElement json);
        default World getWorld() { return getBlockData().getWorld(); }
        default Location getLocation() { return getBlockData().getLocation(); }
        default Location getLocation(double x, double y, double z) { return getBlockData().getLocation(x,y,z); }
        default Location getCenterLocation(double x, double y, double z) { return getBlockData().getCenterLocation(x,y,z); }
        default Location getCenterLocation() { return getBlockData().getCenterLocation(); }

        default String getKey() { return getBlockData().getUniqueId().toString(); }
        default String getKey(String prefix) { return prefix + ":" + getKey(); }
        default String getFullKey() { return this.getClass().getName() + "^" + getKey(); }

        void create();
        void destroy();
        void read(T json);
        T write();

        default void readAny(JsonElement json) { read(convert(json)); }

        static void showParticle(Location location, Particle particle) { location.getWorld().spawnParticle(particle, location, 0, 0, 0, 0); }
        static void showParticle(Location location) { showParticle(location, Particle.FLAME); }

        static void showParticle(Player player, Vector position, Particle particle) { player.spawnParticle(particle, position.getX(), position.getY(), position.getZ(), 0, 0, 0, 0); }
        static void showParticle(Player player, Vector position) { showParticle(player, position, Particle.FLAME); }
    }
    public static abstract class IMeta<T extends JsonElement> implements IIMeta<T> {
        private I loaded;
        private boolean removed = false;
        public I getLoaded() { return loaded; }

        public void init(I loaded, JsonElement json) {
            try {
                this.loaded = loaded;
                if (json == null || json.isJsonNull()) {
                    if (json != null && json.isJsonNull()) lime.logOP("NULL: " + getKey());
                    create();
                }
                else read((T)json);
            } catch (Exception e) {
                lime.logOP("ERROR META - " + system.getString(getLoaded().getLocation().toVector()) + ": "+getFullKey()+"\n" + e.getMessage());
                throw new IllegalArgumentException(e);
            }
        }
        public void remove() {
            removed = true;
            this.getLoaded().remove(this);
        }
    }
    public static abstract class IBlockMeta<T extends JsonElement> extends CustomMeta.IMeta<T, CustomMeta.LoadedBlock> {
        public void populate(PopulateLootEvent e) { }
    }*/
}



































