package org.lime.gp.craft.slot.output;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class LevelOutputSlot implements IOutputSlot {
    private final Integer work;
    private final IOutputSlot other;
    private final Map<Integer, IOutputSlot> level = new HashMap<>();

    public LevelOutputSlot(Map<Integer, IOutputSlot> level, IOutputSlot other, Integer work) {
        this.level.putAll(level);
        this.other = other;
        this.work = work;
    }

    private IOutputSlot result(IOutputVariable variable) {
        return work == null
                ? variable.getLevel().map(level::get).orElse(other)
                : variable.getLevel(work).map(level::get).orElse(other);
    }
    @Override public ItemStack modify(ItemStack item, boolean copy, IOutputVariable variable) { return result(variable).modify(item, copy, variable); }
    @Override public ItemStack create(boolean isPreview, IOutputVariable variable) { return result(variable).create(isPreview, variable); }
    @Override public boolean test(ItemStack item) { return other.test(item); }
}

