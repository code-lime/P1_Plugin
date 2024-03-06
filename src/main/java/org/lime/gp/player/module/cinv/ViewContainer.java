package org.lime.gp.player.module.cinv;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.adventure.AdventureComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.InventoryClickType;
import net.minecraft.world.inventory.Slot;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.player.inventory.gui.InterfaceManager;
import org.lime.system.execute.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class ViewContainer extends ContainerChest {
    protected abstract ViewData viewData();

    protected ViewContainer(int syncId, PlayerInventory playerInventory, CreatorElement groups, List<ItemStack> buffer) {
        super(Containers.GENERIC_9x6, syncId, playerInventory, ReadonlyInventory.ofBukkit(buffer), 6);

        ViewData viewData = viewData();
        viewData.container = this;
        viewData.groups = groups;
    }
    public static ViewContainer create(int syncId, PlayerInventory playerInventory, CreatorElement groups, List<ItemStack> buffer, ViewData viewData) {
        return new ViewContainer(syncId, playerInventory, groups, buffer) {
            @Override protected ViewData viewData() { return viewData; }
        };
    }

    public static net.minecraft.world.item.ItemStack headOf(Component name, String data, Component... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta)item.getItemMeta();
        CraftPlayerProfile profile = new CraftPlayerProfile(UUID.randomUUID(), null);
        profile.setProperty(new ProfileProperty("textures", data));
        meta.setPlayerProfile(profile);
        meta.displayName(name);
        if (lore.length > 0) meta.lore(List.of(lore));
        item.setItemMeta(meta);
        return CraftItemStack.asNMSCopy(item);
    }
    public static net.minecraft.world.item.ItemStack of(Component name, int count, Component... lore) {
        ItemStack item = new ItemStack(Material.GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        if (lore.length > 0) meta.lore(List.of(lore));
        item.setItemMeta(meta);
        item.setAmount(count);
        return CraftItemStack.asNMSCopy(item);
    }

    public static final String UP_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTYwYTVhYjBlYjNlYWY0ZTI3NmI4Zjc2M2VlNDdkMjQxYzRhZjAwOTFjYzFiMDQ1ZDk5NGNkNTExNDE3YWY3YyJ9fX0=";
    public static final String DOWN_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2M1Yjk1NDJjYTQ2YjAwMjM1ZDNkZGRhZGEwMjk5M2JjNGQyZjdlNjNhNWJmNDViMDRhZTZlNzI1OWM3M2U0OCJ9fX0=";

    public static final Component[] LEFT_RIGHT_FULL = new Component[] {
            Component.empty()
                    .append(Component.text("[ЛКМ]").color(NamedTextColor.AQUA))
                    .append(Component.text(" Сдвинуть на 1").color(NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false),
            Component.empty()
                    .append(Component.text("[ПКМ]").color(NamedTextColor.AQUA))
                    .append(Component.text(" Сдвинуть на всю страницу").color(NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false),
            Component.empty()
                    .append(Component.text("[SHIFT+ЛКМ/ПКМ]").color(NamedTextColor.AQUA))
                    .append(Component.text(" Сдвинуть до конца").color(NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false)
    };

    public static final net.minecraft.world.item.ItemStack UP = headOf(Component.text("UP"), UP_HEAD, LEFT_RIGHT_FULL);
    public static final net.minecraft.world.item.ItemStack DOWN = headOf(Component.text("DOWN"), DOWN_HEAD, LEFT_RIGHT_FULL);

    public static final String RIGHT_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2E0N2M1MGU1YTBmN2ZlNDQxODY5YzJhN2Q1ZGU4MDc0ZWRiMTNmYTZmYzg1ZWYxYTQwNzBiOTUzMTI2MTI3OSJ9fX0=";
    public static final String LEFT_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjZkOGVmZjRjNjczZTA2MzY5MDdlYTVjMGI1ZmY0ZjY0ZGMzNWM2YWFkOWI3OTdmMWRmNjYzMzUxYjRjMDgxNCJ9fX0=";

    public static final net.minecraft.world.item.ItemStack RIGHT = headOf(Component.text(">"), RIGHT_HEAD, LEFT_RIGHT_FULL);
    public static final net.minecraft.world.item.ItemStack LEFT = headOf(Component.text("<"), LEFT_HEAD, LEFT_RIGHT_FULL);

    public static final net.minecraft.world.item.ItemStack SEARCH = headOf(Component.text("SEARCH"), "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZhYmY1ZDM3MDc1N2FhZjQ2ZmVmOWExOGI1MmVkYzk1OTQ1NzZjMDBhMzUzMWUwNDQ4ZTRkY2ExN2RiZjRlNCJ9fX0=");
    public static final net.minecraft.world.item.ItemStack CRAFTS = headOf(Component.text("CRAFTS"), "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZhYmY1ZDM3MDc1N2FhZjQ2ZmVmOWExOGI1MmVkYzk1OTQ1NzZjMDBhMzUzMWUwNDQ4ZTRkY2ExN2RiZjRlNCJ9fX0=");
    public static final net.minecraft.world.item.ItemStack BACK = headOf(Component.text("BACK"), "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTkzMTZhNjljNmRkNjU1ZjViODlmMmE4MzMxNTI1ZmUwZGM5MmZkMTNkNDMyNjIzYThiZjg1MWNlODIwYjAzNCJ9fX0=");
    public static final net.minecraft.world.item.ItemStack NONE = net.minecraft.world.item.ItemStack.EMPTY;

    public enum SlotType {
        Up((slot, view, slotType, slotTypeIndex) -> new StaticClickSlot(slot, view, slotType, slotTypeIndex, UP) {
            @Override public void onSlotClick(EntityHuman player, InventoryClickType type, ClickType click) {
                view.itemsOffsetMove(-1, click.isRightClick(), click.isShiftClick());
            }
        }),
        Down((slot, view, slotType, slotTypeIndex) -> new StaticClickSlot(slot, view, slotType, slotTypeIndex, DOWN) {
            @Override public void onSlotClick(EntityHuman player, InventoryClickType type, ClickType click) {
                view.itemsOffsetMove(1, click.isRightClick(), click.isShiftClick());
            }
        }),
        Right((slot, view, slotType, slotTypeIndex) -> new StaticClickSlot(slot, view, slotType, slotTypeIndex, RIGHT) {
            @Override public void onSlotClick(EntityHuman player, InventoryClickType type, ClickType click) {
                view.groupOffsetMove(1, click.isRightClick(), click.isShiftClick());
            }
        }),
        Left((slot, view, slotType, slotTypeIndex) -> new StaticClickSlot(slot, view, slotType, slotTypeIndex, LEFT) {
            @Override public void onSlotClick(EntityHuman player, InventoryClickType type, ClickType click) {
                view.groupOffsetMove(-1, click.isRightClick(), click.isShiftClick());
            }
        }),

        Item(ItemSlot::new),
        Group(GroupSlot::new),
        Search((slot, view, slotType, slotTypeIndex) -> new StaticClickSlot(slot, view, slotType, slotTypeIndex, SEARCH) {
            @Override public void onSlotClick(EntityHuman player, InventoryClickType type, ClickType click) {
                SearchQuery.openSearch(view.container, player);
            }
        }),
        None(NoneSlot::new);

        private final Func4<Slot, ViewData, SlotType, Integer, BaseActionSlot> creator;
        SlotType(Func4<Slot, ViewData, SlotType, Integer, BaseActionSlot> creator) { this.creator = creator; }
        public BaseActionSlot createSlot(Slot slot, ViewData view, int slotTypeIndex) { return this.creator.invoke(slot, view, this, slotTypeIndex); }

        public int nextSlotTypeIndex(Iterable<Slot> slots) {
            int slotTypeIndex = 0;
            for (Slot slot : slots) {
                if (slot instanceof BaseActionSlot actionSlot && this.equals(actionSlot.slotType()))
                    slotTypeIndex++;
            }
            return slotTypeIndex;
        }
    }

    private static final SlotType[] slotTypes = new SlotType[] {
//                  0              1               2               3               4               5               6               7               8
/* 0 */     SlotType.Item, SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Up,
/* 1 */     SlotType.Item, SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.None,
/* 2 */     SlotType.Item, SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Search,
/* 3 */     SlotType.Item, SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.None,
/* 4 */     SlotType.Item, SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Item,  SlotType.Down,
/* 5 */     SlotType.Left, SlotType.Group, SlotType.Group, SlotType.Group, SlotType.Group, SlotType.Group, SlotType.Group, SlotType.Group, SlotType.Right,
    };

    SlotType lastSlotType = SlotType.None;
    int lastSlotTypeIndex = -1;

    private String oldTitle = "cinv";
    public void changeTitle(EntityHuman human, String text) {
        if (!Objects.equals(oldTitle, text) && human instanceof EntityPlayer player)
            InterfaceManager.changeTitle(player, this, new AdventureComponent(Component.text(oldTitle = text)));
    }
    public void forceOpen(EntityHuman human) {
        if (human instanceof EntityPlayer player)
            InterfaceManager.changeTitle(player, this, new AdventureComponent(Component.text(oldTitle)), true);
    }

    @Override protected Slot addSlot(Slot slot) {
        slot.index = this.slots.size();
        if (slot.container != getContainer()) return super.addSlot(slot);
        ViewData viewData = viewData();
        SlotType slotType = slotTypes[slot.index];
        int slotTypeIndex = slotType.nextSlotTypeIndex(this.slots);
        slot = slotType.createSlot(slot, viewData, slotTypeIndex);
        switch (slotType) {
            case Group -> viewData.groupsShowLength(slotTypeIndex + 1);
            case Item -> viewData.itemsShowLength(slotTypeIndex + 1);
        }
        if (lastSlotType == SlotType.Item && slotType != lastSlotType && viewData.itemsStepLength() == 0) {
            viewData.itemsStepLength(lastSlotTypeIndex + 1);
        }
        //lime.logOP("ST." + slotType + ": " + slotTypeIndex + " with " + viewData.groupsShowLength() + " & " + viewData.itemsShowLength() + " & " + viewData.itemsStepLength());
        lastSlotType = slotType;
        lastSlotTypeIndex = slotTypeIndex;

/*
        net.minecraft.world.item.ItemStack _item = of(Component.text("Slot: " + slotTypes[slot.index]), slot.index + 1);
        slot = new InterfaceManager.AbstractSlot(slot) {
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return true; }
            @Override public void set(net.minecraft.world.item.ItemStack stack) { }
            @Override public net.minecraft.world.item.ItemStack getItem() { return _item; }
            @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
        };

        /*int slot_y = slot.index / 9;
        int slot_x = slot.index - (9 * slot_y);
            /*net.minecraft.world.item.ItemStack _item = of(Component.text("Slot: " + slot_x + " / " + slot_y), slot.index);
            if (true) return super.addSlot(new InterfaceManager.AbstractSlot(slot) {
                @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                @Override public net.minecraft.world.item.ItemStack getItem() { return _item; }
                @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
            });*//*
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
        }*/
        return super.addSlot(slot);
    }
    @Override public ReadonlyInventory getContainer() { return (ReadonlyInventory)super.getContainer(); }
}
