package org.lime.gp.craft.slot.output;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.item.loot.RandomLoot;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class WeightOutputSlot implements IOutputSlot {
    public record WeightData(IOutputSlot item, double weight) {
        public static WeightData of(JsonObject json) {
            return new WeightData(IOutputSlot.of(json.get("item")), json.get("weight").getAsDouble());
        }
    }

    private final List<WeightData> weights = new ArrayList<>();
    private final double totalWeight;
    public WeightOutputSlot(Stream<WeightData> weights) {
        weights.forEach(this.weights::add);
        if (this.weights.isEmpty()) throw new IllegalArgumentException("Items of output is empty");
        totalWeight = this.weights.stream().mapToDouble(v -> v.weight).sum();
        if (totalWeight <= 0) throw new IllegalArgumentException("Weight of output is <= 0");
    }

    private IOutputSlot random() {
        double value = system.rand(0, totalWeight);
        for (WeightData item : weights) {
            value -= item.weight;
            if (value <= 0) return item.item;
        }
        return weights.get(weights.size() - 1).item;
    }

    @Override public ItemStack create() { return random().create(); }
    @Override public ItemStack apply(ItemStack item, boolean copy) { return random().apply(item, copy); }
    @Override public net.minecraft.world.item.ItemStack nms(boolean isPreview) { return random().nms(isPreview); }
}
