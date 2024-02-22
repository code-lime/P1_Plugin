package org.lime.gp.item.settings.list;

import com.google.gson.JsonNull;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

@Setting(name = "attack_fix") public class AttackFixSetting extends ItemSetting<JsonNull> {
    public AttackFixSetting(ItemCreator creator, JsonNull json) {
        super(creator, json);
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.nullable(), IComment.text("При взятии такого-же предмета происходит сброс времени поднятия"));
    }
}
