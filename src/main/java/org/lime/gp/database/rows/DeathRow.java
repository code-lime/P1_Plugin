package org.lime.gp.database.rows;

import org.bukkit.Location;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.lime;
import org.lime.gp.module.biome.time.DateTime;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.util.HashMap;

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

    public DeathRow(ResultSet set) {
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

    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("user_id", String.valueOf(userID));
        map.put("location", MathUtils.getString(location));
        map.put("die_date", dieDate.toString());
        map.put("skin", skin);
        map.put("status", status.name());
        return map;
    }
}