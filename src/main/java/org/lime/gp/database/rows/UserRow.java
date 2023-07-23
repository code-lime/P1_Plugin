package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.lime.gp.module.biome.time.DateTime;
import org.lime.system;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.module.TabManager;

public class UserRow extends BaseRow {
    public int id;
    public final UUID uuid;
    public final String userName;
    public String firstName = null;
    public String lastName = null;
    public boolean isMale = true;
    public Integer phone = null;
    public Integer cardID = null;
    public int role;
    public int work;
    public Calendar workTime = null;
    public int phoneRegen = 0;
    public int cardRegen = 0;
    public int wanted = 0;
    public int exp = 0;
    
    public Optional<DateTime> dieDate = Optional.empty();

    public Calendar CreateDate = null;
    public Calendar ConnectDate = null;

    public boolean isOnline() {
        return EntityPosition.onlinePlayers.containsKey(uuid);
    }
    public Player getOnline() {
        return EntityPosition.onlinePlayers.getOrDefault(uuid, null);
    }

    private UserRow(Player player) {
        super(null);
        uuid = player.getUniqueId();
        userName = player.getName();
    }

    public boolean isOwner() {
        return switch (role) {
            case 1, 9 -> true;
            default -> false;
        };
    }

    public UserRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
        userName = MySql.readObject(set, "user_name", String.class);
        firstName = MySql.readObject(set, "first_name", String.class);
        lastName = MySql.readObject(set, "last_name", String.class);
        isMale = MySql.readObject(set, "male", Integer.class) == 1;
        phone = MySql.readObject(set, "phone", Integer.class);
        cardID = MySql.readObject(set, "card_id", Integer.class);
        //birthdayDate = MySql.readObject(set, "birthday_date", Calendar.class);
        role = MySql.readObject(set, "role", Integer.class);
        work = MySql.readObject(set, "work", Integer.class);
        workTime = MySql.readObject(set, "work_time", Calendar.class);
        wanted = MySql.readObject(set, "wanted", Integer.class);
        exp = MySql.readObject(set, "exp", Integer.class);
        phoneRegen = MySql.readObject(set, "phone_regen", Integer.class);
        cardRegen = MySql.readObject(set, "card_regen", Integer.class);

        dieDate = MySql.readObjectOptional(set, "die_date", String.class).flatMap(DateTime::tryParse);
        //dieDate = MySql.readObjectOptional(set, "die_date", Calendar.class).map(v -> DateTime.ofHours(v.getTimeInMillis() / 1000.0));

        CreateDate = MySql.readObject(set, "create_date", Calendar.class);
        ConnectDate = MySql.readObject(set, "connect_date", Calendar.class);
    }

    public static boolean hasBy(UUID uuid) { return Tables.USER_TABLE.hasOther("uuid", uuid.toString()); }
    public static Optional<UserRow> getBy(UUID uuid) { return Tables.USER_TABLE.getOther("uuid", uuid.toString()); }
    public static Optional<UserRow> getBy(Player player) { return getBy(player.getUniqueId()); }
    public static Optional<UserRow> getBy(int id) { return Tables.USER_TABLE.get(id + ""); }
    public static Optional<UserRow> getByTimedID(int timed_id) { return Optional.ofNullable(TabManager.getUUIDorNull(timed_id)).flatMap(UserRow::getBy); }

    @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", Tables.valueOfInt(id));
        map.put("uuid", uuid.toString());
        map.put("user_name", userName);
        map.put("first_name", firstName);
        map.put("last_name", lastName);
        map.put("male", isMale ? "true" : "false");
        map.put("phone", Tables.valueOfInt(phone));
        map.put("card_id", Tables.valueOfInt(cardID));
        //map.put("birthday_date", system.formatCalendar(birthdayDate, false));
        map.put("role", String.valueOf(role));
        map.put("work", String.valueOf(work));
        map.put("work_time", system.formatCalendar(workTime, true));
        map.put("wanted", String.valueOf(wanted));
        map.put("exp", String.valueOf(exp));
        map.put("phone_regen", Tables.valueOfInt(phoneRegen));
        map.put("card_regen", Tables.valueOfInt(cardRegen));
        map.put("create_date", system.formatCalendar(CreateDate, true));
        map.put("connect_date", system.formatCalendar(ConnectDate, true));
        map.put("timed_id", Tables.valueOfInt(TabManager.getPayerIDorNull(uuid)));
        map.put("is_online", isOnline() ? "true" : "false");
        return map;
    }
}