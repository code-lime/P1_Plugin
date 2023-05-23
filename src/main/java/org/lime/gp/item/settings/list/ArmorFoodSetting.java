package org.lime.gp.item.settings.list;

import org.bukkit.entity.Player;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.player.module.ProxyFoodMetaData;

import com.google.gson.JsonObject;

@Setting(name = "armor_food") public class ArmorFoodSetting extends ItemSetting<JsonObject> {
    public final float saturation;
    public final float food;

    public ArmorFoodSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.saturation = -json.get("saturation").getAsFloat();
        this.food = -json.get("food").getAsFloat();
    }

    public void change(Player player) {
        ProxyFoodMetaData.ofPlayer(player)
            .ifPresent(data -> {
                if (data.modifySaturation(saturation) <= 0);
                    data.modifyFoodLevel(food);
            });
    }
}