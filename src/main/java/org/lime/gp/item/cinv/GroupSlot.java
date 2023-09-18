package org.lime.gp.item.cinv;

import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.InventoryClickType;
import net.minecraft.world.inventory.Slot;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.inventory.ClickType;
import org.lime.gp.lime;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.Objects;

public final class GroupSlot extends BaseActionSlot {
    public GroupSlot(Slot slot, ViewData view, ViewContainer.SlotType slotType, int slotTypeIndex) {
        super(slot, view, slotType, slotTypeIndex);
    }

    private final Toast2<String, net.minecraft.world.item.ItemStack> lastItem = Toast.of(null, ViewContainer.NONE);

    @Override public net.minecraft.world.item.ItemStack item() {
        int count = view.groups.size();
        int showLength = view.groupsShowLength();
        int delta = count - showLength;

        if (delta <= 0) view.groupOffset(0);
        else view.groupOffset(Math.max(0, Math.min(delta, view.groupOffset())));
        /*
        if (view.groupOffset > delta) view.groupOffset = delta;
        else if (view.groupOffset < 0) view.groupOffset = 0;
        */

        var group = view.groups.get(view.groupOffset() + slotTypeIndex);
        String currentName = group == null ? null : group.name();
        if (Objects.equals(currentName, lastItem.val0)) return lastItem.val1;
        lastItem.val0 = currentName;
        if (currentName == null) lastItem.val1 = ViewContainer.NONE;
        else lastItem.val1 = CraftItemStack.asNMSCopy(group.show());
        return lastItem.val1;
    }

    @Override public void onSlotClick(EntityHuman human, InventoryClickType type, ClickType click) {
        int index = view.groupOffset() + slotTypeIndex;
        var group = view.groups.get(index);
        if (group == null) return;
        view.groupIndex(view.groupOffset() + slotTypeIndex);
        view.itemsOffset(0);
        view.container.changeTitle(human, "Selected: " + group.name());
    }
}
