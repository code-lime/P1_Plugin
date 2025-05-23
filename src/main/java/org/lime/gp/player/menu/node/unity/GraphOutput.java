package org.lime.gp.player.menu.node.unity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GraphOutput extends BaseNode {
    public GraphOutput(int id, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .build());
    }

    
    @SuppressWarnings("unchecked")
    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        Map<String, Object> _data = data.get(-1);
        if (_data == null) return;
        Object _graphOutput = _data.get("graph_output");
        if (_graphOutput == null) return;
        Action1<Map<String, Object>> graphOutput = (Action1<Map<String, Object>>)_graphOutput;
        graphOutput.invoke(variable);
    }
}























