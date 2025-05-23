package org.lime.gp.database.rows;

import org.bukkit.Location;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.gp.module.biome.time.DateTime;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public class DeathRow extends BaseRow {
    public final String uniqueRowKey;

    public int id;
    public int userID;
    public Location location;
    public DateTime dieDate;
    @Nullable public String skin;
    public String equipment;
    public Status status;

    public enum Status {
        SHOW,
        HIDE
    }

    public DeathRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        userID = MySql.readObject(set, "user_id", Integer.class);
        location = MathUtils.getLocation(lime.MainWorld, MySql.readObject(set, "location", String.class));
        dieDate = DateTime.throwParse(MySql.readObject(set, "die_date", String.class));
        skin = MySql.readObject(set, "skin", String.class);
        equipment = MySql.readObject(set, "equipment", String.class);
        status = Status.valueOf(MySql.readObject(set, "status", String.class));
        uniqueRowKey = id + ", " + MySql.readObject(set, "last_update", String.class);
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("user_id", String.valueOf(userID));
        map.put("location", MathUtils.getString(location));
        map.put("die_date", dieDate.toString());
        map.put("skin", skin);
        map.put("status", status.name());
        return map;
    }

    public static boolean hasDeath(UUID uuid) { return UserRow.getBy(uuid).map(v -> Tables.DEATH_TABLE.hasOther("user_id", String.valueOf(v.id))).orElse(false); }
    public static boolean hasDeath(int userID) { return Tables.DEATH_TABLE.hasOther("user_id", String.valueOf(userID)); }
}