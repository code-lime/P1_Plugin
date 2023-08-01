package org.lime.gp.filter.data;

import org.lime.gp.filter.IFilter;

import java.util.Optional;

public interface IFilterParameterInfo<TData extends IFilterData<TData>, TValue> {
    String name();
    void appendTo(IFilterData<TData> data, AppendFunction function);
    Optional<TValue> getValue(IFilterData<TData> data);
    boolean hasKey(IFilterData<TData> data);
    default boolean emptyKey(IFilterData<TData> data) { return !hasKey(data); }
    IFilter<TData> createVariableFilter(String value);

    IFilterParameter<TData, ?> type();
}
