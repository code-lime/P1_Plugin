package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.player.module.Drugs;

import java.util.HashSet;

@Setting(name = "level_food_mutate") public class LevelFoodMutateSetting extends ItemSetting<JsonObject> {
    public final double sec;

    public LevelFoodMutateSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.sec = json.get("sec").getAsDouble();
    }
}