package org.lime.gp.player.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.lime.gp.lime;
import org.lime.system;

public interface EntityCompare {
    boolean isCompare(Entity entity);

    private static system.Func1<EntityType, Boolean> regexType(String regex) {
        Set<EntityType> types = system.filterRegex(List.of(EntityType.values()), EntityType::name, regex)
                .collect(Collectors.toSet());
        if (types.isEmpty()) lime.logOP("EntityTypes in '"+regex+"' is EMPTY! Maybe error...");
        return types::contains;
    }

    static EntityCompare create(String value) {
        if (value.endsWith("]")) value = value.substring(0, value.length() - 1);
        String[] p1 = value.split(Pattern.quote("["), 2);
        system.Func1<EntityType, Boolean> type = regexType(p1[0]);
        if (p1.length == 1) return e -> type.invoke(e.getType());
        return create(type, p1[1].split(Pattern.quote(";")));
    }
    private static EntityCompare create(system.Func1<EntityType, Boolean> type, String[] args) {
        List<EntityCompare> filters = new ArrayList<>();
        for (String argItem : args) {
            String[] kv = argItem.split(Pattern.quote("="), 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            switch (key) {
                case "age" -> {
                    system.IRange range = system.IRange.parse(value);
                    filters.add(e -> e instanceof Ageable _e && range.inRange(_e.getAge(), 16));
                }
            }
        }
        return create(type, filters);
    }
    private static EntityCompare create(system.Func1<EntityType, Boolean> type, List<EntityCompare> filters) {
        return e -> type.invoke(e.getType()) && filters.stream().allMatch(v -> v.isCompare(e));
    }
}
