package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.rows.AnyRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import javax.annotation.Nullable;
import java.util.UUID;

public class SQL extends Base {
    public final String sql;
    public final ActionSlot called;

    public SQL(JsonObject json) {
        super(json);
        sql = json.get("sql").getAsString();
        called = ActionSlot.parse(this, json.get("called").getAsJsonObject());
    }

    @Override protected void showGenerate(UserRow row, @Nullable Player player, int page, Apply apply) {
        Methods.SQL.Async.rawSqlQuery(apply.apply(sql), (set) -> Toast.of(UUID.fromString(MySql.readObject(set, "uuid", String.class)), AnyRow.of(set)), (list) -> list.forEach(kv -> {
            Player to = Bukkit.getPlayer(kv.val0);
            if (to == null) return;
            called.invoke(to, apply.add(kv.val1), false);
        })).withSQL((sql) -> Logged.log(player, sql, this));
    }
}













