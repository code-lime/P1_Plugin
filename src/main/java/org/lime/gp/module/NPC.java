package org.lime.gp.module;

import net.minecraft.world.entity.player.PlayerModelPart;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.lime.display.*;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.display.models.display.ChildEntityDisplay;
import org.lime.display.models.shadow.Builder;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.player.module.Advancements;
import org.lime.gp.player.module.Skins;
import org.lime.packetwrapper.WrapperPlayServerEntityTeleport;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.ChatHelper;
import org.lime.plugin.CoreElement;
import org.lime.system.utils.MathUtils;

import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class NPC {
    public static UUID createUUID() {
        String uuid = UUID.randomUUID().toString();
        return UUID.fromString(uuid.substring(0,14)+'1'+uuid.substring(15));
    }

    public static CoreElement create() {
        return CoreElement.create(NPC.class)
                .withInit(NPC::init)
                .<JsonObject>addConfig("npc", v -> v.withInvoke(NPC::config).withDefault(new JsonObject()));
    }

    public static HashMap<UUID, Set<String>> npc_show = new HashMap<>();
    public static void show(UUID uuid, String npc) {
        Set<String> npcList = npc_show.getOrDefault(uuid, null);
        if (npcList == null) npc_show.put(uuid, npcList = new HashSet<>());
        npcList.add(npc);
    }
    public static void hide(UUID uuid, String npc) {
        Set<String> npcList = npc_show.getOrDefault(uuid, null);
        if (npcList == null) return;
        npcList.remove(npc);
    }
    public static List<String> shows(UUID uuid) {
        List<String> list = new ArrayList<>();
        Set<String> npcList = npc_show.getOrDefault(uuid, null);
        if (npcList != null) list.addAll(npcList);
        return list;
    }

    private static final NPCManager NPC_MANAGER = new NPCManager();
    private static class NPCObject {
        public final String key;

        public final Location location;
        public final String skin;
        public final String menu;
        public final Map<String, String> args = new HashMap<>();
        public final boolean single;
        public final List<Component> name = new ArrayList<>();
        public final boolean hide;
        public final boolean sit;
        public final String shift_menu;
        public final HashMap<EnumItemSlot, ItemStack> equipment = new HashMap<>();

        public final List<Component> advancementName = new ArrayList<>();
        public final List<String> advancements = new ArrayList<>();

        public NPCObject(String key, JsonObject json) {
            this.key = key;

            location = MathUtils.getLocation(json.has("world") ? Bukkit.getWorlds().get(json.get("world").getAsInt()) : lime.MainWorld, json.get("location").getAsString());
            skin = json.get("skin").getAsString();
            menu = json.has("menu") ? json.get("menu").getAsString() : null;
            single = !json.has("single") || json.get("single").getAsBoolean();
            hide = json.has("hide") && json.get("hide").getAsBoolean();
            sit = json.has("sit") && json.get("sit").getAsBoolean();
            if (json.has("name")) {
                if (json.get("name").isJsonArray()) json.get("name").getAsJsonArray().forEach(_name -> name.add(ChatHelper.formatComponent(_name.getAsString())));
                else name.add(ChatHelper.formatComponent(json.get("name").getAsString()));
                Collections.reverse(name);
            }
            if (json.has("advancement_name")) {
                if (json.get("advancement_name").isJsonArray()) json.get("advancement_name").getAsJsonArray().forEach(_name -> advancementName.add(ChatHelper.formatComponent(_name.getAsString())));
                else advancementName.add(ChatHelper.formatComponent(json.get("advancement_name").getAsString()));
                Collections.reverse(advancementName);
            }
            if (json.has("advancements")) {
                if (json.get("advancements").isJsonArray()) json.get("advancements").getAsJsonArray().forEach(_name -> advancements.add(_name.getAsString()));
                else advancements.add(json.get("advancements").getAsString());
            }
            shift_menu = json.has("shift_menu") ? json.get("shift_menu").getAsString() : null;
            if (json.has("args")) json.getAsJsonObject("args").entrySet().forEach(kv -> this.args.put(kv.getKey(), kv.getValue().getAsString()));
            if (json.has("equipment")) json.get("equipment").getAsJsonObject().entrySet().forEach(kv -> equipment.put(EnumItemSlot.byName(kv.getKey()), Items.createItem(kv.getValue().getAsString()).orElseThrow()));
        }

        public boolean isShow(UUID uuid) {
            return !hide || shows(uuid).contains(key);
        }

        public Map<EnumItemSlot, net.minecraft.world.item.ItemStack> createEquipment() {
            Map<EnumItemSlot, net.minecraft.world.item.ItemStack> equipment = new HashMap<>();
            this.equipment.forEach((k,v) -> equipment.put(k, CraftItemStack.asNMSCopy(v)));
            return equipment;
        }

        public List<Component> getNick(Player player) {
            List<Component> nick = new ArrayList<>(name);
            for (String advancement : advancements)
                if (Advancements.advancementState(player, advancement) == Advancements.AdvancementState.Parent) {
                    nick.addAll(advancementName);
                    break;
                }
            return nick;
        }
    }
    private static final HashMap<String, NPCObject> npc_list = new HashMap<>();
    private static NPCDisplay getClick(int entityID) {
        for (NPCDisplay display : NPC_MANAGER.getDisplays().values()) {
            if (display.entityID == entityID)
                return display;
        }
        return null;
    }

    public static void click(PlayerUseUnknownEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.isAttack()) return;
        NPCDisplay display = getClick(event.getEntityId());
        if (display == null) return;
        Player player = event.getPlayer();
        boolean isShift = event.getPlayer().isSneaking();
        display.click(player, isShift);
    }
    public static void init() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(lime._plugin, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketPlayInUseEntity use = (PacketPlayInUseEntity)event.getPacket().getHandle();
                use.dispatch(new PacketPlayInUseEntity.c() {
                    @Override public void onAttack() { onInteraction(EnumHand.MAIN_HAND); }
                    @Override public void onInteraction(EnumHand enumHand) { onInteraction(enumHand, Vec3D.ZERO); }
                    @Override public void onInteraction(EnumHand enumHand, Vec3D vec3D) {
                        PlayerUseUnknownEntityEvent e = new PlayerUseUnknownEntityEvent(
                                event.getPlayer(),
                                use.getEntityId(),
                                use.getActionType() == PacketPlayInUseEntity.b.ATTACK,
                                enumHand == EnumHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND
                        );
                        lime.invokeSync(() -> click(e));
                    }
                });
            }
        });
        DrawText.load(NPC_MANAGER);
    }
    public static void config(JsonObject json) {
        HashMap<String, NPCObject> npc_list = new HashMap<>();
        List<String> skins = new ArrayList<>();
        lime.combineParent(json, true, true).entrySet().forEach(kv -> npc_list.put(kv.getKey(), new NPCObject(kv.getKey(), kv.getValue().getAsJsonObject())));
        npc_list.forEach((k,v) -> skins.add(v.skin));
        Skins.addSkins(skins, () -> {
            NPC.npc_list.clear();
            NPC.npc_list.putAll(npc_list);

            Displays.uninitDisplay(NPC_MANAGER);
            Displays.initDisplay(NPC_MANAGER);
        });
    }

    private static class NPCDisplay extends ObjectDisplay<NPCObject, EntityPlayer> {
        public static class ShowNickName implements DrawText.IShow {

            @Override public String getID() { return display.npc.key + ".NPC.NickName"; }
            @Override public boolean filter(Player player) { return display.isFilter(player); }
            @Override public Component text(Player player) {
                List<Component> components = display.npc.getNick(player);
                if (components.isEmpty()) return null;
                Collections.reverse(components);
                return Component.join(JoinConfiguration.newlines(), components);
            }
            @Override public Optional<Integer> parent() { return Optional.of(display.entityID); }
            @Override public Location location() { return location; }
            @Override public double distance() { return 20; }
            @Override public boolean tryRemove() { return false; }

            public final NPCDisplay display;

            public final Location location;

            public ShowNickName(NPCDisplay display) {
                this.display = display;
                this.location = display.lastLocation();
            }
        }
        
        public static class ShowNone implements DrawText.IShow {

            @Override public String getID() { return display.npc.key + ".NPC.None"; }
            @Override public boolean filter(Player player) { return display.isFilter(player); }
            @Override public Component text(Player player) { return Component.empty(); }
            @Override public Optional<Integer> parent() { return Optional.of(display.entityID); }
            @Override public Location location() { return location; }
            @Override public double distance() { return display.getDistance(); }
            @Override public boolean tryRemove() { return false; }

            public final NPCDisplay display;

            public final Location location;

            public ShowNone(NPCDisplay display) {
                this.display = display;
                this.location = display.lastLocation();
            }
        }

        @Override public double getDistance() { return 40; }
        public double getTargetDistance() { return 7; }

        private final NPCObject npc;
        public final List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipment;

        private Location target = null;

        @Override public boolean isFilter(Player player) { return npc.isShow(player.getUniqueId()) && ExtMethods.isPlayerLoaded(player); }
        @Override public Location location() { return getByTarget(target, null); }

        private Location getByTarget(Location target, Double minDistance) {
            Location location = super.location();
            if (target == null || location.getWorld() != target.getWorld()) return location;
            if (minDistance != null && location.distance(target) > minDistance) return location;
            return location.clone().setDirection(target.toVector().subtract(location.toVector()));
        }

        public Stream<DrawText.IShow> nickList() {
            return Stream.of(new ShowNickName(this), new ShowNone(this));
        }

        protected NPCDisplay(NPCObject npc) {
            super(npc.location);
            this.npc = npc;
            this.equipment = ChildEntityDisplay.toPacketData(npc.createEquipment());
            BaseChildDisplay<?, NPCObject, ?> sitParent;
            if (npc.sit) sitParent = preInitDisplay(lime.models.builder().block().display(this));
            else sitParent = null;
            postInit();
            if (sitParent != null) Displays.addPassengerID(sitParent.entityID, this.entityID);
        }
        @Override protected void sendData(Player player, boolean child) {
            PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook relMoveLook = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(entityID, (short)0, (short)0, (short)0, (byte)0, (byte)0, true);
            PacketPlayOutNamedEntitySpawn ppones = new PacketPlayOutNamedEntitySpawn(entity);
            ClientboundPlayerInfoUpdatePacket ppopi_add = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.a.ADD_PLAYER, entity);
            ClientboundPlayerInfoRemovePacket ppopi_del = new ClientboundPlayerInfoRemovePacket(Collections.singletonList(entity.getUUID()));
            PacketPlayOutEntityEquipment ppoee = new PacketPlayOutEntityEquipment(entityID, equipment);

            if (npc.single) {
                PacketPlayOutEntityTeleport movePacket = new PacketPlayOutEntityTeleport(entity);
                PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(entity, (byte) MathHelper.floor((entity.getYRot() % 360.0F) * 256.0F / 360.0F));
                lime.nextTick(() -> PacketManager.sendPackets(player, movePacket, headPacket));
            }

            PacketManager.sendPackets(player, ppopi_add, ppones, relMoveLook);
            PacketPlayOutEntityMetadata packet = getDataWatcherPacket(player).orElse(null);
            lime.once(() -> PacketManager.sendPackets(player, ppopi_add, packet, ppoee), 0.5);
            lime.once(() -> PacketManager.sendPackets(player, ppopi_del, packet), 5);
            super.sendData(player, child);
        }
        public static final byte PLAYER_FULL_PARTS = Arrays.stream(PlayerModelPart.values()).map(PlayerModelPart::getMask).reduce(0, (a, b) -> a | b).byteValue();
        @Override protected EntityPlayer createEntity(Location location) {
            UUID fakePlayerUUID = createUUID();

            WorldServer world = ((CraftWorld)location.getWorld()).getHandle();
            EntityPlayer fakePlayer = new EntityPlayer(
                    ((CraftServer)Bukkit.getServer()).getServer(),
                    world,
                    Skins.setSkinOrDownload(new GameProfile(fakePlayerUUID, ""), npc.skin)
            );
            fakePlayer.getEntityData().set(EntityHuman.DATA_PLAYER_MODE_CUSTOMISATION, PLAYER_FULL_PARTS);

            fakePlayer.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            return fakePlayer;
        }
        @Override protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) {
            dataWatcher.setCustom(EntityHuman.DATA_PLAYER_MODE_CUSTOMISATION, Byte.MAX_VALUE);
            super.editDataWatcher(player, dataWatcher);
        }
        @Override public void update(NPCObject npc, double delta) {
            if (npc.single) {
                Player near = this.getNearShow(getTargetDistance(), p -> p.getGameMode() != GameMode.SPECTATOR);
                target = near == null ? null : near.getLocation();
            } else {
                target = null;
            }

            Location location = this.location();
            if (npc.single) {
                if (location.equals(last_location)) {
                    super.update(npc, delta);
                    this.invokeAll(this::sendDataChild);
                    return;
                }
                entity.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
                super.update(npc, delta);
                PacketPlayOutEntityTeleport movePacket = new PacketPlayOutEntityTeleport(entity);
                PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(entity, (byte) MathHelper.floor((location.getYaw() % 360.0F) * 256.0F / 360.0F));
                this.invokeAll(player -> PacketManager.sendPackets(player, movePacket, headPacket));
            } else {
                super.update(npc, delta);
                this.invokeAll(player -> {
                    Location _location = getByTarget(player.getLocation(), getTargetDistance());
                    WrapperPlayServerEntityTeleport wpset = new WrapperPlayServerEntityTeleport();
                    wpset.setEntityID(entityID);
                    wpset.setX(_location.getX());
                    wpset.setY(_location.getY());
                    wpset.setZ(_location.getZ());
                    wpset.setYaw(_location.getYaw());
                    wpset.setPitch(_location.getPitch());
                    wpset.setOnGround(entity.isOnGround());
                    wpset.sendPacket(player);

                    PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(entity, (byte)MathHelper.floor((_location.getYaw() % 360.0F) * 256.0F / 360.0F));
                    PacketManager.sendPacket(player, headPacket);
                });
            }
            this.invokeAll(this::sendDataChild);
        }
        public void click(Player player, boolean isShift) {
            String menu = npc.menu;
            if (isShift) menu = npc.shift_menu == null ? menu : npc.shift_menu;
            MenuCreator.show(player, menu, Apply.of().add(npc.args));
        }
        @Override public void hide(Player player) {
            super.hide(player);
            ClientboundPlayerInfoRemovePacket ppopi = new ClientboundPlayerInfoRemovePacket(Collections.singletonList(entity.getUUID()));
            PacketManager.sendPackets(player, ppopi);
        }
    }
    private static class NPCManager extends DisplayManager<String, NPCObject, NPCDisplay> implements DrawText.IShowGroup {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }

        @Override public HashMap<String, NPCObject> getData() { return npc_list; }
        @Override public NPCDisplay create(String key, NPCObject npc) { return new NPCDisplay(npc); }

        @Override public Stream<DrawText.IShow> list() { return getDisplays().values().stream().flatMap(NPCDisplay::nickList); }
    }
}





















