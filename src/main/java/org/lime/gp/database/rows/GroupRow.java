package org.lime.gp.database.rows;

import net.kyori.adventure.text.format.TextColor;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;

import java.sql.ResultSet;
import java.util.Optional;

public class GroupRow extends BaseRow {
    public final int id;
    public final Optional<Integer> city;

    public GroupRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        city = MySql.readObjectOptional(set, "id_city", Integer.class);
    }

    public static Optional<GroupRow> getBy(int group) { return Tables.GROUPS_TABLE.get(group + ""); }
}