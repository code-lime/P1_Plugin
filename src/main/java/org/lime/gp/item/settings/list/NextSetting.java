package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.system;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Setting(name = "next") public class NextSetting extends ItemSetting<JsonElement> {
    private final Map<String, Double> next;
    private final double totalWeight;

    public NextSetting(ItemCreator creator, JsonElement json) {
        super(creator, json);
        if (json.isJsonArray()) this.next = json.getAsJsonArray()
                .asList()
                .stream()
                .collect(Collectors.toMap(JsonElement::getAsString, k -> 1.0));
        else if (json.isJsonObject()) this.next = json.getAsJsonObject()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().getAsDouble()));
        else this.next = Collections.singletonMap(json.getAsString(), 1.0);
        totalWeight = next.values().stream().mapToDouble(v -> v).sum();
        if (this.next.size() == 0) throw new IllegalArgumentException("Next count == 0");
        if (totalWeight <= 0) throw new IllegalArgumentException("Total weight <= 0");
    }

    public String next() {
        double value = system.rand(0, totalWeight);
        String last = null;
        for (Map.Entry<String, Double> item : next.entrySet()) {
            value -= item.getValue();
            if (value <= 0) return item.getKey();
            last = item.getKey();
        }
        return last;
    }
}