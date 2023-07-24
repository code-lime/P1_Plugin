package org.lime.gp.item.loot.filter;

import org.lime.gp.module.loot.IPopulateLoot;

public class AnyLootFilter extends ListLootFilter {
    public AnyLootFilter(String argLine) {
        super(argLine);
    }
    @Override public boolean isFilter(IPopulateLoot loot) {
        return filters.stream().anyMatch(v -> v.isFilter(loot));
    }
}
