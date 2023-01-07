package org.lime.gp.block.component.data.cauldron;

import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import org.bukkit.Color;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.item.Items;
import org.lime.gp.lime;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Recipe {
    public final String key;
    public final List<Step> items;
    public final String result;
    public final Color color;

    private Recipe(String key, String result, Color color, Step... items) {
        this(key, result, color, Arrays.asList(items));
    }

    private Recipe(String key, String result, Color color, List<Step> items) {
        this.key = key;
        this.result = result;
        this.items = items;
        this.color = color == null ? Color.fromRGB(key.hashCode() & 0xFFFFFF) : color;

        if (Items.hasItem(result)) return;
        lime.logOP("CauldronRecipe." + key + " result '" + result + "' not founded in items!");
    }

    public boolean check(List<LoadedStep> steps) {
        int size = items.size();
        if (steps.size() != size) return false;
        for (int i = 0; i < size; i++)
            if (!steps.get(i).compare(items.get(i)))
                return false;
        return true;
    }

    public static Recipe create(String key, String result, Color color, Step... items) {
        return new Recipe(key, result, color, items);
    }

    public static Recipe parse(String key, JsonObject json) {
        return new Recipe(
                key,
                json.get("result").getAsString(),
                json.has("color") ? ChatColorHex.of(json.get("color").getAsString()).toBukkitColor() : null,
                Streams.stream(json.get("steps").getAsJsonArray().iterator()).map(v -> Step.parse(v.getAsJsonObject())).collect(Collectors.toList())
        );
    }

    @Override
    public String toString() {
        return "Recipe." + key;
    }
}
