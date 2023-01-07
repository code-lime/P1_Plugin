package org.lime.gp.player.menu;

import org.bukkit.entity.Player;
import org.lime.gp.database.Methods;
import org.lime.gp.database.MySql;
import org.lime.gp.lime;
import org.lime.system;

public class Logged {
    public static void log(Player player, String sql, ILoggedDelete base) {
        if (!base.isLogged()) return;
        String lower = sql.toLowerCase();
        if (lower.startsWith("select") && lower.contains("from")) return;
        Methods.SQL.Async.rawSql("INSERT INTO logger (`uuid`, `sql`, `menu`) VALUES ('"+(player == null ? "????????-????-????-????-????????????" : player.getUniqueId())+"', @sql, @menu)", MySql.args().add("sql", sql).add("menu", base.getLoggedKey()).build(), () -> {});
    }

    public interface ILoggedDelete extends system.IDelete {
        ILoggedDelete NONE = new ILoggedDelete() {
            @Override public boolean isDeleted() { return false; }
            @Override public String getLoggedKey() { return "NONE"; }
            @Override public boolean isLogged() { return true; }
            @Override public void delete() { }
        };
        String getLoggedKey();
        boolean isLogged();
    }
    public static class ChildLoggedDeleteHandle extends system.ChildDeleteHandle implements ILoggedDelete {
        private final ILoggedDelete base;
        public ChildLoggedDeleteHandle(ILoggedDelete base) {
            super(base);
            this.base = base;
        }
        @Override public String getLoggedKey() { return base.getLoggedKey(); }
        @Override public boolean isLogged() { return base.isDeleted(); }
    }
}
