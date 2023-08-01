package org.lime.gp.craft.slot.output;

import net.minecraft.world.item.ItemStack;
import org.lime.system;

public class DurabilityOutputSlot implements IOutputSlot {
    private final IOutputSlot item;
    private final system.IRange durability;

    public DurabilityOutputSlot(IOutputSlot item, system.IRange durability) {
        this.item = item;
        this.durability = durability;
    }


    @Override public ItemStack modify(ItemStack item, boolean copy, IOutputVariable variable) {
        ItemStack result = this.item.modify(item, copy, variable);
        int max = result.getMaxDamage();
        int value = durability.getIntValue(max);
        result.setDamageValue(max - value);
        return result;
    }

    @Override public ItemStack create(boolean isPreview, IOutputVariable variable) {
        ItemStack result = this.item.create(isPreview, variable);
        int max = result.getMaxDamage();
        int value = durability.getIntValue(max);
        result.setDamageValue(max - value);
        return result;
    }
}
