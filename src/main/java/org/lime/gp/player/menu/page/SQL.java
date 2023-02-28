package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Methods;
import org.lime.gp.database.MySql;
import org.lime.gp.database.Rows;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.system;

import java.util.UUID;

public class SQL extends Base {
    public final String sql;
    public final ActionSlot called;

    public SQL(JsonObject json) {
        super(json);
        sql = json.get("sql").getAsString();
        called = ActionSlot.parse(this, json.get("called").getAsJsonObject());
    }

    @Override protected void showGenerate(Rows.UserRow row, Player player, int page, Apply apply) {
        Methods.SQL.Async.rawSqlQuery(apply.apply(sql), (set) -> system.toast(UUID.fromString(MySql.readObject(set, "uuid", String.class)), Rows.AnyRow.of(set)), (list) -> list.forEach(kv -> {
            Player to = Bukkit.getPlayer(kv.val0);
            if (to == null) return;
            called.invoke(to, apply.add(kv.val1), false);
        })).withSQL((sql) -> Logged.log(player, sql, this));
    }
}













