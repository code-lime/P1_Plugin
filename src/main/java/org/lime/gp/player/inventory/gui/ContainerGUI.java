package org.lime.gp.player.inventory.gui;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.Slot;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.system.execute.Action1;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;

public abstract class ContainerGUI extends ContainerChest {
    public ContainerGUI(int syncId, PlayerInventory playerInventory, int rows) {
        this(syncId, playerInventory, ReadonlyInventory.ofNMS(NonNullList.withSize(rows * 9, net.minecraft.world.item.ItemStack.EMPTY)), rows);
    }
    private ContainerGUI(int syncId, PlayerInventory playerInventory, ReadonlyInventory inventory, int rows) {
        this(syncId, playerInventory, Toast.of(null), inventory, rows);
    }
    private ContainerGUI(int syncId, PlayerInventory playerInventory, Toast1<Action1<EntityHuman>> link, ReadonlyInventory inventory, int rows) {
        super(switch (rows) {
            case 1 -> Containers.GENERIC_9x1;
            case 2 -> Containers.GENERIC_9x2;
            case 3 -> Containers.GENERIC_9x3;
            case 4 -> Containers.GENERIC_9x4;
            case 5 -> Containers.GENERIC_9x5;
            case 6 -> Containers.GENERIC_9x6;
            default -> throw new IllegalArgumentException("Rows '"+rows+"' not in [1..6]");
        }, syncId, playerInventory, inventory.withCloseAction(human -> link.val0.invoke(human)), rows);
        link.val0 = this::onClose;
    }

    public Slot createInventorySlot(Slot slot) { return slot; }
    public Slot createPlayerSlot(Slot slot) { return slot; }

    public void onClose(EntityHuman human) {}

    @Override protected Slot addSlot(Slot slot) {
        slot.index = this.slots.size();
        return super.addSlot(slot.container instanceof PlayerInventory ? createPlayerSlot(slot) : createInventorySlot(slot));
    }
}
