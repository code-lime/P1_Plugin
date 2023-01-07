package org.lime.gp.module;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.database.Rows;
import org.lime.gp.display.DisplayManager;
import org.lime.gp.display.Displays;
import org.lime.gp.display.ObjectDisplay;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.gp.player.module.TabManager;
import org.lime.system;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.display.Model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RustBackPack {
    private static final BackPackDisplayManager manager = new BackPackDisplayManager();
    public static core.element create() {
        return core.element.create(RustBackPack.class)
                .withInit(RustBackPack::init);
    }
    private static DrawText.IShowID createShow(String id, BackPackDisplay display, Component component, Vector local) {
        return createShow(id, display, () -> component, local);
    }
    private static DrawText.IShowID createShow(String id, BackPackDisplay display, system.Func0<Component> component, Vector local) {
        return new DrawText.IShowID(id) {
            @Override public boolean filter(Player player) { return true; }
            @Override public Component text(Player player) { return component.invoke(); }
            @Override public Location location() { return display.location().add(local); }
            @Override public double distance() { return 5; }
            @Override public boolean tryRemove() { return false; }
        };
    }

    private static class BackPack {
        private final Marker marker;
        private final UUID owner;
        private final int owner_id;
        public UUID uuid() { return marker.getUniqueId(); }
        public Location location() { return marker.getLocation(); }
        public Optional<UUID> owner() { return Optional.ofNullable(owner); }
        public List<ItemStack> items() {
            return Streams.stream(JManager.get(JsonArray.class, marker.getPersistentDataContainer(), "items", new JsonArray()).iterator()).map(v -> system.loadItem(v.getAsString())).collect(Collectors.toList());
        }
        public void items(List<ItemStack> items) {
            JManager.set(marker.getPersistentDataContainer(), "items", system.json.array().add(items, system::saveItem).build());
        }
        public String getDisplayText() {
            if (owner == null) return "#???";
            Integer id = TabManager.getPayerIDorNull(owner);
            if (id != null) return "<" + id + ">";
            return "#" + (owner_id == -1 ? "???" : (owner_id+""));
        }

        public void dropAndDestory() {
            Items.dropItem(location(), items());
            items(new ArrayList<>());
            backPacks.remove(uuid());
            marker.remove();
        }

        public BackPack(Marker marker) {
            this.marker = marker;
            this.owner = Optional.ofNullable(marker.getPersistentDataContainer().get(JManager.key("owner"), PersistentDataType.STRING)).map(UUID::fromString).orElse(null);
            this.owner_id = marker.getPersistentDataContainer().getOrDefault(JManager.key("owner_id"), PersistentDataType.INTEGER, -1);
        }

        public static BackPack create(Location location, UUID owner, List<ItemStack> items) {
            BackPack backPack = new BackPack(location.getWorld().spawn(location, Marker.class, marker -> {
                marker.getPersistentDataContainer().set(JManager.key("owner"), PersistentDataType.STRING, owner.toString());
                marker.getPersistentDataContainer().set(JManager.key("owner_id"), PersistentDataType.INTEGER, Optional.ofNullable(Rows.UserRow.getBy(owner)).map(user -> user.ID).orElse(-1));
                marker.addScoreboardTag("back_pack");
            }));
            backPack.items(items);
            backPacks.put(backPack.uuid(), backPack);
            return backPack;
        }
    }
    private static final HashMap<UUID, BackPack> backPacks = new HashMap<>();

    public static void update() {
        HashMap<UUID, Marker> backPacks = new HashMap<>();
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(Marker.class).forEach(marker -> {
            if (!marker.getScoreboardTags().contains("back_pack")) return;
            backPacks.put(marker.getUniqueId(), marker);
        }));
        RustBackPack.backPacks.keySet().removeIf(v -> backPacks.remove(v) == null);
        backPacks.forEach((uuid, marker) -> RustBackPack.backPacks.put(uuid, new BackPack(marker)));
    }
    public static void init() {
        lime.repeat(RustBackPack::update, 1);
        //CustomMeta.loadMeta(CustomMeta.MetaLoader.create(BackPackMeta.class, CustomMeta.LoadedEntity.class));
        Displays.initDisplay(manager);
        final Component TEXT_UNDERLINE = ChatHelper.formatComponent("</>[<YELLOW>Shift+ПКМ</>]");
        DrawText.load(() -> manager.getDisplays().entrySet().stream().flatMap(kv -> Stream.of(
                createShow(kv.getKey() + ".0", kv.getValue(), () -> Component.text(kv.getValue().meta.getDisplayText()), new Vector(0, 0.7, 0)),
                createShow(kv.getKey() + ".1", kv.getValue(), TEXT_UNDERLINE, new Vector(0, 0.5, 0))
        )));
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
                        lime.invokeSync(() -> on(e));
                    }
                });
            }
        });
    }

    public static void on(PlayerUseUnknownEntityEvent e) {
        if (e.isAttack()) return;
        if (!e.getPlayer().isSneaking()) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Model.ChildDisplay<?> display = Displays.byID(Model.ChildDisplay.class, e.getEntityId());
        if (display == null) return;
        if (!(display.objectParent() instanceof BackPackDisplay bp)) return;
        bp.meta.dropAndDestory();
    }
    public static void dropItems(Player player, Location location, List<ItemStack> items) {
        BackPack.create(location, player.getUniqueId(), items);
    }

    public static class BackPackDisplay extends ObjectDisplay<BackPack, net.minecraft.world.entity.Marker> {
        @Override public double getDistance() { return 30; }
        @Override public Location location() { return meta.location(); }

        public BackPack meta;

        protected BackPackDisplay(BackPack meta) {
            this.meta = meta;
            Model.Variable.BACK_PACK.get().ifPresent(model -> preInitDisplay(model.display(this)));
            postInit();
        }

        @Override protected net.minecraft.world.entity.Marker createEntity(Location location) {
            return new net.minecraft.world.entity.Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
        }
    }
    public static class BackPackDisplayManager extends DisplayManager<UUID, BackPack, BackPackDisplay> {
        @Override public boolean isFast() { return true; }

        @Override public Map<UUID, BackPack> getData() { return backPacks; }//CustomMeta.LoadedEntity.allReadOnly(BackPackMeta.class).stream().collect(Collectors.toMap(k -> k.getLoaded().getUniqueId(), v -> v)); }
        @Override public BackPackDisplay create(UUID uuid, BackPack meta) { return new BackPackDisplay(meta); }
    }
}




















