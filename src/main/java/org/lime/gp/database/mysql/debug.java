package org.lime.gp.database.mysql;

import org.lime.system.toast.*;
import org.lime.system.execute.*;

public class debug {
    private Action1<String> sql = null;
    private Action1<String> log = null;
    public debug withSQL(Action1<String> sql) {
        this.sql = sql;
        return this;
    }
    public debug withLog(Action1<String> log) {
        this.log = log;
        return this;
    }

    public void callSQL(String sql) {
        if (this.sql != null) this.sql.invoke(sql);
    }
    public void log(String log) {
        if (this.log != null) this.log.invoke(log);
    }
}