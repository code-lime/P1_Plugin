package org.lime.gp.player.level;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.lime.system;

public interface EntityCompare {
    boolean isCompare(Entity entity);
    public static EntityCompare create(String value) {
        if (value.endsWith("]")) value = value.substring(0, value.length() - 1);
        String[] p1 = value.split(Pattern.quote("["), 2);
        EntityType type = EntityType.valueOf(p1[0]);
        if (p1.length == 1) return e -> e.getType() == type;
        return create(type, p1[1].split(Pattern.quote(";")));
    }
    private static EntityCompare create(EntityType type, String[] args) {
        List<EntityCompare> filters = new ArrayList<>();
        for (String argItem : args) {
            String[] kv = argItem.split(Pattern.quote("="), 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            switch (key) {
                case "age" -> {
                    system.IRange range = system.IRange.parse(value);
                    filters.add(e -> e instanceof Ageable _e ? range.inRange(_e.getAge(), 16) : false);
                }
            }
        }
        return create(type, filters);
    }
    private static EntityCompare create(EntityType type, List<EntityCompare> filters) {
        return e -> e.getType() == type && filters.stream().allMatch(v -> v.isCompare(e));
    }
}
