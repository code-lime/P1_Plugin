package org.lime.gp.filter;

import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.gp.lime;

public class AllFilter<T extends IFilterData<T>> extends ListFilter<T> {
    public AllFilter(IFilterInfo<T> filterInfo, String argLine) { super(filterInfo, argLine); }
    @Override public boolean isFilter(T data) { return filters.stream().allMatch(v -> v.isFilter(data)); }
}
