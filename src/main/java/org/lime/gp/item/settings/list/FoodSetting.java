package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import net.minecraft.world.food.FoodInfo;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.player.module.food.FoodType;

import java.util.HashMap;

@Setting(name = "food") public class FoodSetting extends ItemSetting<JsonObject> {
    public final HashMap<FoodType, Info> types = new HashMap<>();

    public record Info(float value, float saturation) {
        public static Info of(FoodInfo info) {
            return new Info(info.getNutrition(), info.getSaturationModifier());
        }
        public static Info of(JsonObject json) {
            return new Info(json.get("value").getAsFloat(), json.get("saturation").getAsFloat());
        }
    }

    public FoodSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        if (json.has("types")) json.get("types")
                .getAsJsonObject()
                .entrySet()
                .forEach(kv -> this.types.put(
                        FoodType.parse(kv.getKey()),
                        Info.of(kv.getValue().getAsJsonObject())
                ));
        else this.types.put(FoodType.Vanilla, Info.of(json));
    }
}
