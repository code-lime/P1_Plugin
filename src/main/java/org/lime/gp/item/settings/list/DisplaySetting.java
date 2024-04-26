package org.lime.gp.item.settings.list;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

@Setting(name = "display") public class DisplaySetting extends ItemSetting<JsonObject> {
    private static final ConcurrentHashMap<Toast2<String, Integer>, ItemStack> cacheItems = new ConcurrentHashMap<>();
    private static int loaded_index = 0;

    public Checker item;
    public DisplaySetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        item = Checker.createCheck(json.get("item").getAsString());
    }

    public Optional<ItemStack> item(int original_id) {
        if (loaded_index != Items.getLoadedIndex()) {
            cacheItems.clear();
            loaded_index = Items.getLoadedIndex();
        }
        return item.getRandomCreator()
                .map(v -> cacheItems.computeIfAbsent(Toast.of(v.getKey(), original_id), _v -> v
                        .createItem(Apply.of().add("original_id", String.valueOf(original_id)))));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.link(docs.regexItem()), IComment.text("В руках игрока будет отображатся как указанный предмет. В указанный предмет в `args` передается `original_id` являющийся `id` текущего предмета"));
    }
}