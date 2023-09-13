package org.lime.gp.item.settings;

import org.bukkit.inventory.meta.ItemMeta;
import org.lime.docs.IIndexGroup;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.data.ItemCreator;

public interface IItemSetting {
    ItemCreator creator();
    String name();
    void apply(ItemMeta meta, Apply apply);
    void appendArgs(ItemMeta meta, Apply apply);

    IIndexGroup docs(String index, IDocsLink docs);
}