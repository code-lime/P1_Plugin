package org.lime.gp.module.loot;

import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterParameter;

import java.util.*;

public interface IPopulateLoot extends IFilterData<IPopulateLoot> {
    record Variable<T>(IFilterParameter<IPopulateLoot, ?> parameter, T value){}
    static <T>Variable<T> var(IFilterParameter<IPopulateLoot, ?> parameter, T value) { return new Variable<>(parameter, value); }
    static IPopulateLoot of(World world, Collection<Variable<?>> variables) {
        Map<IFilterParameter<IPopulateLoot, ?>, Object> map = new HashMap<>();
        variables.forEach(v -> map.put(v.parameter, v.value));
        return new IPopulateLoot() {
            @Override public Optional<IBlockData> blockData() { return Optional.empty(); }
            @Override public Optional<Collection<String>> tags() { return Optional.empty(); }
            @Override public boolean has(IFilterParameter<IPopulateLoot, ?> parameter) { return map.containsKey(parameter); }
            @Override public <T> T get(IFilterParameter<IPopulateLoot, T> parameter) {
                T value = (T)map.get(parameter);
                if (value == null) throw new NoSuchElementException(parameter.name());
                return value;
            }
            @Override public <T> Optional<T> getOptional(IFilterParameter<IPopulateLoot, T> parameter) {
                return Optional.ofNullable((T)map.get(parameter));
            }
            @Override public <T> T getOrDefault(IFilterParameter<IPopulateLoot, T> parameter, T def) {
                return (T)map.getOrDefault(parameter, (Object)def);
            }

            @Override public World world() { return world; }
        };
    }
}





