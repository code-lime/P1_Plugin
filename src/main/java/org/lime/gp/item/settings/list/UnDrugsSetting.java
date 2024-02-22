package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

import com.google.gson.JsonPrimitive;

@Setting(name = "undrugs") public class UnDrugsSetting extends ItemSetting<JsonPrimitive> {
    public final double time;
    public UnDrugsSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.time = json.getAsDouble();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.raw(1.0), IComment.text("Ускоряет прохождение стадий из ").append(IComment.link(docs.settingsLink(DrugsSetting.class))).append(IComment.text(" в 4 раза на определеннове время в минутах")));
    }
}