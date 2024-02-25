package org.lime.gp.database.rows;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.lime.gp.block.Blocks;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.player.inventory.TownInventory;
import org.lime.system.Regex;
import org.lime.system.execute.Func1;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrivatePatternRow extends BaseRow implements TownInventory.IPrivatePattern {
    public enum PatternType {
        BREAK,
        BLOCK,
        ENTITY
    }

    public int id;
    public PatternType type;
    public String filter;

    public PrivatePatternRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        type = PatternType.valueOf(MySql.readObject(set, "type", String.class));
        filter = MySql.readObject(set, "filter", String.class);
    }

    public static Optional<PrivatePatternRow> getBy(int id) {
        return Tables.PRIVATE_PATTERN.get(String.valueOf(id));
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("type", String.valueOf(type));
        map.put("filter", filter);
        return map;
    }

    private long index = -1;
    private @Nullable HashSet<String> blocks = null;
    private @Nullable HashSet<String> breaks = null;
    private @Nullable HashSet<EntityType> entities = null;

    private void tryInitialize() {
        long nowIndex = switch (type) {
            case BLOCK, BREAK -> Blocks.index;
            default -> 0;
        };
        if (nowIndex == index) return;
        index = nowIndex;
        switch (type) {
            case BLOCK -> blocks = getValues(filter, Stream.concat(Blocks.creators.keySet().stream(), Arrays.stream(Material.values()).map(Enum::name)).collect(Collectors.toSet()), v -> v);
            case BREAK -> breaks = getValues(filter, Stream.concat(Blocks.creators.keySet().stream(), Arrays.stream(Material.values()).map(Enum::name)).collect(Collectors.toSet()), v -> v);
            case ENTITY -> entities = getValues(filter, List.of(EntityType.values()), Enum::name);
        }
    }

    private static <T>HashSet<T> getValues(String regex, Collection<T> values, Func1<T, String> nameGetter) {
        HashSet<T> arr = new HashSet<>();
        for (T value : values) {
            if (Regex.compareRegex(nameGetter.invoke(value), regex))
                arr.add(value);
        }
        return arr;
    }

    @Override public boolean isCantBlock(String block) {
        tryInitialize();
        return blocks == null || !blocks.contains(block);
    }
    @Override public boolean isCantBreaks(String block) {
        tryInitialize();
        return breaks == null || !breaks.contains(block);
    }
    @Override public boolean isCantEntity(EntityType entity) {
        tryInitialize();
        return entities == null || entities.contains(entity);
    }

    @Override public boolean isCheck(String block) {
        tryInitialize();
        return (blocks != null && blocks.contains(block)) || (breaks != null && breaks.contains(block));
    }
    @Override public boolean isCheck(EntityType entity) {
        tryInitialize();
        return entities != null && entities.contains(entity);
    }
}