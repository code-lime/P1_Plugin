package org.lime.gp.item.loot;

import java.util.List;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.module.loot.IPopulateLoot;

import com.google.gson.JsonElement;

public interface ILoot {
    List<ItemStack> generateLoot(IPopulateLoot loot);

    static ILoot parse(JsonElement json) {
        if (json.isJsonPrimitive()) return new SingleLoot(json.getAsString());
        else if (json.isJsonArray()) return new MultiLoot(json.getAsJsonArray());
        else if (json.isJsonNull()) return EmptyLoot.Instance;
        else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            return obj.has("type") ? switch (obj.get("type").getAsString()) {
                case "random" -> new RandomLoot(obj);
                case "js" -> new JavaScriptLoot(obj);
                case "variable" -> new VariableLoot(obj);
                default -> throw new IllegalArgumentException("[LOOT] Type '"+obj.get("type").getAsString()+"' not supported");
            } : new FilterLoot(obj);
        }
        throw new IllegalArgumentException("[LOOT] Error parse LootTable");
    }
}