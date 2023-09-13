package org.lime.gp.module.mobs;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterParameter;

import java.util.*;

public interface IPopulateSpawn extends IFilterData<IPopulateSpawn> {
    record Variable<T>(IFilterParameter<IPopulateSpawn, ?> parameter, T value){}
    static <T> IPopulateSpawn.Variable<T> var(IFilterParameter<IPopulateSpawn, ?> parameter, T value) { return new Variable<>(parameter, value); }
    static IPopulateSpawn of(World world, Collection<Variable<?>> variables) {
        Map<IFilterParameter<IPopulateSpawn, ?>, Object> map = new HashMap<>();
        variables.forEach(v -> {
            if (v.value == null) return;
            map.put(v.parameter, v.value);
        });

        return new IPopulateSpawn() {
            @Override public Optional<IBlockData> blockData() { return getOptional(Parameters.FloorBlock); }
            @Override public Optional<Collection<String>> tags() { return getOptional(Parameters.ThisEntity).map(Entity::getTags); }
            @Override public boolean has(IFilterParameter<IPopulateSpawn, ?> parameter) { return map.containsKey(parameter); }
            @Override public <T> T get(IFilterParameter<IPopulateSpawn, T> parameter) {
                T value = (T)map.get(parameter);
                if (value == null) throw new NoSuchElementException(parameter.name());
                return value;
            }
            @Override public <T> Optional<T> getOptional(IFilterParameter<IPopulateSpawn, T> parameter) {
                return Optional.ofNullable((T)map.get(parameter));
            }
            @Override public <T> T getOrDefault(IFilterParameter<IPopulateSpawn, T> parameter, T def) {
                return (T)map.getOrDefault(parameter, (Object)def);
            }

            @Override public World world() { return world; }
        };
    }
}
