package org.lime.gp.item.settings.list;

import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "bucket") public class BucketSetting extends ItemSetting<JsonNull> {
    public BucketSetting(Items.ItemCreator creator) { super(creator); }
}