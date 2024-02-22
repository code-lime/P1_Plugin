package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

import java.util.ArrayList;
import java.util.List;

@Setting(name = "bait") public class BaitSetting extends ItemSetting<JsonElement> {
    public final List<String> tags = new ArrayList<>();

    public BaitSetting(ItemCreator creator, JsonElement json) {
        super(creator, json);
        if (json.isJsonArray()) json.getAsJsonArray().forEach(item -> tags.add(item.getAsString()));
        else tags.add(json.getAsString());
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.raw("TAG").or(IJElement.anyList(IJElement.raw("TAG"))), IComment.text("Одноразования наживка. Тратится из инвентаря пояса. Добавляет TAG's к поплавку"));
    }
}