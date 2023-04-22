package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.UUID;

import org.lime.gp.database.MySql;

public class DiscordRow extends BaseRow {
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