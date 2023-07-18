package org.lime.gp.item.loot;

import java.util.List;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.module.PopulateLootEvent;

import com.google.gson.JsonElement;

public abstract class ILoot {
    public abstract List<ItemStack> generate();
    public List<ItemStack> generateFilter(PopulateLootEvent loot) {
        return generate();
    }

    public static ILoot parse(JsonElement json) {
        if (json.isJsonPrimitive()) return new SingleLoot(json.getAsJsonPrimitive());
        else if (json.isJsonArray()) return new MultiLoot(json.getAsJsonArray());
        else if (json.isJsonNull()) return EmptyLoot.Instance;
        else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            return obj.has("type") ? switch (obj.get("type").getAsString()) {
                case "random" -> new RandomLoot(obj);
                default -> throw new IllegalArgumentException("[LOOT] Type '"+obj.get("type").getAsString()+"' not supported");
            } : new FilterLoot(obj);
        }
        throw new IllegalArgumentException("[LOOT] Error parse LootTable");
    }
}