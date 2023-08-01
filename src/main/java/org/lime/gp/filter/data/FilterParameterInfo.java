package org.lime.gp.filter.data;

import net.minecraft.world.level.World;
import org.lime.gp.filter.IFilter;
import org.lime.gp.module.loot.IPopulateLoot;

import java.util.Optional;

public class FilterParameterInfo<TData extends IFilterData<TData>, TValue> implements IFilterParameterInfo<TData, TValue> {
    public final String name;
    public final IFilterParameter<TData, TValue> type;

    public interface IAction<TData extends IFilterData<TData>, TValue> {
        String convert(TValue value, World world);
        IFilter<TData> createFilter(String rawValue, FilterParameterInfo<TData, TValue> info);
    }

    private final IAction<TData, TValue> action;

    public FilterParameterInfo(
            String name,
            IFilterParameter<TData, TValue> type,
            IAction<TData, TValue> action
    ) {
        this.name = name;
        this.type = type;
        this.action = action;
    }

    @Override public String name() { return name; }
    @Override public void appendTo(IFilterData<TData> data, AppendFunction function) {
        data.getOptional(type).ifPresentOrElse(
                v -> {
                    String value = action.convert(v, data.world());
                    if (value == null || value.isEmpty()) function.appendWithoutValue(name);
                    else function.appendValue(name, value);
                },
                () -> function.appendNan(name)
        );
    }
    @Override public Optional<TValue> getValue(IFilterData<TData> data) { return data.getOptional(type); }
    @Override public boolean hasKey(IFilterData<TData> data) { return data.has(type); }
    @Override public IFilter<TData> createVariableFilter(String value) { return action.createFilter(value, this); }
    @Override public IFilterParameter<TData, ?> type() { return type; }
}











