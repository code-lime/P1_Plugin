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

import java.util.*;

public class GraphInput extends BaseNode {
    private final Map<String, ActionOutput> outputActions;
    private final Map<String, ObjectDefaultOutput> outputValues;

    private static class ObjectDefaultOutput extends ObjectOutput {
        public Object defValue;
        public ObjectDefaultOutput(String key, List<system.Toast2<Integer, String>> target, Object defValue) {
            super(key, target);
            this.defValue = defValue;
        }
    }

    public GraphInput(int id, JsonObject json, NodeCreator.NodeGroup group) {
        super(id, json, system.map.<String, system.Func2<String, Optional<JsonElement>, IInput>>of()
                .build(), system.map.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>of()
                .add(group.controlInput.stream()
                        .map(v -> system.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>toast(v, ActionOutput::new))
                        .iterator()
                )
                .add(group.valueInput.entrySet().stream()
                        .map(kv -> system.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>toast(kv.getKey(), (key, list) -> new ObjectDefaultOutput(key, list, kv.getValue())))
                        .iterator()
                )
                .build());

        outputActions = new HashMap<>();
        group.controlInput.forEach(key -> outputActions.put(key, (ActionOutput) this.output.get(key)));

        outputValues = new HashMap<>();
        group.valueInput.keySet().forEach(key -> outputValues.put(key, (ObjectDefaultOutput) this.output.get(key)));
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {}

    public void invokeGraphNode(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable) {
        invokeGraphNode(player, nodes, variable, null);
    }
    public void invokeGraphNode(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, system.Action1<Map<String, Object>> graphOutput) {
        Map<Integer, Map<String, Object>> data = new HashMap<>();
        if (graphOutput != null) {
            Map<String, Object> _data = new HashMap<>();
            _data.put("graph_output", graphOutput);
            data.put(-1, _data);
        }
        outputValues.forEach((key, outputValue) -> outputValue.setNext(data, variable.getOrDefault(key, outputValue.defValue)));
        outputActions.forEach((key, outputAction) -> {
            if (!Boolean.TRUE.equals(variable.get(key))) return;
            outputAction.setNext(data, true);
            outputAction.executeNext(player, nodes, data);
        });
    }
}























