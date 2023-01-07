package org.lime.gp.block.component.data.recipe;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.lime.gp.block.component.data.anvil.AnvilLoader;
import org.lime.gp.item.Items;
import org.lime.system;

import java.util.List;

public class RecipeRepair extends IRecipe {
    private final String input;
    private final String repair;
    private final system.IRange range;
    private final int clicks;

    public RecipeRepair(JsonObject json) {
        input = json.get("input").getAsString();
        if (Items.getItemCreator(input).isEmpty()) throw new IllegalArgumentException("ANVIL.REPAIR INPUT ITEM '" + input + "' NOT FOUNDED!");
        repair = json.get("repair").getAsString();
        if (Items.getItemCreator(repair).isEmpty()) throw new IllegalArgumentException("ANVIL.REPAIR REPAIR ITEM '" + repair + "' NOT FOUNDED!");
        range = system.IRange.parse(json.get("range").getAsString());
        clicks = json.has("clicks") ? json.get("clicks").getAsInt() : 1;
    }

    public boolean check(List<ItemStack> items) {
        ItemStack _input = system.getOrDefault(items, 0, null);
        if (_input == null || !(_input.getItemMeta() instanceof Damageable damageable) || damageable.getDamage() == 0) return false;
        if (Items.getGlobalKeyByItem(_input).filter(input::equals).isEmpty()) return false;
        ItemStack _repair = system.getOrDefault(items, 1, null);
        return Items.getGlobalKeyByItem(_repair).filter(repair::equals).isPresent();
    }
    @Override public ItemStack craft(List<ItemStack> items) {
        ItemStack input = items.get(0).clone();
        ItemStack repair = items.get(1);
        if (input.getItemMeta() instanceof Damageable damageable) {
            double maxDamage = input.getType().getMaxDurability();
            double damage = damageable.getDamage();
            int amount = repair.getAmount();
            for (int i = 0; i < amount; i++) damage -= this.range.getValue(maxDamage);
            damage = Math.max(0, damage);
            damageable.setDamage((int) Math.floor(damage));
            input.setItemMeta(damageable);
        }
        return input;
    }
    @Override public ItemStack checkCraft(List<ItemStack> items) { return check(items) ? craft(items) : null; }
    @Override public void addToWhitelist() {
        AnvilLoader.whitelistMaterial.add(Items.getItemCreator(this.input).orElseThrow().createItem().getType());
        AnvilLoader.whitelistMaterial.add(Items.getItemCreator(this.repair).orElseThrow().createItem().getType());
    }
    @Override public int getClicks() { return clicks; }
    @Override public int getItemCount() { return 2; }
}
