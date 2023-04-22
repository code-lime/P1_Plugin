package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "wallet") public class WalletSetting extends ItemSetting<JsonNull> {
    public WalletSetting(ItemCreator creator) { super(creator); }
}