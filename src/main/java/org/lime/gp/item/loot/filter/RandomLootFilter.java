package org.lime.gp.item.loot.filter;

import org.lime.gp.module.PopulateLootEvent;
import org.lime.system;

public class RandomLootFilter implements ILootFilter {
    public final system.IRange range;
    public RandomLootFilter(String range) {
        this.range = system.IRange.parse(range);
    }

    public boolean isFilter(PopulateLootEvent e) {
        return system.rand(0.0, 1.0) <= range.getValue(1.0);
    }
}