package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "card") public class CardSetting extends ItemSetting<JsonNull> {
    public CardSetting(ItemCreator creator) { super(creator); }
}