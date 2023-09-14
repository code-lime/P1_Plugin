package org.lime.gp.player.menu.node.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.input.ObjectInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.gp.player.menu.node.connect.output.StringOutput;
import org.lime.json.JsonElementOptional;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class ToStringNode extends BaseNode {
    private final ActionInput inputAction;
    private final List<ObjectInput> inputValues;

    private final ActionOutput outputAction;
    private final List<StringOutput> outputTexts;

    private final int count;

    public ToStringNode(int id, int count, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .add(IntStream.range(0, count)
                        .map(v -> v + 1)
                        .mapToObj(i -> Toast.<String, Func2<String, Optional<JsonElement>, IInput>>of("value_" + i, (key, def) -> new ObjectInput(key, def.map(JsonElementOptional::of).flatMap(JsonElementOptional::getAsObject).orElse(null))))
                        .iterator())
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .add(IntStream.range(0, count)
                        .map(v -> v + 1)
                        .mapToObj(i -> Toast.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of("text_" + i, StringOutput::new))
                        .iterator())
                .build());
        this.count = count;

        inputAction = (ActionInput)this.input.get("input");
        inputValues = new ArrayList<>();
        for (int i = 0; i < count; i++) inputValues.add((ObjectInput) this.input.get("value_" + (i + 1)));

        outputAction = (ActionOutput)this.output.get("output");
        outputTexts = new ArrayList<>();
        for (int i = 0; i < count; i++) outputTexts.add((StringOutput) this.output.get("text_" + (i + 1)));
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        for (int i = 0; i < count; i++) outputTexts.get(i).setNext(data, inputExecute.get(inputValues.get(i)) + "");
        outputAction.setNext(data, true);
        outputAction.executeNext(player, nodes, data);
    }
}



