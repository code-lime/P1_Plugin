package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "bucket") public class BucketSetting extends ItemSetting<JsonNull> {
    public BucketSetting(ItemCreator creator) { super(creator); }
}