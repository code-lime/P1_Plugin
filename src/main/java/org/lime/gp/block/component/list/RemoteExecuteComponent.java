package org.lime.gp.block.component.list;

import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;

import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.docs.IDocsLink;

@InfoComponent.Component(name = "remote_execute")
public final class RemoteExecuteComponent extends ComponentStatic<JsonArray> {
    public final List<String> execute;
    public RemoteExecuteComponent(BlockInfo creator, JsonArray array) {
        super(creator);
        execute = new ArrayList<>();
        array.forEach(item -> execute.add(item.getAsString()));
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
