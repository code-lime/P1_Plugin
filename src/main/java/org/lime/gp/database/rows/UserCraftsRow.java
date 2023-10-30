package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.extension.ExtMethods;

import com.google.common.collect.ImmutableSet;

public class UserCraftsRow extends BaseRow {
    public int id;
    public UUID uuid;
    public ImmutableSet<Integer> craftWorks;
    public String craftRegex;
    public Integer useCount;

    public UserCraftsRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
        craftWorks = Optional.ofNullable(MySql.readObject(set, "craft_works", String.class))
                .map(v -> Arrays.stream(v.split(",")).map(ExtMethods::parseInt).flatMap(Optional::stream).collect(ImmutableSet.toImmutableSet()))
                .orElse(null);
        craftRegex = MySql.readObject(set, "craft_regex", String.class);
        useCount = MySql.readObject(set, "use_count", Integer.class);
    }
    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("uuid", uuid.toString());
        map.put("craft_works", craftWorks == null ? null : craftWorks.stream().map(Object::toString).collect(Collectors.joining(",")));
        map.put("craft_regex", craftRegex);
        map.put("use_count", String.valueOf(useCount));
        return map;
    }
}