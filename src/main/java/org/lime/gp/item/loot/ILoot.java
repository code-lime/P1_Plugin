package org.lime.gp.item.loot;

import java.util.List;
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
        else if (json.isJsonNull()) return new EmptyLoot();
        else if (json.isJsonObject()) return new FilterLoot(json.getAsJsonObject());
        throw new IllegalArgumentException("[LOOT] Error parse LootTable");
    }

    /*public static system.Toast2<List<system.Toast2<Material, ILoot>>, List<system.Toast3<Material, CraftManager.RecipeSlot, ILoot>>> ParseTable(JsonObject json) {
        List<system.Toast3<Material, CraftManager.RecipeSlot, ILoot>> breakLootTable = new ArrayList<>();
        List<system.Toast2<Material, ItemManager.ILoot>> anyBreakLootTable = new ArrayList<>();
        json.entrySet().forEach(_kv -> {

            if (_kv.getKey().equals("Default")) {
                _kv.getValue().getAsJsonObject().entrySet().forEach(kv -> {
                    ItemManager.GetRegexMaterials(kv.getKey()).forEach(mat -> anyBreakLootTable.add(system.toast(mat, ILoot.parse(mat.name(), kv.getValue()))));
                });
                return;
            }

            CraftManager.RecipeSlot islot = CraftManager.ParseSlot(_kv.getKey(), true);
            _kv.getValue().getAsJsonObject().entrySet().forEach(kv -> {
                ItemManager.GetRegexMaterials(kv.getKey()).forEach(mat -> breakLootTable.add(system.toast(mat, islot, ILoot.parse(mat.name(), kv.getValue()))));
            });
        });
        return system.toast(anyBreakLootTable, breakLootTable);
    }*/
}