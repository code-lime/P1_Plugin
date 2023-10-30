package org.lime.gp.player.module.cinv;

import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Slot;
import org.lime.gp.player.inventory.gui.InterfaceManager;

public abstract class BaseActionSlot extends InterfaceManager.AbstractSlot {
    protected final ViewData view;
    protected final ViewContainer.SlotType slotType;
    protected final int slotTypeIndex;

    public BaseActionSlot(Slot slot, ViewData view, ViewContainer.SlotType slotType, int slotTypeIndex) {
        super(slot);
        this.view = view;
        this.slotType = slotType;
        this.slotTypeIndex = slotTypeIndex;
    }

    public abstract net.minecraft.world.item.ItemStack item();

    @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return true; }
    @Override public void set(net.minecraft.world.item.ItemStack stack) {}
    @Override public net.minecraft.world.item.ItemStack getItem() { return item(); }
    @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
    @Override public boolean isPacketOnly() { return true; }

    public ViewContainer.SlotType slotType() { return slotType; }
}
