package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

@Setting(name = "arrow") public class ArrowSetting extends ItemSetting<JsonObject> {
    public final double damageMultiply;

    public ArrowSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.damageMultiply = json.get("damage_multiply").getAsDouble();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("damage_multiply"), IJElement.raw(0.5), IComment.text("Множитель, на который умножается урон выпускаемой стрелы"))
        ), "Настройка стрелы выпускаемой из лука");
    }
}