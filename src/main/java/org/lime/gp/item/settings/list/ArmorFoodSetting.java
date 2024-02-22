package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.player.module.needs.food.ProxyFoodMetaData;

@Setting(name = "armor_food") public class ArmorFoodSetting extends ItemSetting<JsonObject> {
    public final float saturation;
    public final float food;
    public final boolean inHand;

    public ArmorFoodSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.saturation = -json.get("saturation").getAsFloat();
        this.food = -json.get("food").getAsFloat();
        this.inHand = json.has("inHand") && json.get("inHand").getAsBoolean();
    }

    public void change(Player player) {
        ProxyFoodMetaData.ofPlayer(player)
                .ifPresent(data -> {
                    if (data.modify(0, saturation))
                        data.modify(food, 0);
                });
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("saturation"), IJElement.raw(0.5), IComment.text("Количество насыщенности снимаемое за прыжок")),
                JProperty.require(IName.raw("food"), IJElement.raw(0.5), IComment.text("Количество еды снимаемое за прыжок при пустой насыщенности")),
                JProperty.optional(IName.raw("isHand"), IJElement.bool(), IComment.text("Указывает срабатывает ли снятие при нахождении предмета в основной или второстепенной руке"))
        ), IComment.text("Снятие насыщенности и еды при прыжках"));
    }
}










