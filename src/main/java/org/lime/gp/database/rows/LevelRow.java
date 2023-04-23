package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;

public class LevelRow extends BaseRow {
    public final int id;

    public final int user_id;
    public final int work;

    public final int level;
    public final double exp;

    public LevelRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);

        user_id = MySql.readObject(set, "user_id", Integer.class);
        work = MySql.readObject(set, "work", Integer.class);

        level = MySql.readObject(set, "level", Integer.class);
        exp = MySql.readObject(set, "exp", Double.class);
    }

    public static Optional<LevelRow> getBy(UUID uuid, int work) {
        return UserRow.getBy(uuid).flatMap(v -> getBy(v.id, work));
    }
    public static Optional<LevelRow> getActiveBy(UUID uuid) {
        return UserRow.getBy(uuid).flatMap(v -> getBy(v.id, v.work));
    }
    public static Optional<LevelRow> getBy(int user_id, int work) {
        return Tables.LEVEL_TABLE.getOther("work", user_id + "^" + work);
    }
}