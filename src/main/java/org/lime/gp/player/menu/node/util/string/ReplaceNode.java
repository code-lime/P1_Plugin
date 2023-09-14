package org.lime.gp.player.menu.node.util.string;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.input.StringInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.gp.player.menu.node.connect.output.StringOutput;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class ReplaceNode extends BaseNode {
    private final int count;

    private final ActionInput inputAction;
    private final StringInput inputModify;
    private final List<StringInput> inputKey;
    private final List<StringInput> inputValue;

    private final ActionOutput outputAction;
    private final StringOutput outputResult;

    public ReplaceNode(int id, int count, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .add("modify", (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("")))
                .add(IntStream.range(0, count)
                        .map(v -> v + 1)
                        .mapToObj(i -> Toast.<String, Func2<String, Optional<JsonElement>, IInput>>of("key_" + i, (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("false"))))
                        .iterator())
                .add(IntStream.range(0, count)
                        .map(v -> v + 1)
                        .mapToObj(i -> Toast.<String, Func2<String, Optional<JsonElement>, IInput>>of("value_" + i, (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("false"))))
                        .iterator())
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .add("result", StringOutput::new)
                .build());
        this.count = count;
        inputAction = (ActionInput) this.input.get("input");
        inputModify = (StringInput) this.input.get("modify");

        inputKey = new ArrayList<>();
        for (int i = 0; i < count; i++) inputKey.add((StringInput) this.input.get("key_" + (i + 1)));
        inputValue = new ArrayList<>();
        for (int i = 0; i < count; i++) inputValue.add((StringInput) this.input.get("key_" + (i + 1)));

        outputAction = (ActionOutput) this.output.get("output");
        outputResult = (StringOutput) this.output.get("result");
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        String modify = String.valueOf(inputExecute.get(inputModify));
        for (int i = 0; i < count; i++) {
            String key = String.valueOf(inputExecute.get(this.input.get("key_" + (i + 1))));
            String value = String.valueOf(inputExecute.get(this.input.get("value_" + (i + 1))));
            modify = modify.replace("{" + key + "}", value);
        }
        outputResult.setNext(data, modify);
        outputAction.setNext(data, true);
        outputAction.executeNext(player, nodes, data);
    }
}

























