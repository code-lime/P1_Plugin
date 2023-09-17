package org.lime.gp.item;

import org.bukkit.inventory.ItemStack;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.utils.ItemUtils;

public class RawItem {
    public static CoreElement create() {
        return CoreElement.create(RawItem.class)
                .withInit(RawItem::init);
    }

    private static void init() {
        AnyEvent.addEvent("give.raw.item", AnyEvent.type.other, v -> v.createParam("[RAW]"), (player, raw) -> {
            ItemStack rawItem = ItemUtils.loadItem(raw);
            lime.logOP("Raw item: " + rawItem);
            Items.dropGiveItem(player, rawItem, false);
        });
    }
}
