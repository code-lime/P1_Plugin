package org.lime.gp.player.menu.node.global;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RunNode extends BaseNode {
    private final ActionOutput outputAction;
    public final String key;

    public RunNode(int id, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .build());
        key = json.getAsJsonObject("string").get("key").getAsString();
        outputAction = (ActionOutput) this.output.get("output");
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        outputAction.setNext(data, true);
        outputAction.executeNext(player, nodes, data);
    }
}
