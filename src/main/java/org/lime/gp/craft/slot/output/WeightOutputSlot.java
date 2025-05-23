package org.lime.gp.craft.slot.output;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import org.lime.system.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
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
        double value = RandomUtils.rand(0, totalWeight);
        for (WeightData item : weights) {
            value -= item.weight;
            if (value <= 0) return item.item;
        }
        return weights.get(weights.size() - 1).item;
    }

    @Override public ItemStack modify(ItemStack item, boolean copy, IOutputVariable variable) { return random().modify(item, copy, variable); }
    @Override public ItemStack create(boolean isPreview, IOutputVariable variable) { return random().create(isPreview, variable); }
    @Override public int maxStackSize() { return weights.stream().mapToInt(v -> v.item.maxStackSize()).max().orElseThrow(); }
    @Override public boolean test(ItemStack item) {
        for (WeightData data : weights)
            if (data.item.test(item))
                return true;
        return false;
    }
}
