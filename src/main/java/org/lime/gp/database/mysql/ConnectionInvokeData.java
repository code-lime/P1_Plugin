package org.lime.gp.database.mysql;

import java.sql.PreparedStatement;

import org.lime.system.toast.*;
import org.lime.system.execute.*;

public interface ConnectionInvokeData<T> {
    T invoke(PreparedStatement statement, Action1<String> onStep) throws Throwable;
}