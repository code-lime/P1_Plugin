package org.lime.gp.block.component.data;

import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventoryView;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.inventory.gui.InterfaceManager;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.utils.ItemUtils;

import java.util.Arrays;
import java.util.Collection;

public class TrashInstance extends BlockInstance implements CustomTileMetadata.Tickable, CustomTileMetadata.Removeable, CustomTileMetadata.Lootable, CustomTileMetadata.Interactable {
    public CraftInventory inventory;
    private ItemStack[] oldTick = new ItemStack[0];
    private boolean isParticle;
    public TrashInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(component, metadata);
        inventory = createBlockInventory(InventoryType.CHEST, net.kyori.adventure.text.Component.text("Мусорка"), metadata().block());
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
        if (inventory != null) inventory.close();
        inventory = createBlockInventory(InventoryType.CHEST, net.kyori.adventure.text.Component.text("Мусорка"), metadata().block());
        inventory.setContents(json.getAsJsonArray("items")
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsString)
                .map(v -> v.orElse(null))
                .map(ItemUtils::loadItem)
                .toArray(ItemStack[]::new)
        );
        isParticle = json.getAsBoolean("is_particle").orElse(false);
    }
    @Override public json.builder.object write() {
        return json.object()
                .addArray("items", v -> v.add(inventory.getContents(), ItemUtils::saveItem))
                .add("is_particle", isParticle);
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

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        ItemStack[] items = inventory.getContents();
        if (!equals(oldTick, items)) {
            oldTick = items;
            int length = items.length;
            double not_empty = 0;
            for (ItemStack item : items) {
                if (item == null || item.getType().isAir()) continue;
                not_empty++;
            }
            isParticle = not_empty / length > 0.5;
            saveData();
        }

        /*if (isParticle) {
            Particle.SPELL.builder()
                    .count(1)
                    .location(metadata.location(0.5, 0, 0.5))
                    .offset(0.25, 0, 0.25)
                    .force(false)
                    .color(Color.fromRGB(0x804030))
                    .receivers(7)
                    .force(false)
                    .spawn();
        }*/
    }
    @Override public void onRemove(CustomTileMetadata metadata, TileEntitySkullEventRemove event) {
        inventory.close();
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        event.addItems(Arrays.asList(inventory.getContents()));
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        InterfaceManager.openInventory(event.player(), inventory, (id, playerInventory, iinventory) -> new ContainerChest(Containers.GENERIC_9x3, id, playerInventory, iinventory, 3) {
            private CraftInventoryView bukkitEntity = null;
            @Override public CraftInventoryView getBukkitView() {
                if (this.bukkitEntity != null) return this.bukkitEntity;
                this.bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, this);
                return this.bukkitEntity;
            }
        });
        return EnumInteractionResult.CONSUME;
    }
}
