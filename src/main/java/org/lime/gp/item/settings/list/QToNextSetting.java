package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

@Setting(name = "q_to_next") public class QToNextSetting extends ItemSetting<JsonObject> {
    public final String sound;
    public QToNextSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        sound = json.has("sound") ? json.get("sound").getAsString() : null;
    }
}