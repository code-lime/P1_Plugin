package org.lime.gp.item.settings.list;

import java.util.Optional;

import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

@Setting(name = "display") public class DisplaySetting extends ItemSetting<JsonObject> {
    public Checker item;
    public DisplaySetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        item = Checker.createCheck(json.get("item").getAsString());
    }

    public Optional<ItemStack> item(int original_id) {
        return item.getRandomCreator().map(v -> v.createItem(Apply.of().add("original_id", String.valueOf(original_id))));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.link(docs.regexItem()), IComment.text("В руках игрока будет отображатся как указанный предмет. В указанный предмет в `args` передается `original_id` являющийся `id` текущего предмета"));
    }
}