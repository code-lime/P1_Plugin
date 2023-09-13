package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "lock_menu") public class LockMenuSetting extends ItemSetting<JsonPrimitive> {
    public final boolean isLock;
    public LockMenuSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        isLock = json.getAsBoolean();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.bool(), "Используется в меню " + docs.menuBase().link());
    }
}