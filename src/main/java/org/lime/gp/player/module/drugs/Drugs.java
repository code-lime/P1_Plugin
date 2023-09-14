package org.lime.gp.player.module.drugs;

import com.comphenix.protocol.events.PacketContainer;
import com.google.gson.JsonElement;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.display.EditedDataWatcher;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.DrugsSetting;
import org.lime.gp.item.settings.list.UnDrugsSetting;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.module.PlayerData;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Drugs implements Listener {
    public static final HashMap<UUID, GroupEffect> players = new HashMap<>();

    public static CoreElement create() {
        return CoreElement.create(Drugs.class)
                .withInstance()
                .withInit(Drugs::init)
                .withUninit(Drugs::uninit);
    }
    private static void regive(Player player, PlayerInventory inventory, EquipmentSlot slot, Func1<ItemStack, Boolean> filter) {
        Optional.of(inventory.getItem(slot))
                .filter(v -> !v.getType().isAir())
                .filter(filter::invoke)
                .ifPresent(item -> {
                    inventory.setItem(slot, new ItemStack(Material.STONE, 1));
                    Items.dropGiveItem(player, item, false);
                    inventory.setItem(slot, new ItemStack(Material.AIR));
                });
    }
    public static final ConcurrentHashMap<Integer, Integer> freezeList = new ConcurrentHashMap<>();
    private static final NamespacedKey DRUGS_EFFECTS = new NamespacedKey(lime._plugin, "drugs_effects");

    public static void lockArmsTick(Player player) {
        lockArmsTick(player, v -> true);
    }
    public static void lockArmsTick(Player player, PlayerInventory inventory) {
        lockArmsTick(player, inventory, v -> true);
    }

    public static void lockArmsTick(Player player, Func1<ItemStack, Boolean> filter) {
        if (player == null) return;
        lockArmsTick(player, player.getInventory(), filter);
    }
    public static void lockArmsTick(Player player, PlayerInventory inventory, Func1<ItemStack, Boolean> filter) {
        if (player == null || inventory == null) return;
        regive(player, inventory, EquipmentSlot.OFF_HAND, filter);
        regive(player, inventory, EquipmentSlot.HAND, filter);
    }

    public static void init() {
        PacketManager.adapter()
                .add(PacketPlayOutEntityMetadata.class, (packet, event) -> {
                    int id = event.getPlayer().getEntityId();
                    if (packet.id() == id) return;
                    if (freezeList.containsKey(packet.id())) {
                        List<DataWatcher.b<?>> data = new ArrayList<>(packet.packedItems());
                        data.removeIf(item -> item.id() == EditedDataWatcher.DATA_TICKS_FROZEN.getId());
                        data.add(new DataWatcher.Item<>(EditedDataWatcher.DATA_TICKS_FROZEN, 10000).value());
                        packet = new PacketPlayOutEntityMetadata(packet.id(), data);
                        event.setPacket(new PacketContainer(event.getPacketType(), packet));
                    }
                })
                .listen();
        AnyEvent.addEvent("drugs.effect.reset", AnyEvent.type.other, p -> getGroupEffect(p.getUniqueId()).reset());
        lastExecute = System.currentTimeMillis();
        lime.repeatTicks(Drugs::update, 1);
    }
    private static long lastExecute;
    private static void update() {
        long execute = System.currentTimeMillis();
        double deltaMin = ((execute - lastExecute) / 1000.0) / 60.0;
        lastExecute = execute;
        int tick = MinecraftServer.currentTick;
        freezeList.entrySet().forEach(kv -> kv.setValue(kv.getValue() - 1));
        freezeList.entrySet().removeIf(kv -> {
            if (kv.getValue() > 0) return false;
            Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(v -> v.getEntityId() == kv.getKey())
                    .findAny()
                    .ifPresent(player -> player.setFreezeTicks(player.getFreezeTicks() + 1));
            return true;
        });
        ConcurrentHashMap<UUID, Player> onlinePlayers = EntityPosition.onlinePlayers;
        players.entrySet().removeIf(kv -> {
            UUID uuid = kv.getKey();
            Player player = onlinePlayers.get(uuid);
            if (player == null) {
                PlayerData.getPlayerData(uuid).setJson(DRUGS_EFFECTS, kv.getValue().save());
                return true;
            }
            if (kv.getValue().tickRemove(player, deltaMin)) PlayerData.getPlayerData(uuid).setJson(DRUGS_EFFECTS, kv.getValue().save());
            else kv.getValue().applyEffect(player, tick);
            return false;
        });
        onlinePlayers.keySet().forEach(Drugs::getGroupEffect);
    }
    public static void uninit() {
        players.entrySet().removeIf(kv -> {
            PlayerData.getPlayerData(kv.getKey()).setJson(DRUGS_EFFECTS, kv.getValue().save());
            return true;
        });
    }
    public static GroupEffect getGroupEffect(UUID uuid) {
        return players.computeIfAbsent(uuid, _uuid -> Optional.ofNullable(PlayerData.getPlayerData(_uuid).getJson(DRUGS_EFFECTS))
                .map(JsonElement::getAsJsonObject)
                .map(GroupEffect::new)
                .orElseGet(GroupEffect::new));
    }

    @EventHandler(ignoreCancelled = true) public static void on(PlayerItemConsumeEvent e) {
        Items.getOptional(DrugsSetting.class, e.getItem())
                .ifPresent(drugs -> getGroupEffect(e.getPlayer().getUniqueId())
                        .addEffect(drugs.creator().getID(), drugs.addiction, drugs.first, drugs.last));
        Items.getOptional(UnDrugsSetting.class, e.getItem())
                .ifPresent(undrugs -> getGroupEffect(e.getPlayer().getUniqueId())
                        .setHealthTimer(undrugs.time));
    }
}














