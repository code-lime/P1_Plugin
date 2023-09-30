package org.lime.gp.player.module.cinv;

import net.minecraft.world.inventory.Slot;

public abstract class StaticClickSlot extends BaseActionSlot {
    private final net.minecraft.world.item.ItemStack item;

    public StaticClickSlot(Slot slot, ViewData view, ViewContainer.SlotType slotType, int slotTypeIndex, net.minecraft.world.item.ItemStack item) {
        super(slot, view, slotType, slotTypeIndex);
        this.item = item;
    }

    @Override public net.minecraft.world.item.ItemStack item() { return item; }
}
