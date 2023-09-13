package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "max_stack") public class MaxStackSetting extends ItemSetting<JsonPrimitive> {
    public final int maxStack;
    public MaxStackSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        maxStack = json.getAsInt();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.raw(10), "Устанавливает максимальное количество предмета в стаке");
    }
}