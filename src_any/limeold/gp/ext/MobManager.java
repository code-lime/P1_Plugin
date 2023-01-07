package org.lime.gp.module;

import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftSpider;
import org.lime.gp.extension.JManager;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.gp.player.module.Skins;
import org.lime.packetwrapper.*;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.monster.EntitySpider;
import net.minecraft.world.level.EnumGamemode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.lime.core;
import org.lime.reflection;
import org.lime.system;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.gp.chat.ChatHelper;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MobManager implements Listener {
    public static abstract class IMob {
        public static final IMob EMPTY = new IMob() {
            @Override public void onSpawn(Entity entity, PacketEvent event) { }
            @Override public void onMetadata(Entity entity, PacketEvent event) { }
            @Override public void onHeadRotation(Entity entity, PacketEvent event) { }
            @Override public void onEntityTeleport(Entity entity, PacketEvent event) { }
            @Override public List<Component> getNickLines(Entity entity) { return Collections.emptyList(); }
        };

        private static final HashMap<EntityType, IMob> mobs = new HashMap<>();
        public static void call(PacketEvent event, system.Action3<IMob, Entity, PacketEvent> callback) {
            Entity entity;
            try { entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0); }
            catch (Exception e) { entity = null; }
            if (entity == null) return;
            IMob mob = mobs.getOrDefault(entity.getType(), null);
            if (mob == null) return;
            callback.invoke(mob, entity, event);
        }

        public static IMob parse(JsonObject json) {
            if (json.has("skins")) return new HumanMob(json);
            else if (json.has("override")) return new OverrideMob(json);
            throw new IllegalArgumentException("Type of IMob not founded!");
        }

        public abstract void onSpawn(Entity entity, PacketEvent event);
        public abstract void onMetadata(Entity entity, PacketEvent event);
        public abstract void onHeadRotation(Entity entity, PacketEvent event);
        public abstract void onEntityTeleport(Entity entity, PacketEvent event);
        public abstract List<Component> getNickLines(Entity entity);
    }
    public static abstract class INickMob extends IMob {
        public final List<Component> nick = new LinkedList<>();
        public INickMob(JsonObject json) { for (String name : json.get("nick").getAsString().split("\n")) nick.add(ChatHelper.formatComponent(name)); }
        @Override public List<Component> getNickLines(Entity entity) { return nick; }
    }
    public static class HumanMob extends INickMob {
        public final List<String> skins = new ArrayList<>();

        public HumanMob(JsonObject json) {
            super(json);
            json.getAsJsonArray("skins").forEach(item -> skins.add(item.getAsString()));
            Skins.addSkins(skins);
        }

        private static PacketPlayOutPlayerInfo.PlayerInfoData of(UUID uuid, String name, String skin) {
            GameProfile profile = Skins.setSkin(new GameProfile(uuid, name), skin);
            return new PacketPlayOutPlayerInfo.PlayerInfoData(profile, 0, EnumGamemode.SURVIVAL, ChatHelper.toNMS(Component.text(name)));
        }

        private static final EntityPlayer SINGELTON_ENTITYPLAYER = new EntityPlayer(
                ((CraftServer) Bukkit.getServer()).getServer(),
                ((CraftWorld) lime.MainWorld).getHandle(),
                new GameProfile(UUID.randomUUID(), ".")
        );

        /*
        this.a = player.getId();
        this.b = player.getProfile().getId();
        this.c = player.locX();
        this.d = player.locY();
        this.e = player.locZ();
        this.f = (byte)(player.getYRot() * 256.0f / 360.0f);
        this.g = (byte)(player.getXRot() * 256.0f / 360.0f);
        */

        private static final ConcurrentHashMap<String, system.Action2<PacketPlayOutNamedEntitySpawn, Object>> invokeArgs = new ConcurrentHashMap<>();
        private static void setValue(PacketPlayOutNamedEntitySpawn spawn, String name, Object value) {
            invokeArgs.compute(name, (k,v) -> {
                if (v != null) return v;
                reflection.field<Object> field = reflection.field.ofMojang(PacketPlayOutNamedEntitySpawn.class, name);
                return (_spawn, _value) -> {
                    try { field.set(_spawn, _value); }
                    catch (Exception e) { throw new IllegalArgumentException(e); }
                };
            }).invoke(spawn, value);
        }

        private static PacketPlayOutNamedEntitySpawn spawnPacket(int id, UUID uuid, Location location) {
            PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(SINGELTON_ENTITYPLAYER);
            setValue(spawn, "entityId", id);
            setValue(spawn, "playerId", uuid);
            setValue(spawn, "x", location.getX());
            setValue(spawn, "y", location.getY());
            setValue(spawn, "z", location.getZ());
            setValue(spawn, "yRot", (byte)(location.getYaw() * 256.0f / 360.0f));
            setValue(spawn, "xRot", (byte)(location.getPitch() * 256.0f / 360.0f));
            return spawn;
        }

        @Override public void onSpawn(Entity entity, PacketEvent event) {
            int length;
            if ((length = skins.size()) == 0) return;
            int id = entity.getEntityId();
            UUID uuid = entity.getUniqueId();
            String skin = skins.get(id % length);
            Player player = event.getPlayer();
            event.setCancelled(true);

            Location location = entity.getLocation();

            PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook relMoveLook = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(id, (short)0, (short)0, (short)0, (byte)0, (byte)0, true);
            PacketPlayOutNamedEntitySpawn ppones = spawnPacket(id, uuid, location);
            PacketPlayOutPlayerInfo.PlayerInfoData pid = of(uuid, ".", skin);

            PacketPlayOutPlayerInfo ppopi_add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER);
            ppopi_add.getEntries().add(pid);

            PacketPlayOutPlayerInfo ppopi_del = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
            ppopi_del.getEntries().add(pid);

            PacketManager.sendPackets(player, ppopi_add, ppones, relMoveLook);
            lime.once(() -> PacketManager.sendPacket(player, ppopi_add), 0.5);
            lime.once(() -> PacketManager.sendPacket(player, ppopi_del), 1);

            /*fakePlayer.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook relMoveLook = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(id, (short)0, (short)0, (short)0, (byte)0, (byte)0, true);

            WrapperPlayServerNamedEntitySpawn wpsnes = new WrapperPlayServerNamedEntitySpawn();
            wpsnes.setEntityID(id);
            //wpsnes.setMetadata(new WrappedDataWatcher());
            wpsnes.setPosition(location.toVector());
            wpsnes.setYaw(location.getYaw());
            wpsnes.setPitch(location.getPitch());
            wpsnes.sendPacket();

            PacketPlayOutPlayerInfo.PlayerInfoData pid = of(uuid, ".", skin);

            PacketPlayOutPlayerInfo ppopi_add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a);
            ppopi_add.b().add(pid);

            PacketPlayOutPlayerInfo ppopi_del = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e);
            ppopi_del.b().add(pid);*/

            /*WrapperPlayServerPlayerInfo wpspi_add = new WrapperPlayServerPlayerInfo();
            wpspi_add.(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            wpspi_add.setData(Collections.singletonList(pid));

            WrapperPlayServerPlayerInfo wpspi_del = new WrapperPlayServerPlayerInfo();
            wpspi_del.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            wpspi_add.setData(Collections.singletonList(pid));*/

            /*wpsnes.sendPacket(player);
            PacketManager.SendPacket(player, relMoveLook);
            lime.Once(() -> PacketManager.SendPacket(player, ppopi_add), 0.5);
            lime.Once(() -> PacketManager.SendPacket(player, ppopi_del), 1);*/

            /*PacketPlayOutNamedEntitySpawn ppones = new PacketPlayOutNamedEntitySpawn(fakePlayer);
            PacketPlayOutPlayerInfo ppopi_add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, fakePlayer);
            PacketPlayOutPlayerInfo ppopi_del = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e, fakePlayer);

            PacketManager.SendPackets(player, ppopi_add, ppones, relMoveLook);
            lime.Once(() -> PacketManager.SendPacket(player, ppopi_add), 0.5);
            lime.Once(() -> PacketManager.SendPacket(player, ppopi_del), 1);*/
        }
        @Override public void onMetadata(Entity entity, PacketEvent event) {
            if (skins.size() == 0) return;
            WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(event.getPacket());
            List<WrappedWatchableObject> objects = metadata.getMetadata();
            if (objects == null) return;
            WrappedDataWatcher dataWatcher = new WrappedDataWatcher(objects);

            Field.of(dataWatcher, 0, Byte.class).is().ifPresent(v -> v.edit(_v -> (byte)(_v & ~0x01)));

            Field.ofOptional(dataWatcher, 2, IChatBaseComponent.class).override(IChatBaseComponent.nullToEmpty("."));
            Field.of(dataWatcher, 15, Float.class).override(0.0f);
            Field.of(dataWatcher, 16, Integer.class).override(0);
            Field.of(dataWatcher, 17, Byte.class).override(Byte.MAX_VALUE);
            Field.of(dataWatcher, 18, Byte.class).override((byte)1);
            Field.of(dataWatcher, 19, NBTTagCompound.class).override(new NBTTagCompound());
            Field.of(dataWatcher, 20, NBTTagCompound.class).override(new NBTTagCompound());

            dataWatcher.getIndexes().forEach(index -> { if (index > 20) dataWatcher.remove(index); });
            metadata.setMetadata(dataWatcher.getWatchableObjects());
        }
        @Override public void onHeadRotation(Entity entity, PacketEvent event) {
            if (skins.size() == 0) return;
            Location location = entity.getLocation();
            byte yaw = (byte) MathHelper.floor((location.getYaw() % 360.0F) * 256.0F / 360.0F);
            byte pitch = (byte) MathHelper.floor((location.getPitch() % 360.0F) * 256.0F / 360.0F);
            /*
            PacketPlayOutEntity.PacketPlayOutEntityLook move = (PacketPlayOutEntity.PacketPlayOutEntityLook)event.getPacket().getHandle();
            PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook look = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(
                    entity.getEntityId(),
                    move.b(),
                    move.c(),
                    move.d(),
                    yaw,
                    pitch,
                    entity.isOnGround()
            );
            PacketManager.SendPackets(event.getPlayer(), look);*/
            /*PacketPlayOutEntity.PacketPlayOutEntityLook look = new PacketPlayOutEntity.PacketPlayOutEntityLook(
                    entity.getEntityId(),
                    yaw,
                    pitch,
                    entity.isOnGround()
            );
            PacketManager.SendPackets(event.getPlayer(), look);
            lime.LogOP("TP to: " + system.getString(entity.getLocation().toVector()) + " : " + event.getPacketType().name());
            event.setCancelled(true);
            //event.setCancelled(true);*/
        }
        @Override public void onEntityTeleport(Entity entity, PacketEvent event) {
            if (skins.size() == 0) return;
            /*if (event.getPacketType() == PacketType.Play.Server.REL_ENTITY_MOVE)
            {
                event.getPacket().getHandle()
            }
            lime.LogOP("TP to: " + system.getString(entity.getLocation().toVector()) + " : " + event.getPacketType().name());*/
            //PacketManager.SendPackets(event.getPlayer(), new PacketPlayOutEntityTeleport(((CraftEntity)entity).getHandle()));
            //event.setCancelled(true);
        }
    }
    public static class OverrideMob extends INickMob {
        public final String override;

        public OverrideMob(JsonObject json) {
            super(json);
            override = json.get("override").getAsString();
        }

        @Override public void onSpawn(Entity entity, PacketEvent event) {
            switch (override) {
                case "ANGRY_WOLF": {
                    event.getPacket().getIntegers().write(1, IRegistry.ENTITY_TYPE.getId(EntityTypes.WOLF));
                    return;
                }
            }
        }
        @Override public void onMetadata(Entity entity, PacketEvent event) {
            switch (override) {
                case "ANGRY_WOLF": {
                    WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(event.getPacket());
                    List<WrappedWatchableObject> objects = metadata.getMetadata();
                    if (objects == null) return;

                    WrappedDataWatcher dataWatcher = new WrappedDataWatcher(objects);

                    Field.of(dataWatcher, 15, Byte.class).override((byte)4);
                    Field.of(dataWatcher, 16, Boolean.class).override(false);
                    Field.of(dataWatcher, 17, Byte.class).override((byte)0);
                    Field.ofOptional(dataWatcher, 18, UUID.class).override(null);
                    Field.of(dataWatcher, 19, Boolean.class).override(false);
                    Field.of(dataWatcher, 20, Integer.class).override(14);
                    Field.of(dataWatcher, 21, Integer.class).override(777);

                    dataWatcher.getIndexes().forEach(index -> { if (index > 21) dataWatcher.remove(index); });
                    metadata.setMetadata(dataWatcher.getWatchableObjects());
                    return;
                }
            }
        }
        @Override public void onHeadRotation(Entity entity, PacketEvent event) { }
        @Override public void onEntityTeleport(Entity entity, PacketEvent event) { }
    }
    public static core.element create() {
        return core.element.create(MobManager.class)
                .withInit(MobManager::init)
                .withInstance()
                .<JsonObject>addConfig("config", v -> v
                        .withParent("mob_skins")
                        .withDefault(system.json.object()
                                .addObject(EntityType.DROWNED.name(), _v -> _v
                                        .add("skins", new JsonArray())
                                        .add("nick", "Бомж")
                                )
                                .addObject(EntityType.ZOMBIE.name(), _v -> _v
                                        .add("skins", new JsonArray())
                                        .add("nick", "Бомж")
                                )
                                .addObject(EntityType.SKELETON.name(), _v -> _v
                                        .add("skins", new JsonArray())
                                        .add("nick", "Разбойник")
                                )
                                .addObject(EntityType.STRAY.name(), _v -> _v
                                        .add("skins", new JsonArray())
                                        .add("nick", "Разбойник")
                                )
                                .build()
                        )
                        .withInvoke(j -> {
                            HashMap<EntityType, IMob> mobs = new HashMap<>();
                            j.entrySet().forEach(kv -> mobs.put(EntityType.valueOf(kv.getKey()), IMob.parse(kv.getValue().getAsJsonObject())));
                            MobManager.IMob.mobs.clear();
                            MobManager.IMob.mobs.putAll(mobs);
                        })
                )
                .<JsonObject>addConfig("config", v -> v
                        .withParent("mob_saturation")
                        .withDefault(new JsonObject())
                        .withInvoke(j -> {
                            HashMap<EntityType, SaturationData> saturations = new HashMap<>();
                            j.entrySet().forEach(kv -> saturations.put(EntityType.valueOf(kv.getKey()), new SaturationData(kv.getValue().getAsJsonObject())));
                            MobManager.SaturationData.saturations.clear();
                            MobManager.SaturationData.saturations.putAll(saturations);
                        })
                );
    }
    public static List<Component> getNick(LivingEntity entity) {
        EntityType type = entity.getType();
        IMob imob = IMob.mobs.getOrDefault(type, null);
        if (imob != null) return imob.getNickLines(entity);
        SaturationData data = SaturationData.saturations.getOrDefault(type, null);
        if (data != null) return data.getNickLines(entity);
        return Collections.emptyList();
    }
    public static double getDistance(LivingEntity entity) {
        EntityType type = entity.getType();
        if (IMob.mobs.containsKey(type)) return 15;
        if (SaturationData.saturations.containsKey(type)) return 2.5;
        return 10;
    }
    public static Stream<LivingEntity> getEntities() {
        return entities.values().stream();
    }
    private static CraftLivingEntity getEntityByUUID(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity == null) continue;
            return entity instanceof CraftLivingEntity e ? e : null;
        }
        return null;
    }
    private static class Listener extends PacketAdapter {
        private final HashMap<PacketType, system.Action1<PacketEvent>> listeners;
        public Listener(Plugin plugin, HashMap<PacketType, system.Action1<PacketEvent>> listeners) {
            super(plugin, listeners.keySet());
            this.listeners = listeners;
        }
        @Override public void onPacketSending(PacketEvent event) {
            system.Action1<PacketEvent> listener = listeners.getOrDefault(event.getPacketType(), null);
            if (listener == null) return;
            listener.invoke(event);
        }
    }
    public static final reflection.field<Navigation> navigation_EntityInsentient = reflection.field.ofMojang(EntityInsentient.class, "navigation");
    @EventHandler public static void on(EntitySpawnEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof CraftSpider spider) {
            EntitySpider entitySpider = spider.getHandle();
            navigation_EntityInsentient.set(entitySpider, new Navigation(entitySpider, entitySpider.getLevel()));
        }
        if (!IMob.mobs.containsKey(e.getEntityType())) return;
        if (entity instanceof Ageable ageable) ageable.setAdult();
    }
    @EventHandler public static void on(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof LivingEntity entity)) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        SaturationData data = SaturationData.saturations.getOrDefault(entity.getType(), null);
        if (data == null) return;
        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        Double saturation = data.tryGetFood(item);
        if (saturation == null) return;
        if (!SaturationData.tryAddFood(entity, saturation)) return;
        item.subtract();
        e.setCancelled(true);
    }

    private static class Field<T> {
        public final WrappedDataWatcher dataWatcher;
        public final int index;
        public final Class<T> tClass;
        public final boolean optional;
        public Field(WrappedDataWatcher dataWatcher, int index, Class<T> tClass, boolean optional) {
            this.dataWatcher = dataWatcher;
            this.index = index;
            this.tClass = tClass;
            this.optional = optional;
        }

        public void override(T value) {
            if (dataWatcher.hasIndex(index)) dataWatcher.remove(index);
            dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(index, WrappedDataWatcher.Registry.get(tClass, optional)), optional ? Optional.ofNullable(value) : value, true);
        }
        public Optional<T> read() { return dataWatcher.hasIndex(index) ? Optional.ofNullable((T)dataWatcher.getObject(index)) : Optional.empty(); }
        public void edit(system.Func1<T, T> func) { override(func.invoke(read().orElse(null))); }
        public boolean has() { return dataWatcher.hasIndex(index); }

        public Optional<Field<T>> is() { return has() ? Optional.of(this) : Optional.empty(); }

        public static <T>Field<T> of(WrappedDataWatcher dataWatcher, int index, Class<T> tClass) { return of(dataWatcher, index, tClass, false); }
        public static <T>Field<T> ofOptional(WrappedDataWatcher dataWatcher, int index, Class<T> tClass) { return new Field<>(dataWatcher, index, tClass, true); }
        public static <T>Field<T> of(WrappedDataWatcher dataWatcher, int index, Class<T> tClass, boolean optional) { return new Field<>(dataWatcher, index, tClass, optional); }
    }

    private static final ConcurrentHashMap<UUID, LivingEntity> entities = new ConcurrentHashMap<>();

    private static boolean nearPlayes(Location location, double distance) {
        system.Toast1<Boolean> near = system.toast(false);
        World world = location.getWorld();
        double _distance = distance * distance;
        EntityPosition.playerLocations.forEach((player, target) -> {
            if (near.val0) return;
            if (target.getWorld() != world) return;
            if (target.distanceSquared(location) > _distance) return;
            near.val0 = true;
        });
        return near.val0;
    }

    public static void init() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new Listener(lime._plugin, system.map.<PacketType, system.Action1<PacketEvent>>of()
                .add(PacketType.Play.Server.SPAWN_ENTITY_LIVING, event -> IMob.call(event, IMob::onSpawn))
                .add(PacketType.Play.Server.ENTITY_METADATA, event -> IMob.call(event, IMob::onMetadata))
                .build()
        ));
        lime.repeat(() -> {
            Map<UUID, LivingEntity> map = Bukkit.getWorlds()
                    .stream()
                    .flatMap(v -> v.getEntitiesByClass(LivingEntity.class).stream())
                    .filter(e -> {
                        if (!e.isEmpty()) return false;
                        EntityType type = e.getType();
                        if (IMob.mobs.containsKey(type)) return nearPlayes(e.getLocation(), 20);
                        if (SaturationData.saturations.containsKey(type)) return nearPlayes(e.getLocation(), 5);
                        return nearPlayes(e.getLocation(), 15);
                    })
                    .collect(Collectors.toMap(Entity::getUniqueId, e -> e));

            entities.putAll(map);
            entities.keySet().removeIf(k -> !map.containsKey(k));
        }, 5);
        lime.repeat(() -> Bukkit.getWorlds()
                .stream()
                .flatMap(v -> v.getEntitiesByClass(LivingEntity.class).stream())
                .forEach(e -> {
                    SaturationData data = SaturationData.saturations.getOrDefault(e.getType(), null);
                    if (data == null) return;
                    data.tick(e);
                }), 60);
    }

    public static class SaturationData {
        private static final HashMap<EntityType, SaturationData> saturations = new HashMap<>();
        public static class FoodData {
            public final Items.Checker func;
            public final double saturation;

            public FoodData(String key, JsonElement value) {
                func = Items.createCheck(key);
                saturation = value.getAsDouble();
            }
        }

        public final LinkedList<FoodData> food = new LinkedList<>();
        public final double time_min;
        public final double step_min;

        public SaturationData(JsonObject json) {
            json.getAsJsonObject("food").entrySet().forEach(kv -> food.add(new FoodData(kv.getKey(), kv.getValue())));
            time_min = json.get("time_min").getAsDouble();
            step_min = 1.0 / time_min;
        }
        public List<Component> getNickLines(LivingEntity entity) {
            JsonObject json = JManager.get(JsonObject.class, entity.getPersistentDataContainer(), "saturation", null);
            float value = (float)((json == null || !json.has("value") ? 1 : json.get("value").getAsDouble()) * 20);

            List<ImageBuilder> images = new ArrayList<>();

            int saturation = 20 - Math.round(value);

            //Фон - 0xEFE9
            //Целое - 0xEFF0
            //Кусок - 0xEFF1

            int offset = 18;
            int size = 3;
            int space = 3;

            /*
            boolean isPart = saturation % 2 == 1;
            if (isPart) saturation--;
            List<Component> list = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                int type = saturation - i * 2;
                if (type > 0) {
                    list.add(Component.text((char)0xEFE9));
                } else if (isPart && type == 0) {
                    list.add(Component.text((char)0xEFF1));
                } else {
                    list.add(Component.text((char)0xEFF0));
                }
            }
            return Collections.singletonList(Component.join(Component.text(ChatHelper.GetSpaceSize(-1)), list));
            */

            ImageBuilder _sat = ImageBuilder.of(0xEFF0, size);
            ImageBuilder _part = ImageBuilder.of(0xEFF1, size);
            ImageBuilder _back = ImageBuilder.of(0xEFE9, size);

            boolean isPart = saturation % 2 == 1;
            if (isPart) saturation--;
            for (int i = 0; i < 10; i++) {
                int type = saturation - (9-i) * 2;
                if (type > 0) images.add(_back.withOffset(offset - i * space));
                else if (isPart && type == 0) images.add(_part.withOffset(offset - i * space));
                else images.add(_sat.withOffset(offset - i * space));
            }

            /*
            if (saturation % 2 == 1) {
                int i = (saturation + 1) / 2 - 1;
                images.add(ImageBuilder.of(0xEFF1, size).withOffset(offset - i * space));
            }
            saturation = (saturation / 2) - 1;
            ImageBuilder _sat = ImageBuilder.of(0xEFF0, size);
            for (int i = 0; i <= saturation; i++)
                images.add(_sat.withOffset(offset - i * space));
            ImageBuilder _back = ImageBuilder.of(0xEFE9, size);
            for (int i = saturation + 1; i < 10; i++)
                images.add(_back.withOffset(offset - i * space));
            */



            return Collections.singletonList(
                    Component.text(ChatHelper.getSpaceSize(-10))
                            .append(ImageBuilder.join(images, 1))
            );
            //Arrays.asList(text, Component.text(((int) (saturation * 100)) + "%"));
        }
        public void tick(LivingEntity entity) {
            PersistentDataContainer container = entity.getPersistentDataContainer();
            JsonObject json = JManager.get(JsonObject.class, container, "saturation", null);
            double saturation = json == null || !json.has("value") ? 1 : json.get("value").getAsDouble();
            saturation = Math.max(0, saturation - step_min);
            if (saturation <= 0) {
                ((CraftLivingEntity)entity).getHandle().hurt(DamageSource.STARVE, 999999999);
                return;
            }
            JManager.set(container, "saturation", system.json.object().add("value", saturation).build());
        }
        public static boolean tryAddFood(LivingEntity entity, double food) {
            PersistentDataContainer container = entity.getPersistentDataContainer();
            JsonObject json = JManager.get(JsonObject.class, container, "saturation", null);
            double saturation = json == null || !json.has("value") ? 1 : json.get("value").getAsDouble();
            if (saturation > 0.9) return false;
            saturation = Math.min(1, saturation + food);
            JManager.set(container, "saturation", system.json.object().add("value", saturation).build());
            return true;
        }

        public Double tryGetFood(ItemStack item) {
            for (FoodData data : food) {
                if (data.func.check(item))
                    return data.saturation;
            }
            return null;
        }
    }
}










