package org.lime.gp.database.mysql;

import org.lime.gp.lime;
import org.lime.system.Time;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func1;
import org.lime.system.toast.LockToast1;
import org.lime.system.toast.LockToast2;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MySqlAsync {
    private final MySql instance;
    public MySqlAsync(MySql instance) {
        this.instance = instance;
    }

    final LockToast2<Boolean, Double> debug = Toast.lock(false, 0.0);

    public boolean isDebug() { return debug.get0(); }
    public boolean isFilterDebug(double time) { return time >= debug.get1(); }
    /*private boolean isMenu() {
        if (!debug.get0()) return false;
        boolean is_menu = false;
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (stackTraceElement.toString().contains("MenuCreator") || stackTraceElement.toString().contains("getTable")) {
                is_menu = true;
                break;
            }
        }
        return is_menu;
    }*/

    private final LockToast1<Integer> nextCall = Toast.of(0).lock();
    private int nextCallIndex() { return nextCall.edit0(v -> v + 1); }

    public ExecuteData rawSql(String query, Action0 callback) { return rawSql(query, null, callback); }

    public <T> ExecuteData rawSqlOnce(String query, Class<T> tClass, Action1<T> callback) { return rawSqlOnce(query, null, tClass, callback); }
    public <T> ExecuteData rawSqlOnce(String query, Func1<MySqlRow, T> reader, Action1<T> callback) { return rawSqlOnce(query, null, reader, callback); }
    public <T> ExecuteData rawSqlQuery(String query, Class<T> tClass, Action1<List<T>> callback) { return rawSqlQuery(query, null, tClass, callback); }
    public <T> ExecuteData rawSqlQuery(String query, Func1<MySqlRow, T> reader, Action1<List<T>> callback) { return rawSqlQuery(query, null, reader, callback); }

    public String nowTime() {
        return Time.formatCalendar(Time.moscowNow(), true);
    }

    public ExecuteData rawSql(String query, Map<String, Object> args, Action0 callback) {
        int index = nextCallIndex();
        MySql.calls.put(index, Toast.of("SQL." + query, nowTime()));
        ExecuteData debug = new ExecuteData();
        Toast1<String> _sql = Toast.of(query);
        this.instance.callMySQL(index, debug, new SelectSQL(_sql, args), (stmt, log) -> {
            String _lower = _sql.val0.toLowerCase();
            if (_lower.startsWith("select")) {
                log.invoke("S:t");
                stmt.executeQuery().close();
                log.invoke("S:t+");
            } else {
                log.invoke("S:f");
                try {
                    stmt.executeUpdate();
                } catch (Throwable th) {
                    log.invoke("-" + th);
                    throw th;
                }
                log.invoke("S:f+");
            }

            log.invoke("S:0");
            if (callback == null) return;
            log.invoke("S:1");
            lime.invokeSync(callback);
            log.invoke("S:2");
        }, e -> {
            lime.logOP(_sql.val0);
            lime.logStackTrace(e);
            debug.onError(e);
        }, () -> {
            MySql.calls.remove(index);
            debug.onFinally();
        });
        return debug;
    }

    public ExecuteData rawSqlUpdate(String query, Map<String, Object> args, Action1<Integer> callback) {
        int index = nextCallIndex();
        MySql.calls.put(index, Toast.of("SQL." + query, nowTime()));
        ExecuteData debug = new ExecuteData();
        Toast1<String> _sql = Toast.of(query);
        this.instance.callMySQL(index, debug, new SelectSQL(_sql, args), (stmt, log) -> {
            int result;
            log.invoke("S:f");
            try {
                result = stmt.executeUpdate();
            } catch (Throwable th) {
                log.invoke("-" + th);
                throw th;
            }
            log.invoke("S:f+");

            log.invoke("S:0");
            if (callback == null) return;
            log.invoke("S:1");
            lime.invokeSync(() -> callback.invoke(result));
            log.invoke("S:2");
        }, e -> {
            lime.logOP(_sql.val0);
            lime.logStackTrace(e);
            debug.onError(e);
        }, () -> {
            MySql.calls.remove(index);
            debug.onFinally();
        });
        return debug;
    }

    public <T> ExecuteData rawSqlOnce(String query, Map<String, Object> args, Class<T> tClass, Action1<T> callback) { return rawSqlOnce(query, args, reader -> MySql.readObject(reader, 1, tClass), callback); }
    public <T> ExecuteData rawSqlOnce(String query, Map<String, Object> args, Func1<MySqlRow, T> reader, Action1<T> callback) {
        int index = nextCallIndex();
        MySql.calls.put(index, Toast.of("SQL_ONCE." + query, nowTime()));
        ExecuteData debug = new ExecuteData();
        Toast1<String> _sql = Toast.of(query);
        this.instance.callMySQL(index, debug, new SelectSQL(_sql, args), (stmt, log) -> {
            MySqlRow row;
            try (ResultSet set = stmt.executeQuery()) {
                row = set.next() ? MySqlRow.export(set) : null;
            }
            if (callback == null) return;
            lime.invokeSync(() -> callback.invoke(row == null ? null : reader.invoke(row)));
        }, e -> {
            lime.logOP(_sql.val0);
            lime.logStackTrace(e);
            debug.onError(e);
        }, () -> {
            MySql.calls.remove(index);
            debug.onFinally();
        });
        return debug;
    }

    public <T> ExecuteData rawSqlQuery(String query, Map<String, Object> args, Class<T> tClass, Action1<List<T>> callback) { return rawSqlQuery(query, args, reader -> MySql.readObject(reader, 1, tClass), callback); }
    public <T> ExecuteData rawSqlQuery(String query, Map<String, Object> args, Func1<MySqlRow, T> reader, Action1<List<T>> callback) {
        int index = nextCallIndex();
        MySql.calls.put(index, Toast.of("SQL_QUERY." + query, nowTime()));
        ExecuteData debug = new ExecuteData();
        Toast1<String> _sql = Toast.of(query);
        this.instance.callMySQL(index, debug, new SelectSQL(_sql, args), (stmt, log) -> {
            List<MySqlRow> rows = new ArrayList<>();
            try (ResultSet set = stmt.executeQuery()) {
                while (set.next())
                    rows.add(MySqlRow.export(set));
            }
            if (callback == null) return;
            lime.invokeSync(() -> {
                List<T> ret = new ArrayList<>();
                rows.forEach(v -> ret.add(reader.invoke(v)));
                callback.invoke(ret);
            });
        }, e -> {
            lime.logOP(_sql.val0);
            lime.logStackTrace(e);
            debug.onError(e);
        }, () -> {
            MySql.calls.remove(index);
            debug.onFinally();
        });
        return debug;
    }
}
/*public class _sync {
    private _sync() {}

    public void RawSql(String query) {
        RawSql(query, null);
    }

    public <T>T RawSqlOnce(String query, Class<T> tClass) {
        return RawSqlOnce(query, null, tClass);
    }
    public <T>T RawSqlOnce(String query, Func1<ResultSet, T> reader) {
        return RawSqlOnce(query, null, reader);
    }

    public <T>List<T> RawSqlQuery(String query, Class<T> tClass) {
        return RawSqlQuery(query, null, tClass);
    }
    public <T>List<T> RawSqlQuery(String query, Func1<ResultSet, T> reader) {
        return RawSqlQuery(query, null, reader);
    }

    public void RawSql(String query, Map<String, Object> args) {
        Connection conn = toMySQL();
        Toast1<String> _sql = Toast.of(query);
        try (PreparedStatement stmt = prepareStatement(conn, _sql, args)) {
            stmt.execute();
        } catch (Exception e) {
            lime.Log(_sql.val0);
            throw new IllegalArgumentException(e);
        }
        try { conn.close(); }
        catch (Exception ignored) { }
    }

    public <T>T RawSqlOnce(String query, Map<String, Object> args, Class<T> tClass) {
        return RawSqlOnce(query, args, reader -> ReadObject(reader, 1, tClass));
    }
    public <T>T RawSqlOnce(String query, Map<String, Object> args, Func1<ResultSet, T> reader) {
        T ret = null;
        Connection conn = toMySQL();
        Toast1<String> _sql = Toast.of(query);
        try (PreparedStatement stmt = prepareStatement(conn, _sql, args); ResultSet set = stmt.executeQuery()) {
            if (set.next()) ret = reader.invoke(set);
        } catch (Exception e) {
            lime.Log(_sql.val0);
            throw new IllegalArgumentException(e);
        }
        try { conn.close(); }
        catch (Exception ignored) { }
        return ret;
    }

    public <T>List<T> RawSqlQuery(String query, Map<String, Object> args, Class<T> tClass) {
        return RawSqlQuery(query, args, reader -> ReadObject(reader, 1, tClass));
    }
    public <T>List<T> RawSqlQuery(String query, Map<String, Object> args, Func1<ResultSet, T> reader) {
        List<T> ret = new ArrayList<>();
        Connection conn = toMySQL();
        Toast1<String> _sql = Toast.of(query);
        try (PreparedStatement stmt = prepareStatement(conn, _sql, args); ResultSet set = stmt.executeQuery()) {
            while (set.next()) ret.add(reader.invoke(set));
        } catch (Exception e) {
            lime.Log(_sql.val0);
            throw new IllegalArgumentException(e);
        }
        try { conn.close(); }
        catch (Exception ignored) { }
        return ret;
    }
}*/