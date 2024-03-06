package org.lime.gp.database.mysql;

import com.google.common.collect.Streams;
import com.google.gson.JsonPrimitive;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.lime.database.MiniConnectionPoolManager;
import org.lime.gp.lime;
import org.lime.system.Time;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func0;
import org.lime.system.map;
import org.lime.system.toast.Toast1;
import org.lime.system.toast.Toast2;

import java.io.Closeable;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MySql implements Closeable {
    private final MiniConnectionPoolManager poolManager;
    final Func0<Connection> connection_func;
    public MySql(String host, int port, String database, String login, String password) {
        HashMap<String, String> _map = map.<String, String>of()
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
        String url = "jdbc:mysql://"+host+":"+port+"/"+database+"?" + _map.entrySet().stream().map(kv -> kv.getKey() + "=" + kv.getValue()).collect(Collectors.joining("&"));

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
    public List<String> getDumpCalls() {
        return calls.values().stream().map(v -> v.val0 + " | " + v.val1).collect(Collectors.toList());
    }
    public boolean switchDebug() { return Async.debug.edit0(v -> !v); }
    public double filterDebug(double value) { return Async.debug.edit1(v -> value); }

    @Override public void close() {
        try {
            poolManager.close();
        } catch (Exception e) {
            lime.logStackTrace(e);
        }
    }

    private final ConcurrentLinkedQueue<Action0> invokeQueue = new ConcurrentLinkedQueue<>();
    public void invokeNext() {
        Action0 action = invokeQueue.poll();
        if (action != null) action.invoke();
    }

    public boolean isValidMySQL() {
        try (Connection connection = connection_func.invoke()) { return connection.isValid(100); }
        catch (Exception e) { return false; }
    }
    public <T>void callMySQL(int index, ExecuteData debug, SelectSQL sql, ConnectionInvokeData<T> func, Action1<Throwable> error, Action0 _finally) {
        AsyncConnection<T> connection = AsyncConnection.of(index, debug, sql, func, error, _finally);
        invokeQueue.add(() -> connection.invoke(this));
    }
    public void callMySQL(int index, ExecuteData debug, SelectSQL sql, ConnectionInvoke func, Action1<Throwable> error, Action0 _finally) {
        callMySQL(index, debug, sql, func.toData(), error, _finally);
    }

    public static String calendarToString(Calendar calendar) {
        return String.valueOf(calendar.get(Calendar.YEAR)) +
                '-' +
                (calendar.get(Calendar.MONTH) + 1) +
                '-' +
                calendar.get(Calendar.DAY_OF_MONTH) +
                ' ' +
                calendar.get(Calendar.HOUR_OF_DAY) +
                ':' +
                calendar.get(Calendar.MINUTE) +
                ':' +
                calendar.get(Calendar.SECOND);
    }

    private static String fromCalendar(Calendar calendar) {
        return "'" + calendarToString(calendar) + "'";
    }
    private static Stream<Object> toList(Array array) {
        int length = Array.getLength(array);
        Object[] _array = new Object[length];
        for (int i = 0; i < length; i++) _array[i] = Array.get(array, i);
        return Arrays.stream(_array);
    }
    public static String toSqlObject(Object value) {
        if (value == null) return "NULL";
        else if (value instanceof Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return fromCalendar(calendar);
        } else if (value instanceof Calendar calendar) return fromCalendar(calendar);
        else if (value instanceof Number number) return number.toString();
        else if (value instanceof UUID uuid) return "'"+uuid+"'";
        else if (value instanceof Boolean bool) return bool ? "1" : "0";
        else if (value instanceof Iterable<?> iterable) return toSqlObject(Streams.stream(iterable));
        else if (value instanceof Iterator<?> iterator) return toSqlObject(Streams.stream(iterator));
        else if (value instanceof Array array) return toSqlObject(toList(array));
        else if (value instanceof String str) return "'" + str.replace("\\", "\\\\").replace("'", "\\'") + "'";
        else if (value instanceof JsonPrimitive primitive) {
            if (primitive.isNumber()) return toSqlObject(primitive.getAsNumber());
            else if (primitive.isBoolean()) return toSqlObject(primitive.getAsBoolean());
            else return toSqlObject(primitive.getAsString());
        }
        else if (value instanceof Stream<?> stream) {
            String str = stream.map(MySql::toSqlObject).collect(Collectors.joining(","));
            return "(" + (str.isEmpty() ? "NULL" : str) + ")";
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
                if (value instanceof Calendar) return Time.formatCalendar((Calendar)value, false);
                return value.toString();
        }
    }
    public static String fromSqlObjectString(Object value) {
        if (value == null) return "null";
        if (value instanceof Calendar) return Time.formatCalendar((Calendar)value, false);
        return value.toString();
    }
    static PreparedStatement prepareStatement(int state, ExecuteData debug, Connection connection, Toast1<String> sql, Map<String, Object> args) throws SQLException {
        if (args != null) {
            for (Map.Entry<String, Object> arg : args.entrySet()) sql.val0 = sql.val0.replace("@" + arg.getKey(), toSqlObject(arg.getValue()));
        }
        if (debug != null) debug.onSQL(sql.val0);
        if (state == 1) {
            Component component = Component
                    .text("[" + Time.formatCalendar(Time.moscowNow(), true) + "] SQL QUERY")
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

    public static map.builder<String, Object> args() {
        return map.of();
    }

    /*
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
    public static <T>Optional<T> readObjectOptional(ResultSet set, String column, Class<T> tClass) {
        try {
            ResultSetMetaData meta = set.getMetaData();
            int columns = meta.getColumnCount();
            for (int x = 1; x <= columns; x++)
                if (column.equals(meta.getColumnName(x)))
                    return Optional.ofNullable(set.getObject(x, tClass));
            return Optional.empty();
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
    */
    public static int columnsCount(MySqlRow set) {
        return set.columnsCount();
    }
    public static Object readObject(MySqlRow set, int index) {
        return set.readObject(index);
    }
    public static <T>T readObject(MySqlRow set, int index, Class<T> tClass) {
        return set.readObject(index, tClass);
    }
    public static Object readObject(MySqlRow set, String column) {
        return set.readObject(column);
    }
    public static <T>T readObject(MySqlRow set, String column, Class<T> tClass) {
        return set.readObject(column, tClass);
    }
    public static <T>Optional<T> readObjectOptional(MySqlRow set, String column, Class<T> tClass) {
        return set.readObjectOptional(column, tClass);
    }
    public static boolean hasColumn(MySqlRow set, String column) {
        return set.hasColumn(column);
    }
    public static Object readEmpty(MySqlRow set) {
        return new Object();
    }

    public MySqlAsync Async = new MySqlAsync(this);

    static final ConcurrentHashMap<Integer, Toast2<String, String>> calls = new ConcurrentHashMap<>();
}





















