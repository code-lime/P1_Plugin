package org.lime.gp.item;

import org.bukkit.Bukkit;
import org.lime.core;
import org.lime.gp.item.settings.list.ArmorTagSetting;
import org.lime.gp.lime;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ArmorTags {
    public static core.element create() {
        return core.element.create(ArmorTags.class)
                .withInit(ArmorTags::init);
    }

    private static void init() {
        lime.repeat(ArmorTags::update, 1);
    }
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            List<String> list = Arrays.stream(player.getInventory().getArmorContents())
                    .flatMap(v -> Items.getOptional(ArmorTagSetting.class, v).stream())
                    .flatMap(v -> v.tags.stream())
                    .map(v -> "armor." + v)
                    .collect(Collectors.toList());
            Set<String> tags = player.getScoreboardTags();
            tags.removeIf(v -> v.startsWith("armor.") && !list.remove(v));
            tags.addAll(list);
        });
    }
}





