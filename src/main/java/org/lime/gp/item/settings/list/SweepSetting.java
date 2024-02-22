package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "sweep") public class SweepSetting extends ItemSetting<JsonPrimitive> {
    public final boolean sweep;
    public SweepSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        sweep = json.getAsBoolean();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.bool(), IComment.text("Устанавливает статус работы урона сплешом у меча"));
    }
}