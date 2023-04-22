package org.lime.gp.player.menu.node.execute;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.gp.player.menu.node.connect.output.IntOutput;
import org.lime.gp.player.menu.node.connect.output.StringOutput;
import org.lime.system;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PlayerInfoNode extends BaseNode {
    private final ActionInput inputAction;

    private final ActionOutput outputAction;
    private final IntOutput outputID;
    private final StringOutput outputUUID;
    private final StringOutput outputUserName;

    public PlayerInfoNode(int id, JsonObject json) {
        super(id, json, system.map.<String, system.Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .build(), system.map.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .add("id", IntOutput::new)
                .add("uuid", StringOutput::new)
                .add("user_name", StringOutput::new)
                .build());
        inputAction = (ActionInput)this.input.get("input");

        outputAction = (ActionOutput)this.output.get("output");
        outputID = (IntOutput)this.output.get("id");
        outputUUID = (StringOutput)this.output.get("uuid");
        outputUserName = (StringOutput)this.output.get("user_name");
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        int id = UserRow.getBy(player).map(v -> v.id).orElse(-1);

        outputID.setNext(data, id);
        outputUUID.setNext(data, player.getUniqueId().toString());
        outputUserName.setNext(data, player.getName());

        outputAction.setNext(data, true);
        outputAction.executeNext(player, nodes, data);
    }
}













