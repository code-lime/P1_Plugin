package org.lime.gp.filter;

import org.lime.gp.filter.data.IFilterData;

public class StaticFilter<T extends IFilterData<T>> implements IFilter<T> {
    private final boolean result;
    public StaticFilter(boolean result) { this.result = result; }
    @Override public boolean isFilter(T data) { return this.result; }
}
