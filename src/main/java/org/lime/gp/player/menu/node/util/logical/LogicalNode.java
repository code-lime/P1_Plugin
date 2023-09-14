package org.lime.gp.player.menu.node.util.logical;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.input.StringInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class LogicalNode extends BaseNode {
    private final int count;

    private final ActionInput inputAction;
    private final List<StringInput> inputFunction;

    private final List<ActionOutput> outputActions;

    public LogicalNode(int id, int count, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .add(IntStream.range(0, count)
                        .map(v -> v + 1)
                        .mapToObj(i -> Toast.<String, Func2<String, Optional<JsonElement>, IInput>>of("function_" + i, (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("false"))))
                        .iterator())
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .add(IntStream.range(0, count)
                        .map(v -> v + 1)
                        .mapToObj(i -> Toast.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of("output_" + i, ActionOutput::new))
                        .iterator())
                .build());
        this.count = count;
        inputAction = (ActionInput) this.input.get("input");

        inputFunction = new ArrayList<>();
        for (int i = 0; i < count; i++) inputFunction.add((StringInput) this.input.get("function_" + (i + 1)));
        outputActions = new ArrayList<>();
        for (int i = 0; i < count; i++) outputActions.add((ActionOutput) this.output.get("output_" + (i + 1)));
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        for (int i = 0; i < count; i++) {
            Boolean result = JavaScript.isJsTrue(inputExecute.get(inputFunction.get(i)) + "").orElse(null);
            if (result == null) return;
            if (!result) continue;
            ActionOutput execute = outputActions.get(i);
            execute.setNext(data, true);
            execute.executeNext(player, nodes, data);
            return;
        }
    }
}

























