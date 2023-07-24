package org.lime.gp.item.loot.filter;

import org.lime.gp.module.loot.IPopulateLoot;

public class AllLootFilter extends ListLootFilter {
    public AllLootFilter(String argLine) {
        super(argLine);
    }
    @Override public boolean isFilter(IPopulateLoot loot) {
        return filters.stream().allMatch(v -> v.isFilter(loot));
    }
}
