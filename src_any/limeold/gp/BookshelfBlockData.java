package org.lime.gp.block.component.data;

import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.inventory.ContainerDispenser;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BlockSkullEventInteract;
import net.minecraft.world.level.block.BlockSkullEventShape;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.gp.block.BlocksOld;
import org.lime.gp.block.component.Components;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.item.Items;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookshelfBlockData implements Listener {
    public static core.element create() {
        BlocksOld.addDefaultBlocks(new BlocksOld.InfoCreator("bookshelf", creator -> {
                    creator.add(InfoComponent.GenericDynamicComponent.of("bookshelf", creator, Info::new));
                    creator.add(new Components.LootComponent(creator, system.json.object()
                            .addObject("items", v -> v.add(Material.BOOKSHELF.name(), 1))
                            .build()));
                })
                        .addReplace(Material.BOOKSHELF)
        );
        return core.element.create(BookshelfBlockData.class)
                .withInstance();
    }

    private static CraftInventory createBlockInventory(InventoryType type, net.kyori.adventure.text.Component title, Block block) {
        system.Toast1<CraftInventory> inventory = system.toast(null);
        BlockInventoryHolder holder = new BlockInventoryHolder() {
            @Override public Inventory getInventory() { return inventory.val0; }
            @Override public Block getBlock() { return block; }
        };
        inventory.val0 = new CraftInventoryCustom(holder, type, title) {
            @Override public Location getLocation() { return block.getLocation(); }
        };
        return inventory.val0;
    }

    public static final class Info extends BlocksOld.InfoInstance implements InfoComponent.IShape, InfoComponent.ILoot {
        public static final IBlockData BOOKSHELF =  CraftMagicNumbers
                .getBlock(Material.BOOKSHELF)
                .defaultBlockState();

        public Info(BlocksOld.Info info) {
            super(info);
        }
        public CraftInventory inventory;

        @Override public InfoComponent.IReplace.Result replace(InfoComponent.IReplace.Input input) {
            return input.toResult(BOOKSHELF);
        }
        @Override public void save() {
            setSaved(system.json.object()
                    .addArray("items", v -> v.add(inventory.getContents(), system::saveItem))
                    .build());
        }
        @Override public JsonObject load(JsonObject json) {
            inventory = createBlockInventory(InventoryType.DISPENSER, net.kyori.adventure.text.Component.text("Книжная полка"), info().position().orElseThrow().getBlock());
            if (json.has("items")) inventory.setContents(Streams.stream(json.getAsJsonArray("items").iterator()).map(v -> v.isJsonNull() ? null : v.getAsString()).map(system::loadItem).toArray(ItemStack[]::new));
            return json;
        }

        private ItemStack[] oldTick = new ItemStack[0];
        private static boolean equals(ItemStack[] items1, ItemStack[] items2) {
            int length = items1.length;
            if (length != items2.length) return false;
            for (int i = 0; i < length; i++) {
                ItemStack item1 = items1[i];
                ItemStack item2 = items2[i];
                if (item1 == null) {
                    if (item2 != null) return false;
                    continue;
                }
                if (!item1.isSimilar(item2)) return false;
                if (item1.getAmount() != item2.getAmount()) return false;
            }
            return true;
        }
        @Override public boolean tick(TileEntitySkull skull) {
            List<ItemStack> removeItem = new ArrayList<>();
            ItemStack[] items = inventory.getContents();
            int length = items.length;
            for (int i = 0; i < length; i++) {
                ItemStack item = items[i];
                if (WHITELIST.contains(item == null ? Material.BOOK : item.getType())) continue;
                items[i] = null;
                removeItem.add(item);
            }
            if (removeItem.size() > 0) {
                this.info().position().ifPresent(pos -> {
                    Location location = pos.getLocation(0.5,0.5,0.5);
                    inventory.setContents(items);
                    Items.dropItem(location, removeItem);
                });
            }
            boolean edited = !equals(oldTick, items);
            oldTick = items;
            if (edited) save();
            return true;
        }

        private static final List<Material> WHITELIST = Arrays.asList(
                Material.BOOK,
                Material.WRITABLE_BOOK,
                Material.WRITTEN_BOOK,
                Material.ENCHANTED_BOOK,
                Material.KNOWLEDGE_BOOK,
                Material.MAP,
                Material.FILLED_MAP,
                Material.PAPER,

                Material.PIGLIN_BANNER_PATTERN,
                Material.CREEPER_BANNER_PATTERN,
                Material.FLOWER_BANNER_PATTERN,
                Material.GLOBE_BANNER_PATTERN,
                Material.MOJANG_BANNER_PATTERN,
                Material.SKULL_BANNER_PATTERN
        );
        private static final List<Item> WHITELIST_NMS = WHITELIST.stream().map(CraftMagicNumbers::getItem).toList();

        @Override public void interact(TileEntitySkull state, BlockSkullEventInteract event) {
            InterfaceManager.openInventory(event.getPlayer(), inventory, (id, playerInventory, iinventory) -> new ContainerDispenser(id, playerInventory, iinventory) {
                private CraftInventoryView bukkitEntity = null;
                @Override protected Slot addSlot(Slot slot) {
                    return super.addSlot(slot.container == iinventory
                            ? InterfaceManager.filterSlot(slot, stack -> WHITELIST_NMS.contains(stack.getItem()))
                            : slot);
                }
                @Override public CraftInventoryView getBukkitView() {
                    if (this.bukkitEntity != null) return this.bukkitEntity;
                    this.bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, this);
                    return this.bukkitEntity;
                }
            });
            event.setResult(EnumInteractionResult.CONSUME);
        }
        @Override public boolean asyncShape(TileEntitySkull state, BlockSkullEventShape e) {
            e.setResult(VoxelShapes.block());
            return true;
        }
        @Override public List<ItemStack> populate(TileEntitySkull state, PopulateLootEvent e) {
            return Arrays.asList(inventory.getContents());
        }

        @Override public void close() {
            inventory.close();
        }
    }
}




















