package org.lime.gp.module.loot;

import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import org.lime.gp.filter.data.IFilterParameter;

import java.util.Optional;

public record LootParameter<T>(LootContextParameter<T> nms) implements IFilterParameter<IPopulateLoot, T> {
    public static <T> LootParameter<T> of(LootContextParameter<T> nms) {
        return new LootParameter<>(nms);
    }
    @Override public String name() { return nms.getName().getPath(); }

    public static <T> Optional<LootContextParameter<T>> of(IFilterParameter<IPopulateLoot, T> parameter) {
        return Parameters.raw(parameter);
    }
}
