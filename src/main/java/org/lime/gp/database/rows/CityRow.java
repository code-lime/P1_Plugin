package org.lime.gp.database.rows;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;

import java.sql.ResultSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CityRow extends BaseRow {
    public int id;
    public Optional<Integer> owner;

    public Vector posMin;
    public Vector posMax;

    public Optional<Vector> posMain;
    public Optional<Vector> posSpawn;

    public CityRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        owner = MySql.readObjectOptional(set, "owner", Integer.class);
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
        posMain = Methods.readPositionOptional(set, "pos_main");
        posSpawn = Methods.readPositionOptional(set, "pos_spawn");
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        return map;
    }

    public static Optional<CityRow> getBy(int id) { return Tables.CITY_TABLE.get(String.valueOf(id)); }
    public boolean inZone(Location location) {
        World world = location.getWorld();
        if (world != lime.MainWorld) return false;
        return location.toVector().isInAABB(posMin, posMax);
    }
    public static boolean hasCity(Location location) {
        return Tables.CITY_TABLE.hasBy(v -> v.inZone(location));
    }
}















