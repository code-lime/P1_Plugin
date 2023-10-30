package org.lime.gp.extension.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;

import java.util.Collections;
import java.util.List;

public class ReadonlyInventory implements IInventory {
    public interface ItemList {
        interface ItemBoxed<T> {
            ItemStack of(T item);
            boolean isEmpty(T item);
        }

        boolean isEmpty();
        int size();
        ItemStack get(int slot);
        List<ItemStack> getContents();

        static ItemList ofNMS(List<ItemStack> items) {
            return new ItemList() {
                @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
                @Override public int size() { return items.size(); }
                @Override public ItemStack get(int slot) { return slot < 0 || slot >= items.size() ? ItemStack.EMPTY : items.get(slot); }
                @Override public List<ItemStack> getContents() { return ImmutableList.copyOf(items); }
            };
        }
        static ItemList ofBukkit(List<org.bukkit.inventory.ItemStack> items) {
            return new ItemList() {
                @Override public boolean isEmpty() { return items.stream().allMatch(v -> v.getType().isEmpty() || v.getAmount() < 0); }
                @Override public int size() { return items.size(); }
                @Override public ItemStack get(int slot) { return slot < 0 || slot >= items.size() ? ItemStack.EMPTY : CraftItemStack.asNMSCopy(items.get(slot)); }
                @Override public List<ItemStack> getContents() { return items.stream().map(CraftItemStack::asNMSCopy).collect(ImmutableList.toImmutableList()); }
            };
        }
        static <T>ItemList of(List<T> items, ItemList.ItemBoxed<T> boxed) {
            return new ItemList() {
                @Override public boolean isEmpty() { return items.stream().allMatch(boxed::isEmpty); }
                @Override public int size() { return items.size(); }
                @Override public ItemStack get(int slot) { return slot < 0 || slot >= items.size() ? ItemStack.EMPTY : boxed.of(items.get(slot)); }
                @Override public List<ItemStack> getContents() { return items.stream().map(boxed::of).collect(ImmutableList.toImmutableList()); }
            };
        }
    }

    private final ItemList items;
    private final Location location;
    private final ImmutableList<Action1<EntityHuman>> closeActions;

    public static ReadonlyInventory ofNMS(List<ItemStack> items) { return ofNMS(items, null); }
    public static ReadonlyInventory ofNMS(List<ItemStack> items, Location location) { return new ReadonlyInventory(ItemList.ofNMS(items), location); }
    public static ReadonlyInventory ofBukkit(List<org.bukkit.inventory.ItemStack> items) { return ofBukkit(items, null); }
    public static ReadonlyInventory ofBukkit(List<org.bukkit.inventory.ItemStack> items, Location location) { return new ReadonlyInventory(ItemList.ofBukkit(items), location); }

    public static <T>ReadonlyInventory of(List<T> items, ItemList.ItemBoxed<T> boxed) { return of(items, boxed, null); }
    public static <T>ReadonlyInventory of(List<T> items, ItemList.ItemBoxed<T> boxed, Location location) { return new ReadonlyInventory(ItemList.of(items, boxed), location); }

    private ReadonlyInventory(ItemList items, Location location) {
        this(items, location, ImmutableList.of());
    }
    private ReadonlyInventory(ItemList items, Location location, ImmutableList<Action1<EntityHuman>> closeActions) {
        this.items = items;
        this.location = location;
        this.closeActions = closeActions;
    }

    public ReadonlyInventory withCloseAction(Action1<EntityHuman> action) {
        return new ReadonlyInventory(items, location, ImmutableList.<Action1<EntityHuman>>builder().addAll(closeActions).add(action).build());
    }

    @Override public void stopOpen(EntityHuman player) { closeActions.forEach(action -> action.invoke(player)); }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack item) { }
    @Override public int getMaxStackSize() { return 64; }
    @Override public void setChanged() { }
    @Override public boolean stillValid(EntityHuman entityHuman) { return true; }
    @Override public List<ItemStack> getContents() { return items.getContents(); }
    @Override public void onOpen(CraftHumanEntity craftHumanEntity) { }
    @Override public void onClose(CraftHumanEntity craftHumanEntity) { }
    @Override public List<HumanEntity> getViewers() { return Collections.emptyList(); }
    @Override public InventoryHolder getOwner() { return null; }
    @Override public void setMaxStackSize(int maxStackSize) { }
    @Override public Location getLocation() { return location; }
    @Override public void clearContent() { }
}
















