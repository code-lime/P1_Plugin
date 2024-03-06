package org.lime.gp.database.rows;

import java.util.Map;
import java.util.UUID;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;

public class DiscordRow extends BaseRow {
    public long discordID;
    public UUID uuid;

    public DiscordRow(MySqlRow set) {
        super(set);
        discordID = MySql.readObject(set, "discord_id", Long.class);
        uuid = java.util.UUID.fromString(MySql.readObject(set, "uuid", String.class));
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("discord_id", String.valueOf(discordID));
        map.put("uuid", uuid.toString());
        return map;
    }
}