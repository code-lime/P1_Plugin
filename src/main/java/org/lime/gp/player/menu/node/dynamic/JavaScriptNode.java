package org.lime.gp.player.menu.node.dynamic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IInput;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.gp.player.menu.node.connect.input.ActionInput;
import org.lime.gp.player.menu.node.connect.input.ObjectInput;
import org.lime.gp.player.menu.node.connect.input.StringInput;
import org.lime.gp.player.menu.node.connect.output.ActionOutput;
import org.lime.gp.player.menu.node.connect.output.ObjectOutput;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;

public class JavaScriptNode extends BaseNode {
    private final ActionInput inputAction;
    private final StringInput inputCode;

    private final ActionOutput outputAction;
    private final ObjectOutput outputResult;
    /*
    "Dynamic/JavaScript/Function1": {
        "title": "Function1",
        "input": {
            "_input": "action",
            "js": {
                "type": "string",
                "value": "anyMethod(asd,qwe)"
            },
            "asd": {
                "type": "string",
                "value": ""
            },
            "qwe": {
                "type": "int",
                "value": 1000
            }
        },
        "output": {
            "_output": "action",
            "result": "object"
        }
    }
    */
    public JavaScriptNode(int id, JsonObject json) {
        super(id, json, system.map.<String, system.Func2<String, Optional<JsonElement>, IInput>>of()
                .add("input", (key, def) -> new ActionInput(key))
                .add("js", (key, def) -> new StringInput(key, def.map(JsonElement::getAsString).orElse("")))
                .build(), system.map.<String, system.Func2<String, List<system.Toast2<Integer, String>>, IOutput>>of()
                .add("output", ActionOutput::new)
                .add("result", ObjectOutput::new)
                .build());
        inputAction = (ActionInput)this.input.get("input");
        inputCode = (StringInput)this.input.get("js");

        outputAction = (ActionOutput)this.output.get("output");
        outputResult = (ObjectOutput)this.output.get("result");
    }

    @Override protected void invokeNodeGenerate(Player player, Map<Integer, BaseNode> nodes, Map<String, Object> variable, Map<Integer, Map<String, Object>> data, Map<IInput, Object> inputExecute) {
        if (!Boolean.TRUE.equals(inputExecute.get(inputAction))) return;
        Map<String, Object> args = new HashMap<>();
        variable.forEach((key, value) -> {
            switch (key) {
                case "input", "js": return;
                default: break;
            }
            args.put(key, value);
        });
        JavaScript.invoke(inputExecute.get(inputCode) + "", args)
                        .ifPresent(result -> {
                            outputResult.setNext(data, result);
                            outputAction.setNext(data, true);
                            outputAction.executeNext(player, nodes, data);
                        });
    }
}
