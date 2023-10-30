package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.UseSetting;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Elemental;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

@Setting(name = "use_to_elemental") public class UseToElementalSetting extends ItemSetting<JsonObject> implements UseSetting.ITimeUse {
    public final int time;
    public final Boolean shift;
    public final EquipmentSlot arm;
    public final String elemental;

    public final String prefixSelf;
    public final String prefixTarget;

    public UseToElementalSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.elemental = json.get("elemental").getAsString();
        this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
        this.time = json.get("time").getAsInt();
        this.shift = !json.has("shift") || json.get("shift").isJsonNull() ? null : json.get("shift").getAsBoolean();
        if (json.has("prefix")) {
            JsonElement prefix = json.get("prefix");
            if (prefix.isJsonObject()) {
                JsonObject _prefix = prefix.getAsJsonObject();
                prefixSelf = _prefix.get("self").getAsString();
                prefixTarget = _prefix.get("target").getAsString();
            } else {
                prefixSelf = prefixTarget = prefix.getAsString();
            }
        } else {
            prefixSelf = prefixTarget = "";
        }
    }
    @Override public EquipmentSlot arm() { return arm; }
    @Override public int getTime() { return time; }
    @Override public String prefix(boolean self) { return self ? prefixSelf : prefixTarget; }
    @Override public void timeUse(Player player, Player target, ItemStack item) {
        Elemental.execute(player, new DataContext(), elemental);
    }
    @Override public boolean use(Player player, Player target, EquipmentSlot arm, boolean shift) {
        if (this.shift != null && this.shift != shift) return false;
        return UseSetting.ITimeUse.super.use(player, target, arm, shift);
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("elemental"), IJElement.link(docs.elemental()), IComment.text("Элементаль, который будет вызван")),
                JProperty.require(IName.raw("arm"), IJElement.link(docs.handType()), IComment.text("Тип руки, в которой происходит взаимодействие")),
                JProperty.optional(IName.raw("time"), IJElement.raw(10), IComment.text("Время использования предмета в тиках")),
                JProperty.optional(IName.raw("shift"), IJElement.bool(), IComment.empty()
                        .append(IComment.text("Требуется ли нажимать "))
                        .append(IComment.raw("SHIFT"))
                        .append(IComment.text(" для использования. Если не указано - проверка на нажатие отсуствует"))),
                JProperty.optional(IName.raw("prefix"), IJElement.raw("PREFIX TEXT")
                        .or(JObject.of(
                                JProperty.require(IName.raw("self"), IJElement.raw("PREFIX TEXT")),
                                JProperty.require(IName.raw("target"), IJElement.raw("PREFIX TEXT"))
                        )), IComment.text("Отображаемый префикс перетаймеров использования"))
        ), "Предмет вызывает элементаль. После использования возможен вызов " + docs.settingsLink(NextSetting.class).link());
    }
}