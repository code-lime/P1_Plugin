package org.lime.gp.player.module.cinv;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.InventoryClickType;
import net.minecraft.world.inventory.Slot;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.inventory.ClickType;

public final class ItemSlot extends BaseActionSlot {
    public ItemSlot(Slot slot, ViewData view, ViewContainer.SlotType slotType, int slotTypeIndex) {
        super(slot, view, slotType, slotTypeIndex);
    }

    @Override public net.minecraft.world.item.ItemStack item() {
        var group = view.groups.get(view.groupIndex());
        if (group == null) {
            view.groupIndex(0);
            return ViewContainer.NONE;
        }
        int count = group.size();
        int stepLength = view.itemsStepLength();
        count = ((count / stepLength) + (count % stepLength > 0 ? 1 : 0)) * stepLength;
        int showLength = view.itemsShowLength();
        int delta = count - showLength;
        if (delta <= 0) view.itemsOffset(0);
        else if (view.itemsOffset() > delta) view.itemsOffset(delta);
        else if (view.itemsOffset() < 0) view.itemsOffset(0);

        ItemElement element = group.get(view.itemsOffset() + slotTypeIndex);
        return element == null ? ViewContainer.NONE : CraftItemStack.asNMSCopy(element.show());
    }

    @Override public void onSlotClick(EntityHuman human, InventoryClickType type, ClickType click) {
        if (human instanceof EntityPlayer player) {
            var group = view.groups.get(view.groupIndex());
            if (group == null) return;
            ItemElement element = group.get(view.itemsOffset() + slotTypeIndex);
            if (element == null) return;
            element.click(view.container, player, click);
        }
    }
}
