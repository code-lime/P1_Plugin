package org.lime.gp.player.menu.node.execute;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.database.Methods;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.input.StringInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.gp.player.menu.node.connect.output.ObjectOutput;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;
import java.util.stream.IntStream;

public class SQLNode extends BaseNode {
    private final ActionInput inputAction;
    private final StringInput inputQuery;
    private final List<StringInput> inputRows;

    private final ActionOutput outputAction;
    private final ActionOutput outputEmpty;
    private final List<ObjectOutput> outputValues;

    public SQLNode(int id, int count, JsonObject json) {
        super(id, json, map.<String, Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .add("query", (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("")))
                .add(IntStream.range(0, count)
                        .map(v -> v + 1)
                        .mapToObj(i -> Toast.<String, Func2<String, Optional<JsonElement>, IInput>>of("row_" + i, (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse(""))))
                        .iterator())
                .build(), map.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .add("empty", ActionOutput::new)
                .add(IntStream.range(0, count)
                        .map(v -> v + 1)
                        .mapToObj(i -> Toast.<String, Func2<String, List<Toast2<Integer, String>>, IOutput>>of("value_" + i, ObjectOutput::new))
                        .iterator())
                .build());
        inputAction = (ActionInput)this.input.get("input");
        inputQuery = (StringInput)this.input.get("query");

        inputRows = new ArrayList<>();
        for (int i = 0; i < count; i++) inputRows.add((StringInput) this.input.get("row_" + (i + 1)));

        outputAction = (ActionOutput)this.output.get("output");
        outputEmpty = (ActionOutput)this.output.get("empty");

        outputValues = new ArrayList<>();
        for (int i = 0; i < count; i++) outputValues.add((ObjectOutput) this.output.get("value_" + (i + 1)));
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        String query = String.valueOf(inputExecute.get(inputQuery));
        List<String> keys = new ArrayList<>();
        inputRows.forEach(key -> keys.add(String.valueOf(inputExecute.get(key))));
        if (query.toLowerCase().startsWith("select")) {
            Methods.SQL.Async.rawSqlOnce(query, set -> {
                List<Object> list = new ArrayList<>();
                for (String key : keys) {
                    try { list.add(set.readObject(key)); }
                    catch (Exception e) { list.add(null); }
                }
                return list;
            }, list -> {
                if (list == null) {
                    outputEmpty.setNext(data, true);
                    outputEmpty.executeNext(player, nodes, data);
                    return;
                }
                int count = keys.size();
                for (int i = 0; i < count; i++) outputValues.get(i).setNext(data, list.get(i));
                outputAction.setNext(data, true);
                outputAction.executeNext(player, nodes, data);
            });
        } else {
            Methods.SQL.Async.rawSql(query, () -> {
                outputEmpty.setNext(data, true);
                outputEmpty.executeNext(player, nodes, data);
            });
        }
    }
}













