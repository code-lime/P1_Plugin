package org.lime.gp.database.rows;

import org.apache.commons.lang.StringUtils;
import org.lime.gp.database.mysql.MySql;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class ReJoinRow extends BaseRow {
    public final int index;
    public final Optional<UUID> owner;
    public final String name;
    public final boolean select;

    private final UUID gen_uuid;
    private final String gen_name;
    private final String identifier;
    public UUID genUUID() { return gen_uuid; }
    public String genName() { return gen_name; }
    public String identifier() { return identifier; }

    public ReJoinRow(ResultSet set) {
        super(set);
        index = MySql.readObject(set, "index", Integer.class);
        select = MySql.readObject(set, "select", Integer.class) == 1;
        name = MySql.readObject(set, "name", String.class);
        owner = MySql.readObjectOptional(set, "owner", String.class).map(UUID::fromString);

        gen_name = "Gen-" + StringUtils.leftPad(String.valueOf(index), 3, '0');
        gen_uuid = UUID.fromString("ffffffff-aaaa-aaaa-aaaa-"+StringUtils.leftPad(String.valueOf(index), 12, '0'));

        identifier = name + ":" + index;
    }

    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
        map = super.appendToReplace(map);
        map.put("index", String.valueOf(index));
        map.put("name", name);
        map.put("owner", owner.map(UUID::toString).orElse("null"));
        map.put("select", String.valueOf(select));
        return map;
    }
}
