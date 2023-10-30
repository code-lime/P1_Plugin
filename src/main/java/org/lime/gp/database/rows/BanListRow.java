package org.lime.gp.database.rows;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.util.*;

import org.bukkit.entity.Player;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;
import org.lime.system.Time;

public class BanListRow extends BaseRow {
    public enum Type {
        IP,
        UUID
    }
    public int id;
    public String user;
    public BanListRow.Type type;
    public String reason;
    public String owner;
    public Calendar createTime;

    public BanListRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        user = MySql.readObject(set, "user", String.class);
        type = Type.valueOf(MySql.readObject(set, "type", String.class));
        reason = MySql.readObject(set, "reason", String.class);
        owner = MySql.readObject(set, "owner", String.class);
        createTime = MySql.readObject(set, "create_time", Calendar.class);
    }

    public static Optional<BanListRow> getBy(Player player) {
        return Optional
                .ofNullable(player.getAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .flatMap(ip -> Tables.BANLIST_TABLE.getOther("user", ip).filter(v -> v.type == Type.IP))
                .or(() -> Tables.BANLIST_TABLE.getOther("user", player.getUniqueId().toString()).filter(v -> v.type == Type.UUID));
    }

    public boolean is(Player player) {
        return Optional
                .ofNullable(player.getAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .filter(ip -> user.equals(ip) && type == Type.IP)
                .map(v -> true)
                .orElseGet(() -> user.equals(player.getUniqueId().toString()) && type == Type.UUID);
    }

    public String displayName() {
        return type == Type.UUID
                ? UserRow.getBy(UUID.fromString(user)).map(v -> v.userName).orElse(user)
                : user;
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("user", user);
        map.put("type", type.name());
        map.put("owner", owner);
        map.put("create_time", Time.formatCalendar(createTime, true));
        return map;
    }
}