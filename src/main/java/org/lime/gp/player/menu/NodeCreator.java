package org.lime.gp.player.menu;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.global.RunNode;
import org.lime.gp.player.menu.node.unity.GraphInput;
import org.lime.json.JsonArrayOptional;
import org.lime.json.JsonElementOptional;
import org.lime.system;

import java.util.*;

public class NodeCreator {
    public static core.element create() {
        return core.element.create(NodeCreator.class)
                .withInit(NodeCreator::init)
                .<JsonObject>addConfig("node", v -> v
                        .withInvoke(NodeCreator::config)
                        .withDefault(new JsonObject())
                );
    }
    public static void init() {
        AnyEvent.addEvent("open.node", AnyEvent.type.owner, builder -> builder.createParam(v -> v, () -> nodeRunList.keySet().stream().map(v -> v + "").toList()), NodeCreator::runNode);
    }
    public static void config(JsonObject json) {
        nodeGroups.values().forEach(v -> v.nodeList.values().forEach(BaseNode::delete));
        nodeGroups.clear();

        json.entrySet().forEach(kv -> nodeGroups.put(kv.getKey(), new NodeGroup(kv.getValue().getAsJsonObject())));
        nodeGroups.values().forEach(NodeGroup::loadNodes);
        nodeGroups.values().forEach(group -> group.nodeList.values().forEach(node -> {
            if (node instanceof RunNode runNode)
                nodeRunList.computeIfAbsent(runNode.key, k -> new ArrayList<>()).add(system.toast(group, runNode));
        }));
    }
    public static HashMap<String, NodeGroup> nodeGroups = new HashMap<>();
    public static HashMap<String, List<system.Toast2<NodeGroup, RunNode>>> nodeRunList = new HashMap<>();

    public static class NodeGroup {
        public final HashMap<Integer, BaseNode> nodeList = new HashMap<>();
        public final List<GraphInput> inputPoint = new ArrayList<>();
        public final List<String> controlInput = new ArrayList<>();
        public final List<String> controlOutput = new ArrayList<>();
        public final Map<String, Object> valueInput = new HashMap<>();
        public final List<String> valueOutput = new ArrayList<>();

        private final JsonObject json;

        public NodeGroup(JsonObject json) {
            this.json = json;
            JsonElementOptional.of(json.remove("definition")).getAsJsonObject().ifPresent(definition -> {
                definition.getAsJsonArray("ControlInput")
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(v -> v.getAsJsonObject().flatMap(_v -> _v.getAsString("key")).stream())
                        .forEach(controlInput::add);

                definition.getAsJsonArray("ControlOutput")
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(v -> v.getAsJsonObject().flatMap(_v -> _v.getAsString("key")).stream())
                        .forEach(controlOutput::add);

                definition.getAsJsonArray("ValueInput")
                        .stream()
                        .flatMap(Collection::stream)
                        .forEach(v -> v.getAsJsonObject().ifPresent(_v -> valueInput.put(_v.getAsString("key").orElseThrow(), _v.getAsBoolean("has")
                                .filter(__v -> __v)
                                .flatMap(__v -> _v.get("value"))
                                .flatMap(JsonElementOptional::getAsObject)
                                .orElse(null)
                        )));

                definition.getAsJsonArray("ValueOutput")
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(v -> v.getAsJsonObject().flatMap(_v -> _v.getAsString("key")).stream())
                        .forEach(valueOutput::add);
            });
        }

        public void loadNodes() {
            json.entrySet().forEach(kv -> {
                int id = Integer.parseInt(kv.getKey());
                BaseNode node = BaseNode.parse(id, kv.getValue().getAsJsonObject(), this);
                nodeList.put(id, node);
                if (node instanceof GraphInput input) inputPoint.add(input);
            });
        }
    }

    public static void runNode(Player player, String node_key) {
        nodeRunList.get(node_key).forEach(kv -> kv.invoke((group, node) -> node.invokeNode(player, group.nodeList, new HashMap<>())));
    }
}


















