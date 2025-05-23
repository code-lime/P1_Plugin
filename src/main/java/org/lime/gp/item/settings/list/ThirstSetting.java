package org.lime.gp.item.settings.list;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

@Setting(name = "thirst") public class ThirstSetting extends ItemSetting<JsonObject> {
    public static final Color DEFAULT_WATER_COLOR = Color.fromRGB(0x3F76E4);
    public static final String DEFAULT_WATER_COLOR_HEX = "#3F76E4";

    public static ItemStack createWaterBottle() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta)item.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.WATER));
        item.setItemMeta(meta);
        return item;
    }
    public static ItemStack createClearBottle() {
        return Items.createItem("Potion.Clear_Water").orElseGet(ThirstSetting::createWaterBottle);
    }

    public final String type;
    public final Color color;
    public ThirstSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);

        type = json.get("type").getAsString();
        color = json.has("color") ? ChatColorHex.of(json.get("color").getAsString()).toBukkitColor() : null;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("type"), IJElement.raw("THIRST_TYPE"), IComment.text("Пользовательский тип жидкости")),
                JProperty.optional(IName.raw("color"), IJElement.raw("#FFFFFF"), IComment.text("Цвет жидкости"))
        ), IComment.text("Устанавливает статус работы урона сплешом у меча"), IComment.text(""),
                IComment.warning("ВНИМАНИЕ! Обязательное существование предмета `Potion.Clear_Water`"));
    }
}