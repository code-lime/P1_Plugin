package org.lime.gp.entity.component.list;

import com.google.gson.JsonArray;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentStatic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.player.ui.respurcepack.IRemoteExecute;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@InfoComponent.Component(name = "remote_execute")
public final class RemoteExecuteComponent extends ComponentStatic<JsonArray> implements IRemoteExecute {
    public final List<String> execute;
    public RemoteExecuteComponent(EntityInfo creator, JsonArray array) {
        super(creator);
        execute = new ArrayList<>();
        array.forEach(item -> execute.add(item.getAsString()));
    }

    @Override public Stream<String> executeLines() {
        return execute.stream();
    }
}
