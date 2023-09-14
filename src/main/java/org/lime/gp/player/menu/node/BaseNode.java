package org.lime.gp.player.menu.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.menu.NodeCreator;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.dynamic.JavaScriptNode;
import org.lime.gp.player.menu.node.execute.ChatMessageNode;
import org.lime.gp.player.menu.node.execute.PlayerInfoNode;
import org.lime.gp.player.menu.node.execute.SQLNode;
import org.lime.gp.player.menu.node.global.OtherNode;
import org.lime.gp.player.menu.node.global.RunNode;
import org.lime.gp.player.menu.node.input.SignInput;
import org.lime.gp.player.menu.node.unity.GraphInput;
import org.lime.gp.player.menu.node.unity.GraphOutput;
import org.lime.gp.player.menu.node.unity.SubgraphUnit;
import org.lime.gp.player.menu.node.util.ParseIntNode;
import org.lime.gp.player.menu.node.util.string.ReplaceNode;
import org.lime.gp.player.menu.node.util.ToStringNode;
import org.lime.gp.player.menu.node.util.WaitNode;
import org.lime.gp.player.menu.node.util.logical.LogicalNode;
import org.lime.gp.player.menu.node.util.logical.SwitchNode;
import org.lime.gp.player.menu.node.util.parallel.ParallelNode;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.delete.DeleteHandle;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;

public abstract class BaseNode implements Logged.ILoggedDelete {
    private final int id;
    @SuppressWarnings("unused")
    private final String type;

    public final Map<String, IInput> input = new HashMap<>();
    public final Map<String, IOutput> output = new HashMap<>();

    private final DeleteHandle deleteHandle = new DeleteHandle();

    public BaseNode(int id, JsonObject json, Map<String, Func2<String, Optional<JsonElement>, IInput>> input, Map<String, Func2<String, List<Toast2<Integer, String>>, IOutput>> output) {
        this.type = json.get("type").getAsString();
        this.id = id;

        JsonObjectOptional _json = JsonObjectOptional.of(json);

        input.forEach((key, value) -> this.input.put(key, value.invoke(key, _json.getAsJsonObject("input")
                .flatMap(v -> v.get(key))
                .map(JsonElementOptional::base)
        )));
        output.forEach((key, value) -> this.output.put(key, value.invoke(key, _json.getAsJsonObject("output")
                .flatMap(v -> v.get(key))
                .flatMap(JsonElementOptional::getAsJsonArray)
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsString)
                .flatMap(Optional::stream)
                .map(_v -> _v.split("#", 2))
                .map(_v -> Toast.of(Integer.parseInt(_v[0]), _v[1]))
                .toList()
        )));
    }

    public String getKey() { return "node#" + id; }
    @Override public String getLoggedKey() { return "node#" + id; }
    @Override public boolean isLogged() { return true; }

    @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
    @Override public void delete() { deleteHandle.delete(); }

    public boolean invokeNode(Player player, Map<Integer, BaseNode> nodes, Map<Integer, Map<String, Object>> data) {
        if (isDeleted()) return false;
        Map<IInput, Object> inputExecute = new HashMap<>();
        Map<String, Object> variable = data.getOrDefault(id, Collections.emptyMap());
        input.forEach((key, value) -> inputExecute.put(value, value.orDefault(variable.get(key))));
        data.remove(id);
        invokeNodeGenerate(player, nodes, variable, data, inputExecute);
        return true;
    }

    protected abstract void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute);

    public static BaseNode parse(int id, JsonObject json, NodeCreator.NodeGroup group) {
        if (!json.has("type")) throw new IllegalArgumentException("ERROR PARSE '" + id + "' IN NODE");
        String type = json.get("type").getAsString();
        if (type.startsWith("Application.")) type = type.substring("Application.".length()).replace('.', '/');
        if (type.startsWith("Dynamic/JavaScript/")) return new JavaScriptNode(id, json);

        return switch (type) {
            case "Util/ParseInt" -> new ParseIntNode(id, json);
            case "Util/ToString" -> new ToStringNode(id, json.getAsJsonObject("dynamic").get("count").getAsInt(), json);
            case "Util/Wait" -> new WaitNode(id, json);

            case "Util/Parallel/Parallel" -> new ParallelNode(id, json.getAsJsonObject("dynamic").get("count").getAsInt(), json);
            case "Util/Logical/Logical" -> new LogicalNode(id, json.getAsJsonObject("dynamic").get("count").getAsInt(), json);
            case "Util/Logical/Switch" -> new SwitchNode(id, json.getAsJsonObject("dynamic").get("count").getAsInt(), json);
            case "Util/String/Replace" -> new ReplaceNode(id, json.getAsJsonObject("dynamic").get("count").getAsInt(), json);

            case "Execute/ChatMessage" -> new ChatMessageNode(id, json);
            case "Execute/SQL" -> new SQLNode(id, json.getAsJsonObject("dynamic").get("count").getAsInt(), json);
            case "Execute/PlayerInfo" -> new PlayerInfoNode(id, json);

            case "Input/SignInput" -> new SignInput(id, json);

            case "Global/Run" -> new RunNode(id, json);
            case "Global/Other" -> new OtherNode(id, json);

            case "Unity.VisualScripting.GraphInput" -> new GraphInput(id, json, group);
            case "Unity.VisualScripting.GraphOutput" -> new GraphOutput(id, json);
            case "Unity.VisualScripting.SubgraphUnit" -> new SubgraphUnit(id, json);
            default -> throw new IllegalArgumentException("ERROR PARSE '" + id + "' IN NODE. TYPE '" + type + "' NOT FOUNDED");
        };
    }
}














