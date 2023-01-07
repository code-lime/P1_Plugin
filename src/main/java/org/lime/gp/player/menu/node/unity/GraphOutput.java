package org.lime.gp.player.menu.node.unity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.player.menu.NodeCreator;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.gp.player.menu.node.connect.output.ObjectOutput;
import org.lime.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GraphOutput extends BaseNode {
    public GraphOutput(int id, JsonObject json) {
        super(id, json, system.map.<String, system.Func2<String, Optional<JsonElement>, IInput>>of()
                .build(), system.map.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>of()
                .build());
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        Map<String, Object> _data = data.get(-1);
        if (_data == null) return;
        Object _graphOutput = _data.get("graph_output");
        if (_graphOutput == null) return;
        system.Action1<Map<String, Object>> graphOutput = (system.Action1<Map<String, Object>>)_graphOutput;
        graphOutput.invoke(variable);
    }
}























