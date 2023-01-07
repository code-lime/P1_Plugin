package net.minecraft.world.entity;

import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EntityEquipmentSlotEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final ItemStack stack;
    private EnumItemSlot slot;

    protected EntityEquipmentSlotEvent(ItemStack stack, EnumItemSlot slot) {
        this.stack = stack;
        this.slot = slot;
    }
    public static EnumItemSlot execute(EnumItemSlot slot, ItemStack stack) {
        EntityEquipmentSlotEvent event = new EntityEquipmentSlotEvent(stack, slot);
        Bukkit.getPluginManager().callEvent(event);
        return event.getSlot();
    }

    public ItemStack getItemStack() { return stack; }
    public EnumItemSlot getSlot() { return slot; }
    public void setSlot(EnumItemSlot slot) { this.slot = slot; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
