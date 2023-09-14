package org.lime.gp.database.mysql;

import java.util.Map;

import org.lime.system.toast.*;

public final class SelectSQL {
    public final Toast1<String> sql;
    public final Map<String, Object> args;

    public SelectSQL(Toast1<String> sql, Map<String, Object> args) {
        this.sql = sql;
        this.args = args;
    }
    public SelectSQL(String sql, Map<String, Object> args) {
        this(Toast.of(sql), args);
    }
}