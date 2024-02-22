package org.lime.gp.item.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.module.loot.IPopulateLoot;

import java.util.List;

public interface ILoot {
    List<ItemStack> generateLoot(IPopulateLoot loot);

    static ILoot parse(JsonElement json) {
        if (json.isJsonPrimitive()) return new SingleLoot(json.getAsString());
        else if (json.isJsonArray()) return new MultiLoot(json.getAsJsonArray());
        else if (json.isJsonNull()) return EmptyLoot.Instance;
        else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            return obj.has("type") ? switch (obj.get("type").getAsString()) {
                case "random" -> new RandomLoot(obj);
                case "js" -> new JavaScriptLoot(obj);
                case "variable" -> new VariableLoot(obj);
                default -> throw new IllegalArgumentException("[LOOT] Type '"+obj.get("type").getAsString()+"' not supported");
            } : new FilterLoot(obj);
        }
        throw new IllegalArgumentException("[LOOT] Error parse LootTable");
    }
    static IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup randomLoot = JsonGroup.of("RANDOM", RandomLoot.docs(IJElement.linkParent())
                .addFirst(JProperty.require(IName.raw("type"), IJElement.raw("random"))));
        IIndexGroup jsLoot = JsonGroup.of("JAVASCRIPT", JavaScriptLoot.docs(IJElement.linkParent(), docs)
                .addFirst(JProperty.require(IName.raw("type"), IJElement.raw("js"))));
        IIndexGroup variableLoot = JsonGroup.of("VARIABLE", VariableLoot.docs(IJElement.linkParent(), docs, jsLoot)
                .addFirst(JProperty.require(IName.raw("type"), IJElement.raw("variable"))));
        IIndexGroup filterLoot = FilterLoot.docs("FILTER", IJElement.linkParent(), docs);
        IIndexGroup singleLoot = JsonGroup.of("SINGLE", SingleLoot.docs(docs));
        return JsonEnumInfo.of(index)
                .add(IJElement.link(randomLoot))
                .add(IJElement.link(jsLoot))
                .add(IJElement.link(variableLoot))
                .add(IJElement.link(filterLoot))
                .add(IJElement.link(singleLoot))
                .add(IJElement.nullable(), IComment.text("Без получаемых предметов"))
                .add(IJElement.anyList(IJElement.linkCurrent()), IComment.text("Объеденяет все получаемые предметы"))
                .withChilds(randomLoot, jsLoot, variableLoot, filterLoot, singleLoot);
    }
}