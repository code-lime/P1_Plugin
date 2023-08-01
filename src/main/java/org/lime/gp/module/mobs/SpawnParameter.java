package org.lime.gp.module.mobs;

import org.lime.gp.filter.data.IFilterParameter;

public record SpawnParameter<T>(String name) implements IFilterParameter<IPopulateSpawn, T> {
    public static <T> SpawnParameter<T> of(String name) {
        return new SpawnParameter<>(name);
    }
}
