package org.lime.gp.block.component.list;

import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;

import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;

@InfoComponent.Component(name = "remote_execute")
public final class RemoteExecuteComponent extends ComponentStatic<JsonArray> {
    public final List<String> execute;
    public RemoteExecuteComponent(BlockInfo creator, JsonArray array) {
        super(creator);
        execute = new ArrayList<>();
        array.forEach(item -> execute.add(item.getAsString()));
    }
}
