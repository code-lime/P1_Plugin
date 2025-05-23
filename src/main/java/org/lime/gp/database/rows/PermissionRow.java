package org.lime.gp.database.rows;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.json;

import javax.annotation.Nullable;
import java.util.*;

public class PermissionRow extends BaseRow {
    public static CoreElement create() {
        return CoreElement.create(PermissionRow.class)
                .withInit(PermissionRow::_init);
    }
    private static void _init() {
        lime.repeat(PermissionRow::update, 1);
    }
    private static void update() {
        PermissionRow.sync();
    }

    private @Nullable UUID uuid;
    public String rawUuid;
    public List<String> permissions;

    private Collection<? extends Player> getPlayers() {
        if (uuid == null) return Bukkit.getOnlinePlayers();

        Player player = Bukkit.getPlayer(uuid);
        return player == null ? Collections.emptySet() : Collections.singleton(player);
    }

    public PermissionRow(MySqlRow set) {
        super(set);
        rawUuid = MySql.readObject(set, "uuid", String.class);
        uuid = rawUuid.equals("*") ? null : java.util.UUID.fromString(rawUuid);
        permissions = Streams.stream(json.parse(MySql.readObject(set, "permissions", String.class)).getAsJsonArray().iterator()).map(JsonElement::getAsString).toList();
    }
    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("uuid", uuid == null ? "*" : uuid.toString());
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
            Collection<? extends Player> modifyPlayers = getPlayers();
            modifyPlayers.forEach(player -> permissions.forEach(perm -> unsetPermission(player, perm)));
        });
    }
    public static void sync() {
        Tables.PERMISSIONS_TABLE.getRows().forEach(row -> {
            Collection<? extends Player> modifyPlayers = row.getPlayers();
            modifyPlayers.forEach(player -> row.permissions.forEach(perm -> setPermission(player, perm)));
        });
    }
}