package org.lime.gp.database.mysql;

import org.lime.system;

public class debug {
    private system.Action1<String> sql = null;
    public debug withSQL(system.Action1<String> sql) { this.sql = sql; return this; }

    public void callSQL(String sql) {
        if (this.sql != null) this.sql.invoke(sql);
    }
}