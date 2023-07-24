package org.lime.gp.module.loot;

import net.minecraft.world.level.World;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;

import java.util.*;

public interface IPopulateLoot {
    boolean has(LootContextParameter<?> parameter);
    <T>T get(LootContextParameter<T> parameter);
    <T>Optional<T> getOptional(LootContextParameter<T> parameter);
    <T>T getOrDefault(LootContextParameter<T> parameter, T def);
    World getWorld();

    record Variable<T>(LootContextParameter<T> parameter, T value){}
    static <T>Variable<T> var(LootContextParameter<T> parameter, T value) {
        return new Variable<>(parameter, value);
    }
    static IPopulateLoot of(World world, Collection<Variable<?>> variables) {
        Map<LootContextParameter<?>, Object> map = new HashMap<>();
        variables.forEach(v -> map.put(v.parameter, v.value));
        return new IPopulateLoot() {
            @Override public boolean has(LootContextParameter<?> parameter) { return map.containsKey(parameter); }
            @Override public <T> T get(LootContextParameter<T> parameter) {
                T value = (T)map.get(parameter);
                if (value == null) throw new NoSuchElementException(parameter.getName().toString());
                return value;
            }
            @Override public <T> Optional<T> getOptional(LootContextParameter<T> parameter) {
                return Optional.ofNullable((T)map.get(parameter));
            }
            @Override public <T> T getOrDefault(LootContextParameter<T> parameter, T def) {
                return (T)map.getOrDefault(parameter, (Object)def);
            }
            @Override public World getWorld() { return world; }
        };
    }
}





