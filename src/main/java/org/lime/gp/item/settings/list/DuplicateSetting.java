package org.lime.gp.item.settings.list;

import org.bukkit.Material;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonArray;

@Setting(name = "duplicate") public class DuplicateSetting extends ItemSetting<JsonArray> {
    public DuplicateSetting(ItemCreator creator, JsonArray json) {
        super(creator, json);
        json.forEach(item -> {
            int id = item.getAsInt();
            Items.creators.put(id, creator);
            Items.creatorNamesIDs.put(id, creator.getKey());
            try { Items.creatorMaterials.put(id, Material.valueOf(creator.item)); } catch (Exception ignored) { }
        });
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.anyList(IJElement.raw(10)), "Связывает текущий предмет с указанными ID-шниками отображения");
    }
}