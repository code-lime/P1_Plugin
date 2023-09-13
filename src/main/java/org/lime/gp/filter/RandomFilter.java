package org.lime.gp.filter;

import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.system;

public class RandomFilter<T extends IFilterData<T>> implements IFilter<T> {
    public final system.IRange range;
    public RandomFilter(IFilterInfo<T> filterInfo, String range) { this.range = system.IRange.parse(range); }
    public boolean isFilter(T data) { return system.rand(0.0, 1.0) <= range.getValue(1.0); }
}