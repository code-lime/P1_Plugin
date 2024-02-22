package org.lime.gp.craft.slot.output;

import net.minecraft.world.item.ItemStack;

public class PreviewOutputSlot implements IOutputSlot {
    private final IOutputSlot preview;
    private final IOutputSlot item;

    public PreviewOutputSlot(IOutputSlot preview, IOutputSlot item) {
        this.preview = preview;
        this.item = item;
    }

    @Override public ItemStack modify(ItemStack item, boolean copy, IOutputVariable variable) { return this.item.modify(item, copy, variable); }
    @Override public ItemStack create(boolean isPreview, IOutputVariable variable) { return isPreview ? preview.create(true, variable) : item.create(false, variable); }
    @Override public int maxStackSize() { return item.maxStackSize(); }
    @Override public boolean test(ItemStack item) { return this.item.test(item); }
}
