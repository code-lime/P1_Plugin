package org.lime.gp.filter;

import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;

public class AnyFilter<T extends IFilterData<T>> extends ListFilter<T> {
    public AnyFilter(IFilterInfo<T> filterInfo, String argLine) { super(filterInfo, argLine); }
    @Override public boolean isFilter(T loot) { return filters.stream().anyMatch(v -> v.isFilter(loot)); }
}
