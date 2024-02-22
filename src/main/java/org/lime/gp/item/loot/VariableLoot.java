package org.lime.gp.item.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexDocs;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.module.loot.IPopulateLoot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableLoot implements ILoot {
    public static class VariableItemStack extends ItemStack {
        public final Map<String, JsonElement> variables = new HashMap<>();
        public VariableItemStack(Map<String, JsonElement> variables) {
            super(Material.AIR);
            this.variables.putAll(variables);
        }
    }

    public final ILoot next;
    public final Map<String, JsonElement> variables = new HashMap<>();

    public VariableLoot(JsonObject json) {
        variables.putAll(json.getAsJsonObject("variable").asMap());
        next = ILoot.parse(json.get("next"));
    }

    public static Map<String, JsonElement> exportVariablesWithRemove(List<ItemStack> items) {
        Map<String, JsonElement> otherVariables = new HashMap<>();
        items.removeIf(item -> {
            if (item instanceof VariableItemStack varItem) {
                otherVariables.putAll(varItem.variables);
                return true;
            }
            return false;
        });
        return otherVariables;
    }

    @Override public List<ItemStack> generateLoot(IPopulateLoot loot) {
        List<ItemStack> items = new ArrayList<>(next.generateLoot(loot));
        Map<String, JsonElement> otherVariables = exportVariablesWithRemove(items);
        otherVariables.putAll(variables);
        items.add(new VariableItemStack(otherVariables));
        return items;
    }
    public static JObject docs(IJElement loot, IDocsLink docs, IIndexDocs jsLoot) {
        return JObject.of(
                JProperty.require(IName.raw("variable"), IJElement.anyObject(
                        JProperty.require(IName.raw("VARIABLE_NAME"), IJElement.link(docs.json()))
                ), IComment.join(
                        IComment.text("Параметры, сохраняемые в "),
                        IComment.field("variable"),
                        IComment.text(" для "),
                        IComment.link(jsLoot)
                )),
                JProperty.require(IName.raw("next"), loot, IComment.text("Генератор лута, который будет вызван").append(IComment.field("code")))
        );
    }
}
