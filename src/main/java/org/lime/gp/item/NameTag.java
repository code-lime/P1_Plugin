package org.lime.gp.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.IBlockData;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.core;
import org.lime.gp.craft.RecipesBook;
import org.lime.gp.player.inventory.InterfaceManager;

import java.util.Optional;
import java.util.regex.Pattern;

public class NameTag implements Listener {
    public static core.element create() {
        return core.element.create(NameTag.class)
                .withInstance();
    }

    @EventHandler public static void on(PlayerInteractEvent e) {
        if (e.getAction().isRightClick() && e.getPlayer() instanceof CraftPlayer player && player.isSneaking()) {
            PlayerInventory inventory = e.getPlayer().getInventory();
            ItemStack name_tag = inventory.getItemInMainHand();
            if (name_tag.getType() != Material.NAME_TAG) return;
            player.getHandle().openMenu(getInventory());
        }
    }
    @EventHandler public static void onClick(InventoryClickEvent e) {
        switch (e.getClick()) {
            case RIGHT, LEFT -> {
                ItemStack cursor = e.getCursor();
                ItemStack item = e.getCurrentItem();
                if (cursor == null || item == null) return;
                if (cursor.getType() != Material.NAME_TAG || item.getType() == Material.NAME_TAG || !cursor.hasItemMeta())
                    return;
                ItemMeta name_tag_meta = cursor.getItemMeta();
                if (name_tag_meta == null || !name_tag_meta.hasDisplayName()) return;
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return;
                meta.displayName(name_tag_meta.displayName());
                item.setItemMeta(meta);
                cursor.subtract(1);
                e.setCancelled(true);
            }
            default -> {}
        }
    }

    private static final Component PREFIX = Component.text("§r");
    private static final TextReplacementConfig REMOVE_FORMATS = TextReplacementConfig.builder()
            .replacement("")
            .match(Pattern.compile("§."))
            .build();

    private static ITileInventory getInventory() {
        IChatBaseComponent component = RecipesBook.getCustomWorkbenchName("nametag").orElse(ChatComponentText.EMPTY);
        return new TileInventory((syncId, inventory, player) -> new ContainerAnvil(syncId, inventory) {
            public net.minecraft.world.item.ItemStack OUT = inventory.getSelected().copy();
            @Override protected Slot addSlot(Slot slot) {
                if (slot.container == inventory) return super.addSlot(slot);
                return super.addSlot(new InterfaceManager.AbstractBaseSlot(slot) {
                    @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                    @Override public net.minecraft.world.item.ItemStack getItem() {
                        return switch (index) {
                            case 0 -> {
                                net.minecraft.world.item.ItemStack item = inventory.getSelected().copy();
                                CraftItemStack bukkit = CraftItemStack.asCraftMirror(item);
                                ItemMeta meta = bukkit.getItemMeta();
                                if (meta == null) yield net.minecraft.world.item.ItemStack.EMPTY;
                                Optional.ofNullable(meta.displayName())
                                        .map(_v -> _v.replaceText(REMOVE_FORMATS))
                                        .ifPresent(name -> {
                                            meta.displayName(name);
                                            bukkit.setItemMeta(meta);
                                        });
                                yield item;
                            }
                            case 2 -> OUT;
                            default -> net.minecraft.world.item.ItemStack.EMPTY;
                        };
                    }
                    @Override public boolean mayPickup(EntityHuman playerEntity) {
                        if (index == 2) {
                            if (inventory.getSelected().is(Items.NAME_TAG)) {
                                net.minecraft.world.item.ItemStack out = OUT.copy();
                                out.setCount(inventory.getSelected().getCount());
                                inventory.items.set(inventory.selected, out);
                            }
                            playerEntity.closeContainer();
                            return false;
                        }
                        return false;
                    }
                });
            }
            @Override public void setItemName(String newItemName) {
                ItemStack name_tag = new ItemStack(Material.NAME_TAG);
                if (StringUtils.isBlank(newItemName)) {
                    itemName = "";
                } else {
                    itemName = newItemName;
                    ItemMeta meta = name_tag.getItemMeta();
                    meta.displayName(PREFIX.append(Component.text(itemName)));
                    name_tag.setItemMeta(meta);
                }
                OUT = CraftItemStack.asNMSCopy(name_tag);

                sendAllDataToRemote();
                broadcastChanges();
            }
            @Override public boolean stillValid(EntityHuman player) { return inventory.getSelected().is(Items.NAME_TAG); }
            @Override protected boolean isValidBlock(IBlockData state) { return inventory.getSelected().is(Items.NAME_TAG); }
        }, component);
    }

}















