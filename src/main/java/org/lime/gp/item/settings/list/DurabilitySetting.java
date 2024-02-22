package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "durability") public class DurabilitySetting extends ItemSetting<JsonPrimitive> {
    public final int maxDurability;
    public DurabilitySetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        maxDurability = json.getAsInt();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.raw(10), IComment.text("Устанавливает максимальную прочность предмета"));
    }
}