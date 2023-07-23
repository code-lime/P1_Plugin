package org.lime.gp.craft.slot.output;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.lime;
import org.lime.system;

import java.util.Optional;

public class SingleOutputSlot implements IOutputSlot {
    private final String key;
    private final int amount;

    public SingleOutputSlot(String key, int amount) {
        this.key = key;
        this.amount = amount;
    }

    private SingleOutputSlot(String[] args) {
        this(args[0], args.length > 1 ? Integer.parseUnsignedInt(args[1]) : 1);
    }
    public SingleOutputSlot(JsonPrimitive json) {
        this(json.getAsString().split("\\*"));
    }

    @Override public ItemStack create() {
        return Items.getItemCreator(key)
                .map(v -> v.createItem(amount))
                .orElseGet(() -> {
                    lime.logOP("CRAFT OUTPUT ITEM '" + key + "' NOT FOUNDED!");
                    return new ItemStack(Material.STONE, 0);
                });
    }
    @Override public ItemStack apply(ItemStack item, boolean copy) {
        return Items.getItemCreator(key)
                .map(v -> Optional.ofNullable(v instanceof ItemCreator c ? c : null).map(i -> i.apply(copy ? item.clone() : item, amount, Apply.of())).orElseGet(() -> v.createItem(amount)))
                .orElseGet(() -> {
                    lime.logOP("CRAFT OUTPUT ITEM '" + key + "' NOT FOUNDED!");
                    return new ItemStack(Material.STONE, 0);
                });
    }

    @Override public net.minecraft.world.item.ItemStack nms(boolean isPreview) {
        return CraftItemStack.asNMSCopy(create());
    }
}
