package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.*;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;

public class FriendRow extends BaseRow {
    public int id;
    public int userID;
    public int friendID;
    public String friendName;
    public int sendInfo;

    public FriendRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        userID = MySql.readObject(set, "user_id", Integer.class);
        friendID = MySql.readObject(set, "friend_id", Integer.class);
        friendName = MySql.readObject(set, "friend_name", String.class);
        sendInfo = MySql.readObject(set, "send_info", Integer.class);
    }

    public Optional<Integer> getFriendPhone() {
        return Tables.USER_TABLE.get(friendID + "").map(v -> v.phone);
    }

    public static List<FriendRow> getFriendsByUUID(UUID uuid) {
        return Tables.USER_TABLE.get(uuid + "").map(v -> getFriendsByID(v.id)).orElseGet(ArrayList::new);
    }
    public static List<FriendRow> getFriendsByID(int id) {
        return Tables.FRIEND_TABLE.getRowsBy(row -> row.userID == id);
    }
    public static Optional<FriendRow> getFriendByUUIDandName(UUID uuid, String name) {
        return UserRow.getBy(uuid).flatMap(v -> getFriendByUUIDandName(v.id, name));
    }
    public static Optional<FriendRow> getFriendByUUIDandName(int id, String name) {
        return Tables.FRIEND_TABLE.getBy(row -> row.userID == id && name.equals(row.friendName));
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", Tables.valueOfInt(userID));
        map.put("user_id", Tables.valueOfInt(userID));
        map.put("friend_id", Tables.valueOfInt(friendID));
        map.put("friend_name", friendName);
        map.put("send_info", Tables.valueOfInt(sendInfo));
        return map;
    }
}