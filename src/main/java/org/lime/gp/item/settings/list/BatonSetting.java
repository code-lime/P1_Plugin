package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "baton") public class BatonSetting extends ItemSetting<JsonPrimitive> {
    public final double chance;
    public BatonSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator);
        this.chance = json.getAsDouble();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.range(0.0, 1.0), "При ударе игрока предметом, с указанным шансом будет садить игрока");
    }
}