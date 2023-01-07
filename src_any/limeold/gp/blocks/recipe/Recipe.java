package org.lime.gp.block.component.data.recipe;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.block.component.data.anvil.AnvilLoader;
import org.lime.gp.item.Items;
import org.lime.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Recipe extends IRecipe {
    private final List<system.Toast2<Material, Integer>> items = new ArrayList<>();
    private final String output;
    private final int count;
    private final int clicks;

    public Recipe(JsonObject json) {
        output = json.get("output").getAsString();
        if (Items.getItemCreator(output).isEmpty())
            throw new IllegalArgumentException("ANVIL.RECIPE ITEM '" + output + "' NOT FOUNDED!");
        count = json.has("count") ? json.get("count").getAsInt() : 1;
        clicks = json.has("clicks") ? json.get("clicks").getAsInt() : 1;
        json.get("items").getAsJsonObject().entrySet().forEach(kv -> items.add(system.toast(Material.valueOf(kv.getKey()), kv.getValue().getAsInt())));
    }

    public boolean check(List<ItemStack> items) {
        if (AnvilLoader.ANVIL_ORDER) {
            int size = items.size();
            if (size != this.items.size()) return false;
            for (int i = 0; i < size; i++) {
                system.Toast2<Material, Integer> filter = this.items.get(i);
                ItemStack item = items.get(i);
                if (item.getType() != filter.val0) return false;
                if (item.getAmount() != filter.val1) return false;
            }
            return true;
        } else {
            HashMap<Material, Integer> _items = new HashMap<>();
            items.forEach(item -> {
                Material material = item.getType();
                _items.put(material, _items.getOrDefault(material, 0) + item.getAmount());
            });
            for (system.Toast2<Material, Integer> kv : this.items) {
                Integer count = _items.remove(kv.val0);
                if (!kv.val1.equals(count)) return false;
            }
            return _items.size() == 0;
        }
    }

    @Override public ItemStack craft(List<ItemStack> items) { return Items.getItemCreator(output).orElseThrow().createItem(count); }
    @Override public ItemStack checkCraft(List<ItemStack> items) { return check(items) ? craft(items) : null; }
    @Override public void addToWhitelist() { items.forEach(kv -> AnvilLoader.whitelistMaterial.add(kv.val0)); }
    @Override public int getClicks() { return clicks; }
    @Override public int getItemCount() { return items.size(); }
}
