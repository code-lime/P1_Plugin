package org.lime.gp.item.settings.list;

import java.util.HashMap;
import java.util.Map;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

import net.kyori.adventure.text.Component;
import org.lime.system;

@Setting(name = "vest") public class VestSetting extends ItemSetting<JsonObject> {
    public final int rows;
    public final Component title;
    public final Map<Integer, Checker> slots = new HashMap<>();

    public VestSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);

        this.rows = json.has("rows") ? json.get("rows").getAsInt() : 1;
        this.title = ChatHelper.formatComponent(json.get("title").getAsString());
        json.getAsJsonObject("slots").entrySet().forEach(kv -> {
            Checker checker = Checker.createCheck(kv.getValue().getAsString());
            system.IRange.parse(kv.getKey()).getAllInts(rows * 9).forEach(slot -> this.slots.put(slot, checker));
            //Menu.rangeOf(kv.getKey()).forEach(slot -> this.slots.put(slot, checker));
        });
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("title"), IJElement.link(docs.formattedChat()), IComment.text("Название инвентаря")),
                JProperty.optional(IName.raw("rows"), IJElement.range(1, 6), IComment.text("Количество строчек в инвентаре")),
                JProperty.optional(IName.raw("slots"), IJElement.anyObject(
                        JProperty.require(IName.link(docs.range()), IJElement.link(docs.regexItem()))
                ), IComment.text("Список слотов и предметов которые в него можно положить. Пропущенные слоты будут автоматически заблокированы для взаимодействия"))
        ), "Позволяет открыть внутренний инвентарь предмета при его нахождении в слотах брони");
    }
}