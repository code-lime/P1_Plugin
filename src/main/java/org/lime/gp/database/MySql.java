package org.lime.gp.database;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.lime.gp.lime;
import org.lime.gp.module.ThreadPool;
import org.lime.system;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MySql implements Closeable {
    private final MiniConnectionPoolManager poolManager;
    private final system.Func0<Connection> connection_func;
    public MySql(String host, int port, String database, String login, String password) {
        HashMap<String, String> map = system.map.<String, String>of()
                .add("useSSL", "false")
                .add("useUnicode", "true")
                .add("characterEncoding", "utf-8")
                .add("defaultFetchSize", "100")
                .add("useServerPrepStmts", "true")
                .add("allowMultiQueries", "true")
                .add("useUsageAdvisor", "false")
                .add("autoReconnect", "true")
                .add("failOverReadOnly", "false")
                .add("maxReconnects", "10")
                .build();
        String url = "jdbc:mysql://"+host+":"+port+"/"+database+"?" + map.entrySet().stream().map(kv -> kv.getKey() + "=" + kv.getValue()).collect(Collectors.joining("&"));

        MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setUrl(url);
        dataSource.setUser(login);
        dataSource.setPassword(password);
        poolManager = new MiniConnectionPoolManager(dataSource, 10);

        connection_func = poolManager::getValidConnection;
        /*connection_func = () -> {
            try { return DriverManager.getConnection(url, login, password); }
            catch (SQLException e) { throw new IllegalArgumentException(e); }
        };*/
    }

    public int getCalls() { return calls.size(); }
    public List<String> getDumpCalls() { return calls.values().stream().map(v -> v.val0 + " | " + v.val1).collect(Collectors.toList()); }
    public boolean switchDebug() { return Async.debug.edit0(v -> !v); }

    @Override public void close() {
        try {
            poolManager.close();
        } catch (Exception e) {
            lime.logStackTrace(e);
        }
    }

    public interface ConnectionInvoke {
        void invoke(PreparedStatement statement, system.Action1<String> onStep) throws Exception;

        default ConnectionInvokeData<Object> toData() {
            return (v,a) -> {
                invoke(v,a);
                return null;
            };
        }
    }
    public interface ConnectionInvokeData<T> {
        T invoke(PreparedStatement statement, system.Action1<String> onStep) throws Exception;
    }

    public static final class SelectSQL {
        public final system.Toast1<String> sql;
        public final Map<String, Object> args;

        public SelectSQL(system.Toast1<String> sql, Map<String, Object> args) {
            this.sql = sql;
            this.args = args;
        }
        public SelectSQL(String sql, Map<String, Object> args) {
            this(system.toast(sql), args);
        }
    }
    public static final class AsyncConnection<T> {
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
            system.Toast2<String, String> call = calls.getOrDefault(index, null);
            if (call == null) return;
            call.val1 += " & " + dat;
        }

        public boolean invoke(MySql sql) {
            Exception exception = null;
            log("C");
            try (Connection connection = sql.connection_func.invoke()) {
                log("P");
                try (PreparedStatement statement = prepareStatement(0, debug, connection, this.sql.sql, this.sql.args)) {
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

    private final ConcurrentLinkedQueue<system.Action0> invokeQueue = new ConcurrentLinkedQueue<>();
    public void invokeNext() {
        system.Action0 action = invokeQueue.poll();
        if (action != null) action.invoke();
    }

    public boolean isValidMySQL() {
        try (Connection connection = connection_func.invoke()) { return connection.isValid(100); }
        catch (Exception e) { return false; }
    }
    public <T>void callMySQL(int index, debug debug, SelectSQL sql, ConnectionInvokeData<T> func, system.Action1<Exception> error, system.Action0 _finally) {
        AsyncConnection<T> connection = AsyncConnection.of(index, debug, sql, func, error, _finally);
        invokeQueue.add(() -> connection.invoke(this));
    }
    public void callMySQL(int index, debug debug, SelectSQL sql, ConnectionInvoke func, system.Action1<Exception> error, system.Action0 _finally) {
        callMySQL(index, debug, sql, func.toData(), error, _finally);
    }

    private static String fromCalendar(Calendar calendar) {
        return "'" + String.valueOf(calendar.get(Calendar.YEAR)) +
                '-' +
                (calendar.get(Calendar.MONTH) + 1) +
                '-' +
                calendar.get(Calendar.DAY_OF_MONTH) +
                ' ' +
                calendar.get(Calendar.HOUR_OF_DAY) +
                ':' +
                calendar.get(Calendar.MINUTE) +
                ':' +
                calendar.get(Calendar.SECOND) + "'";
    }
    private static Stream<Object> toList(Array array) {
        int length = Array.getLength(array);
        Object[] _array = new Object[length];
        for (int i = 0; i < length; i++) _array[i] = Array.get(array, i);
        return Arrays.stream(_array);
    }
    public static String toSqlObject(Object value) {
        if (value == null) return "NULL";
        else if (value instanceof Date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime((Date) value);
            return fromCalendar(calendar);
        } else if (value instanceof Calendar) return fromCalendar((Calendar) value);
        else if (value instanceof Number) {
            return value.toString();
        }
        else if (value instanceof UUID) return "'"+value+"'";
        else if (value instanceof Boolean) return ((Boolean)value) ? "1" : "0";
        else if (value instanceof Iterable<?>) return toSqlObject(Streams.stream((Iterable<?>) value));
        else if (value instanceof Iterator<?>) return toSqlObject(Streams.stream((Iterator<?>) value));
        else if (value instanceof Array) return toSqlObject(toList((Array)value));
        else if (value instanceof String) return "'" + value.toString().replace("\\", "\\\\").replace("'", "\\'") + "'";
        else if (value instanceof JsonPrimitive) {
            JsonPrimitive primitive = (JsonPrimitive)value;
            if (primitive.isNumber()) return toSqlObject(primitive.getAsNumber());
            else if (primitive.isBoolean()) return toSqlObject(primitive.getAsBoolean());
            else return toSqlObject(primitive.getAsString());
        }
        else if (value instanceof Stream<?>) {
            String str = ((Stream<?>)value).map(MySql::toSqlObject).collect(Collectors.joining(","));
            return "(" + (str.isEmpty() ? "NULL" : str) + ")";
        }
        else if (value instanceof ScriptObjectMirror) {
            ScriptObjectMirror obj = (ScriptObjectMirror)value;
            if (obj.isArray()) {
                JsonArray json = new JsonArray();
                obj.values().forEach(v -> json.add(toSqlObject(v)));
                return toSqlObject(json);
            }
            return toSqlObject(new Gson().toJsonTree(obj));
        }
        else {
            lime.logOP("CLASS: " + value.getClass());
            lime.logOP(value.toString());
            return toSqlObject(value.toString());
        }
    }
    public static String fromSqlObjectString(int type, Object value) {
        switch (type) {
            case Types.NULL: return "null";
            default:
                if (value == null) return "null";
                if (value instanceof Calendar) return system.formatCalendar((Calendar)value, false);
                return value.toString();
        }
    }
    private static PreparedStatement prepareStatement(int state, debug debug, Connection connection, system.Toast1<String> sql, Map<String, Object> args) throws SQLException {
        if (args != null) {
            for (Map.Entry<String, Object> arg : args.entrySet()) sql.val0 = sql.val0.replace("@" + arg.getKey(), toSqlObject(arg.getValue()));
        }
        if (debug != null) debug.callSQL(sql.val0);
        if (state == 1) {
            Component component = Component
                    .text("[" + system.formatCalendar(system.getMoscowNow(), true) + "] SQL QUERY")
                    .color(NamedTextColor.GOLD)
                    .hoverEvent(HoverEvent.showText(Component.text(sql.val0)))
                    .clickEvent(ClickEvent.copyToClipboard(sql.val0));
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (!p.isOp()) return;
                p.sendMessage(component);
            });
        }
        return connection.prepareStatement(sql.val0);
    }

    public static system.map.builder<String, Object> args() {
        return system.map.of();
    }

    public static int columnsCount(ResultSet set) {
        try {
            return set.getMetaData().getColumnCount();
        } catch (SQLException ex) {
            throw new IllegalArgumentException("ReadObject", ex);
        }
    }
    public static Object readObject(ResultSet set, int index) {
        try {
            return set.getObject(index);
        } catch (SQLException ex) {
            throw new IllegalArgumentException("ReadObject", ex);
        }
    }
    public static <T>T readObject(ResultSet set, int index, Class<T> tClass) {
        try {
            return set.getObject(index, tClass);
        } catch (SQLException ex) {
            throw new IllegalArgumentException("ReadObject", ex);
        }
    }
    public static Object readObject(ResultSet set, String column) {
        try {
            return set.getObject(set.findColumn(column));
        } catch (SQLException ex) {
            throw new IllegalArgumentException("ReadObject", ex);
        }
    }
    public static <T>T readObject(ResultSet set, String column, Class<T> tClass) {
        try {
            return set.getObject(set.findColumn(column), tClass);
        } catch (SQLException ex) {
            throw new IllegalArgumentException("ReadObject", ex);
        }
    }
    public static boolean hasColumn(ResultSet set, String column) {
        try {
            ResultSetMetaData meta = set.getMetaData();
            int columns = meta.getColumnCount();
            for (int x = 1; x <= columns; x++)
                if (column.equals(meta.getColumnName(x)))
                    return true;
            return false;
        } catch (SQLException ex) {
            throw new IllegalArgumentException("ReadObject", ex);
        }
    }

    public static Object readEmpty(ResultSet set) {
        return new Object();
    }

    public _async Async = new _async();
    /*public _sync Sync = new _sync();*/

    public static class debug {
        private system.Action1<String> sql = null;
        public debug withSQL(system.Action1<String> sql) { this.sql = sql; return this; }

        public void callSQL(String sql) {
            if (this.sql != null) this.sql.invoke(sql);
        }
    }

    private static final ConcurrentHashMap<Integer, system.Toast2<String, String>> calls = new ConcurrentHashMap<>();

    public class _async {
        private _async() {}

        private final system.LockToast1<Boolean> debug = system.toast(false).lock();
        private boolean isMenu() {
            if (!debug.get0()) return false;
            boolean is_menu = false;
            for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                if (stackTraceElement.toString().contains("MenuCreator") || stackTraceElement.toString().contains("getTable")) {
                    is_menu = true;
                    break;
                }
            }
            return is_menu;
        }

        private final system.LockToast1<Integer> nextCall = system.toast(0).lock();
        private int nextCallIndex() { return nextCall.edit0(v -> v + 1); }

        public debug rawSql(String query, system.Action0 callback) { return rawSql(query, null, callback); }

        public <T>debug rawSqlOnce(String query, Class<T> tClass, system.Action1<T> callback) { return rawSqlOnce(query, null, tClass, callback); }
        public <T>debug rawSqlOnce(String query, system.Func1<ResultSet, T> reader, system.Action1<T> callback) { return rawSqlOnce(query, null, reader, callback); }
        public <T>debug rawSqlQuery(String query, Class<T> tClass, system.Action1<List<T>> callback) { return rawSqlQuery(query, null, tClass, callback); }
        public <T>debug rawSqlQuery(String query, system.Func1<ResultSet, T> reader, system.Action1<List<T>> callback) { return rawSqlQuery(query, null, reader, callback); }

        public String nowTime() {
            return system.formatCalendar(system.getMoscowNow(), true);
        }

        public debug rawSql(String query, Map<String, Object> args, system.Action0 callback) {
            int index = nextCallIndex();
            calls.put(index, system.toast("SQL." + query, nowTime()));
            boolean is_menu = isMenu();
            debug debug = new debug();
            system.Toast1<String> _sql = system.toast(query);
            callMySQL(index, debug, new SelectSQL(_sql, args), (stmt, log) -> {
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
            }, () -> calls.remove(index));
            return debug;
        }

        public <T>debug rawSqlOnce(String query, Map<String, Object> args, Class<T> tClass, system.Action1<T> callback) { return rawSqlOnce(query, args, reader -> readObject(reader, 1, tClass), callback); }
        public <T>debug rawSqlOnce(String query, Map<String, Object> args, system.Func1<ResultSet, T> reader, system.Action1<T> callback) {
            int index = nextCallIndex();
            calls.put(index, system.toast("SQL_ONCE." + query, nowTime()));
            boolean is_menu = isMenu();
            debug debug = new debug();
            system.Toast1<String> _sql = system.toast(query);
            callMySQL(index, debug, new SelectSQL(_sql, args), (stmt, log) -> {
                T ret;
                try (ResultSet set = stmt.executeQuery()) {
                    ret = set.next() ? reader.invoke(set) : null;
                }
                if (callback == null) return;
                lime.invokeSync(() -> callback.invoke(ret));
            }, e -> {
                lime.logOP(_sql.val0);
                lime.logStackTrace(e);
            }, () -> calls.remove(index));
            return debug;
        }

        public <T>debug rawSqlQuery(String query, Map<String, Object> args, Class<T> tClass, system.Action1<List<T>> callback) { return rawSqlQuery(query, args, reader -> readObject(reader, 1, tClass), callback); }
        public <T>debug rawSqlQuery(String query, Map<String, Object> args, system.Func1<ResultSet, T> reader, system.Action1<List<T>> callback) {
            int index = nextCallIndex();
            calls.put(index, system.toast("SQL_QUERY." + query, nowTime()));
            boolean is_menu = isMenu();
            debug debug = new debug();
            system.Toast1<String> _sql = system.toast(query);
            callMySQL(index, debug, new SelectSQL(_sql, args), (stmt, log) -> {
                List<T> ret = new ArrayList<>();
                try (ResultSet set = stmt.executeQuery()) {
                    while (set.next())
                        ret.add(reader.invoke(set));
                }
                if (callback == null) return;
                lime.invokeSync(() -> callback.invoke(ret));
            }, e -> {
                lime.logOP(_sql.val0);
                lime.logStackTrace(e);
            }, () -> calls.remove(index));
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
        public <T>T RawSqlOnce(String query, system.Func1<ResultSet, T> reader) {
            return RawSqlOnce(query, null, reader);
        }

        public <T>List<T> RawSqlQuery(String query, Class<T> tClass) {
            return RawSqlQuery(query, null, tClass);
        }
        public <T>List<T> RawSqlQuery(String query, system.Func1<ResultSet, T> reader) {
            return RawSqlQuery(query, null, reader);
        }

        public void RawSql(String query, Map<String, Object> args) {
            Connection conn = toMySQL();
            system.Toast1<String> _sql = system.toast(query);
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
        public <T>T RawSqlOnce(String query, Map<String, Object> args, system.Func1<ResultSet, T> reader) {
            T ret = null;
            Connection conn = toMySQL();
            system.Toast1<String> _sql = system.toast(query);
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
        public <T>List<T> RawSqlQuery(String query, Map<String, Object> args, system.Func1<ResultSet, T> reader) {
            List<T> ret = new ArrayList<>();
            Connection conn = toMySQL();
            system.Toast1<String> _sql = system.toast(query);
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
}





















