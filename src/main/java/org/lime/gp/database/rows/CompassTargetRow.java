package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.HashMap;

import org.bukkit.Location;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.database.MySql;
import org.lime.gp.module.EntityPosition;

public class CompassTargetRow extends BaseRow {
    public int id;
    public java.util.UUID uuid;
    public String target;
    public enum TargetType {
        Position(target -> system.getLocation(lime.MainWorld, target)),
        Entity(target -> EntityPosition.entityLocations.getOrDefault(java.util.UUID.fromString(target), null));

        final system.Func1<String, Location> parse;
        TargetType(system.Func1<String, Location> parse) {
            this.parse = parse;
        }
        public Location getLocation(String target) {
            try {
                return parse.invoke(target);
            } catch (Exception e) {
                return null;
            }
        }
    }
    public CompassTargetRow.TargetType type;
    public String info;
    public String color;

    public Location getTargetLocation() {
        return type.getLocation(target);
    }

    public CompassTargetRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
        target = MySql.readObject(set, "target", String.class);
        type = TargetType.valueOf(MySql.readObject(set, "type", String.class));
        info = MySql.readObject(set, "info", String.class);
        color = MySql.readObject(set, "color", String.class);
    }

    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", id + "");
        map.put("uuid", uuid.toString());
        map.put("target", target);
        map.put("type", type.name());
        map.put("info", info);
        map.put("color", color);
        return map;
    }
}