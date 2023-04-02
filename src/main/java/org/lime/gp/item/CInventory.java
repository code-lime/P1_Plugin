package org.lime.gp.item;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.ServerOperator;
import org.lime.core;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Rows;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.lime;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.system;

import java.util.*;
import java.util.stream.Collectors;

public class CInventory {
    public static core.element create() {
        return core.element.create(CInventory.class)
                .addCommand("cinv", v -> v.withTab().withCheck(ServerOperator::isOp).withExecutor(sender -> {
                    if (!(sender instanceof CraftPlayer player)) return false;
                    Apply apply = Rows.UserRow.getBy(player.getUniqueId()).map(_v -> Apply.of().add(_v)).orElseGet(Apply::of);
                    List<system.Toast2<String, List<ItemStack>>> items = Items.creators.values()
                            .stream()
                            //.filter(ExtMethods.filterLog("CINV.0: {0}"))
                            .map(creator -> system.toast(creator.getKey(), system.funcEx(() -> creator.createItem(1, apply)).optional().invoke().orElse(null)))
                            //.filter(ExtMethods.filterLog("CINV.1: {0}"))
                            .filter(_v -> _v.val1 != null && _v.val0 != null)
                            //.filter(ExtMethods.filterLog("CINV.2: {0}"))
                            .collect(Collectors.groupingBy(kv -> kv.val0.contains(".") ? kv.val0.split("\\.")[0].toLowerCase() : "item"))
                            .entrySet()
                            .stream()
                            //.filter(ExtMethods.filterLog("CINV.3: {0}"))
                            .map(kv -> system.toast(kv.getKey(), kv.getValue().stream().map(_v -> _v.val1).toList()))
                            //.filter(ExtMethods.filterLog("CINV.4: {0}"))
                            .collect(Collectors.toList());
                    player.getHandle().openMenu(getMenuProvider((syncId, inv, player1) -> new ContainerCInventory(syncId, inv, items, NonNullList.withSize(9*6, new ItemStack(Material.AIR))), IChatBaseComponent.literal("cinv")));
                    return true;
                }));
    }

    private static ITileInventory getMenuProvider(ITileEntityContainer init, IChatBaseComponent CONTAINER_TITLE) {
        return new TileInventory(init, CONTAINER_TITLE);
    }

    private static class ContainerCInventory extends ContainerChest {
        @SuppressWarnings("unused")
        private final List<ItemStack> buffer;
        @SuppressWarnings("unused")
        private final ReadonlyInventory view;
        private final List<system.Toast2<String, List<ItemStack>>> view_groups;

        private int scroll_line = 0;
        private int scroll_group = 0;

        private int select_group = 0;

        public ContainerCInventory(int syncId, PlayerInventory playerInventory, List<system.Toast2<String, List<ItemStack>>> view_groups, List<ItemStack> buffer) {
            super(Containers.GENERIC_9x6, syncId, playerInventory, ReadonlyInventory.ofBukkit(buffer), 6);
            this.buffer = buffer;
            this.view_groups = view_groups;
            this.view = getContainer();
        }

        private static net.minecraft.world.item.ItemStack headOf(Component name, String data) {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta)item.getItemMeta();
            CraftPlayerProfile profile = new CraftPlayerProfile(UUID.randomUUID(), null);
            profile.setProperty(new ProfileProperty("textures", data));
            meta.setPlayerProfile(profile);
            meta.displayName(name);
            item.setItemMeta(meta);
            return CraftItemStack.asNMSCopy(item);
        }
        private static net.minecraft.world.item.ItemStack of(Component name, int count) {
            ItemStack item = new ItemStack(Material.GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(name);
            item.setItemMeta(meta);
            item.setAmount(count);
            return CraftItemStack.asNMSCopy(item);
        }

        private static final net.minecraft.world.item.ItemStack UP = headOf(Component.text("UP"), "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTYwYTVhYjBlYjNlYWY0ZTI3NmI4Zjc2M2VlNDdkMjQxYzRhZjAwOTFjYzFiMDQ1ZDk5NGNkNTExNDE3YWY3YyJ9fX0=");
        private static final net.minecraft.world.item.ItemStack DOWN = headOf(Component.text("DOWN"), "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2M1Yjk1NDJjYTQ2YjAwMjM1ZDNkZGRhZGEwMjk5M2JjNGQyZjdlNjNhNWJmNDViMDRhZTZlNzI1OWM3M2U0OCJ9fX0=");
        private static final net.minecraft.world.item.ItemStack RIGHT = headOf(Component.text(">"), "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2E0N2M1MGU1YTBmN2ZlNDQxODY5YzJhN2Q1ZGU4MDc0ZWRiMTNmYTZmYzg1ZWYxYTQwNzBiOTUzMTI2MTI3OSJ9fX0=");
        private static final net.minecraft.world.item.ItemStack LEFT = headOf(Component.text("<"), "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjZkOGVmZjRjNjczZTA2MzY5MDdlYTVjMGI1ZmY0ZjY0ZGMzNWM2YWFkOWI3OTdmMWRmNjYzMzUxYjRjMDgxNCJ9fX0=");

        @Override protected Slot addSlot(Slot slot) {
            if (slot.container != getContainer()) return super.addSlot(slot);
            slot.index = this.slots.size();
            int slot_y = slot.index / 9;
            int slot_x = slot.index - (9 * slot_y);
            /*net.minecraft.world.item.ItemStack _item = of(Component.text("Slot: " + slot_x + " / " + slot_y), slot.index);
            if (true) return super.addSlot(new InterfaceManager.AbstractSlot(slot) {
                @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                @Override public net.minecraft.world.item.ItemStack getItem() { return _item; }
                @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
            });*/
            if (slot_y == 5) {
                if (slot_x == 0) {
                    slot = new InterfaceManager.AbstractSlot(slot) {
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return true; }
                        @Override public void set(net.minecraft.world.item.ItemStack stack) { }
                        @Override public net.minecraft.world.item.ItemStack getItem() { return LEFT; }
                        @Override public boolean mayPickup(EntityHuman playerEntity) {
                            scroll_group = Math.max(0, scroll_group - 1);
                            return false;
                        }
                    };
                } else if (slot_x == 8) {
                    slot = new InterfaceManager.AbstractSlot(slot) {
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return true; }
                        @Override public void set(net.minecraft.world.item.ItemStack stack) { }
                        @Override public net.minecraft.world.item.ItemStack getItem() { return RIGHT; }
                        @Override public boolean mayPickup(EntityHuman playerEntity) {
                            scroll_group = Math.max(0, Math.min(view_groups.size() - 6, scroll_group + 1));
                            return false;
                        }
                    };
                } else {
                    slot = new InterfaceManager.AbstractSlot(slot) {
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return true; }
                        @Override public void set(net.minecraft.world.item.ItemStack stack) { }
                        @Override public net.minecraft.world.item.ItemStack getItem() {
                            int groupID = select_group + slot_x - 1;
                            return groupID < 0 || groupID >= view_groups.size() ? net.minecraft.world.item.ItemStack.EMPTY : of(Component.text(view_groups.get(groupID).val0), groupID + 1);
                        }
                        @Override public boolean mayPickup(EntityHuman playerEntity) {
                            select_group = Math.max(0, Math.min(view_groups.size() - 1, scroll_group + x - 1));
                            lime.logOP("Select group: " + select_group);
                            return false;
                        }
                    };
                }
            } else if (slot_x == 8) {
                if (slot_y == 0) {
                    slot = new InterfaceManager.AbstractSlot(slot) {
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return true; }
                        @Override public void set(net.minecraft.world.item.ItemStack stack) { }
                        @Override public net.minecraft.world.item.ItemStack getItem() { return UP; }
                        @Override public boolean mayPickup(EntityHuman playerEntity) {
                            scroll_line = Math.max(0, scroll_line - 1);
                            return false;
                        }
                    };
                } else if (slot_y == 4) {
                    slot = new InterfaceManager.AbstractSlot(slot) {
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return true; }
                        @Override public void set(net.minecraft.world.item.ItemStack stack) { }
                        @Override public net.minecraft.world.item.ItemStack getItem() { return DOWN; }
                        @Override public boolean mayPickup(EntityHuman playerEntity) {
                            List<ItemStack> items = view_groups.get(select_group).val1;
                            int lines = (int)Math.ceil(items.size() / 5.0);
                            scroll_line = Math.max(0, Math.min(lines - 4, scroll_line + 1));
                            return false;
                        }
                    };
                } else {
                    slot = InterfaceManager.AbstractSlot.noneSlot(slot);
                }
            } else {
                slot = new InterfaceManager.AbstractSlot(slot) {
                    public Optional<Integer> getItemIndex() {
                        return Optional.of((scroll_line + slot_y * 7) + slot_x)
                                .filter(v -> v < view_groups.get(select_group).val1.size());
                    }
                    @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return true; }
                    @Override public void set(net.minecraft.world.item.ItemStack stack) { }
                    @Override public net.minecraft.world.item.ItemStack getItem() {
                        return getItemIndex()
                                .map(view_groups.get(select_group).val1::get)
                                .map(CraftItemStack::asNMSCopy)
                                .orElse(net.minecraft.world.item.ItemStack.EMPTY);
                    }
                    @Override public boolean mayPickup(EntityHuman playerEntity) {
                        return false;
                    }
                };
            }
            return super.addSlot(slot);
        }
        @Override public ReadonlyInventory getContainer() { return (ReadonlyInventory)super.getContainer(); }
    }
}
















