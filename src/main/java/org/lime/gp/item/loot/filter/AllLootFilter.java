package org.lime.gp.item.loot.filter;

import org.lime.gp.module.PopulateLootEvent;

public class AllLootFilter extends ListLootFilter {
    public AllLootFilter(String argLine) {
        super(argLine);
    }
    @Override public boolean isFilter(PopulateLootEvent loot) {
        return filters.stream().allMatch(v -> v.isFilter(loot));
    }
}
