package org.lime.gp.player.inventory;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.list.SlotsSetting;
import org.lime.gp.lime;
import org.lime.gp.player.menu.LangEnum;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.module.Ghost;
import org.lime.gp.player.module.cinv.CreativeInventory;
import org.lime.gp.player.module.cinv.ViewContainer;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Action2;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import java.util.HashMap;
import java.util.UUID;

public final class MainPlayerInventory implements Listener {
    private static boolean PREDONATE_SLOT_ALWAYS = false;

    public static CoreElement create() {
        return CoreElement.create(MainPlayerInventory.class)
                .withInit(MainPlayerInventory::init)
                .withInstance()
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("predonate.slot.always")
                        .withDefault(new JsonPrimitive(false))
                        .withInvoke(json -> PREDONATE_SLOT_ALWAYS = json.getAsBoolean())
                )
                .<JsonObject>addConfig("slot_counter", v -> v.withInvoke(MainPlayerInventory::reloadConfig).withDefault(new JsonObject()));
    }

    private static final HashMap<Material, Integer> slotCounter = new HashMap<>();
    private static int defaultCount = 0;
    private static final ItemStack CINV = CraftItemStack.asBukkitCopy(ViewContainer.of(
                    Component.text("CINV")
                            .decoration(TextDecoration.ITALIC, false)
                            .color(NamedTextColor.YELLOW),
                    1,
                    Component.text("[ЛКМ] - Открыть список")
                            .decoration(TextDecoration.ITALIC, false)
                            .color(NamedTextColor.GOLD),
                    Component.text("[Q] - Открыть поиск")
                            .decoration(TextDecoration.ITALIC, false)
                            .color(NamedTextColor.GOLD)
            )
    );
    private static boolean isCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (Cooldown.hasCooldown(uuid, "MainPlayerInventory.click")) return true;
        Cooldown.setCooldown(uuid, "MainPlayerInventory.click", 0.1);
        return false;
    }
    public static void init() {
        clickData.put(22, new ClickSlot(null, player -> {
            if (isCooldown(player)) return;
            lime.nextTick(() -> {
                if (!UserRow.hasBy(player.getUniqueId()) || !(PREDONATE_SLOT_ALWAYS || Tables.PREDONATE_ITEMS_TABLE.getBy(v -> v.uuid.equals(player.getUniqueId())).isPresent())) return;
                player.closeInventory();
                MenuCreator.show(player, "phone.predonate");
            });
        }) {
            private static ItemStack PRE_DONATE = null;
            @Override public ItemStack create(Player player) {
                return UserRow.hasBy(player.getUniqueId()) && (PREDONATE_SLOT_ALWAYS || Tables.PREDONATE_ITEMS_TABLE.getBy(v -> v.uuid.equals(player.getUniqueId())).isPresent())
                        ? PRE_DONATE == null ? (PRE_DONATE = MainPlayerInventory.createPreDonate()) : PRE_DONATE
                        : MainPlayerInventory.createBarrier(false);
            }
        });
        clickData.put(23, new ClickSlot(Items.createItem("phone.cinv").orElse(CINV), (player, clickType) -> {
            if (isCooldown(player)) return;
            lime.nextTick(() -> {
                if (clickType == ClickType.DROP) CreativeInventory.openSearch(player, false);
                else CreativeInventory.open(player);
            });
        }));
        clickData.put(24, new ClickSlot(Items.createItem("phone.off").orElseThrow(), player -> {
            if (isCooldown(player)) return;
            lime.nextTick(() -> {
                player.closeInventory();
                MenuCreator.show(player, "phone.main");
            });
        }));
        clickData.put(25, new ClickSlot(Items.createItem("phone.on").orElseThrow(), player -> {
            if (isCooldown(player)) return;
            lime.nextTick(() -> {
                player.closeInventory();
                if (MenuCreator.show(player, "phone.main")) {
                    MenuCreator.showLang(player, LangEnum.ME, Apply.of().add("key", "PHONE"));
                }
            });
        }));
        clickData.put(26, new ClickSlot(Items.createItem("wallet.full").orElseThrow(), player -> {
            if (isCooldown(player)) return;
            lime.nextTick(() -> {
                if (WalletInventory.openWallet(player)) {
                    MenuCreator.showLang(player, LangEnum.ME, Apply.of().add("key", "WALLET"));
                }
            });
        }));
        lime.repeat(MainPlayerInventory::update, 0.5);
    }
    public static void reloadConfig(JsonObject json) {
        defaultCount = 0;
        slotCounter.clear();
        json.entrySet().forEach(kv -> {
            if (kv.getKey().equals("DEFAULT")) {
                defaultCount = kv.getValue().getAsInt();
            } else {
                slotCounter.put(Material.valueOf(kv.getKey()), kv.getValue().getAsInt());
            }
        });
    }
    public static void update() {
        Bukkit.getOnlinePlayers().forEach(MainPlayerInventory::updateInventory);
        backgrounds.entrySet().removeIf(kv -> Bukkit.getPlayer(kv.getKey()) == null);
    }

    private static final HashMap<UUID, Toast2<Inventory, ItemStack>> backgrounds = new HashMap<>();
    public static void setBackground(Player player, Inventory inventory, ItemStack background) {
        player.getInventory().setItem(13, background);
        backgrounds.put(player.getUniqueId(), Toast.of(inventory, background));
    }
    public static void clearBackground(Player player) {
        backgrounds.remove(player.getUniqueId());
        updateInventory(player);
    }

    private static final HashMap<Integer, ClickSlot> clickData = new HashMap<>();
    private static class ClickSlot {
        private final ItemStack item;
        private final Action2<Player, ClickType> callback;
        public ClickSlot(ItemStack item, Action1<Player> callback) {
            this(item, (p,c) -> callback.invoke(p));
        }
        public ClickSlot(ItemStack item, Action2<Player, ClickType> callback) {
            this.item = item;
            this.callback = callback;
        }
        public ItemStack create(Player player) { return item.clone(); }
        public void onClick(Player player, ClickType clickType) { callback.invoke(player, clickType); }
    }
    private static int getSlotCounter(ItemStack item) {
        if (item == null) return 0;
        return Items.getItemCreator(item)
                .map(i -> i instanceof ItemCreator c ? c : null)
                .map(creator -> creator.getOptional(SlotsSetting.class).map(v -> v.slots).orElse(0))
                .orElseGet(() -> slotCounter.getOrDefault(item.getType(), 0));
    }

    private interface ISlot {
        ItemStack invoke(int slot, ItemStack item, boolean isBarrier, Player player, int slots);
    }
    private interface ILockSlot extends ISlot {
        static ILockSlot create(ILockSlot action) {
            return action;
        }
    }

    private static final HashMap<Integer, ISlot> slots = new HashMap<>();
    static {
        final ItemStack AIR = new ItemStack(Material.AIR);
        ISlot def_slot = (slot, item, isBarrier, player, slots) -> {
            if (slot < slots) return isBarrier ? AIR : null;
            if (isBarrier) return null;
            if (item != null && item.getType() != Material.AIR) {
                Location location = player.getLocation();
                location.getWorld().dropItemNaturally(location, item);
            }
            return createBarrier(true);
        };
        ISlot def_offset_slot = (slot, item, isBarrier, player, slots) -> def_slot.invoke(slot - 18, item, isBarrier, player, slots);
        ILockSlot lock_slot = (slot, item, isBarrier, player, slots) -> {
            if (Ghost.isGhost(player)) return isBarrier ? null : MainPlayerInventory.createBarrier(false);

            if (slot == 23 || slot == 24) return player.isOp() ? clickData.get(slot).create(player) : (isBarrier ? null : MainPlayerInventory.createBarrier(false));
            ClickSlot _slot = clickData.getOrDefault(slot, null);
            if (_slot == null || !UserRow.hasBy(player.getUniqueId())) {
                return isBarrier ? null : MainPlayerInventory.createBarrier(false);
            }
            return _slot.create(player);
        };

        for (int i = 0; i < 9; i++) slots.put(i, def_slot);
        for (int i = 9; i < 36; i++) slots.put(i, lock_slot);
        slots.put(13, ILockSlot.create((slot, item, isBarrier, player, slots) -> {
            UUID uuid = player.getUniqueId();
            Toast2<Inventory, ItemStack> _slot = backgrounds.getOrDefault(uuid, null);
            if (_slot == null) return lock_slot.invoke(slot, item, isBarrier, player, slots);
            if (player.getOpenInventory().getTopInventory() == _slot.val0) return _slot.val1;
            backgrounds.remove(uuid);
            return null;
        }));
        for (int i = 27; i < 36; i++) slots.put(i, def_offset_slot);
    }

    public static void updateInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        int slots = player.getScoreboardTags().contains("inventory.full") ? 18 : defaultCount;
        for (ItemStack item : inventory.getArmorContents()) slots += getSlotCounter(item);
        if (slots > 18) slots = 18;
        else if (slots < 1) slots = 1;

        int _slots = slots;
        MainPlayerInventory.slots.forEach((slot, islot) -> {
            ItemStack item = inventory.getItem(slot);
            ItemStack _item = item;
            item = islot.invoke(slot, item, checkBarrier(item), player, _slots);
            if (item == null || item.equals(_item)) return;
            inventory.setItem(slot, item);
        });

        ItemStack offhand = inventory.getItem(EquipmentSlot.OFF_HAND);
        if (checkBarrier(offhand)) inventory.setItem(EquipmentSlot.OFF_HAND, null);
        if (inventory.getHeldItemSlot() >= slots) inventory.setHeldItemSlot(0);
    }

    private static IItemCreator barrier_main = null;
    private static IItemCreator barrier_belt = null;

    //private static final ItemStack barrier = ItemManager.Builder;//lime.CreateItem(Material.BARRIER, ChatColor.RED + "НЕДОСТУПНО", 1);
    public static ItemStack createBarrier(boolean is_belt) {
        if (barrier_main == null || barrier_main.isDestroy) barrier_main = Items.getItemCreator("Inventory.Barrier").orElseThrow();
        if (barrier_belt == null || barrier_belt.isDestroy) barrier_belt = Items.getItemCreator("Inventory.Barrier.Belt").orElseThrow();
        return (is_belt ? barrier_belt : barrier_main).createItem(1);
    }

    private static IItemCreator predonate = null;

    public static ItemStack createPreDonate() {
        if (predonate == null || predonate.isDestroy) predonate = Items.getItemCreator("Inventory.PreDonate").orElseThrow();
        return predonate.createItem(1);
    }

    public static boolean checkBarrier(net.minecraft.world.item.ItemStack item) {
        return Items.getKeyByItem(item).filter(v -> v.startsWith("Inventory.Barrier")).isPresent();
    }
    public static boolean checkBarrier(ItemStack item) {
        return Items.getKeyByItem(item).filter(v -> v.startsWith("Inventory.Barrier")).isPresent();
    }
    @EventHandler public void onSlot(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        if (!checkBarrier(inventory.getItem(e.getNewSlot()))) return;
        if (checkBarrier(inventory.getItem(e.getPreviousSlot()))) inventory.setHeldItemSlot(0);
        e.setCancelled(true);
    }
    @EventHandler public void onSwap(PlayerSwapHandItemsEvent e) {
        if (checkBarrier(e.getMainHandItem()) || checkBarrier(e.getOffHandItem()))
            e.setCancelled(true);
    }
    @EventHandler public void onDrop(PlayerDropItemEvent e) {
        if (checkBarrier(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            return;
        }
        Player player = e.getPlayer();
        if (player.isOp() && e.getItemDrop().getItemStack().isSimilar(CINV)) {
            e.getItemDrop().remove();
            CreativeInventory.openSearch(player, false);
            isCooldown(player);
        }
    }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if (checkBarrier(e.getCurrentItem())) {
            e.setCancelled(true);
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getHotbarButton() != -1 && checkBarrier(player.getInventory().getItem(e.getHotbarButton()))) {
            e.setCancelled(true);
            return;
        }
        if (e.getClickedInventory() == player.getInventory()) {
            int slot = e.getSlot();
            ISlot _slot = slots.getOrDefault(slot, null);
            if (_slot instanceof ILockSlot) {
                ClickSlot clickSlot = clickData.getOrDefault(slot, null);
                if (clickSlot != null) clickSlot.onClick(player, e.getClick());
                e.setCancelled(true);
            }
        }
    }
}





















