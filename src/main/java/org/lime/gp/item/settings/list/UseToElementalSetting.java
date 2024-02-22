package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.settings.use.ITimeUse;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Elemental;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.item.settings.use.target.SelfTarget;

import java.util.Optional;

@Setting(name = "use_to_elemental") public class UseToElementalSetting extends ItemSetting<JsonObject> implements ITimeUse<SelfTarget> {
    public final int time;
    public final int cooldown;
    public final Boolean shift;
    public final EquipmentSlot arm;
    public final String elemental;

    public final String prefixSelf;
    public final String prefixTarget;
    public final String prefixCooldown;
    private final boolean silent;

    public UseToElementalSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.elemental = json.get("elemental").getAsString();
        this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
        this.time = json.has("time") ? json.get("time").getAsInt() : 0;
        this.cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;
        this.shift = !json.has("shift") || json.get("shift").isJsonNull() ? null : json.get("shift").getAsBoolean();
        if (json.has("prefix")) {
            JsonElement prefix = json.get("prefix");
            if (prefix.isJsonObject()) {
                JsonObject _prefix = prefix.getAsJsonObject();
                prefixSelf = _prefix.get("self").getAsString();
                prefixTarget = _prefix.has("target") ? _prefix.get("target").getAsString() : prefixSelf;
                prefixCooldown = _prefix.has("cooldown") ? _prefix.get("cooldown").getAsString() : "";
            } else {
                prefixSelf = prefixTarget = prefix.getAsString();
                prefixCooldown = "";
            }
        } else {
            prefixSelf = prefixTarget = prefixCooldown = "";
        }
        this.silent = !json.has("silent") || json.get("silent").getAsBoolean();
    }
    @Override public boolean silent() { return silent; }
    @Override public EquipmentSlot arm() { return arm; }

    @Override public int getTime() { return time; }
    @Override public int getCooldown() { return cooldown; }
    @Override public String prefix(boolean self) { return self ? prefixSelf : prefixTarget; }
    @Override public String cooldownPrefix() { return prefixCooldown; }

    @Override public Optional<SelfTarget> tryCast(Player player, ITarget target, EquipmentSlot arm, boolean shift) {
        return this.shift != null && this.shift != shift
                ? Optional.empty()
                : Optional.of(SelfTarget.Instance);
    }
    @Override public boolean timeUse(Player player, SelfTarget target, ItemStack item) {
        Elemental.execute(player, new DataContext(), elemental);
        return true;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("elemental"), IJElement.link(docs.elementalName()), IComment.text("Элементаль, который будет вызван")),
                JProperty.require(IName.raw("arm"), IJElement.link(docs.handType()), IComment.text("Тип руки, в которой происходит взаимодействие")),
                JProperty.optional(IName.raw("time"), IJElement.raw(10), IComment.text("Время использования предмета в тиках")),
                JProperty.optional(IName.raw("silent"), IJElement.bool(), IComment.text("Будет ли проигран звук ломания предмета")),
                JProperty.optional(IName.raw("cooldown"), IJElement.raw(10), IComment.text("Время между использованиями предмета в тиках")),
                JProperty.optional(IName.raw("shift"), IJElement.bool(), IComment.empty()
                        .append(IComment.text("Требуется ли нажимать "))
                        .append(IComment.raw("SHIFT"))
                        .append(IComment.text(" для использования. Если не указано - проверка на нажатие отсуствует"))),
                JProperty.optional(IName.raw("prefix"), IJElement.raw("PREFIX TEXT")
                        .or(JObject.of(
                                JProperty.require(IName.raw("self"), IJElement.raw("PREFIX TEXT")),
                                JProperty.optional(IName.raw("target"), IJElement.raw("PREFIX TEXT")),
                                JProperty.optional(IName.raw("cooldown"), IJElement.raw("PREFIX TEXT"))
                        )), IComment.text("Отображаемый префикс перетаймеров использования"))
        ), IComment.text("Предмет вызывает элементаль. После использования возможен вызов ").append(IComment.link(docs.settingsLink(NextSetting.class))));
    }
}