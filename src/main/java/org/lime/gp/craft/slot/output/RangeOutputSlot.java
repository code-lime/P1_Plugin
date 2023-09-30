package org.lime.gp.craft.slot.output;

import com.google.gson.JsonPrimitive;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.lime;
import org.lime.system.range.*;

import java.util.Optional;

public class RangeOutputSlot implements IOutputSlot {
    private final String key;
    private final IRange amount;

    public RangeOutputSlot(String key, int amount) {
        this(key, new OnceRange(amount));
    }
    public RangeOutputSlot(String key, IRange amount) {
        this.key = key;
        this.amount = amount;
    }

    private RangeOutputSlot(String[] args) {
        this(args[0], args.length > 1 ? IRange.parse(args[1]) : new OnceRange(1));
    }
    public RangeOutputSlot(JsonPrimitive json) {
        this(json.getAsString().split("\\*"));
    }

    @Override public ItemStack modify(ItemStack item, boolean copy, IOutputVariable variable) {
        return Items.getItemCreator(key)
                .map(v -> Optional.ofNullable(v instanceof ItemCreator c ? c : null)
                        .map(i -> i.apply(CraftItemStack.asCraftMirror(copy ? item.copy() : item), amount.getIntValue(64), Apply.of()))
                        .orElseGet(() -> v.createItem(amount.getIntValue(64)))
                )
                .map(CraftItemStack::asNMSCopy)
                .orElseGet(() -> {
                    lime.logOP("CRAFT OUTPUT ITEM '" + key + "' NOT FOUNDED!");
                    return new ItemStack(net.minecraft.world.item.Items.STONE, 0);
                });
    }

    @Override public net.minecraft.world.item.ItemStack create(boolean isPreview, IOutputVariable variable) {
        return Items.getItemCreator(key)
                .map(v -> v.createItem(amount.getIntValue(64)))
                .map(CraftItemStack::asNMSCopy)
                .orElseGet(() -> {
                    lime.logOP("CRAFT OUTPUT ITEM '" + key + "' NOT FOUNDED!");
                    return new ItemStack(net.minecraft.world.item.Items.STONE, 0);
                });
    }
    @Override public boolean test(ItemStack item) {
        return Items.getGlobalKeyByItem(item).map(key::equalsIgnoreCase).orElse(false);
    }
}








