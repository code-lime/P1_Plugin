package org.lime.gp.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityAreaEffectCloud;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.player.EntityHuman;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.LangMessages;
import org.lime_old.gp.*;
import org.limeold.gp.*;
import org.lime.gp.module.NPC;
import org.lime.packetwrapper.WrapperPlayServerMount;
import org.lime.system;
import org.lime.web;
import org.lime.gp.chat.ChatHelper;
import org.limeold.gp.web.DataReader;
import su.plo.voice.common.packets.udp.PacketUDP;
import su.plo.voice.common.packets.udp.VoiceServerPacket;
import su.plo.voice.events.PlayerSpeakEvent;
import su.plo.voice.socket.SocketClientUDP;
import su.plo.voice.socket.SocketServerUDP;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class PlayerLogger implements Listener {
    public static core.element create() {
        return core.element.create(PlayerLogger.class)
                .withInstance()
                .withInit(PlayerLogger::init)
                .<JsonObject>addConfig("config", v -> v
                        .withParent("player_logger")
                        .withDefault(system.json.object()
                                .add("url", "http://217.8.127.18:16193/log")
                                .add("log_size", 10)
                                .add("token", "TOKEN")
                                .add("enable", true)
                                .build()
                        )
                        .withInvoke(PlayerLogger::config)
                )
                .addCommand("logger", v -> v
                        .withUsage("/logger load [date] [time] [minutes] [radius]")
                        .withUsage("/logger close")
                        .withCheck(_v -> _v instanceof Player && _v.isOp())
                        .withTab((s,args) -> switch (args.length) {
                            case 1 -> Arrays.asList("load","close");
                            case 2 -> args[0].equals("load") ? Collections.singletonList("[date]") : Collections.emptyList();
                            case 3 -> args[0].equals("load") ? Collections.singletonList("[time]") : Collections.emptyList();
                            case 4 -> args[0].equals("load") ? Collections.singletonList("[minutes]") : Collections.emptyList();
                            case 5 -> args[0].equals("load") ? Collections.singletonList("[radius]") : Collections.emptyList();
                            default -> Collections.emptyList();
                        })
                        .withExecutor((s,args) -> switch (args.length) {
                            case 1 -> s instanceof Player p ? of(() -> {
                                if (args[0].equals("close")) LogReader.close(p);
                            }, true) : false;
                            case 5 -> s instanceof Player p ? of(() -> {
                                if (args[0].equals("load")) {
                                    p.sendMessage("Loading logger...");
                                    LogReader.load(p, system.parseCalendar(args[1] + " " + args[2]), (int) (Double.parseDouble(args[3]) * 60), Double.parseDouble(args[4]), dat -> p.sendMessage("Logger loaded!"));
                                    return;
                                }
                            }, true) : false;
                            default -> false;
                        })
                );
    }
    private static <T>T of(system.Action0 call, T ret) {
        call.invoke();
        return ret;
    }
    private enum AnyType {
        VOICE("voice"),
        MOVE("move"),
        EQUIPMENT("equipment"),
        SKIN("skin");

        public final String key;
        public String getKey() {
            return key;
        }
        AnyType(String key) {
            this.key = key;
        }
    }
    private static abstract class ILog {
        public abstract AnyType getType();
        public abstract JsonObject toJson();
    }
    private static final HashMap<UUID, Integer> worlds = new HashMap<>();
    private static int getWorldIndex(World world) {
        UUID uuid = world.getUID();
        Integer index = worlds.getOrDefault(uuid, null);
        if (index == null) {
            index = Bukkit.getWorlds().indexOf(world);
            worlds.put(uuid, index);
        }
        return index;
    }
    private static World getWorldByIndex(int world_index) {
        List<World> worlds = Bukkit.getWorlds();
        return worlds.size() > world_index ? worlds.get(world_index) : null;
    }
    private static class VoiceLog extends ILog {
        public UUID uuid;
        public Vector pos;
        public int world;
        public short distance;
        public byte[] data;
        public long tick;
        public VoiceLog(PlayerSpeakEvent e) {
            Player player = e.getPlayer();
            uuid = player.getUniqueId();
            Location location = player.getLocation();
            pos = location.toVector();
            world = getWorldIndex(location.getWorld());
            distance = e.getDistance();
            data = e.getData();
            tick = System.currentTimeMillis();
        }
        @Override public AnyType getType() { return AnyType.VOICE; }
        @Override public JsonObject toJson() {
            return system.json.object()
                    .add("uuid", uuid.toString())
                    .add("x", system.round(pos.getX(), 3))
                    .add("y", system.round(pos.getY(), 3))
                    .add("z", system.round(pos.getZ(), 3))
                    .add("world", world)
                    .add("distance", distance)
                    .add("data", Base64.getEncoder().encodeToString(data))
                    .add("tick", tick)
                    .build();
        }
    }
    private static class MoveLog extends ILog {
        private static final HashMap<UUID, Location> lastLogs = new HashMap<>();
        private static boolean isEquals(Location location1, Location location2) {
            if (location1 == null) return location2 == null;
            if (location2 == null) return false;
            if (location1.getWorld() != location2.getWorld()) return false;
            if (location1.distanceSquared(location2) > 0.0001) return false;
            if (Math.abs(location1.getYaw() - location2.getYaw()) > 0.01) return false;
            return true;
        }
        public static void tick() {
            if (!ENABLE) return;
            List<UUID> called = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> {
                UUID uuid = player.getUniqueId();
                called.add(uuid);
                Location pos = player.getLocation();
                Location last = lastLogs.getOrDefault(uuid, null);
                if (isEquals(pos, last)) return;
                log(new MoveLog(player, pos));
                lastLogs.put(uuid, pos);
            });
            lastLogs.entrySet().removeIf(v -> !called.contains(v.getKey()));
        }
        public static void resetCacheTick() {
            lastLogs.clear();
        }
        public UUID uuid;
        public Vector pos;
        public float yaw;
        public float pitch;
        public int world;
        public Pose pose;
        public long tick;
        public MoveLog(PlayerMoveEvent e) { this(e.getPlayer(), e.getTo()); }
        private MoveLog(Player player, Location location) { this(player, location, player.getPose()); }
        public MoveLog(Player player, Location location, Pose pose) {
            uuid = player.getUniqueId();
            pos = location.toVector();
            yaw = location.getYaw();
            pitch = location.getPitch();
            world = getWorldIndex(location.getWorld());
            this.pose = pose;
            tick = System.currentTimeMillis();
        }
        @Override public AnyType getType() { return AnyType.MOVE; }
        @Override public JsonObject toJson() {
            return system.json.object()
                    .add("uuid", uuid.toString())
                    .add("x", system.round(pos.getX(), 3))
                    .add("y", system.round(pos.getY(), 3))
                    .add("z", system.round(pos.getZ(), 3))
                    .add("yaw", system.round(yaw, 3))
                    .add("pitch", system.round(pitch, 3))
                    .add("world", world)
                    .add("pose", pose.name())
                    .add("tick", tick)
                    .build();
        }
    }
    private static class EquipmentLog extends ILog {
        private static final HashMap<UUID, EquipmentLog> lastLogs = new HashMap<>();
        public static void tick() {
            if (!ENABLE) return;
            List<UUID> called = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> {
                UUID uuid = player.getUniqueId();
                called.add(uuid);
                EquipmentLog log = new EquipmentLog(player);
                EquipmentLog last = lastLogs.getOrDefault(uuid, null);
                if (log.isEquals(last)) return;
                log(log);
                lastLogs.put(uuid, log);
            });
            lastLogs.entrySet().removeIf(v -> !called.contains(v.getKey()));
        }
        public static void resetCacheTick() {
            lastLogs.clear();
        }
        public UUID uuid;
        public HashMap<EquipmentSlot, ItemStack> items;
        public long tick;
        private static ItemStack getClone(ItemStack item) {
            return item == null || item.getType().isAir() ? null : item.clone();
        }
        private EquipmentLog(Player player) {
            uuid = player.getUniqueId();
            items = new HashMap<>();
            EntityEquipment equipment = player.getEquipment();
            for (EquipmentSlot slot : EquipmentSlot.values()) items.put(slot, getClone(equipment.getItem(slot)));
            tick = System.currentTimeMillis();
        }
        private static boolean isEquals(ItemStack item1, ItemStack item2) {
            if (item1 == null) return item2 == null;
            if (item2 == null) return false;
            return item1.getAmount() == item2.getAmount() && item1.isSimilar(item2);
        }
        private boolean isEquals(EquipmentLog last) {
            if (last == null) return false;
            HashMap<EquipmentSlot, ItemStack> last_items = last.items;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!isEquals(last_items.get(slot), items.get(slot)))
                    return false;
            }
            return true;
        }
        @Override public AnyType getType() { return AnyType.EQUIPMENT; }
        @Override public JsonObject toJson() {
            return system.json.object()
                    .add("uuid", uuid.toString())
                    .add(items, v -> v.name().toLowerCase(), system::saveItem)
                    .add("tick", tick)
                    .build();
        }
    }
    private static class SkinLog extends ILog {
        private static final HashMap<UUID, SkinLog> lastLogs = new HashMap<>();
        public static void tick() {
            if (!ENABLE) return;
            List<UUID> called = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> {
                UUID uuid = player.getUniqueId();
                called.add(uuid);
                SkinLog log = new SkinLog(player);
                SkinLog last = lastLogs.getOrDefault(uuid, null);
                if (log.isEquals(last)) return;
                log(log);
                lastLogs.put(uuid, log);
            });
            lastLogs.entrySet().removeIf(v -> !called.contains(v.getKey()));
        }
        public static void resetCacheTick() {
            lastLogs.clear();
        }
        public UUID uuid;
        public String value;
        public String signature;
        public long tick;
        private SkinLog(Player player) {
            uuid = player.getUniqueId();
            system.Toast2<String, String> map = Skins.getSkinData(player);
            value = map == null ? null : map.val0;
            signature = map == null ? null : map.val1;
            tick = System.currentTimeMillis();
        }
        private static boolean isEquals(String item1, String item2) {
            if (item1 == null) return item2 == null;
            if (item2 == null) return false;
            return item1.equals(item2);
        }
        private boolean isEquals(SkinLog last) {
            if (last == null) return false;
            return isEquals(last.value, value) && isEquals(last.signature, signature);
        }
        @Override public AnyType getType() { return AnyType.SKIN; }
        @Override public JsonObject toJson() {
            return system.json.object()
                    .add("uuid", uuid.toString())
                    .add("value", value)
                    .add("signature", signature)
                    .add("tick", tick)
                    .build();
        }
    }

    private static final ConcurrentLinkedQueue<ILog> logs = new ConcurrentLinkedQueue<>();
    private static void log(ILog log) {
        if (!ENABLE) return;
        logs.add(log);
        if (logs.size() <= LOG_SIZE) return;
        PlayerLogger.syncLogs();
    }
    private static void syncLogs() {
        if (!ENABLE) return;
        lime.invokeAsync(() -> {
            List<ILog> logs = new ArrayList<>();
            PlayerLogger.logs.removeIf(logs::add);
            if (logs.size() == 0) return;
            HashMap<AnyType, List<ILog>> log_map = new HashMap<>();
            logs.forEach(log -> {
                AnyType type = log.getType();
                List<ILog> _logs = log_map.getOrDefault(type, null);
                if (_logs == null) _logs = new ArrayList<>();
                _logs.add(log);
                log_map.put(type, _logs);
            });
            web.method.POST.create(URL + "/write?token=" + TOKEN, system.json
                    .object()
                    .add(log_map, AnyType::getKey, v -> system.json.array().add(v, ILog::toJson))
                    .build()
                    .toString()
            ).json().executeAsync((result, code) -> {
                if (DEBUG) lime.logOP("[" + code + "] Result: " + result);
            });
        }, () -> {});
    }
    public static int LOG_SIZE;
    public static String URL;
    public static String TOKEN;
    public static boolean ENABLE;
    public static boolean DEBUG = false;
    public static final LogManager MANAGER = new LogManager();
    public static void config(JsonObject json) {
        LOG_SIZE = json.get("log_size").getAsInt();
        URL = json.get("url").getAsString();
        TOKEN = json.get("token").getAsString();
        ENABLE = json.get("enable").getAsBoolean();
        Displays.uninitDisplay(MANAGER);
        if (ENABLE) Displays.initDisplay(MANAGER);
    }
    public static void init() {
        AnyEvent.addEvent("player_logger.debug", AnyEvent.type.owner_console, p -> DEBUG = !DEBUG);
        AnyEvent.addEvent("player_logger.index", AnyEvent.type.none, v -> v.createParam(Long::parseUnsignedLong, "[index]"), (player, index) -> {
            LogReader reader = LogReader.logReaders.getOrDefault(player.getUniqueId(), null);
            if (reader == null) return;
            reader.toIndex.set0(index);
        });
        AnyEvent.addEvent("player_logger.state", AnyEvent.type.none, v -> v.createParam(LogReader.State.values()), (player, state) -> {
            LogReader reader = LogReader.logReaders.getOrDefault(player.getUniqueId(), null);
            if (reader == null) return;
            reader.toState.set0(state);
        });

        lime.repeat(PlayerLogger::resetCacheTick, 5*60);

        lime.repeat(EquipmentLog::tick, 0.5);
        lime.repeat(SkinLog::tick, 0.5);
        lime.repeat(MoveLog::tick, 0.1);

        lime.repeat(PlayerLogger::syncLogs, 10);
        lime.timer()
                .setSync()
                .withLoopTicks(1)
                .withCallbackTicks(LogReader::tickAll)
                .run();
    }
    public static void resetCacheTick() {
        EquipmentLog.resetCacheTick();
        SkinLog.resetCacheTick();
        MoveLog.resetCacheTick();
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void on(PlayerSpeakEvent e) {
        if (!ENABLE) return;
        log(new VoiceLog(e));
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void on(EntityPoseChangeEvent e) {
        if (!ENABLE) return;
        if (e.getEntity() instanceof Player player) log(new MoveLog(player, player.getLocation(), e.getPose()));
    }

    private static class LogDisplay extends Displays.ObjectDisplay<LogPlayer, EntityPlayer> {
        private static class LogName extends Displays.ObjectDisplay<LogPlayer, EntityAreaEffectCloud> {
            @Override public double getDistance() { return 100; }
            private final Component name;
            private final Displays.ObjectDisplay<?, ?> parent;
            private final system.Func1<Player, Boolean> filter;
            private boolean isInit = false;
            protected LogName(Displays.ObjectDisplay<?, ?> parent, Component name, Location location, system.Func1<Player, Boolean> filter) {
                super(location);
                this.name = name;
                this.parent = parent;
                this.filter = filter;
                postInit();
            }
            @Override protected EntityAreaEffectCloud createEntity(Location location) {
                EntityAreaEffectCloud stand = new EntityAreaEffectCloud(
                        ((CraftWorld)location.getWorld()).getHandle(),
                        location.getBlockX(), location.getBlockY(), location.getBlockZ());
                stand.setDuration(2000000000);
                stand.tickCount = 2000000000;
                stand.setCustomName(ChatHelper.toNMS(name));
                stand.setRadius(0);
                stand.setCustomNameVisible(true);
                stand.setOnGround(true);
                stand.setNoGravity(true);
                return stand;
            }
            @Override protected void show(Player player) {
                if (!isInit) {
                    Displays.addPassengerID(this.parent.entityID, entityID);
                    isInit = true;
                }
                super.show(player);
                lime.once(() -> {
                    WrapperPlayServerMount mount = new WrapperPlayServerMount();
                    mount.setEntityID(this.parent.entityID);
                    mount.setPassengerIds(Displays.getPassengerIDs(this.parent.entityID));
                    mount.sendPacket(player);
                }, 1);
            }
            @Override public boolean isFilter(Player player) { return filter == null ? true : filter.invoke(player); }
            public static void create(LogDisplay base, String text, system.Toast1<Displays.ObjectDisplay<?, ?>> parent) {
                create(base, text, parent, null);
            }
            public static void create(LogDisplay base, String text, system.Toast1<Displays.ObjectDisplay<?, ?>> parent, system.Func1<Player, Boolean> filter) {
                LogName npcName = new LogName(parent.val0, ChatHelper.formatComponent(text), new Location(lime.LoginWorld,0,0,0), filter);
                parent.val0 = npcName;
                base.preInitDisplay(npcName);
            }
        }

        @Override public double getDistance() { return 150; }

        private final LogPlayer npc;
        private Location location = new Location(lime.LoginWorld,0,0,0);
        private List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipment;
        private EntityPose pose = EntityPose.STANDING;

        @Override public boolean isFilter(Player player) { return player.getUniqueId().equals(npc.reader) && lime.isPlayerLoaded(player); }
        @Override public Location location() { return location; }

        protected LogDisplay(LogPlayer npc) {
            this.npc = npc;
            this.equipment = npc.createEquipment();

            system.Toast1<Displays.ObjectDisplay<?, ?>> parent = system.toast(this);
            npc.name.forEach(name -> LogName.create(this, name, parent));

            postInit();
        }
        @Override protected void sendData(Player player, boolean child) {
            PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook relMoveLook = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(entityID, (short)0, (short)0, (short)0, (byte)0, (byte)0, true);
            PacketPlayOutNamedEntitySpawn ppones = new PacketPlayOutNamedEntitySpawn(entity);
            PacketPlayOutPlayerInfo ppopi_add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entity);
            PacketPlayOutPlayerInfo ppopi_del = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entity);
            PacketPlayOutEntityEquipment ppoee = equipment.size() <= 0 ? null : new PacketPlayOutEntityEquipment(entityID, equipment);

            PacketManager.sendPackets(player, ppopi_add, ppones, relMoveLook, ppoee);
            lime.once(() -> PacketManager.sendPacket(player, ppopi_add), 0.5);
            lime.once(() -> PacketManager.sendPacket(player, ppopi_del), 5);
            super.sendData(player, child);
        }
        @Override protected EntityPlayer createEntity(Location location) {
            WorldServer world = ((CraftWorld)location.getWorld()).getHandle();
            EntityPlayer fakePlayer = new EntityPlayer(
                    ((CraftServer)Bukkit.getServer()).getServer(),
                    world,
                    new GameProfile(npc.fakeUUID, "")
            );
            fakePlayer.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            return fakePlayer;
        }
        @Override protected void editDataWatcher(Player player, Displays.EditedDataWatcher dataWatcher) {
            dataWatcher.setCustom(EntityHuman.DATA_PLAYER_MODE_CUSTOMISATION, Byte.MAX_VALUE);
            dataWatcher.setCustom(Displays.EditedDataWatcher.DATA_POSE, pose);
            super.editDataWatcher(player, dataWatcher);
        }

        @Override public void update(LogPlayer npc, double delta) {
            List<LogPlayer.DirtyType> dirty = npc.getDirty(true);
            if (dirty.size() == 0) {
                super.update(npc, delta);
                return;
            }

            if (dirty.contains(LogPlayer.DirtyType.Location)) {
                location = npc.getLocation();
                entity.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

                super.update(npc, delta);

                PacketPlayOutEntityTeleport movePacket = new PacketPlayOutEntityTeleport(entity);
                PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(entity, (byte) MathHelper.floor((location.getYaw() % 360.0F) * 256.0F / 360.0F));
                this.invokeAll(player -> PacketManager.sendPackets(player, movePacket, headPacket));
            }
            if (dirty.contains(LogPlayer.DirtyType.Equipment)) {
                equipment = npc.createEquipment();
                PacketPlayOutEntityEquipment ppoee = equipment.size() <= 0 ? null : new PacketPlayOutEntityEquipment(entityID, equipment);
                this.invokeAll(player -> PacketManager.sendPackets(player, ppoee));
            }
            if (dirty.contains(LogPlayer.DirtyType.Pose)) {
                pose = EntityPose.values()[npc.getPose().ordinal()];
                this.invokeAll(this::sendDataWatcher);
            }
            if (dirty.contains(LogPlayer.DirtyType.Skin)) {
                system.Toast2<String, String> skin = npc.getSkin();
                Skins.setProfile(entity.gameProfile, skin.val0, skin.val1, false);
                hideAll();
            }
        }
        public void OnClick(Player player, boolean isShift) {
            lime.logOP("CLICK"+(isShift ? ".SHIFT" : "")+": " + npc.uuid);
        }

        @Override public void hide(Player player) {
            super.hide(player);
            PacketPlayOutPlayerInfo ppopi = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entity);
            PacketManager.sendPackets(player, ppopi);
        }

        public static LogDisplay create(system.Toast2<UUID, UUID> key, LogPlayer npc) {
            return new LogDisplay(npc);
        }
    }
    private static class LogManager extends Displays.DisplayManager<system.Toast2<UUID, UUID>, LogPlayer, LogDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }

        @Override public Map<system.Toast2<UUID, UUID>, LogPlayer> getData() { return LogReader.getAllReaders(); }
        @Override public LogDisplay create(system.Toast2<UUID, UUID> key, LogPlayer npc) { return LogDisplay.create(key, npc); }
    }
    public static class LogPlayer {
        public final UUID reader;
        public final UUID uuid;
        public final UUID fakeUUID = NPC.createUUID();
        public final List<String> name;

        public enum DirtyType {
            Location,
            Pose,
            Skin,
            Equipment
        }

        private final system.LockToast1<Location> location = system.toast(new Location(null,0,0,0)).lock();
        public Location getLocation() { return location.get0(); }
        public void setLocation(Location location) {
            this.location.set0(location);
            setDirty(DirtyType.Location, true);
        }

        private final system.LockToast1<Pose> pose = system.toast(Pose.STANDING).lock();
        public Pose getPose() { return pose.get0(); }
        public void setPose(Pose pose) {
            this.pose.edit0(v -> {
                if (v == pose) return v;
                setDirty(DirtyType.Pose, true);
                return pose;
            });
        }

        private final system.LockToast1<system.Toast2<String, String>> skin = system.toast(system.<String,String>toast(null, null)).lock();
        public system.Toast2<String, String> getSkin() { return skin.get0(); }
        public void setSkin(String value, String signature) {
            system.Toast2<String, String> val = system.toast(value, signature);
            this.skin.edit0(v -> {
                if (system.IToast.equals(v, val)) return v;
                setDirty(DirtyType.Skin, true);
                return val;
            });
        }

        private final system.LockToast1<HashMap<EquipmentSlot, ItemStack>> items = system.toast(new HashMap<EquipmentSlot, ItemStack>()).lock();
        public List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> createEquipment() {
            List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipment = new ArrayList<>();
            items.get0().forEach((k,v) -> equipment.add(new Pair<>(EnumItemSlot.values()[k.ordinal()], CraftItemStack.asNMSCopy(v))));
            return equipment;
        }
        public void setItems(HashMap<EquipmentSlot, ItemStack> items) {
            this.items.set0(items);
            setDirty(DirtyType.Equipment, true);
        }

        private final system.LockToast1<Set<DirtyType>> dirty = system.<Set<DirtyType>>toast(new HashSet<>()).lock();
        public List<DirtyType> getDirty(boolean reset) {
            List<DirtyType> types = new ArrayList<>();
            dirty.edit0(list -> {
                types.addAll(list);
                if (reset) list.clear();
                return list;
            });
            return types;
        }
        public void setDirty(DirtyType type, boolean state) {
            dirty.edit0(list -> {
                if (state) list.add(type);
                else list.remove(type);
                return list;
            });
        }

        public LogPlayer(UUID reader, UUID uuid, List<String> name) {
            this.reader = reader;
            this.uuid = uuid;
            this.name = name;
        }
    }
    public static class LogReader {
        private static final String APPEND_SPACE = StringUtils.repeat(" \n", 20);

        private static final ConcurrentHashMap<UUID, LogReader> logReaders = new ConcurrentHashMap<>();
        public static void tickAll(long delta) {
            logReaders.values().removeIf(v -> v.tick(delta));
        }
        public static Map<system.Toast2<UUID, UUID>, LogPlayer> getAllReaders() {
            return logReaders.entrySet()
                    .stream()
                    .flatMap(v -> v.getValue().fakePlayers.entrySet().stream().map(_v -> system.toast(v.getKey(), _v.getKey(), _v.getValue())))
                    .collect(Collectors.toMap(v -> system.toast(v.val0, v.val1), v -> v.val2));
        }
        public static void load(Player player, Calendar start_time, int sec_length, double distance, system.Action1<LogReader> callback) {
            Calendar end_time = (Calendar)start_time.clone();
            end_time.add(Calendar.SECOND, sec_length);
            load(player, start_time, end_time, distance, callback);
        }
        public static void load(Player player, Calendar start_time, Calendar end_time, double distance, system.Action1<LogReader> callback) {
            load(player, start_time.getTimeInMillis(), end_time.getTimeInMillis(), distance, callback);
        }
        public static void load(Player player, long start_time, long end_time, double distance, system.Action1<LogReader> callback) {
            Location location = player.getLocation();
            web.method.GET.create(URL + "/read?" + system.map
                    .<String, Object>of()
                    .add("token", TOKEN)
                    .add("from", start_time)
                    .add("to", end_time)
                    .add("x", system.round(location.getX(), 3))
                    .add("z", system.round(location.getZ(), 3))
                    .add("world", getWorldIndex(location.getWorld()))
                    .add("distance", system.round(distance, 3))
                    .build()
                    .entrySet()
                    .stream()
                    .map(kv -> kv.getKey() + "=" + kv.getValue())
                    .collect(Collectors.joining("&"))
            ).json().executeAsync((result, code) -> {
                if (code == 200) {
                    LogReader reader = new LogReader(player, result.getAsJsonObject().getAsJsonObject("response"), start_time);
                    logReaders.put(player.getUniqueId(), reader);
                    callback.invoke(reader);
                    return;
                }
                lime.logOP("[ERROR:"+code+"] " + result);
                callback.invoke(null);
            });
        }
        public static void close(Player player) {
            LogReader reader = LogReader.logReaders.getOrDefault(player.getUniqueId(), null);
            if (reader == null) return;
            reader.toState.set0(State.Close);
        }

        public final UUID player_uuid;
        public final List<Frame> frames = new ArrayList<>();
        public final List<Frame> startFrames = new ArrayList<>();
        public abstract class Frame {
            public final long ms;
            public final UUID uuid;
            private final String key;
            public Frame(UUID uuid, long start_time, long ms) {
                this.key = this.getClass().getName();
                this.ms = ms - start_time;
                this.uuid = uuid;
            }
            public long getMs() { return ms; }
            public UUID getUUID() { return uuid; }
            public String getKey() { return key; }
            public abstract void invoke(Player player, LogPlayer logPlayer);
            public void invoke(Player player) {
                LogPlayer logPlayer = LogReader.this.fakePlayers.getOrDefault(uuid, null);
                if (logPlayer == null) return;
                invoke(player, logPlayer);
            }
        }
        public class MoveFrame extends Frame {
            public final Location location;
            public final Pose pose;
            public MoveFrame(long start_time, JsonObject json) {
                super(UUID.fromString(json.get("uuid").getAsString()), start_time, json.get("tick").getAsLong());
                location = new Location(
                        getWorldByIndex(json.get("world").getAsInt()),
                        json.get("x").getAsDouble(),
                        json.get("y").getAsDouble(),
                        json.get("z").getAsDouble(),
                        json.get("yaw").getAsFloat(),
                        json.get("pitch").getAsFloat()
                );
                pose = Pose.valueOf(json.get("pose").getAsString());
            }
            @Override public void invoke(Player player, LogPlayer logPlayer) {
                logPlayer.setLocation(location);
                logPlayer.setPose(pose);
            }
        }
        public class VoiceFrame extends Frame {
            public final short distance;
            public final byte[] data;
            public VoiceFrame(long start_time, JsonObject json) {
                super(UUID.fromString(json.get("uuid").getAsString()), start_time, json.get("tick").getAsLong());
                distance = json.get("distance").getAsShort();
                data = Base64.getDecoder().decode(json.get("data").getAsString());
            }
            @Override public void invoke(Player player, LogPlayer logPlayer) {
                if (player == null) return;
                SocketClientUDP socket = SocketServerUDP.clients.getOrDefault(player, null);
                if (socket == null) return;
                try { SocketServerUDP.sendTo(PacketUDP.write(new VoiceServerPacket(data, uuid, 0, distance)), socket); }
                catch (Exception ignore) { }
            }
        }
        public class SkinFrame extends Frame {
            public String value;
            public String signature;
            public SkinFrame(long start_time, JsonObject json) {
                super(UUID.fromString(json.get("uuid").getAsString()), start_time, json.get("tick").getAsLong());
                value = json.get("value").getAsString();
                signature = json.get("signature").getAsString();
            }
            @Override public void invoke(Player player, LogPlayer logPlayer) {
                logPlayer.setSkin(value, signature);
            }
        }
        public class EquipmentFrame extends Frame {
            public final HashMap<EquipmentSlot, ItemStack> items;
            public EquipmentFrame(long start_time, JsonObject json) {
                super(UUID.fromString(json.get("uuid").getAsString()), start_time, json.get("tick").getAsLong());
                items = new HashMap<>();
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    String slot_key = slot.name().toLowerCase();
                    if (!json.has(slot_key)) continue;
                    JsonElement item = json.get(slot_key);
                    if (item.isJsonNull()) continue;
                    items.put(slot, system.loadItem(item.getAsString()));
                }
            }
            @Override public void invoke(Player player, LogPlayer logPlayer) {
                logPlayer.setItems(items);
            }
        }

        private final long totalMsStart;
        private final int length;
        private final long length_time;
        private final ConcurrentHashMap<UUID, LogPlayer> fakePlayers = new ConcurrentHashMap<>();
        private LogReader(Player player, JsonObject json, long start_time) {
            this.player_uuid = player.getUniqueId();
            this.totalMsStart = start_time;
            List<Frame> frames = new ArrayList<>();
            json.get("move").getAsJsonArray().forEach(item -> frames.add(new MoveFrame(this.totalMsStart, item.getAsJsonObject())));
            json.get("skin").getAsJsonArray().forEach(item -> frames.add(new SkinFrame(this.totalMsStart, item.getAsJsonObject())));
            json.get("voice").getAsJsonArray().forEach(item -> frames.add(new VoiceFrame(this.totalMsStart, item.getAsJsonObject())));
            json.get("equipment").getAsJsonArray().forEach(item -> frames.add(new EquipmentFrame(this.totalMsStart, item.getAsJsonObject())));
            List<Frame> _frames = frames.stream().sorted(Comparator.comparingLong(Frame::getMs)).collect(Collectors.toList());

            _frames.stream()
                    .map(v -> v.uuid)
                    .distinct()
                    .forEach(uuid -> fakePlayers.put(uuid, new LogPlayer(this.player_uuid, uuid, Arrays.asList("UUID: " + uuid, "[Player Logger]"))));

            this.frames.addAll(_frames);
            this.frames.removeIf(v -> {
                if (v.ms >= 0) return false;
                startFrames.add(v);
                return true;
            });
            this.length = this.frames.size();
            this.length_time = this.frames.get(this.length - 1).ms;
            init();
        }
        public final system.LockToast1<Long> toIndex = system.<Long>toast(null).lock();
        public final system.LockToast1<State> toState = system.<State>toast(null).lock();

        public enum State {
            Play,
            Pause,
            Close
        }
        private int index;
        private long localMs;
        private State state;
        private void init() { init(Collections.emptyList()); }
        private void init(List<Frame> postinit_frames) {
            index = 0;
            localMs = 0;
            state = State.Pause;
            Player player = DataReader.onlinePlayers.getOrDefault(player_uuid, null);
            startFrames.forEach(frame -> frame.invoke(player));
            postinit_frames.forEach(frame -> frame.invoke(player));
        }
        private static system.Toast3<Integer, Long, List<Frame>> getIndexOfMs(List<Frame> frames, long ms) {
            HashMap<String, Frame> postinit_frames = new HashMap<>();
            int length = frames.size();
            for (int i = 0; i < length; i++) {
                Frame frame = frames.get(i);
                if (frame.ms > ms) return system.toast(i, ms, new ArrayList<>(postinit_frames.values()));
                postinit_frames.put(frame.getKey(), frame);
            }
            return system.toast(length, frames.get(length - 1).ms, new ArrayList<>(postinit_frames.values()));
        }
        private boolean tick(long delta) {
            Player player = DataReader.onlinePlayers.getOrDefault(player_uuid, null);
            Long to_index = toIndex.get0();
            if (to_index != null) {
                toIndex.set0(null);
                system.Toast3<Integer, Long, List<Frame>> dat = getIndexOfMs(frames, to_index);
                index = dat.val0;
                localMs = dat.val1;
                init(dat.val2);
            }
            State to_state = toState.get0();
            if (to_state != null) state = to_state;
            if (player == null) state = State.Close;
            switch (state) {
                case Play: {
                    localMs += delta;
                    if (index >= length || localMs >= length_time) {
                        index = length - 1;
                        localMs = length_time;
                        state = State.Pause;
                    }
                    else for (; index < length; index++) {
                        Frame frame = frames.get(index);
                        if (frame.ms > localMs) break;
                        frame.invoke(player);
                    }
                    break;
                }
                case Close: {
                    logReaders.remove(player_uuid);
                    if (player != null) {
                        player.sendMessage(Component.text(APPEND_SPACE));
                        LangMessages.Message.Chat_Join.sendMessage(player);
                    }
                    return true;
                }
            }
            if (player != null) panel(player);
            return false;
        }
        private static String generateText(long totalMsStart, long localMs, long length) {
            return String.join("\n", Arrays.asList(
                    "Локальное время: " + system.formatTotalTime(localMs, system.FormatTime.MINUTE_TIME),
                    "Время: " + system.formatCalendar(system.getMoscowTime(localMs + totalMsStart), true),
                    "Позиция: " + ((localMs * 100) / length) + "%"
            ));
        }
        private static Component generateLine(int size, long totalMsStart, long localMs, long length) {
            Component pixel = Component.text("|");
            double step = length / (size - 1.0);
            Component line = Component.empty();
            for (int i = 0; i < size; i++) {
                long time = (long)(step * i);
                line = line.append(pixel
                        .color(time < localMs ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY)
                        .hoverEvent(Component.text(generateText(totalMsStart, localMs, length)))
                        .clickEvent(ClickEvent.runCommand("/any.event player_logger.index " + time))
                );
            }
            return line;
        }
        private static Component time(long totalMsStart, long localMs) {
            return Component.text(StringUtils.leftPad(system.formatTotalTime(localMs, system.FormatTime.MINUTE_TIME), 6, ChatHelper.getSpaceSize(6)))
                    .hoverEvent(Component.text("Реальное время: " + system.formatCalendar(system.getMoscowTime(totalMsStart + localMs), true)));
        }
        private static Component button(String text, String hover, String click) {
            return Component.text(text)
                    .hoverEvent(Component.text(hover))
                    .clickEvent(ClickEvent.runCommand(click));
        }
        private Component move_button(String text, String hover, long deltaMs) {
            return button(text, hover, "/any.event player_logger.index " + Math.min(length_time, Math.max(0, localMs + deltaMs)));
        }
        private void panel(Player player) {
            player.sendMessage(Component
                    .text(APPEND_SPACE)
                    .append(button("[×]", "Закрыть", "/any.event player_logger.state " + State.Close.name()))
                    .append(Component.text("Просмотрщик:\n ["))
                    .append(generateLine(78, totalMsStart, localMs, length_time))
                    .append(Component.text("] ["))
                    .append(time(totalMsStart, localMs))
                    .append(Component.text("/"))
                    .append(time(totalMsStart, length_time))
                    .append(Component.text("] ["))
                    .append(move_button("⏮ ", "Назад на 5 мин.", 5*60*1000))
                    .append(move_button("⏪ ", "Вперед на 1 мин.", 60*1000))
                    .append(state == State.Pause
                            ? button("[⏵]", "Запустить", "/any.event player_logger.state " + State.Play.name())
                            : button("[⏸]", "Остановить", "/any.event player_logger.state " + State.Pause.name())
                    )
                    .append(move_button(" ⏩", "Вперед на 1 мин.", 60*1000))
                    .append(move_button(" ⏭", "Назад на 5 мин.", 5*60*1000))
                    .append(Component.text("]")));
        }
    }
}































