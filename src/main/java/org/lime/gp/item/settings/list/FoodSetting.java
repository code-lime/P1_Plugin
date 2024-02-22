package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import net.minecraft.world.food.FoodInfo;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.player.module.needs.food.FoodType;

import java.util.HashMap;

@Setting(name = "food") public class FoodSetting extends ItemSetting<JsonObject> {
    public final HashMap<FoodType, Info> types = new HashMap<>();

    public record Info(float value, float saturation) {
        public static Info of(FoodInfo info) { return new Info(info.getNutrition(), info.getSaturationModifier()); }
        public static Info of(JsonObject json) { return new Info(json.get("value").getAsFloat(), json.get("saturation").getAsFloat()); }
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

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup food_info = JsonGroup.of("FOOD_INFO", "food_info",
                JObject.of(
                        JProperty.require(IName.raw("value"), IJElement.raw(1.0), IComment.text("Значение восстанавливаемой еды")),
                        JProperty.require(IName.raw("saturation"), IJElement.raw(1.0), IComment.text("Модификатор восстанавливаемого насыщения"))
                )
        );
        IIndexGroup food_type = JsonEnumInfo.of("FOOD_TYPE", "food_type", FoodType.class);
        return JsonGroup.of(index, index, IJElement.link(food_info).or(JObject.of(
                JProperty.require(IName.raw("types"), IJElement.anyObject(
                        JProperty.require(IName.link(food_type), IJElement.link(food_info))
                ))
        )), IComment.text("Добавляет характеристики еде. Если не указывать ").append(IComment.link(food_type)).append(IComment.text(" то настройка устанавливается для ")).append(IComment.raw(FoodType.Vanilla.name())))
                .withChilds(food_info, food_type);
    }
}
