package org.lime.gp.database.rows;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.gp.database.tables.Tables;

import java.util.Optional;

public class GroupRow extends BaseRow {
    public final int id;
    public final Optional<Integer> city;

    public GroupRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        city = MySql.readObjectOptional(set, "id_city", Integer.class);
    }

    public static Optional<GroupRow> getBy(int group) { return Tables.GROUPS_TABLE.get(String.valueOf(group)); }
}