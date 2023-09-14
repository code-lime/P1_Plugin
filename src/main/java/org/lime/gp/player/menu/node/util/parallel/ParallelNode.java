package org.lime.gp.player.menu.node.util.parallel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class ParallelNode extends BaseNode {
    private final ActionInput inputAction;

    private final List<ActionOutput> outputActions;

    public ParallelNode(int id, int count, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .add(IntStream.range(0, count)
                        .map(v -> v + 1)
                        .mapToObj(i -> Toast.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of("output_" + i, ActionOutput::new))
                        .iterator())
                .build());
        inputAction = (ActionInput) this.input.get("input");
        outputActions = new ArrayList<>();
        for (int i = 0; i < count; i++) outputActions.add((ActionOutput) this.output.get("output_" + (i + 1)));
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        outputActions.forEach(action -> {
            action.setNext(data, true);
            action.executeNext(player, nodes, data);
        });
    }
}

















