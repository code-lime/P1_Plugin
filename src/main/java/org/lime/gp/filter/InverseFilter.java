package org.lime.gp.filter;

import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;

public class InverseFilter<T extends IFilterData<T>> implements IFilter<T> {
    private final IFilter<T> filter;
    public InverseFilter(IFilter<T> filter) { this.filter = filter; }
    @Override public boolean isFilter(T data) { return !filter.isFilter(data); }
}
