package org.lime.gp.craft.slot.output;

import org.bukkit.inventory.ItemStack;
import org.lime.gp.item.data.IItemCreator;

import java.util.Optional;

public class PreviewOutputSlot implements IOutputSlot {
    private final IOutputSlot preview;
    private final IOutputSlot item;

    public PreviewOutputSlot(IOutputSlot preview, IOutputSlot item) {
        this.preview = preview;
        this.item = item;
    }

    @Override public ItemStack create() { return this.item.create(); }
    @Override public ItemStack apply(ItemStack item, boolean copy) { return this.item.apply(item, copy); }
    @Override public net.minecraft.world.item.ItemStack nms(boolean isPreview) { return isPreview ? preview.nms(true) : item.nms(false); }
}
