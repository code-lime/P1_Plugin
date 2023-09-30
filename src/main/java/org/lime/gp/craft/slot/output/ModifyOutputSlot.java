package org.lime.gp.craft.slot.output;

import net.kyori.adventure.text.Component;
import net.minecraft.world.item.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.system.range.IRange;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ModifyOutputSlot implements IOutputSlot {
    private final IOutputSlot item;
    private final @Nullable IRange durability;
    private final @Nullable String name;
    private final @Nullable List<String> lore;
    private final @Nullable String id;

    public ModifyOutputSlot(IOutputSlot item,
                            @Nullable IRange durability,
                            @Nullable String name,
                            @Nullable List<String> lore,
                            @Nullable String id
    ) {
        this.item = item;
        this.durability = durability;
        this.name = name;
        this.lore = lore == null ? null : new ArrayList<>(lore);
        this.id = id;
    }

    private ItemStack result(ItemStack item) {
        if (durability != null) {
            int max = item.getMaxDamage();
            int value = durability.getIntValue(max);
            item.setDamageValue(max - value);
        }
        if (name != null || lore != null || id != null) {
            org.bukkit.inventory.ItemStack mirror = item.asBukkitMirror();
            ItemMeta meta = mirror.getItemMeta();
            if (lore != null) {
                List<Component> lore = new ArrayList<>();
                this.lore.forEach(line -> lore.add(ChatHelper.formatComponent(line, Apply.of())));
                meta.lore(lore);
            }
            if (name != null) {
                meta.displayName(ChatHelper.formatComponent(name, Apply.of()));
            }
            if (id != null) {
                meta.setCustomModelData(Integer.parseInt(ChatHelper.formatText(id, Apply.of())));
            }
            mirror.setItemMeta(meta);
        }
        return item;
    }
    @Override public ItemStack modify(ItemStack item, boolean copy, IOutputVariable variable) {
        return result(this.item.modify(item, copy, variable));
    }
    @Override public ItemStack create(boolean isPreview, IOutputVariable variable) {
        return result(this.item.create(isPreview, variable));
    }
    @Override public boolean test(ItemStack item) { return this.item.test(item); }
}














