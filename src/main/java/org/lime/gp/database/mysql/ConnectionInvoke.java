package org.lime.gp.database.mysql;

import java.sql.PreparedStatement;

import org.lime.system;

public interface ConnectionInvoke {
    void invoke(PreparedStatement statement, system.Action1<String> onStep) throws Exception;

    default ConnectionInvokeData<Object> toData() {
        return (v,a) -> {
            invoke(v,a);
            return null;
        };
    }
}