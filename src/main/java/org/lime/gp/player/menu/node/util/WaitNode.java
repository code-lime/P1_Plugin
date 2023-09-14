package org.lime.gp.player.menu.node.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.input.IntInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.json.JsonElementOptional;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WaitNode extends BaseNode {
    private final ActionInput inputAction;
    private final IntInput inputTicks;

    private final ActionOutput outputAction;

    public WaitNode(int id, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .add("ticks", (key, def) -> new IntInput(key, def.map(JsonElementOptional::of).flatMap(JsonElementOptional::getAsInt).orElse(0)))
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .build());
        inputAction = (ActionInput)this.input.get("input");
        inputTicks = (IntInput)this.input.get("ticks");

        outputAction = (ActionOutput)this.output.get("output");
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        int ticks = (int)inputExecute.get(inputTicks);
        if (ticks == 0) {
            outputAction.setNext(data, true);
            outputAction.executeNext(player, nodes, data);
            return;
        }
        lime.onceTicks(() -> {
            outputAction.setNext(data, true);
            outputAction.executeNext(player, nodes, data);
        }, ticks);
    }
}



