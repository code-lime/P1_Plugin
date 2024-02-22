package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

@Setting(name = "level_food_mutate") public class LevelFoodMutateSetting extends ItemSetting<JsonObject> {
    public final double sec;

    public LevelFoodMutateSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.sec = json.get("sec").getAsDouble();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("sec"), IJElement.raw(1.0), IComment.text("Время действия модификатора"))
        ), IComment.text("Увеличивает значения получаемого опыта на 25%"));
    }
}