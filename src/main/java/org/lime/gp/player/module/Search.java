package org.lime.gp.player.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import github.scarsz.discordsrv.dependencies.google.common.collect.Streams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.Slot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.gp.database.Rows.UserRow;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.Vest;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.inventory.MainPlayerInventory;
import org.lime.gp.player.inventory.WalletInventory;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.system;
import org.lime.display.lime;

import java.util.*;

public class Search implements Listener {
    private static Items.Checker checker = Items.Checker.empty();
    public static core.element create() {
        return core.element.create(Search.class)
                .withInit(Search::init)
                .<JsonArray>addConfig("config", v -> v
                        .withParent("weapon.search")
                        .withDefault(new JsonArray())
                        .withInvoke(_v -> checker = Items.createCheck(Streams.stream(_v.iterator()).map(JsonElement::getAsString).toList()))
                )
                .withInstance();
    }
    public static void init() {
        AnyEvent.addEvent("weapon.search", AnyEvent.type.other, v -> v.createParam(UUID::fromString, "[uuid]"), (player, other_uuid) -> {
            Player other = Bukkit.getPlayer(other_uuid);
            if (other == null) return;
            search(player, other, false, checker::check);
        });
        AnyEvent.addEvent("inventory.open", AnyEvent.type.owner, v -> v.createParam(UUID::fromString, "[uuid]"), Search::searchAdmin);
    }

    private static boolean inDistance(Location loc1, Location loc2, double distance) { return loc1.getWorld() == loc2.getWorld() && loc1.toVector().distance(loc2.toVector()) < distance; }

    public static void search(Player player, Player other, boolean readonly, system.Func1<net.minecraft.world.item.ItemStack, Boolean> filter) {
        Apply _other = Apply.of().add("other_uuid", other.getUniqueId().toString());
        MenuCreator.show(player, "lang.search", _other);

        ReadonlyInventory view = ReadonlyInventory.ofNMS(NonNullList.withSize(4 * 9, net.minecraft.world.item.ItemStack.EMPTY));

        EntityPlayer target_player = ((CraftPlayer)other).getHandle();
        net.minecraft.world.entity.player.PlayerInventory target_inventory = target_player.getInventory();

        ((CraftPlayer)player).getHandle().openMenu(new TileInventory((syncId, inventory, target) -> new ContainerChest(Containers.GENERIC_9x4, syncId, inventory, view, 4) {
            private static ItemStack loreSearch(ItemStack item, Component title, Component... lore) {
                ItemMeta meta = item.getItemMeta();
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.displayName(title);
                meta.lore(List.of(lore));
                item.setItemMeta(meta);
                return item;
            }
            private static ItemStack loreSearch(ItemStack item) {
                return loreSearch(item, Component.text("Обыск элемента"), ChatHelper.formatComponent("<YELLOW>[ПКМ] <GRAY>Обыскать элемент"));
            }
            private static final net.minecraft.world.item.ItemStack NONE_SLOT = CraftItemStack.asNMSCopy(MainPlayerInventory.createBarrier(false));
            private static final net.minecraft.world.item.ItemStack NON_FILTER_SLOT = Optional.of(Material.BARRIER)
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v, Component.text("Недоступно").color(NamedTextColor.RED), ChatHelper.formatComponent("<GRAY>Предмет недоступен для конфискации")))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack WALLET_SLOT = Items.createItem("wallet.full")
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack HELMET = Optional.of(Material.IRON_HELMET)
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack CHESTPLATE = Optional.of(Material.IRON_CHESTPLATE)
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack LEGGINGS = Optional.of(Material.IRON_LEGGINGS)
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack BOOTS = Optional.of(Material.IRON_BOOTS)
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private boolean clickDrop(net.minecraft.world.item.ItemStack item, boolean readonly) {
                if (item.isEmpty() || readonly || MainPlayerInventory.checkBarrier(item) || !filter.invoke(item)) return false;
                Location location = other.getLocation();
                ItemStack move_item = item.asBukkitCopy();
                CoreProtectHandle.logDrop(location, other, move_item);
                Items.dropGiveItem(player, move_item, true);
                MenuCreator.show(player, "lang.search.item", _other);
                item.setCount(0);
                return false;
            }
            private boolean drop(net.minecraft.world.item.ItemStack item, boolean readonly) {
                if (item.isEmpty() || readonly || MainPlayerInventory.checkBarrier(item) || !filter.invoke(item)) return false;
                Location location = other.getLocation();
                ItemStack move_item = item.asBukkitCopy();
                CoreProtectHandle.logDrop(location, other, move_item);
                Items.dropGiveItem(player, move_item, true);
                MenuCreator.show(player, "lang.search.item", _other);
                return true;
            }
            private net.minecraft.world.item.ItemStack itemFilter(net.minecraft.world.item.ItemStack item) {
                return item.isEmpty() || filter.invoke(item) ? item : NON_FILTER_SLOT;
            }

            @Override protected Slot addSlot(Slot slot) {
                if (slot.container == view) {
                    return super.addSlot(new InterfaceManager.AbstractBaseSlot(slot) {
                        @Override public net.minecraft.world.item.ItemStack getItem() {
                            return switch (getRowY()) {
                                case 0 -> switch (getRowX()) {
                                    case 0 -> BOOTS;
                                    case 1 -> LEGGINGS;
                                    case 2 -> CHESTPLATE;
                                    case 3 -> HELMET;
                                    case 4 -> WALLET_SLOT;
                                    case 8 -> Optional.of(target_player.containerMenu.getCarried()).filter(v -> !v.isEmpty()).map(v -> itemFilter(v)).orElse(NONE_SLOT);
                                    default -> NONE_SLOT;
                                };
                                case 1 -> switch (getRowX()) {
                                    case 0,1,2,3 -> itemFilter(target_inventory.armor.get(getRowX()));
                                    case 4 -> itemFilter(target_inventory.offhand.get(0));
                                    case 5,6,7,8 -> itemFilter(target_player.inventoryMenu.getCraftSlots().getItem(getRowX() - 5));
                                    default -> NONE_SLOT;
                                };
                                case 2 -> itemFilter(target_inventory.items.get(getRowX() + 9 * 3));
                                case 3 -> itemFilter(target_inventory.items.get(getRowX()));
                                default -> NONE_SLOT;
                            };
                        }
                        @Override public boolean mayPickup(EntityHuman playerEntity) {
                            switch (getRowY()) {
                                case 0 -> {
                                    switch (getRowX()) {
                                        case 0 -> Vest.openVestTarget(player, other, EquipmentSlot.FEET, true, item -> itemFilter(item), item -> drop(item, readonly));
                                        case 1 -> Vest.openVestTarget(player, other, EquipmentSlot.LEGS, true, item -> itemFilter(item), item -> drop(item, readonly));
                                        case 2 -> Vest.openVestTarget(player, other, EquipmentSlot.CHEST, true, item -> itemFilter(item), item -> drop(item, readonly));
                                        case 3 -> Vest.openVestTarget(player, other, EquipmentSlot.HEAD, true, item -> itemFilter(item), item -> drop(item, readonly));
                                        case 4 -> WalletInventory.openWalletTarget(player, other.getUniqueId(), true, item -> itemFilter(item), item -> drop(item, readonly));
                                        case 8 -> clickDrop(target_player.containerMenu.getCarried(), readonly);
                                    }
                                }
                                case 1 -> {
                                    switch (getRowX()) {
                                        case 0, 1, 2, 3 -> clickDrop(target_inventory.armor.get(getRowX()), readonly);
                                        case 4 -> clickDrop(target_inventory.offhand.get(0), readonly);
                                        case 5,6,7,8 -> clickDrop(target_player.inventoryMenu.getCraftSlots().getItem(getRowX() - 5), readonly);
                                    }
                                }
                                case 2 -> clickDrop(target_inventory.items.get(getRowX() + 9 * 3), readonly);
                                case 3 -> clickDrop(target_inventory.items.get(getRowX()), readonly);
                            }
                            return false;
                        }
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                    });
                }
                return InterfaceManager.AbstractSlot.noneInteractSlot(super.addSlot(slot));
            }
            @Override public boolean stillValid(EntityHuman _player) {
                if (!this.checkReachable) return true;
                return other.isOnline() && player.isOnline() && inDistance(player.getLocation(), other.getLocation(), 5);
            }
        }, ChatHelper.toNMS(Component.text("Вещи человека" + (readonly ? " *просмотр*" : "")))));
    }
    public static void searchAdmin(Player player, UUID other_uuid) {
        ReadonlyInventory view = ReadonlyInventory.ofNMS(NonNullList.withSize(1 * 9, net.minecraft.world.item.ItemStack.EMPTY));
        
        ((CraftPlayer)player).getHandle().openMenu(new TileInventory((syncId, inventory, target) -> new ContainerChest(Containers.GENERIC_9x1, syncId, inventory, view, 1) {
            private static ItemStack lore(ItemStack item, Component title, Component... lore) {
                ItemMeta meta = item.getItemMeta();
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.displayName(title);
                meta.lore(List.of(lore));
                item.setItemMeta(meta);
                return item;
            }
            private static ItemStack loreSearch(ItemStack item) {
                return lore(item, Component.text("Открыть инвентарь"), ChatHelper.formatComponent("<YELLOW>[ПКМ] <GRAY>Открыть инвентарь"));
            }
            private static final net.minecraft.world.item.ItemStack NONE_SLOT = CraftItemStack.asNMSCopy(MainPlayerInventory.createBarrier(false));
            private static final net.minecraft.world.item.ItemStack EMPTY_SLOT = Optional.of(Material.BARRIER)
                    .map(ItemStack::new)
                    .map(v -> lore(v, Component.text("Пустой слот"), ChatHelper.formatComponent("<AQUA>В данном слоте отсутствует инвентарь")))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack WALLET_SLOT = Items.createItem("wallet.full")
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack HELMET = Optional.of(Material.IRON_HELMET)
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack CHESTPLATE = Optional.of(Material.IRON_CHESTPLATE)
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack LEGGINGS = Optional.of(Material.IRON_LEGGINGS)
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();
            private static final net.minecraft.world.item.ItemStack BOOTS = Optional.of(Material.IRON_BOOTS)
                    .map(ItemStack::new)
                    .map(v -> loreSearch(v))
                    .map(CraftItemStack::asNMSCopy)
                    .get();

            @Override protected Slot addSlot(Slot slot) {
                if (slot.container == view) {
                    return super.addSlot(new InterfaceManager.AbstractBaseSlot(slot) {
                        @Override public net.minecraft.world.item.ItemStack getItem() {
                            return switch (getRowY()) {
                                case 0 -> switch (getRowX()) {
                                    case 0 -> Vest.hasVest(player, other_uuid, EquipmentSlot.FEET) ? BOOTS : EMPTY_SLOT;
                                    case 1 -> Vest.hasVest(player, other_uuid, EquipmentSlot.LEGS) ? LEGGINGS : EMPTY_SLOT;
                                    case 2 -> Vest.hasVest(player, other_uuid, EquipmentSlot.CHEST) ? CHESTPLATE : EMPTY_SLOT;
                                    case 3 -> Vest.hasVest(player, other_uuid, EquipmentSlot.HEAD) ? HELMET : EMPTY_SLOT;
                                    case 4 -> WALLET_SLOT;
                                    default -> NONE_SLOT;
                                };
                                default -> NONE_SLOT;
                            };
                        }
                        @Override public boolean mayPickup(EntityHuman playerEntity) {
                            lime.once(() -> {
                                switch (getRowY()) {
                                    case 0 -> {
                                        switch (getRowX()) {
                                            case 0 -> Vest.openVestAdmin(player, other_uuid, EquipmentSlot.FEET);
                                            case 1 -> Vest.openVestAdmin(player, other_uuid, EquipmentSlot.LEGS);
                                            case 2 -> Vest.openVestAdmin(player, other_uuid, EquipmentSlot.CHEST);
                                            case 3 -> Vest.openVestAdmin(player, other_uuid, EquipmentSlot.HEAD);
                                            case 4 -> WalletInventory.openWallet(player, other_uuid);
                                        }
                                    }
                                }
                            }, 0.1);
                            return false;
                        }
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                    });
                }
                return InterfaceManager.AbstractSlot.noneInteractSlot(super.addSlot(slot));
            }
            @Override public boolean stillValid(EntityHuman _player) {
                return true;
            }
        }, ChatHelper.toNMS(Component.text("Вещи человека " + UserRow.getBy(other_uuid)
            .map(v -> v.firstName + " " + v.lastName + " (" + v.userName + ")")
            .orElse("*Not user: "+other_uuid+"*")))));
    }
}

























