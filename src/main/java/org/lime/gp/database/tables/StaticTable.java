package org.lime.gp.database.tables;

import java.util.List;

import org.lime.system;
import org.lime.gp.database.rows.BaseRow;

import com.google.common.collect.ImmutableList;

public class StaticTable<V extends BaseRow> extends ITable<V> {
    private final ImmutableList<V> data;
    public StaticTable(List<V> data) {this.data = ImmutableList.copyOf(data); }
    @Override public List<V> getRows() { return data; }
    @Override public void forEach(system.Action1<V> callback) { data.forEach(callback); }
}