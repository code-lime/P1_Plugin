package org.lime.gp.item.loot;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexDocs;
import org.lime.docs.json.*;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.system.utils.RandomUtils;
import org.openjdk.nashorn.internal.scripts.JO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RandomLoot implements ILoot {
    public record LootWeight(ILoot loot, double weight) {
        public static LootWeight parse(JsonObject json) {
            return new LootWeight(ILoot.parse(json.get("loot")), json.get("weight").getAsDouble());
        }
        public List<ItemStack> generateFilter(IPopulateLoot loot) { return this.loot.generateLoot(loot); }
    }
    public final List<LootWeight> values = new ArrayList<>();
    public final double totalWeight;

    public RandomLoot(JsonObject json) {
        json.get("values").getAsJsonArray().forEach(item -> values.add(LootWeight.parse(item.getAsJsonObject())));
        totalWeight = values.stream().mapToDouble(v -> v.weight).sum();
    }

    private Optional<LootWeight> random() {
        if (totalWeight <= 0) return Optional.empty();
        int length = values.size();
        if (length == 0) return Optional.empty();
        double value = RandomUtils.rand(0, totalWeight);
        for (LootWeight item : values) {
            value -= item.weight;
            if (value <= 0) return Optional.of(item);
        }
        return Optional.of(values.get(length - 1));
    }

    @Override public List<ItemStack> generateLoot(IPopulateLoot loot) {
        return random().map(v -> v.generateFilter(loot)).orElseGet(Collections::emptyList);
    }

    public static JObject docs(IJElement loot) {
        return JObject.of(
                JProperty.require(IName.raw("values"), IJElement.anyList(
                        JObject.of(
                                JProperty.require(IName.raw("loot"), loot),
                                JProperty.require(IName.raw("weight"), IJElement.raw(1.5))
                        )
                ), IComment.join(
                        IComment.text("Суммирует "),
                        IComment.field("weight"),
                        IComment.text(" у всех элементов из "),
                        IComment.field("values"),
                        IComment.text(", выбирает рандомное значение и использует его "),
                        IComment.field("loot")
                ))
        );
    }
}
