package org.lime.gp.player.menu.node.global;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.player.menu.NodeCreator;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.system;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OtherNode extends BaseNode {
    private final ActionInput inputAction;
    public final String key;

    public OtherNode(int id, JsonObject json) {
        super(id, json, system.map.<String, system.Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .build(), system.map.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>of()
                .build());
        key = json.getAsJsonObject("string").get("key").getAsString();
        inputAction = (ActionInput) this.input.get("output");
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        NodeCreator.runNode(player, key);
    }
}
