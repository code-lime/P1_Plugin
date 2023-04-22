package org.lime.gp.database.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.lime.system;

public final class AsyncConnection<T> {
    public final int index;
    public final SelectSQL sql;
    public final debug debug;
    public final ConnectionInvokeData<T> onData;
    public final system.Action1<Exception> onError;
    public final system.Action0 onFinally;

    public AsyncConnection(int index, debug debug, SelectSQL sql, ConnectionInvokeData<T> onData, system.Action1<Exception> onError, system.Action0 onFinally) {
        this.index = index;
        this.debug = debug;
        this.sql = sql;
        this.onData = onData;
        this.onError = onError;
        this.onFinally = onFinally;
    }

    public static <T>AsyncConnection<T> of(int index, debug debug, SelectSQL sql, ConnectionInvokeData<T> onData, system.Action1<Exception> onError, system.Action0 onFinally) {
        return new AsyncConnection<>(index, debug, sql, onData, onError, onFinally);
    }
    public static AsyncConnection<Object> of(int index, debug debug, SelectSQL sql, ConnectionInvoke onData, system.Action1<Exception> onError, system.Action0 onFinally) {
        return of(index, debug, sql, onData.toData(), onError, onFinally);
    }

    private void log(String dat) {
        system.Toast2<String, String> call = MySql.calls.getOrDefault(index, null);
        if (call == null) return;
        call.val1 += " & " + dat;
    }

    public boolean invoke(MySql sql) {
        Exception exception = null;
        log("C");
        try (Connection connection = sql.connection_func.invoke()) {
            log("P");
            try (PreparedStatement statement = MySql.prepareStatement(0, debug, connection, this.sql.sql, this.sql.args)) {
                log("I");
                onData.invoke(statement, this::log);
                log("</I>");
            }
            log("</P>");
        }
        catch (Exception e) {
            log("E");
            exception = e;
        }
        if (exception == null) {
            log("F");
            onFinally.invoke();
            log("DONE!");
            return true;
        }
        log("EI");
        onError.invoke(exception);
        log("F");
        onFinally.invoke();
        log("DONE!");
        return false;
    }
}