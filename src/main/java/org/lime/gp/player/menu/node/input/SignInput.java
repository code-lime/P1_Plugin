package org.lime.gp.player.menu.node.input;

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
import org.lime.gp.player.ui.EditorUI;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;

public class SignInput extends BaseNode {
    private final ActionInput inputAction;
    private final StringInput[] inputText;

    private final ActionOutput outputAction;
    private final StringOutput[] outputText;

    public SignInput(int id, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .add("line_0", (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("")))
                .add("line_1", (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("")))
                .add("line_2", (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("")))
                .add("line_3", (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("")))
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .add("line_0", StringOutput::new)
                .add("line_1", StringOutput::new)
                .add("line_2", StringOutput::new)
                .add("line_3", StringOutput::new)
                .build());
        inputAction = (ActionInput)this.input.get("input");
        inputText = new StringInput[] {
                (StringInput)this.input.get("line_0"),
                (StringInput)this.input.get("line_1"),
                (StringInput)this.input.get("line_2"),
                (StringInput)this.input.get("line_3")
        };

        outputAction = (ActionOutput)this.output.get("output");
        outputText = new StringOutput[] {
                (StringOutput)this.output.get("line_0"),
                (StringOutput)this.output.get("line_1"),
                (StringOutput)this.output.get("line_2"),
                (StringOutput)this.output.get("line_3")
        };
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        List<String> lines = new ArrayList<>();
        for (StringInput input : inputText) lines.add(Optional.ofNullable(inputExecute.get(input)).map(v -> v + "").orElse(""));
        EditorUI.openSign(player, lines, _lines -> {
            for (int i = 0; i < 4; i++) outputText[i].setNext(data, _lines.get(i));
            outputAction.setNext(data, true);
            outputAction.executeNext(player, nodes, data);
        });
    }
}













