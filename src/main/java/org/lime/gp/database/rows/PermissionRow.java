package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.database.MySql;
import org.lime.gp.database.tables.Tables;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;

public class PermissionRow extends BaseRow {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(PermissionRow.class)
                .withInit(PermissionRow::_init);
    }
    private static void _init() {
        lime.repeat(PermissionRow::update, 1);
    }
    private static void update() {
        PermissionRow.sync();
    }

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