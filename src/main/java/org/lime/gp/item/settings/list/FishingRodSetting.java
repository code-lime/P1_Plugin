package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

@Setting(name = "fishing_rod")
public class FishingRodSetting extends ItemSetting<JsonObject> {
    public final double launchMultiply;
    public FishingRodSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);

        launchMultiply = json.has("launch_multiply") ? json.get("launch_multiply").getAsDouble() : 1;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.optional(IName.raw("launch_multiply"), IJElement.raw(1.0), IComment.text("Указывает множитель, на который будет умножена сила броска"))
        ), IComment.text("Изменияет характеристики удочки"));
    }
}





