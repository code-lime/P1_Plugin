package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

@Setting(name = "q_to_next") public class QToNextSetting extends ItemSetting<JsonObject> {
    public final String sound;
    public QToNextSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        sound = json.has("sound") ? json.get("sound").getAsString() : null;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.optional(IName.raw("sound"), IJElement.link(docs.sound()), IComment.text("Звук при взаимодействии"))
        ), "Предмет, заменяемый на предмет из "+docs.settingsLink(NextSetting.class).link()+" по нажатию `Q` в инвентаре");
    }
}