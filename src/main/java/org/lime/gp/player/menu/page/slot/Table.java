package org.lime.gp.player.menu.page.slot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.rows.BaseRow;
import org.lime.gp.database.tables.ITable;
import org.lime.gp.player.menu.Logged;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Table implements Logged.ILoggedDelete {
    public List<Integer> slots = new ArrayList<>();
    public List<ISlot> adds = new ArrayList<>();
    public ISlot format;

    private final Logged.ILoggedDelete base;
    private final Logged.ChildLoggedDeleteHandle deleteHandle;

    @Override public String getLoggedKey() { return base.getLoggedKey(); }
    @Override public boolean isLogged() { return base.isLogged(); }
    @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
    @Override public void delete() { deleteHandle.delete(); }

    protected Table(Logged.ILoggedDelete base) {
        this.base = base;
        deleteHandle = new Logged.ChildLoggedDeleteHandle(base);
    }

    public static Table parse(Logged.ILoggedDelete base, JsonObject json) {
        Table table = new Table(base);
        json.get("slots").getAsJsonArray().forEach(kv -> {
            if (kv.isJsonArray()) {
                JsonArray arr = kv.getAsJsonArray();
                IntStream.range(arr.get(0).getAsInt(), arr.get(1).getAsInt() + 1).forEach(table.slots::add);
            } else {
                table.slots.add(kv.getAsInt());
            }
        });
        table.format = ISlot.parse(table, json.get("format").getAsJsonObject());
        //table.compare = json.has("compare") ? Compare.parse(json.get("compare").getAsJsonObject()) : null;
        if (json.has("adds"))
            json.get("adds").getAsJsonArray().forEach(slot -> table.adds.add(ISlot.parse(table, slot.getAsJsonObject())));
        if (table.slots.size() <= 0) throw new IllegalArgumentException("TABLE.SLOTS SIZE ZERO");
        return table;
    }

    public List<? extends BaseRow> getList(ITable<? extends BaseRow> table, Apply apply) {
        return table.getRows();
    }
}
