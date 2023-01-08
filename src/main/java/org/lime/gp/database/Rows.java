package org.lime.gp.database;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.core;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.module.TabManager;
import org.lime.system;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class Rows {
    public static core.element create() {
        return core.element.create(Rows.class)
                .withInit(Rows::init);
    }
    public static void init() {
        lime.repeat(Rows::update, 1);
    }
    public static void update() {
        PermissionRow.sync();
    }

    private static String valueOfInt(Integer v) {
        return v == null ? "" : String.valueOf(v);
    }
    
    public static abstract class DataBaseRow {
        protected DataBaseRow(ResultSet set) { }
        public HashMap<String, String> appendToReplace(HashMap<String, String> map) { return map; }
        public String applyToString(String line) { return applyToString(line, '{', '}'); }
        public String applyToString(String line, char start, char end) { return applyToString(line, start, end, new HashMap<>()); }
        @SuppressWarnings("unchecked")
        public String applyToString(String line, system.Toast2<String, String>... map) { return applyToString(line, '{', '}', map); }
        @SuppressWarnings("unchecked")
        public String applyToString(String line, char start, char end, system.Toast2<String, String>... map) {
            HashMap<String, String> _map = new HashMap<>();
            for (system.Toast2<String, String> kv : map) _map.put(kv.val0, kv.val1);
            return applyToString(line, start, end, _map);
        }
        public String applyToString(String line, HashMap<String, String> map) { return applyToString(line, '{', '}', map); }
        public String applyToString(String line, String prefix, HashMap<String, String> map) { return applyToString(line, '{', '}', prefix, map); }
        public String applyToString(String line, char start, char end, HashMap<String, String> map) {
            HashMap<String, String> _map = appendToReplace(new HashMap<>());
            map.forEach(_map::put);
            _map.replaceAll((k,v) -> v == null ? "" : v);
            return ChatHelper.replaceBy(line, start, end, ChatHelper.jsFix(_map));
        }
        public String applyToString(String line, char start, char end, String prefix, HashMap<String, String> map) {
            HashMap<String, String> _map = new HashMap<>();
            appendToReplace(new HashMap<>()).forEach((k,v) -> _map.put(prefix + k, v));
            map.forEach(_map::put);
            _map.replaceAll((k,v) -> v == null ? "" : v);
            return ChatHelper.replaceBy(line, start, end, ChatHelper.jsFix(_map));
        }
        public void init() {}

        @Override public String toString() { return appendToReplace(new HashMap<>()).entrySet().stream().map(v -> v.getKey() + ": " + v.getValue()).collect(Collectors.joining(", ")); }
    }

    public static class AnyRow extends DataBaseRow {
        public HashMap<String, String> columns = new HashMap<>();
        public static AnyRow of(ResultSet set) {
            return new AnyRow(set);
        }
        public AnyRow(ResultSet set) {
            super(set);
            try {
                ResultSetMetaData data = set.getMetaData();
                for (int i = 1; i <= data.getColumnCount(); i++)
                    columns.put(data.getColumnLabel(i), MySql.fromSqlObjectString(data.getColumnType(i), set.getObject(i)));
            } catch (SQLException throwables) {
                throw new IllegalArgumentException(throwables);
            }
        }
        public AnyRow(HashMap<String, String> columns) {
            super(null);
            this.columns.putAll(columns);
        }
        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            columns.forEach(map::put);
            return map;
        }
    }
    public static class CompassTargetRow extends DataBaseRow {
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
        public TargetType type;
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
    public static class DiscordRow extends DataBaseRow {
        public long discordID;
        public UUID uuid;

        public DiscordRow(ResultSet set) {
            super(set);
            discordID = MySql.readObject(set, "discord_id", Long.class);
            uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
        }

        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("discord_id", String.valueOf(discordID));
            map.put("uuid", uuid.toString());
            return map;
        }
    }
    public static class AAnyRow extends DataBaseRow {
        public int id;
        public UUID uuid;
        public String reason;
        public Integer timeToEnd;

        public AAnyRow(ResultSet set) {
            super(set);
            id =  MySql.readObject(set, "id", Integer.class);
            uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
            reason = MySql.readObject(set, "reason", String.class);
            timeToEnd = MySql.readObject(set, "time", Integer.class);
        }
        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", id + "");
            map.put("uuid", uuid.toString());
            map.put("reason", reason);
            map.put("time", timeToEnd == null ? "null" : String.valueOf(timeToEnd));
            return map;
        }
    }
    public static class RolesRow extends DataBaseRow {
        public int id;
        public TextColor color;
        public String name;
        public int permissions;
        public int groupID;
        public boolean isStatic;

        public RolesRow(ResultSet set) {
            super(set);
            id = MySql.readObject(set, "id", Integer.class);
            color = TextColor.fromHexString("#" + MySql.readObject(set, "color", String.class));
            name = MySql.readObject(set, "name", String.class);
            permissions = MySql.readObject(set, "permissions", Integer.class);
            groupID = MySql.readObject(set, "id_group", Integer.class);
            isStatic = MySql.readObject(set, "static", Integer.class) > 0;
        }

        public static Optional<RolesRow> getBy(int role) { return Tables.ROLES_TABLE.get(role + ""); }
    }
    public static class PetsRow extends DataBaseRow {
        public int id;
        public UUID uuid;
        public String pet;
        public boolean isHide;
        public String name;
        public String color;

        public PetsRow(ResultSet set) {
            super(set);
            id = MySql.readObject(set, "id", Integer.class);
            uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
            pet = MySql.readObject(set, "pet", String.class);
            isHide = MySql.readObject(set, "is_hide", Integer.class) != 0;
            name = MySql.readObject(set, "name", String.class);
            color = MySql.readObject(set, "color", String.class);
        }
        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", String.valueOf(id));
            map.put("uuid", uuid.toString());
            map.put("pet", pet);
            map.put("is_hide", String.valueOf(isHide ? 1 : 0));
            map.put("name", name);
            map.put("color", color);
            return map;
        }
    }

    public static class PermissionRow extends DataBaseRow {
        public UUID uuid;
        public List<String> permissions;

        public PermissionRow(ResultSet set) {
            super(set);
            uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
            permissions = Streams.stream(system.json.parse(MySql.readObject(set, "permissions", String.class)).getAsJsonArray().iterator()).map(JsonElement::getAsString).toList();
        }
        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("uuid", uuid.toString());
            map.put("permissions", String.join(", ", permissions));
            return map;
        }

        private static void setPermission(Player player, String perm) {
            if (player.hasPermission(perm)) return;
            player.addAttachment(lime._plugin).setPermission(perm, true);
        }
        private static void unsetPermission(Player player, String perm) {
            player.addAttachment(lime._plugin).unsetPermission(perm);
        }

        public void removed() {
            lime.nextTick(() -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) return;
                permissions.forEach(perm -> unsetPermission(player, perm));
            });
        }
        public static void sync() {
            Tables.PERMISSIONS_TABLE.getRows().forEach(row -> {
                Player player = Bukkit.getPlayer(row.uuid);
                if (player == null) return;
                row.permissions.forEach(perm -> setPermission(player, perm));
            });
        }
    }
    public static class Variable extends DataBaseRow {
        public double church;
        public Variable(ResultSet set) {
            super(set);
            church = MySql.readObject(set, "church", Double.class);
        }
        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("church", String.valueOf(church));
            return map;
        }

        public static void addChurch(int add) {
            Methods.SQL.Async.rawSql("UPDATE variable SET church = church + @add", MySql.args().add("add", add).build(), () -> {});
        }

        public static Variable getSingle() {
            return Tables.VARIABLE_TABLE.getFirstRow().orElseThrow();
        }
    }
    public static class UserRow extends DataBaseRow {
        public int id;
        public final UUID uuid;
        public final String userName;
        public String firstName = null;
        public String lastName = null;
        public boolean isMale = true;
        public Integer phone = null;
        public Integer cardID = null;
        public Calendar birthdayDate = null;
        public int role;
        public Integer work = null;
        public Calendar workTime = null;
        public int phoneRegen = 0;
        public int cardRegen = 0;
        public int wanted = 0;
        public int exp = 0;

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
            birthdayDate = MySql.readObject(set, "birthday_date", Calendar.class);
            role = MySql.readObject(set, "role", Integer.class);
            work = MySql.readObject(set, "work", Integer.class);
            workTime = MySql.readObject(set, "work_time", Calendar.class);
            wanted = MySql.readObject(set, "wanted", Integer.class);
            exp = MySql.readObject(set, "exp", Integer.class);
            phoneRegen = MySql.readObject(set, "phone_regen", Integer.class);
            cardRegen = MySql.readObject(set, "card_regen", Integer.class);
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
            map.put("id", valueOfInt(id));
            map.put("uuid", uuid.toString());
            map.put("user_name", userName);
            map.put("first_name", firstName);
            map.put("last_name", lastName);
            map.put("male", isMale ? "true" : "false");
            map.put("phone", valueOfInt(phone));
            map.put("card_id", valueOfInt(cardID));
            map.put("birthday_date", system.formatCalendar(birthdayDate, false));
            map.put("role", String.valueOf(role));
            map.put("work", valueOfInt(work));
            map.put("work_time", system.formatCalendar(workTime, true));
            map.put("wanted", String.valueOf(wanted));
            map.put("exp", String.valueOf(exp));
            map.put("phone_regen", valueOfInt(phoneRegen));
            map.put("card_regen", valueOfInt(cardRegen));
            map.put("create_date", system.formatCalendar(CreateDate, true));
            map.put("connect_date", system.formatCalendar(ConnectDate, true));
            map.put("timed_id", valueOfInt(TabManager.getPayerIDorNull(uuid)));
            map.put("is_online", isOnline() ? "true" : "false");
            return map;
        }
    }
    public static class FriendRow extends DataBaseRow {
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

        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", valueOfInt(userID));
            map.put("user_id", valueOfInt(userID));
            map.put("friend_id", valueOfInt(friendID));
            map.put("friend_name", friendName);
            map.put("send_info", valueOfInt(sendInfo));
            return map;
        }
    }
    public static class PrisonRow extends DataBaseRow {
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

        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", String.valueOf(id));
            map.put("house_id", String.valueOf(houseID));
            map.put("user_id", String.valueOf(userID));
            map.put("owner_id", String.valueOf(ownerID));
            map.put("reason", String.valueOf(reason));
            map.put("is_log", String.valueOf(isLog));
            map.put("out_pos", outPos.toSave());
            map.put("end_time", system.formatCalendar(endTime, true));
            return map;
        }
    }
    public static class HouseRow extends DataBaseRow {
        public enum HouseType {
            HOSPITAL,
            POLICE,
            PRISON,
            CHURCH,
            BANK_VAULT
        }

        public int id;
        public String name;
        public boolean isRoom;
        public int street;
        public int cost;
        public int rent;
        public int cash;
        public Integer ownerID;
        public Vector posMin;
        public Vector posMax;
        public Position posMain;
        public BlockFace posFace;
        public HouseType type;
        public long private_flags;
        public JsonObject data;

        public HouseRow(ResultSet set) {
            super(set);
            id = MySql.readObject(set, "id", Integer.class);
            isRoom = MySql.readObject(set, "is_room", Integer.class) == 1;
            name = MySql.readObject(set, "name", String.class);
            street = MySql.readObject(set, "street", Integer.class);
            cost = MySql.readObject(set, "cost", Integer.class);
            rent = MySql.readObject(set, "rent", Integer.class);
            cash = MySql.readObject(set, "cash", Integer.class);
            ownerID = MySql.readObject(set, "owner", Integer.class);
            Vector pos1 = Methods.readPosition(set, "pos1");
            Vector pos2 = Methods.readPosition(set, "pos2");
            posMin = new Vector(
                    Math.min(pos1.getBlockX(), pos2.getBlockX()),
                    Math.min(pos1.getBlockY(), pos2.getBlockY()),
                    Math.min(pos1.getBlockZ(), pos2.getBlockZ())
            );
            posMax = new Vector(
                    Math.max(pos1.getBlockX(), pos2.getBlockX()),
                    Math.max(pos1.getBlockY(), pos2.getBlockY()),
                    Math.max(pos1.getBlockZ(), pos2.getBlockZ())
            ).add(new Vector(1, 1, 1));

            posMain = Methods.readPosition(set, lime.MainWorld, "posMain");
            posFace = BlockFace.valueOf(MySql.readObject(set, "posFace", String.class));
            String type = MySql.readObject(set, "type", String.class);
            this.type = type == null ? null : HouseType.valueOf(type);
            private_flags = MySql.readObject(set, "private", Long.class);
            String data = MySql.readObject(set, "data", String.class);
            this.data = data == null ? null : system.json.parse(data).getAsJsonObject();
        }

        public boolean inZone(Location location) {
            World world = location.getWorld();
            if (world != posMain.world) return false;
            return location.toVector().isInAABB(posMin, posMax);
        }

        public enum UseType {
            Deny,
            Owner,
            Sub,
            World
        }
        public static UseType useType(List<Rows.HouseRow> houseRows, UUID uuid) {
            if (houseRows.isEmpty()) return UseType.World;
            Optional<Rows.UserRow> user = Rows.UserRow.getBy(uuid);
            return houseRows.stream().flatMap(v -> user.map(_v -> _v.id)
                    .flatMap(id -> {
                        if (id.equals(v.ownerID)) return Optional.of(UseType.Owner);
                        if (v.isSub(id)) return Optional.of(UseType.Sub);
                        return Optional.empty();
                    })
                    .stream())
                    .findFirst()
                    .orElse(UseType.Deny);
        }

        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", String.valueOf(id));
            map.put("is_room", String.valueOf(isRoom ? 1 : 0));
            map.put("name", name);
            map.put("street", String.valueOf(street));
            map.put("cost", String.valueOf(cost));
            map.put("rent", String.valueOf(rent));
            map.put("cash", String.valueOf(cash));
            map.put("owner", valueOfInt(ownerID));
            map.put("pos1", system.getString(posMin));
            map.put("pos2", system.getString(posMax));
            map.put("posMain", posMain.toSave());
            map.put("posFace", posFace.name());
            map.put("type", type == null ? "" : type.name());
            map.put("private", String.valueOf(private_flags));
            return map;
        }

        public Location center() {
            Vector pos = posMin.getMidpoint(posMax);
            return new Location(posMain.world, pos.getX(), pos.getY(), pos.getZ());
        }

        public List<HouseSubsRow> subs() { return subs(id); }
        public boolean isSub(int userID) { return getIsSub(id, userID); }

        public static List<HouseRow> getInHouse(Player player) { return getInHouse(player, null); }
        public static List<HouseRow> getInHouse(Location pos) { return getInHouse(pos, null); }
        public static List<HouseRow> getInHouse(Player player, system.Func1<HouseRow, Boolean> filter) {
            Location location = player.getLocation();
            return location.getWorld() == lime.LoginWorld ? getInHouse(location, filter) : Collections.emptyList();
        }
        public static List<HouseRow> getInHouse(Location pos, system.Func1<HouseRow, Boolean> filter) {
            List<HouseRow> rows = new ArrayList<>();
            Tables.HOUSE_TABLE.forEach(row -> {
                if (row.inZone(pos) && (filter == null || filter.invoke(row)))
                    rows.add(row);
            });
            return rows;
        }

        public static List<Player> getInHouse(system.Func1<HouseRow, Boolean> filter) {
            List<Player> players = new ArrayList<>();
            Tables.HOUSE_TABLE.forEach(row -> Bukkit.getOnlinePlayers().forEach(player -> {
                Location location = player.getLocation();
                if (location.getWorld() != lime.LoginWorld) return;
                if (row.inZone(location) && (filter == null || filter.invoke(row))) players.add(player);
            }));
            return players;
        }

        private static List<HouseSubsRow> subs(int house_id) {
            List<HouseSubsRow> subs = new ArrayList<>();
            for (HouseSubsRow row : Tables.HOUSE_SUBS_TABLE.getRows()) {
                if (row.houseID == house_id)
                    subs.add(row);
            }
            return subs;
        }
        private static boolean getIsSub(int house_id, int user_id) {
            for (HouseSubsRow row : Tables.HOUSE_SUBS_TABLE.getRows()) {
                if (row.houseID == house_id && row.userID == user_id)
                    return true;
            }
            return false;
        }
    }
    public static class HouseSubsRow extends DataBaseRow {
        public int id;
        public int userID;
        public int houseID;
        public int isOwner;

        public HouseSubsRow(ResultSet set) {
            super(set);
            id = MySql.readObject(set, "id", Integer.class);
            userID = MySql.readObject(set, "user_id", Integer.class);
            houseID = MySql.readObject(set, "house_id", Integer.class);
            isOwner = MySql.readObject(set, "is_owner", Integer.class);
        }

        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", String.valueOf(id));
            map.put("user_id", String.valueOf(userID));
            map.put("house_id", String.valueOf(houseID));
            map.put("is_owner", String.valueOf(isOwner));
            return map;
        }
    }

    public static class UserFlagsRow extends DataBaseRow {
        public int id;
        public UUID uuid;
        public int backPackID;

        protected UserFlagsRow(ResultSet set) {
            super(set);
            id = MySql.readObject(set, "id", Integer.class);
            uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
            backPackID = MySql.readObject(set, "backpack_id", Integer.class);
        }

        public static Optional<Integer> backPackID(UUID uuid) {
            return Tables.USERFLAGS_TABLE.getOther("uuid", uuid.toString()).map(v -> v.backPackID);
        }

        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", String.valueOf(id));
            map.put("uuid", uuid.toString());
            map.put("backpack_id", String.valueOf(backPackID));
            return map;
        }
    }
    public static class UserCraftsRow extends DataBaseRow {
        public int id;
        public int userID;
        public ImmutableSet<Integer> craftWorks;
        public String craftRegex;
        public Integer useCount;

        protected UserCraftsRow(ResultSet set) {
            super(set);
            id = MySql.readObject(set, "id", Integer.class);
            userID = MySql.readObject(set, "user_id", Integer.class);
            craftWorks = Optional.ofNullable(MySql.readObject(set, "craft_works", String.class))
                    .map(v -> Arrays.stream(v.split(",")).map(ExtMethods::parseInt).flatMap(Optional::stream).collect(ImmutableSet.toImmutableSet()))
                    .orElse(null);
            craftRegex = MySql.readObject(set, "craft_regex", String.class);
            useCount = MySql.readObject(set, "use_count", Integer.class);
        }
        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", String.valueOf(id));
            map.put("user_id", String.valueOf(userID));
            map.put("craft_works", craftWorks == null ? null : craftWorks.stream().map(Object::toString).collect(Collectors.joining(",")));
            map.put("craft_regex", craftRegex);
            map.put("use_count", String.valueOf(useCount));
            return map;
        }
    }

    public static class BanListRow extends DataBaseRow {
        public enum Type {
            IP,
            UUID
        }
        public int id;
        public String user;
        public Type type;
        public String reason;
        public String owner;
        public Calendar createTime;

        protected BanListRow(ResultSet set) {
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
                    ? Rows.UserRow.getBy(UUID.fromString(user)).map(v -> v.userName).orElse(user)
                    : user;
        }

        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", String.valueOf(id));
            map.put("user", user);
            map.put("type", type.name());
            map.put("owner", owner);
            map.put("create_time", system.formatCalendar(createTime, true));
            return map;
        }
    }
    public static class PreDonateRow extends DataBaseRow {
        public enum State {
            NONE,
            GIVED,
            ERROR
        }
        public enum Type {
            DIAKA,
            TRADEMC,
            QIWI
        }
        public int id;
        public String name;
        public double amount;
        public Type type;
        public String info;
        public Calendar create_time;
        public State state;
        public State whitelist;

        protected PreDonateRow(ResultSet set) {
            super(set);
            id = MySql.readObject(set, "id", Integer.class);
            name = MySql.readObject(set, "name", String.class);
            amount = MySql.readObject(set, "amount", Double.class);
            type = Type.valueOf(MySql.readObject(set, "type", String.class));
            info = MySql.readObject(set, "info", String.class);
            create_time = MySql.readObject(set, "create_time", Calendar.class);
            state = State.valueOf(MySql.readObject(set, "state", String.class));
            whitelist = State.valueOf(MySql.readObject(set, "whitelist", String.class));
        }

        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", String.valueOf(id));
            map.put("name", name);
            map.put("amount", amount + "");
            map.put("type", type.name());
            map.put("info", info);
            map.put("create_time", system.formatCalendar(create_time, true));
            map.put("state", state.name());
            map.put("whitelist", whitelist.name());
            return map;
        }
    }
    public static class PreDonateItemsRow extends DataBaseRow {
        public int id;
        public UUID uuid;
        public String type;
        public int amount;

        protected PreDonateItemsRow(ResultSet set) {
            super(set);
            id = MySql.readObject(set, "id", Integer.class);
            uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
            type = MySql.readObject(set, "type", String.class);
            amount = MySql.readObject(set, "amount", Integer.class);
        }

        @Override public HashMap<String, String> appendToReplace(HashMap<String, String> map) {
            map = super.appendToReplace(map);
            map.put("id", String.valueOf(id));
            map.put("uuid", uuid.toString());
            map.put("type", type);
            map.put("amount", amount + "");
            return map;
        }
    }
}





















