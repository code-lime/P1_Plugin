package org.lime.gp.filter;

import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.system.range.IRange;
import org.lime.system.utils.RandomUtils;

public class RandomFilter<T extends IFilterData<T>> implements IFilter<T> {
    public final IRange range;
    public RandomFilter(IFilterInfo<T> filterInfo, String range) { this.range = IRange.parse(range); }
    public boolean isFilter(T data) { return RandomUtils.rand(0.0, 1.0) <= range.getValue(1.0); }
}