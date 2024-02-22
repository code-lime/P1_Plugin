package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

@Setting(name = "insert") public class InsertSetting extends ItemSetting<JsonObject> {
    public final String type;
    public final int weight;
    public InsertSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        type = json.get("type").getAsString();
        weight = json.get("weight").getAsInt();
    }
    public static List<ItemStack> createOf(String type, int weight) {
        List<ItemStack> items = new ArrayList<>();
        if (weight <= 0) return items;
        HashMap<IItemCreator, Integer> list = new HashMap<>();
        List<IItemCreator> creators = new ArrayList<>(Items.creatorIDs.values());
        Collections.reverse(creators);
        for (IItemCreator _v :creators) {
            if (!(_v instanceof ItemCreator creator)) continue;
            Optional<Integer> _weight = creator.getOptional(InsertSetting.class).filter(v -> v.type.equals(type)).map(v -> v.weight);
            if (_weight.isEmpty()) continue;
            if (weight < _weight.get()) continue;
            list.put(creator, list.getOrDefault(creator, 0) + weight / _weight.get());
            weight %= _weight.get();
        }
        list.forEach((k,v) -> items.add(k.createItem(v)));
        return items;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("type"), IJElement.raw("INSERT_TYPE"), IComment.text("Пользвательский тип предмета для расчетов в меню")),
                JProperty.require(IName.raw("weight"), IJElement.raw(10), IComment.text("Значение веса однго предмета для расчетов в меню"))
        ), IComment.text("Используется в меню"));
    }
}