package org.lime.gp.item.loot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.IItemCreator;

import org.lime.system.range.*;
import org.lime.system.utils.RandomUtils;

public class SingleLoot implements ILoot {
    public Checker item;
    public IRange amount;

    public SingleLoot(String value) {
        String[] key = value.split("\\*");
        String type = Arrays.stream(key).limit(key.length - 1).collect(Collectors.joining("*"));

        item = Checker.createCheck(type);
        amount = key.length > 1 ? IRange.parse(key[key.length - 1]) : new OnceRange(1);
    }

    public SingleLoot(Checker item, IRange amount) {
        this.item = item;
        this.amount = amount;
        if (item.getWhitelistCreators().anyMatch(v -> true)) throw new IllegalArgumentException("[LOOT] ITEM '"+item+"' NOT FOUNDED!");
    }

    public static IJElement docs(IDocsLink docs) {
        return IJElement.join(
                IJElement.link(docs.regexItem()),
                IJElement.text("*"),
                IJElement.link(docs.range())
        );
    }


    @Override public List<ItemStack> generateLoot(IPopulateLoot loot) {
        int amount = this.amount.getIntValue(64);
        if (amount < 1) return Collections.emptyList();
        List<IItemCreator> creators = item.getWhitelistCreators().toList();
        if (creators.isEmpty()) return Collections.emptyList();
        if (creators.size() == 1) return Collections.singletonList(creators.get(0).createItem(amount));
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < amount; i++) items.add(RandomUtils.rand(creators).createItem());
        return items;
    }
}