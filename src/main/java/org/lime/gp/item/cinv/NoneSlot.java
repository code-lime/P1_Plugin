package org.lime.gp.item.cinv;

import net.minecraft.world.inventory.Slot;

public final class NoneSlot extends BaseActionSlot {
    public NoneSlot(Slot slot, ViewData view, ViewContainer.SlotType slotType, int slotTypeIndex) {
        super(slot, view, slotType, slotTypeIndex);
    }

    @Override
    public net.minecraft.world.item.ItemStack item() {
        return ViewContainer.NONE;
    }
}
