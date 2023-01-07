package org.lime.gp.player.menu.node.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.input.StringInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.gp.player.menu.node.connect.output.IntOutput;
import org.lime.system;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParseIntNode extends BaseNode {
    private final ActionInput inputAction;
    private final StringInput inputText;

    private final ActionOutput outputAction;
    private final ActionOutput outputError;
    private final IntOutput outputNum;

    public ParseIntNode(int id, JsonObject json) {
        super(id, json, system.map.<String, system.Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .add("text", (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("")))
                .build(), system.map.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .add("error", ActionOutput::new)
                .add("num", IntOutput::new)
                .build());
        inputAction = (ActionInput)this.input.get("input");
        inputText = (StringInput)this.input.get("text");

        outputAction = (ActionOutput)this.output.get("output");
        outputError = (ActionOutput)this.output.get("error");
        outputNum = (IntOutput)this.output.get("num");
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        Optional.of(inputExecute.get(inputText) + "")
                .flatMap(ExtMethods::parseInt)
                .ifPresentOrElse(value -> {
                    outputNum.setNext(data, value);
                    outputAction.setNext(data, true);
                    outputAction.executeNext(player, nodes, data);
                }, () -> {
                    outputError.setNext(data, true);
                    outputError.executeNext(player, nodes, data);
                });
    }
}

























