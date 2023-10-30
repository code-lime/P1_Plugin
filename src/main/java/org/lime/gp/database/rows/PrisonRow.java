package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Map;

import org.lime.Position;
import org.lime.gp.lime;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.system.Time;

public class PrisonRow extends BaseRow {
    public int id;
    public int houseID;
    public int userID;
    public int ownerID;
    public boolean isLog;
    public String reason;
    public Position outPos;
    public Calendar endTime;

    public PrisonRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        houseID = MySql.readObject(set, "house_id", Integer.class);
        userID = MySql.readObject(set, "user_id", Integer.class);
        ownerID = MySql.readObject(set, "owner_id", Integer.class);
        reason = MySql.readObject(set, "reason", String.class);
        isLog = MySql.readObject(set, "is_log", Integer.class) == 1;
        outPos = Methods.readPosition(set, lime.MainWorld, "out_pos");
        endTime = MySql.readObject(set, "end_time", Calendar.class);
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("house_id", String.valueOf(houseID));
        map.put("user_id", String.valueOf(userID));
        map.put("owner_id", String.valueOf(ownerID));
        map.put("reason", String.valueOf(reason));
        map.put("is_log", String.valueOf(isLog));
        map.put("out_pos", outPos.toSave());
        map.put("end_time", Time.formatCalendar(endTime, true));
        return map;
    }
}