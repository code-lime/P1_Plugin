package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "wallet") public class WalletSetting extends ItemSetting<JsonNull> {
    public WalletSetting(Items.ItemCreator creator) { super(creator); }
}