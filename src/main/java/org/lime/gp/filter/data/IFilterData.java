package org.lime.gp.filter.data;

import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;

import java.util.Collection;
import java.util.Optional;

public interface IFilterData<TData extends IFilterData<TData>> {
    World world();
    Optional<IBlockData> blockData();
    Optional<Collection<String>> tags();

    boolean has(IFilterParameter<TData, ?> parameter);
    <TValue> TValue get(IFilterParameter<TData, TValue> parameter);
    <TValue>Optional<TValue> getOptional(IFilterParameter<TData, TValue> parameter);
    <TValue> TValue getOrDefault(IFilterParameter<TData, TValue> parameter, TValue def);
}