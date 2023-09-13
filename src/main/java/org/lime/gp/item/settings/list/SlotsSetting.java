package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "slots") public class SlotsSetting extends ItemSetting<JsonPrimitive> {
    public final int slots;

    public SlotsSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.slots = json.getAsInt();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.raw(5), "Открывает определенное количество слотов в инвентаре");
    }
}