package org.lime.gp.extension.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

import java.util.Collections;
import java.util.List;

public class EmptyInventory implements IInventory {
    private final Location location;
    private final int size;
    public EmptyInventory(Location location) {
        this(location, 0);
    }
    public EmptyInventory(Location location, int size) {
        this.location = location;
        this.size = size;
    }

    @Override public int getContainerSize() { return size; }
    @Override public boolean isEmpty() { return true; }
    @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack item) { }
    @Override public int getMaxStackSize() { return 64; }
    @Override public void setChanged() { }
    @Override public boolean stillValid(EntityHuman entityHuman) { return true; }
    @Override public List<ItemStack> getContents() { return Collections.emptyList(); }
    @Override public void onOpen(CraftHumanEntity craftHumanEntity) { }
    @Override public void onClose(CraftHumanEntity craftHumanEntity) { }
    @Override public List<HumanEntity> getViewers() { return Collections.emptyList(); }
    @Override public InventoryHolder getOwner() { return null; }
    @Override public void setMaxStackSize(int maxStackSize) { }
    @Override public Location getLocation() { return location; }
    @Override public void clearContent() { }
}
















