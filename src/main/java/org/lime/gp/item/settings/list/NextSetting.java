package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.system;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Setting(name = "next") public class NextSetting extends ItemSetting<JsonElement> {
    private final Map<Checker, Double> next;
    private final double totalWeight;

    public NextSetting(ItemCreator creator, JsonElement json) {
        super(creator, json);
        if (json.isJsonArray()) this.next = json.getAsJsonArray()
                .asList()
                .stream()
                .collect(Collectors.toMap(k -> Checker.createCheck(k.getAsString()), k -> 1.0));
        else if (json.isJsonObject()) this.next = json.getAsJsonObject()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(kv -> Checker.createCheck(kv.getKey()), kv -> kv.getValue().getAsDouble()));
        else this.next = Collections.singletonMap(Checker.createCheck(json.getAsString()), 1.0);
        totalWeight = next.values().stream().mapToDouble(v -> v).sum();
        if (this.next.size() == 0) throw new IllegalArgumentException("Next count == 0");
        if (totalWeight <= 0) throw new IllegalArgumentException("Total weight <= 0");
    }

    public Optional<IItemCreator> next() {
        double value = system.rand(0, totalWeight);
        Checker select = null;
        for (Map.Entry<Checker, Double> item : next.entrySet()) {
            value -= item.getValue();
            select = item.getKey();
            if (value <= 0) break;
        }
        return select == null
                ? Optional.empty()
                : select.getRandomCreator();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.link(docs.regexItem())
                        .or(IJElement.anyList(IJElement.link(docs.regexItem())))
                        .or(IJElement.anyObject(
                                JProperty.require(IName.link(docs.regexItem()), IJElement.raw(1.0), IComment.text("Числовым значением является вес рандома"))
                        )),
                "Настройка-расширение к которому обращаются другие настройки");
    }
}











