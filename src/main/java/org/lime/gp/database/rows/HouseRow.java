package org.lime.gp.database.rows;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.system.json;
import org.lime.system.execute.*;
import org.lime.gp.lime;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;

import com.google.gson.JsonObject;
import org.lime.system.utils.MathUtils;

public class HouseRow extends BaseRow {
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
    public HouseRow.HouseType type;
    public long private_flags;
    public JsonObject data;

    public HouseRow(MySqlRow set) {
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
        this.data = data == null ? null : json.parse(data).getAsJsonObject();
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
    public static HouseRow.UseType useType(List<HouseRow> houseRows, UUID uuid) {
        if (houseRows.isEmpty()) return UseType.World;
        Optional<UserRow> user = UserRow.getBy(uuid);
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

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("is_room", String.valueOf(isRoom ? 1 : 0));
        map.put("name", name);
        map.put("street", String.valueOf(street));
        map.put("cost", String.valueOf(cost));
        map.put("rent", String.valueOf(rent));
        map.put("cash", String.valueOf(cash));
        map.put("owner", Tables.valueOfInt(ownerID));
        map.put("pos1", MathUtils.getString(posMin));
        map.put("pos2", MathUtils.getString(posMax));
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
    public static boolean hasHouse(Location pos) {
        return Tables.HOUSE_TABLE.hasBy(v -> v.inZone(pos));
    }
    public static List<HouseRow> getInHouse(Player player, Func1<HouseRow, Boolean> filter) {
        Location location = player.getLocation();
        return location.getWorld() == lime.LoginWorld ? getInHouse(location, filter) : Collections.emptyList();
    }
    public static List<HouseRow> getInHouse(Location pos, Func1<HouseRow, Boolean> filter) {
        List<HouseRow> rows = new ArrayList<>();
        Tables.HOUSE_TABLE.forEach(row -> {
            if (row.inZone(pos) && (filter == null || filter.invoke(row)))
                rows.add(row);
        });
        return rows;
    }

    public static List<Player> getInHouse(Func1<HouseRow, Boolean> filter) {
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