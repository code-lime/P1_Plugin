package org.lime.gp.item.settings.list;

import com.google.gson.JsonPrimitive;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

@Setting(name = "horse_armor") public class HorseArmorSetting extends ItemSetting<JsonPrimitive> {
    public final boolean isArmor;
    public HorseArmorSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        isArmor = json.getAsBoolean();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.bool(), IComment.text("Указывает, является ли предмет конской броней"));
    }
}
