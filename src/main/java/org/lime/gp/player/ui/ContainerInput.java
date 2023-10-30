package org.lime.gp.player.ui;

import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.InventoryClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.state.IBlockData;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.event.inventory.ClickType;
import org.lime.gp.player.inventory.gui.InterfaceManager;

import java.util.Objects;

public abstract class ContainerInput extends ContainerAnvil {
    public ContainerInput(int syncId, net.minecraft.world.entity.player.PlayerInventory inventory) { super(syncId, inventory); }

    public abstract boolean isValid();

    public abstract net.minecraft.world.item.ItemStack getInput();
    public abstract net.minecraft.world.item.ItemStack getCenter();
    public abstract net.minecraft.world.item.ItemStack getOutput();

    public void clickInput(EntityHuman human, ClickType click) {}
    public void clickCenter(EntityHuman human, ClickType click) {}
    public void clickOutput(EntityHuman human, ClickType click) {}

    public abstract void input(String line);

    public Slot createPlayerSlot(Slot slot) { return slot; }

    @Override protected Slot addSlot(Slot slot) {
        slot.index = this.slots.size();
        return super.addSlot(slot.container == player.getInventory() ? createPlayerSlot(slot) : new InterfaceManager.AbstractBaseSlot(slot) {
            @Override public boolean isPacketOnly() { return true; }
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
            @Override public net.minecraft.world.item.ItemStack getItem() {
                return switch (index) {
                    case 0 -> getInput();
                    case 1 -> getCenter();
                    case 2 -> getOutput();
                    default -> net.minecraft.world.item.ItemStack.EMPTY;
                };
            }
            @Override public void onSlotClick(EntityHuman human, InventoryClickType type, ClickType click) {
                switch (index) {
                    case 0 -> clickInput(human, click);
                    case 1 -> clickCenter(human, click);
                    case 2 -> clickOutput(human, click);
                }
            }
            @Override public boolean mayPickup(EntityHuman human) { return false; }
        });
    }

    @Override public boolean setItemName(String newItemName) {
        newItemName = StringUtils.isBlank(newItemName) ? "" : newItemName;
        if (Objects.equals(itemName, newItemName)) return false;
        itemName = newItemName;
        input(itemName);

        sendAllDataToRemote();
        broadcastChanges();
        return true;
    }

    @Override public boolean stillValid(EntityHuman player) { return isValid(); }
    @Override protected boolean isValidBlock(IBlockData state) { return isValid(); }
}
