package org.lime.gp.database.mysql;

import java.sql.PreparedStatement;

import org.lime.system;

public interface ConnectionInvokeData<T> {
    T invoke(PreparedStatement statement, system.Action1<String> onStep) throws Exception;
}