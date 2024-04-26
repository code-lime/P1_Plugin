package org.lime.gp.database.rows;

import net.kyori.adventure.text.format.TextColor;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.gp.database.tables.Tables;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RolesRow extends BaseRow {
    public int id;
    public TextColor color;
    public String name;
    public List<Integer> permissions;
    public int groupID;
    public boolean isStatic;

    public RolesRow(MySqlRow set) {
        super(set);
        id = MySql.readObject(set, "id", Integer.class);
        color = TextColor.fromHexString("#" + MySql.readObject(set, "color", String.class));
        name = MySql.readObject(set, "name", String.class);
        permissions = Arrays.stream(MySql.readObject(set, "permissions", String.class).split(","))
                .filter(v -> !v.isBlank())
                .map(Integer::parseInt)
                .toList();
        groupID = MySql.readObject(set, "id_group", Integer.class);
        isStatic = MySql.readObject(set, "static", Integer.class) > 0;
    }

    public static Optional<RolesRow> getBy(int role) { return Tables.ROLES_TABLE.get(role + ""); }
}