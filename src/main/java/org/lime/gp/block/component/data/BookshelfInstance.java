package org.lime.gp.block.component.data;

import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.inventory.ContainerDispenser;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.BlockSkullShapeInfo;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.CacheBlockDisplay;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.LootComponent;
import org.lime.gp.lime;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.ItemUtils;
import org.lime.system.utils.RandomUtils;

import java.util.*;

public class BookshelfInstance extends BlockInstance implements CustomTileMetadata.Tickable, CustomTileMetadata.FirstTickable, CustomTileMetadata.Removeable, CustomTileMetadata.Lootable, CustomTileMetadata.Interactable, CustomTileMetadata.Shapeable {
    public static CoreElement create() {
        Blocks.addDefaultBlocks(new BlockInfo("bookshelf")
                .add(v -> InfoComponent.GenericDynamicComponent.of("bookshelf", v, BookshelfInstance::new))
                .add(v -> new LootComponent(v, List.of(Material.BOOKSHELF)))
                .addReplace(Material.BOOKSHELF)
        );
        return CoreElement.create(BookshelfInstance.class);
    }
    public CraftInventory inventory;
    private ItemStack[] oldTick = new ItemStack[0];
    private final boolean DEBUG;
    private final int UINDEX = RandomUtils.rand(0, 9999999);
    private final static HashMap<TileEntityLimeSkull, Integer> TILE_INDEX = new HashMap<>();
    public BookshelfInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(component, metadata);
        DEBUG = metadata.skull.getLevel() == lime.EndWorld.getHandle();
        if (DEBUG) {
            Integer index = TILE_INDEX.get(metadata.skull);
            lime.logStackTrace();
            if (index == null) {
                index = RandomUtils.rand(0, 9999999);
                lime.logOP("["+UINDEX+"] Create bookshelf instance with new index: " + index);
            } else {
                lime.logOP("["+UINDEX+"] Create bookshelf instance: " + index);
            }
            TILE_INDEX.put(metadata.skull, index);
        }
        inventory = createBlockInventory(InventoryType.DISPENSER, net.kyori.adventure.text.Component.text("Книжная полка"), metadata().block());
        CacheBlockDisplay.replaceCacheBlock(metadata.skull, CacheBlockDisplay.ICacheInfo.of(net.minecraft.world.level.block.Blocks.BOOKSHELF.defaultBlockState()));
        DisplayInstance.markDirtyBlock(metadata.position());
    }

    @Override public void onFirstTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        DisplayInstance.markDirtyBlock(metadata.position());
    }

    private static CraftInventory createBlockInventory(InventoryType type, net.kyori.adventure.text.Component title, Block block) {
        Toast1<CraftInventory> inventory = Toast.of(null);
        BlockInventoryHolder holder = new BlockInventoryHolder() {
            @Override public Inventory getInventory() { return inventory.val0; }
            @Override public Block getBlock() { return block; }
        };
        inventory.val0 = new CraftInventoryCustom(holder, type, title) {
            @Override public Location getLocation() { return block.getLocation(); }
        };
        return inventory.val0;
    }

    @Override public void read(JsonObjectOptional json) {
        if (DEBUG) lime.logOP("["+UINDEX+"] Read bookshelf");
        if (inventory != null) inventory.close();
        inventory = createBlockInventory(InventoryType.DISPENSER, net.kyori.adventure.text.Component.text("Книжная полка"), metadata().block());
        inventory.setContents(json.getAsJsonArray("items")
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsString)
                .map(v -> v.orElse(null))
                .map(ItemUtils::loadItem)
                .toArray(ItemStack[]::new)
        );
    }
    @Override public json.builder.object write() {
        if (DEBUG) lime.logOP("["+UINDEX+"] Save bookshelf");
        return json.object()
                .addArray("items", v -> v.add(inventory.getContents(), ItemUtils::saveItem));
    }

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

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        ItemStack[] items = inventory.getContents();
        if (!equals(oldTick, items)) {
            oldTick = items;
            saveData();
        }
    }
    @Override public void onRemove(CustomTileMetadata metadata, TileEntitySkullEventRemove event) {
        if (DEBUG) lime.logOP("["+UINDEX+"]Remove bookshelf");
        inventory.close();
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        event.addItems(Arrays.asList(inventory.getContents()));
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        if (DEBUG) lime.logOP("["+UINDEX+"]Interact bookshelf");
        InterfaceManager.openInventory(event.player(), inventory, (id, playerInventory, iinventory) -> new ContainerDispenser(id, playerInventory, iinventory) {
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
        return EnumInteractionResult.CONSUME;
    }
    @Override public VoxelShape onShape(CustomTileMetadata metadata, BlockSkullShapeInfo event) {
        return VoxelShapes.block();
    }
}
























