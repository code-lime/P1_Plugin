package org.lime.gp.item.loot.filter;

import java.util.regex.Pattern;

import org.lime.gp.module.PopulateLootEvent;

public interface ILootFilter {
    boolean isFilter(PopulateLootEvent loot);

    static ILootFilter parse(String value) {
        if (value.endsWith("]")) value = value.substring(0, value.length() - 1);
        String[] p1 = value.split(Pattern.quote("["), 2);
        return switch (p1[0]) {
            case "any" -> new AnyLootFilter(p1[1]);
            case "all" -> new AllLootFilter(p1[1]);
            case "block" -> new BlockLootFilter(p1[1]);
            case "debug" -> new DebugLootFilter(p1[1]);
            case "random" -> new RandomLootFilter(p1[1]);
            default -> throw new IllegalArgumentException("LootFilter type '" + p1[0] + "' not founded!");
        };
    }
}
