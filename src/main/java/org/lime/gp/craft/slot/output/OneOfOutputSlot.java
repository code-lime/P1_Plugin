package org.lime.gp.craft.slot.output;

import org.bukkit.inventory.ItemStack;
import org.lime.gp.item.data.IItemCreator;
import org.lime.system;

import java.util.*;
import java.util.stream.Stream;

public class OneOfOutputSlot implements IOutputSlot {
    private final List<IOutputSlot> slots = new ArrayList<>();

    public OneOfOutputSlot(Stream<IOutputSlot> slots) {
        slots.forEach(this.slots::add);
        if (this.slots.isEmpty()) throw new IllegalArgumentException("Items of output is empty");
    }

    private IOutputSlot oneOf() { return system.rand(slots); }
    @Override public ItemStack create() { return oneOf().create(); }
    @Override public ItemStack apply(ItemStack item, boolean copy) { return oneOf().apply(item, copy); }
    @Override public net.minecraft.world.item.ItemStack nms(boolean isPreview) { return oneOf().nms(isPreview); }
}
