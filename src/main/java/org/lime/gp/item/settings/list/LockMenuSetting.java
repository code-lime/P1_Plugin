package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "lock_menu") public class LockMenuSetting extends ItemSetting<JsonPrimitive> {
    public final boolean isLock;
    public LockMenuSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        isLock = json.getAsBoolean();
    }
}