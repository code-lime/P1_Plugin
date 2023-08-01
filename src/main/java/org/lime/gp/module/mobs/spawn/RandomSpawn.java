package org.lime.gp.module.mobs.spawn;

import com.google.gson.JsonObject;
import org.lime.gp.module.mobs.IMobCreator;
import org.lime.gp.module.mobs.IPopulateSpawn;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RandomSpawn implements ISpawn {
    public record SpawnWeight(ISpawn spawn, double weight) {
        public static SpawnWeight parse(JsonObject json) {
            return new SpawnWeight(ISpawn.parse(json.get("spawn")), json.get("weight").getAsDouble());
        }
        public Optional<IMobCreator> generateFilter(IPopulateSpawn populate) { return this.spawn.generateMob(populate); }
    }
    public final List<SpawnWeight> values = new ArrayList<>();
    public final double totalWeight;

    public RandomSpawn(JsonObject json) {
        json.get("values").getAsJsonArray().forEach(item -> values.add(SpawnWeight.parse(item.getAsJsonObject())));
        totalWeight = values.stream().mapToDouble(v -> v.weight).sum();
    }

    private Optional<SpawnWeight> random() {
        if (totalWeight <= 0) return Optional.empty();
        int length = values.size();
        if (length == 0) return Optional.empty();
        double value = system.rand(0, totalWeight);
        for (SpawnWeight item : values) {
            value -= item.weight;
            if (value <= 0) return Optional.of(item);
        }
        return Optional.of(values.get(length - 1));
    }

    @Override public Optional<IMobCreator> generateMob(IPopulateSpawn populate) {
        return random().flatMap(v -> v.generateFilter(populate));
    }
}
