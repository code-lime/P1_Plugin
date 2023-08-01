package org.lime.gp.filter.data;

import java.util.Collection;
import java.util.Optional;

public interface IFilterInfo<TData extends IFilterData<TData>> {
    Optional<IFilterParameterInfo<TData, ?>> getParamInfo(String key);
    Collection<IFilterParameterInfo<TData, ?>> getAllParams();
}
