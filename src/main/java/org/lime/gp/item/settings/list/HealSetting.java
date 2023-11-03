package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.UseSetting;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.module.Death;

import com.google.gson.JsonObject;
import org.lime.gp.player.module.needs.thirst.Thirst;
import org.lime.system.range.IRange;

@Setting(name = "heal") public class HealSetting extends ItemSetting<JsonObject> implements UseSetting.ITimeUse {
    public final IRange heal;
    public final Integer total;
    public final int time;
    public final int cooldown;
    public final boolean up;
    public final boolean fixLegs;

    public final String prefixSelf;
    public final String prefixTarget;
    public final String prefixCooldown;

    public HealSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.heal = IRange.parse(json.get("heal").getAsString());
        this.total = json.has("total") ? json.get("total").getAsInt() : null;
        this.time = json.has("time") ? json.get("time").getAsInt() : 0;
        this.cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;
        this.up = json.has("up") && json.get("up").getAsBoolean();
        this.fixLegs = json.has("fixLegs") && json.get("fixLegs").getAsBoolean();
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
            prefixSelf = prefixCooldown = prefixTarget = "";
        }
    }

    @Override public EquipmentSlot arm() { return EquipmentSlot.HAND; }
    @Override public int getTime() { return time; }
    @Override public int getCooldown() { return cooldown; }
    @Override public String prefix(boolean self) { return self ? prefixSelf : prefixTarget; }
    @Override public String cooldownPrefix() { return prefixCooldown; }

    @Override public void timeUse(Player player, Player target, ItemStack item) {
        if (up) {
            Death.up(target);
            Thirst.thirstValueCheck(target, 3, true);
            MenuCreator.show(player, "phone.user.die.medic_up", Apply.of().add("other_uuid", target.getUniqueId().toString()));
        }
        if (fixLegs) target.removeScoreboardTag("leg.broken");
        double total = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double health = heal.getValue(total);
        double hp = target.getHealth();
        double appendHeal = Math.min(hp + health, this.total == null ? total : this.total);
        target.setHealth(Math.max(hp, appendHeal));
    }

    @Override public boolean use(Player player, Player target, EquipmentSlot arm, boolean shift) {
        if (shift) return UseSetting.ITimeUse.super.use(player, player, arm, shift);
        else if (player != target) return UseSetting.ITimeUse.super.use(player, target, arm, shift);
        else return false;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("heal"), IJElement.link(docs.range()), IComment.text("Значение здоровья, которое будет восстановлено")),
                JProperty.optional(IName.raw("total"), IJElement.raw(10), IComment.text("Максимальное значение здоровья до которого возможно восстановить")),
                JProperty.optional(IName.raw("time"), IJElement.raw(10), IComment.text("Время использования предмета в тиках")),
                JProperty.optional(IName.raw("cooldown"), IJElement.raw(10), IComment.text("Время между использованиями предмета в тиках")),
                JProperty.optional(IName.raw("up"), IJElement.bool(), IComment.text("Требуется ли поднимать игрока")),
                JProperty.optional(IName.raw("fixLegs"), IJElement.bool(), IComment.text("Требуется ли восстанавливать перелом ноги")),
                JProperty.optional(IName.raw("prefix"), IJElement.raw("PREFIX TEXT")
                        .or(JObject.of(
                                JProperty.require(IName.raw("self"), IJElement.raw("PREFIX TEXT")),
                                JProperty.optional(IName.raw("target"), IJElement.raw("PREFIX TEXT")),
                                JProperty.optional(IName.raw("cooldown"), IJElement.raw("PREFIX TEXT"))
                        )), IComment.text("Отображаемый префикс перетаймеров использования"))
        ), "Предмет востанавливающий здоровье. После использования возможен вызов " + docs.settingsLink(NextSetting.class).link());
    }
}