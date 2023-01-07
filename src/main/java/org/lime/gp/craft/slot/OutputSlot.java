package org.lime.gp.craft.slot;

import com.google.gson.JsonElement;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.lime;

import java.util.Optional;

public class OutputSlot {
    private final int amount;
    private final String key;

    public OutputSlot(String key, int amount) {
        this.key = key;
        this.amount = amount;
    }

    private OutputSlot(String[] args) { this(args[0], args.length > 1 ? Integer.parseUnsignedInt(args[1]) : 1); }
    public OutputSlot(JsonElement json) { this(json.getAsString().split("\\*")); }

    public static OutputSlot of(String str) {
        String[] args = str.split("\\*");
        return new OutputSlot(args[0], args.length > 1 ? Integer.parseUnsignedInt(args[1]) : 1);
    }

    public Optional<Items.IItemCreator> creator() {
        return Items.getItemCreator(key);
    }

    public ItemStack create() {
        return Items.getItemCreator(key)
                .map(v -> v.createItem(amount))
                .orElseGet(() -> {
                    lime.logOP("CRAFT OUTPUT ITEM '" + key + "' NOT FOUNDED!");
                    return new ItemStack(Material.STONE, 0);
                });
    }
    public ItemStack apply(ItemStack item, boolean copy) {
        return Items.getItemCreator(key)
                .map(v -> Optional.ofNullable(v instanceof Items.ItemCreator c ? c : null).map(i -> i.apply(copy ? item.clone() : item, amount, Apply.of())).orElseGet(() -> v.createItem(amount)))
                .orElseGet(() -> {
                    lime.logOP("CRAFT OUTPUT ITEM '" + key + "' NOT FOUNDED!");
                    return new ItemStack(Material.STONE, 0);
                });
    }

    public net.minecraft.world.item.ItemStack nms() {
        return CraftItemStack.asNMSCopy(create());
    }
}
