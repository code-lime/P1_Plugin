package org.lime.gp.player.menu.node.execute;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.input.StringInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChatMessageNode extends BaseNode {
    private final ActionInput inputAction;
    private final StringInput inputText;

    private final ActionOutput outputAction;

    public ChatMessageNode(int id, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .add("text", (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("")))
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .build());
        inputAction = (ActionInput)this.input.get("input");
        inputText = (StringInput)this.input.get("text");

        outputAction = (ActionOutput)this.output.get("output");
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        String message = inputExecute.get(inputText) + "";
        player.sendMessage(ChatHelper.formatComponent(message));
        outputAction.setNext(data, true);
        outputAction.executeNext(player, nodes, data);
    }
}













