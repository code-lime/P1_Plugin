package org.lime.gp.filter.data;

import net.minecraft.world.level.World;
import org.lime.gp.filter.IFilter;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.Optional;

public interface IFilterParameter<TData extends IFilterData<TData>, TValue> {
    String name();

    default IFilterParameterInfo<TData, TValue> createInfoEqualsIgnoreCase(String name, Func1<TValue, String> convert) {
        return createInfo(name, new FilterParameterInfo.IAction<>() {
            @Override public String convert(TValue value, World world) { return convert.invoke(value); }
            @Override public IFilter<TData> createFilter(String rawValue, FilterParameterInfo<TData, TValue> info) {
                return loot -> loot.getOptional(info.type).map(v -> convert(v, loot.world())).map(rawValue::equalsIgnoreCase).orElse(false);
            }
        });
    }
    default IFilterParameterInfo<TData, TValue> createInfoEqualsIgnoreCase(String name, Func2<TValue, World, String> convert) {
        return createInfo(name, new FilterParameterInfo.IAction<>() {
            @Override public String convert(TValue value, World world) { return convert.invoke(value, world); }
            @Override public IFilter<TData> createFilter(String rawValue, FilterParameterInfo<TData, TValue> info) {
                return loot -> loot.getOptional(info.type).map(v -> convert(v, loot.world())).map(rawValue::equalsIgnoreCase).orElse(false);
            }
        });
    }
    default <TFilter>IFilterParameterInfo<TData, TValue> createInfoWorldFilter(String name, Func2<TValue, World, String> convert, Func1<String, TFilter> createFilter, Func3<TFilter, TValue, World, Boolean> filter) {
        return createInfo(name, new FilterParameterInfo.IAction<>() {
            @Override public String convert(TValue value, World world) { return convert.invoke(value, world); }
            @Override public IFilter<TData> createFilter(String rawValue, FilterParameterInfo<TData, TValue> info) {
                TFilter _filter = createFilter.invoke(rawValue);
                return loot -> loot.getOptional(info.type).map(v -> filter.invoke(_filter, v, loot.world())).orElse(false);
            }
        });
    }
    default <TFilter>IFilterParameterInfo<TData, TValue> createInfoFilter(String name, Func1<TValue, String> convert, Func1<String, TFilter> createFilter, Func2<TFilter, TValue, Boolean> filter) {
        return createInfo(name, new FilterParameterInfo.IAction<>() {
            @Override public String convert(TValue value, World world) { return convert.invoke(value); }
            @Override public IFilter<TData> createFilter(String rawValue, FilterParameterInfo<TData, TValue> info) {
                TFilter _filter = createFilter.invoke(rawValue);
                return loot -> loot.getOptional(info.type).map(v -> filter.invoke(_filter, v)).orElse(false);
            }
        });
    }
    default IFilterParameterInfo<TData, TValue> createInfo(String name, FilterParameterInfo.IAction<TData, TValue> action) {
        return new FilterParameterInfo<>(name, this, action);
    }
}
