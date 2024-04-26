package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Elemental;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.item.settings.use.INext;

@Setting(name = "next_to_elemental") public class NextToElementalSetting extends ItemSetting<JsonObject> implements INext {
    public final String elemental;

    public NextToElementalSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.elemental = json.get("elemental").getAsString();
    }

    @Override public void executeNext(Player player) {
        Elemental.execute(player, new DataContext(), elemental);
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("elemental"), IJElement.link(docs.elementalName()), IComment.text("Элементаль, который будет вызван"))
        ), IComment.text("Настройка-расширение к которому обращаются другие настройки которые вызывают ").append(IComment.link(docs.settingsLink(NextSetting.class))));
    }
}