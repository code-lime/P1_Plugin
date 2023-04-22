package org.lime.gp.database.rows;

import java.sql.ResultSet;
import java.util.Optional;

import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;

import net.kyori.adventure.text.format.TextColor;

public class RolesRow extends BaseRow {
    public int id;
    public TextColor color;
    public String name;
    public int permissions;
    public int groupID;
    public boolean isStatic;

    public RolesRow(ResultSet set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        color = TextColor.fromHexString("#" + MySql.readObject(set, "color", String.class));
        name = MySql.readObject(set, "name", String.class);
        permissions = MySql.readObject(set, "permissions", Integer.class);
        groupID = MySql.readObject(set, "id_group", Integer.class);
        isStatic = MySql.readObject(set, "static", Integer.class) > 0;
    }

    public static Optional<RolesRow> getBy(int role) { return Tables.ROLES_TABLE.get(role + ""); }
}