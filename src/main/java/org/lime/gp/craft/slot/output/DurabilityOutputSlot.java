package org.lime.gp.craft.slot.output;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.lime.gp.item.Items;
import org.lime.system;

public class DurabilityOutputSlot implements IOutputSlot {
    private final IOutputSlot item;
    private final system.IRange durability;

    public DurabilityOutputSlot(IOutputSlot item, system.IRange durability) {
        this.item = item;
        this.durability = durability;
    }


    @Override public ItemStack create() {
        ItemStack result = this.item.create();
        if (result.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage((int)Math.round(durability.getValue(Items.getMaxDamage(result))));
            result.setItemMeta(damageable);
        }
        return result;
    }
    @Override public ItemStack apply(ItemStack item, boolean copy) {
        ItemStack result = this.item.apply(item, copy);
        if (result.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage((int)Math.round(durability.getValue(Items.getMaxDamage(result))));
            result.setItemMeta(damageable);
        }
        return result;
    }

    @Override public net.minecraft.world.item.ItemStack nms(boolean isPreview) {
        net.minecraft.world.item.ItemStack result = this.item.nms(isPreview);
        result.setDamageValue((int)Math.round(durability.getValue(result.getMaxDamage())));
        return result;
    }
}
