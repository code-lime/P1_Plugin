package org.lime.gp.database.mysql;

import com.mysql.cj.MysqlType;

import java.sql.Date;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

public class MySqlRow {
    private final int count;

    public record Field(Integer index, String column, int type, Object value) {
        public <T>T cast(Class<T> tClass) {
            return convertObject(type, value, tClass);
        }
    }

    private final Map<Integer, Field> indexToField = new HashMap<>();
    private final Map<String, Field> columnToField = new HashMap<>();

    public Stream<Field> fields() {
        return indexToField.values().stream();
    }

    /*
                ResultSetMetaData data = set.getMetaData();
                for (int i = 1; i <= data.getColumnCount(); i++)
                    columns.put(data.getColumnLabel(i), MySql.fromSqlObjectString(data.getColumnType(i), set.getObject(i)));*/
    private MySqlRow(int count, Collection<Field> fields) {
        this.count = count;
        fields.forEach(field -> {
            indexToField.put(field.index, field);
            columnToField.put(field.column, field);
        });
    }

    public static MySqlRow export(ResultSet set) throws SQLException {
        var meta = set.getMetaData();
        int count = meta.getColumnCount();
        List<Field> fields = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            fields.add(new Field(i, meta.getColumnLabel(i), meta.getColumnType(i), set.getObject(i)));
        }
        return new MySqlRow(count, fields);
    }

    private static <T>T from(Boolean value, Class<T> tClass) {
        if (tClass == Boolean.class) return tClass.cast(value);
        else if (tClass == Integer.class) return tClass.cast(value ? 1 : 0);
        else if (tClass == Long.class) return tClass.cast(value ? 1 : 0);
        else if (tClass == Float.class) return tClass.cast(value ? 1 : 0);
        else if (tClass == Double.class) return tClass.cast(value ? 1 : 0);
        else if (tClass == String.class) return tClass.cast(value ? "true" : "false");
        else throw new IllegalArgumentException("Not supported type " + tClass + " from type " + value.getClass());
    }
    private static <T>T from(Number value, Class<T> tClass) {
        if (tClass == Boolean.class) return tClass.cast(value.doubleValue() > 0);
        else if (tClass == Integer.class) return tClass.cast(value.intValue());
        else if (tClass == Long.class) return tClass.cast(value.longValue());
        else if (tClass == Float.class) return tClass.cast(value.floatValue());
        else if (tClass == Double.class) return tClass.cast(value.doubleValue());
        else if (tClass == String.class) return tClass.cast(String.valueOf(value));
        else throw new IllegalArgumentException("Not supported type " + tClass + " from type " + value.getClass());
    }
    private static <T>T from(String value, Class<T> tClass) {
        if (tClass == Boolean.class) return tClass.cast(value.equalsIgnoreCase("true"));
        else if (tClass == Integer.class) return tClass.cast(Integer.parseInt(value));
        else if (tClass == Long.class) return tClass.cast(Long.parseLong(value));
        else if (tClass == Float.class) return tClass.cast(Float.parseFloat(value));
        else if (tClass == Double.class) return tClass.cast(Double.parseDouble(value));
        else if (tClass == String.class) return tClass.cast(value);
        else throw new IllegalArgumentException("Not supported type " + tClass + " from type " + value.getClass());
    }
    private static <T>T from(java.util.Date value, Class<T> tClass) {
        if (tClass == Calendar.class) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(value);
            return tClass.cast(calendar);
        }
        else if (tClass == String.class) return tClass.cast(value.toString());
        else throw new IllegalArgumentException("Not supported type " + tClass + " from type " + value.getClass());
    }
    private static <T>T from(LocalDateTime value, Class<T> tClass) {
        return from(Date.from(value.atZone(ZoneId.systemDefault()).toInstant()), tClass);
    }

    private static <T>T convertObject(int type, Object value, Class<T> tClass) {
        if (value == null)
            return null;

        MysqlType mysqlType = MysqlType.getByJdbcType(type);

        return switch (mysqlType) {
            case BIT, BOOLEAN -> from((Boolean) value, tClass);
            case TINYINT, TINYINT_UNSIGNED, SMALLINT, SMALLINT_UNSIGNED, MEDIUMINT, MEDIUMINT_UNSIGNED, INT -> from((Integer) value, tClass);
            case INT_UNSIGNED, BIGINT -> from((Long) value, tClass);
            case FLOAT, FLOAT_UNSIGNED -> from((Float) value, tClass);
            case DOUBLE, DOUBLE_UNSIGNED -> from((Double) value, tClass);
            case CHAR, ENUM, SET, VARCHAR, TINYTEXT, TEXT, MEDIUMTEXT, LONGTEXT, JSON -> from((String) value, tClass);
            case DATE, TIME, TIMESTAMP, DATETIME -> {
                if (value instanceof Date date)
                    yield from(date, tClass);
                else if (value instanceof Timestamp timestamp)
                    yield from(timestamp, tClass);
                else if (value instanceof Time time)
                    yield from(time, tClass);
                else if (value instanceof LocalDateTime localDateTime)
                    yield from(localDateTime, tClass);
                else
                    throw new IllegalArgumentException("Not supported type " + mysqlType + " with value " + value.getClass());
            }
            default -> throw new IllegalArgumentException("Not supported type: " + mysqlType);

            /*case TIME: return from((Time)value, tClass);
            case DATETIME: return from((LocalDateTime)value, tClass);
            case MysqlType.BINARY:
            case MysqlType.VARBINARY:
            case MysqlType.TINYBLOB:
            case MysqlType.MEDIUMBLOB:
            case MysqlType.LONGBLOB:
            case MysqlType.YEAR: return this.yearIsDateType ? getDate(columnIndex) : Short.valueOf(getShort(columnIndex));
            */
        };
    }

    public int columnsCount() {
        return count;
    }

    public boolean hasColumn(int index) {
        return indexToField.containsKey(index);
    }
    public Object readObject(int index) {
        return indexToField.get(index).value;
    }
    public <T>T readObject(int index, Class<T> tClass) {
        return indexToField.get(index).cast(tClass);
    }
    public <T>Optional<T> readObjectOptional(int index, Class<T> tClass) {
        return hasColumn(index)
                ? Optional.ofNullable(readObject(index, tClass))
                : Optional.empty();
    }

    public boolean hasColumn(String column) {
        return columnToField.containsKey(column);
    }
    public Object readObject(String column) {
        return Objects.requireNonNull(columnToField.get(column), () -> "Column not found: " + column + ". Column list: " + String.join(", ", columnToField.keySet())).value;
    }
    public <T>T readObject(String column, Class<T> tClass) {
        return Objects.requireNonNull(columnToField.get(column), () -> "Column not found: " + column + ". Column list: " + String.join(", ", columnToField.keySet())).cast(tClass);
    }
    public <T>Optional<T> readObjectOptional(String column, Class<T> tClass) {
        return hasColumn(column)
                ? Optional.ofNullable(readObject(column, tClass))
                : Optional.empty();
    }
}
