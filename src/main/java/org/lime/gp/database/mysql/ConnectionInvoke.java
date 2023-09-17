package org.lime.gp.database.mysql;

import java.sql.PreparedStatement;

import org.lime.system.toast.*;
import org.lime.system.execute.*;

public interface ConnectionInvoke {
    void invoke(PreparedStatement statement, Action1<String> onStep) throws Throwable;

    default ConnectionInvokeData<Object> toData() {
        return (v,a) -> {
            invoke(v,a);
            return null;
        };
    }
}