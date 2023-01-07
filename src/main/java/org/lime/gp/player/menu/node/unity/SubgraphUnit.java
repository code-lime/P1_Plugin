package org.lime.gp.player.menu.node.unity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.core.util.Assert;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.gp.player.menu.NodeCreator;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.input.IntInput;
import org.lime.gp.player.menu.node.connect.input.ObjectInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.gp.player.menu.node.connect.output.ObjectOutput;
import org.lime.json.JsonElementOptional;
import org.lime.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubgraphUnit extends BaseNode {
    private final Map<String, ActionInput> inputActions;
    private final Map<String, ObjectInput> inputValues;
    private final Map<String, ActionOutput> outputActions;
    private final Map<String, ObjectOutput> outputValues;
    private final NodeCreator.NodeGroup target;

    public SubgraphUnit(int id, JsonObject json) {
        this(id, json, Assert.requireNonEmpty(NodeCreator.nodeGroups.get(json.get("target").getAsString()), "target is empty"));
    }
    private SubgraphUnit(int id, JsonObject json, NodeCreator.NodeGroup target) {
        super(id, json, system.map.<String, system.Func2<String, Optional<JsonElement>, IInput>>of()
                .add(target.controlInput.stream()
                        .map(v -> system.<String, system.Func2<String, Optional<JsonElement>, IInput>>toast(v, (key, def) -> new ActionInput(key)))
                        .iterator()
                )
                .add(target.valueInput.keySet().stream()
                        .map(o -> system.<String, system.Func2<String, Optional<JsonElement>, IInput>>toast(o, (key, def) -> new ObjectInput(key, def.map(JsonElementOptional::of).flatMap(JsonElementOptional::getAsObject).orElse(null))))
                        .iterator()
                )
                .build(), system.map.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>of()
                .add(target.controlOutput.stream()
                        .map(v -> system.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>toast(v, ActionOutput::new))
                        .iterator()
                )
                .add(target.valueOutput.stream()
                        .map(v -> system.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>toast(v, ObjectOutput::new))
                        .iterator()
                )
                .build());
        this.target = target;

        inputActions = new HashMap<>();
        target.controlInput.forEach(key -> inputActions.put(key, (ActionInput) this.input.get(key)));
        inputValues = new HashMap<>();
        target.valueInput.keySet().forEach(key -> inputValues.put(key, (ObjectInput) this.input.get(key)));

        outputActions = new HashMap<>();
        target.controlOutput.forEach(key -> outputActions.put(key, (ActionOutput) this.output.get(key)));
        outputValues = new HashMap<>();
        target.valueOutput.forEach(key -> outputValues.put(key, (ObjectOutput) this.output.get(key)));
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        boolean anyExecute = false;
        for (ActionInput inputAction : inputActions.values()) {
            if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) continue;
            anyExecute = true;
            break;
        }
        if (!anyExecute) return;
        target.inputPoint.forEach(point -> point.invokeGraphNode(player, target.nodeList, variable, output -> {
            outputValues.forEach((key, value) -> value.setNext(data, output.get(key)));
            outputActions.forEach((key, value) -> {
                if (!Boolean.TRUE.equals(output.containsKey(key))) return;
                value.setNext(data, true);
                value.executeNext(player, nodes, data);
            });
        }));
    }
}


