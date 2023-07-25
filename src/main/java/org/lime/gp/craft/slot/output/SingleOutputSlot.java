package org.lime.gp.craft.slot.output;

import com.google.gson.JsonPrimitive;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.lime;

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

    @Override public ItemStack modify(ItemStack item, boolean copy, IOutputVariable variable) {
        return Items.getItemCreator(key)
                .map(v -> Optional.ofNullable(v instanceof ItemCreator c ? c : null).map(i -> i.apply(CraftItemStack.asCraftMirror(copy ? item.copy() : item), amount, Apply.of())).orElseGet(() -> v.createItem(amount)))
                .map(CraftItemStack::asNMSCopy)
                .orElseGet(() -> {
                    lime.logOP("CRAFT OUTPUT ITEM '" + key + "' NOT FOUNDED!");
                    return new ItemStack(net.minecraft.world.item.Items.STONE, 0);
                });
    }

    @Override public net.minecraft.world.item.ItemStack create(boolean isPreview, IOutputVariable variable) {
        return Items.getItemCreator(key)
                .map(v -> v.createItem(amount))
                .map(CraftItemStack::asNMSCopy)
                .orElseGet(() -> {
                    lime.logOP("CRAFT OUTPUT ITEM '" + key + "' NOT FOUNDED!");
                    return new ItemStack(net.minecraft.world.item.Items.STONE, 0);
                });
    }
}
