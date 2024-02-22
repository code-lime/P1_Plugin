package org.lime.gp.craft.slot.output;

import net.minecraft.world.item.ItemStack;
import org.lime.system.utils.RandomUtils;

import java.util.*;
import java.util.stream.Stream;

public class OneOfOutputSlot implements IOutputSlot {
    private final List<IOutputSlot> slots = new ArrayList<>();

    public OneOfOutputSlot(Stream<IOutputSlot> slots) {
        slots.forEach(this.slots::add);
        if (this.slots.isEmpty()) throw new IllegalArgumentException("Items of output is empty");
    }

    private IOutputSlot oneOf() { return RandomUtils.rand(slots); }
    @Override public ItemStack modify(ItemStack item, boolean copy, IOutputVariable variable) { return oneOf().modify(item, copy, variable); }
    @Override public ItemStack create(boolean isPreview, IOutputVariable variable) { return oneOf().create(isPreview, variable); }
    @Override public int maxStackSize() { return slots.stream().mapToInt(IOutputSlot::maxStackSize).max().orElseThrow(); }
    @Override public boolean test(ItemStack item) {
        for (IOutputSlot slot : slots)
            if (slot.test(item))
                return true;
        return false;
    }
}
