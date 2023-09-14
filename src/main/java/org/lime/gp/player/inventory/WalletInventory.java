package org.lime.gp.player.inventory;

import com.google.gson.JsonArray;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Slot;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.WalletSetting;
import org.lime.gp.lime;
import org.lime.gp.player.module.PlayerData;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.lime.system.utils.ItemUtils;

import java.util.*;

public final class WalletInventory implements Listener {
    public static CoreElement create() {
        return CoreElement.create(WalletInventory.class)
                .withInstance()
                .withInit(WalletInventory::init)
                .withUninit(WalletInventory::uninit);
    }

    public static boolean filterWallet(ItemStack item) {
        return item.getType() == Material.PAPER || Items.has(WalletSetting.class, item);
    }
    public static boolean filterWallet(net.minecraft.world.item.ItemStack item) {
        return item.is(net.minecraft.world.item.Items.PAPER) || Items.has(WalletSetting.class, CraftItemStack.asBukkitCopy(item));
    }
    private static boolean openFilterInventory(Player player, CraftInventory inventory, boolean readonly, Func1<net.minecraft.world.item.ItemStack, net.minecraft.world.item.ItemStack> filter, Func1<net.minecraft.world.item.ItemStack, Boolean> click) {
        player.closeInventory();
        return InterfaceManager.of(player, inventory)
                .slots(slot -> {
                    Slot _slot = slot;
                    if (!readonly) return InterfaceManager.filterSlot(slot, WalletInventory::filterWallet);
                    Slot out = new Slot(slot.container, slot.slot, slot.x, slot.y) {
                        @Override public net.minecraft.world.item.ItemStack getItem() { return filter.invoke(super.getItem()); }
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                        @Override public boolean mayPickup(EntityHuman playerEntity) {
                            if (click.invoke(_slot.getItem())) _slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                            return false;
                        }
                    };
                    out.index = slot.index;
                    return out;
                })
                .open();
    }

    private static JsonArray saveItems(List<ItemStack> items) {
        JsonArray arr = new JsonArray();
        for (ItemStack item : items)
            arr.add(item != null && item.getType() != Material.AIR ? ItemUtils.saveItem(item) : null);
        return arr;
    }
    private static List<ItemStack> loadItems(JsonArray json) {
        List<ItemStack> items = new ArrayList<>();
        json.forEach(item -> items.add(item == null || item.isJsonNull() ? new ItemStack(Material.AIR) : ItemUtils.loadItem(item.getAsString())));
        int size = items.size();
        for (int i = 0; i < 9 - size; i++) items.add(new ItemStack(Material.AIR));
        return items;
    }

    private static final NamespacedKey WALLET_KEY = new NamespacedKey(lime._plugin, "wallet");

    private static void writeItems(UUID uuid, List<ItemStack> items) {
        PlayerData.getPlayerData(uuid).set(WALLET_KEY, LimePersistentDataType.JSON_ARRAY, saveItems(items));
    }
    private static void writeWallet(UUID uuid, Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory) items.add(item == null ? new ItemStack(Material.AIR) : item);
        writeItems(uuid, items);
    }
    private static List<ItemStack> readItems(UUID uuid) {
        return Optional.ofNullable(PlayerData.getPlayerData(uuid).get(WALLET_KEY, LimePersistentDataType.JSON_ARRAY)).map(WalletInventory::loadItems).orElseGet(Collections::emptyList);
    }
    private static CraftInventory readWallet(UUID uuid) {
        List<ItemStack> items = readItems(uuid);
        CraftInventoryCustom inventory = new CraftInventoryCustom(null, 9, LangMessages.Message.Phone_Wallet_Title.getSingleMessage());
        inventory.setContents(items.toArray(ItemStack[]::new));
        return inventory;
    }

    private static final HashMap<UUID, CraftInventory> inventoryWallets = new HashMap<>();
    private static void tick() {
        inventoryWallets.entrySet().removeIf(kv -> {
            if (kv.getValue().getViewers().size() != 0) return false;
            writeWallet(kv.getKey(), kv.getValue());
            return true;
        });
    }

    public static boolean openWallet(Player player) {
        return openWallet(player, player.getUniqueId());
    }
    public static boolean openWallet(Player player, UUID wallet) {
        return openWalletTarget(player, wallet, false, item -> item, item -> false);
    }
    public static boolean openWalletTarget(Player player, UUID wallet, boolean readonly, Func1<net.minecraft.world.item.ItemStack, net.minecraft.world.item.ItemStack> filter, Func1<net.minecraft.world.item.ItemStack, Boolean> click) {
        return openFilterInventory(player, inventoryWallets.compute(wallet, (uuid, inv) -> inv == null ? readWallet(uuid) : inv), readonly, filter, click);
    }

    private static void init() {
        AnyEvent.addEvent("wallet.open", AnyEvent.type.owner, b -> b.createParam(UUID::fromString, "[uuid]"), WalletInventory::openWallet);
        lime.repeat(WalletInventory::tick, 0.1);
    }
    public static void uninit() {
        inventoryWallets.values().forEach(CraftInventory::close);
        tick();
    }

    public static List<ItemStack> tryAddToWallet(Player player, ItemStack item) {
        if (!filterWallet(item)) return Collections.singletonList(item);
        UUID uuid = player.getUniqueId();
        Inventory inventory = inventoryWallets.getOrDefault(uuid, null);
        if (inventory != null) return new ArrayList<>(inventory.addItem(item).values());
        inventory = readWallet(uuid);
        List<ItemStack> out = new ArrayList<>(inventory.addItem(item).values());
        writeWallet(uuid, inventory);
        return out;
    }
    public static boolean dropDieItems(Player player, Location location, Action1<ItemStack> callback) {
        return false;
    }
}

















