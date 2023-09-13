package org.lime.gp.item.settings.list;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.UseSetting;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;
import org.lime.gp.sound.Sounds;

@Setting(name = "use_to_next") public class UseToNextSetting extends ItemSetting<JsonObject> implements UseSetting.ITimeUse {
    public final int time;
    public final Boolean shift;
    public final EquipmentSlot arm;
    public final String sound;
    public UseToNextSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
        this.time = json.get("time").getAsInt();
        this.shift = json.get("shift").isJsonNull() ? null : json.get("shift").getAsBoolean();
        sound = json.has("sound") ? json.get("sound").getAsString() : null;
    }
    @Override public EquipmentSlot arm() { return arm; }
    @Override public int getTime() { return time; }
    @Override public void timeUse(Player player, Player target, ItemStack item) {
        Sounds.playSound(sound, player.getLocation());
    }
    @Override public boolean use(Player player, Player target, EquipmentSlot arm, boolean shift) {
        if (this.shift != null && this.shift != shift) return false;
        return UseSetting.ITimeUse.super.use(player, target, arm, shift);
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("arm"), IJElement.link(docs.handType()), IComment.text("Тип руки, в которой происходит взаимодействие. Возможные значения: ")),
                JProperty.optional(IName.raw("time"), IJElement.raw(10), IComment.text("Время использования предмета в тиках")),
                JProperty.optional(IName.raw("shift"), IJElement.bool(), IComment.empty()
                        .append(IComment.text("Требуется ли нажимать "))
                        .append(IComment.raw("SHIFT"))
                        .append(IComment.text(" для использования. Если не указано - проверка на нажатие отсуствует"))),
                JProperty.optional(IName.raw("sound"), IJElement.link(docs.sound()), IComment.text("Звук при взаимодействии"))
        ), "Предмет используется. После использования возможен вызов " + docs.settingsLink(NextSetting.class).link());
    }
}