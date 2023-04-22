package org.lime.gp.item.loot;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.inventory.ItemStack;
import org.lime.system;
import org.lime.gp.item.data.Checker;

import com.google.gson.JsonElement;

public abstract class ILoot {
    public abstract List<ItemStack> generate();

    private static system.Toast2<Checker, system.IRange> parseElement(String value) {
        String[] key = value.split("\\*");
        String type = Arrays.stream(key).limit(key.length - 1).collect(Collectors.joining("*"));

        return system.toast(Checker.createCheck(type), key.length > 1 ? system.IRange.parse(key[key.length - 1]) : new system.OnceRange(1));
    }

    public static ILoot parse(String parent, JsonElement json) {
        if (json.isJsonPrimitive()) {
            return parseElement(json.getAsString()).invokeGet(SingleLoot::new);
        } else if (json.isJsonArray()) {
            MultiLoot loot = new MultiLoot();
            json.getAsJsonArray().forEach(kv -> loot.loot.add(parse(parent, kv)));
            return loot;
        } else if (json.isJsonNull())  {
            return new EmptyLoot();
        }
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