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
import org.lime.gp.item.settings.use.ITimeUse;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;
import org.lime.gp.item.settings.use.target.IPlayerTarget;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.item.settings.use.target.SelfTarget;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.module.Death;

import com.google.gson.JsonObject;
import org.lime.gp.player.module.needs.thirst.Thirst;
import org.lime.system.range.IRange;

import java.util.Optional;

@Setting(name = "heal") public class HealSetting extends ItemSetting<JsonObject> implements ITimeUse<IPlayerTarget> {
    public final IRange heal;
    public final Integer total;
    public final int time;
    public final int cooldown;
    public final boolean up;
    public final boolean fixLegs;

    public final String prefixSelf;
    public final String prefixTarget;
    public final String prefixCooldown;
    private final boolean silent;

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
        this.silent = !json.has("silent") || json.get("silent").getAsBoolean();
    }
    @Override public boolean silent() { return silent; }

    @Override public EquipmentSlot arm() { return EquipmentSlot.HAND; }

    @Override public int getTime() { return time; }
    @Override public int getCooldown() { return cooldown; }
    @Override public String prefix(boolean self) { return self ? prefixSelf : prefixTarget; }
    @Override public String cooldownPrefix() { return prefixCooldown; }

    @Override public Optional<IPlayerTarget> tryCast(Player player, ITarget target, EquipmentSlot arm, boolean shift) {
        if (shift) return Optional.of(SelfTarget.Instance);
        else if (!target.isSelf()) return target.castToPlayer().map(v -> v);
        else return Optional.empty();
    }
    @Override public boolean timeUse(Player player, IPlayerTarget target, ItemStack item) {
        Player targetPlayer = target.getTargetPlayer(player);
        if (up) {
            Death.up(targetPlayer);
            Thirst.thirstValueCheck(targetPlayer, 3, true);
            MenuCreator.show(player, "phone.user.die.medic_up", Apply.of().add("other_uuid", targetPlayer.getUniqueId().toString()));
        }
        if (fixLegs) targetPlayer.removeScoreboardTag("leg.broken");
        double total = targetPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double health = heal.getValue(total);
        double hp = targetPlayer.getHealth();
        double appendHeal = Math.min(hp + health, this.total == null ? total : this.total);
        targetPlayer.setHealth(Math.max(hp, appendHeal));
        return true;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("heal"), IJElement.link(docs.range()), IComment.text("Значение здоровья, которое будет восстановлено")),
                JProperty.optional(IName.raw("total"), IJElement.raw(10), IComment.text("Максимальное значение здоровья до которого возможно восстановить")),
                JProperty.optional(IName.raw("time"), IJElement.raw(10), IComment.text("Время использования предмета в тиках")),
                JProperty.optional(IName.raw("silent"), IJElement.bool(), IComment.text("Будет ли проигран звук ломания предмета")),
                JProperty.optional(IName.raw("cooldown"), IJElement.raw(10), IComment.text("Время между использованиями предмета в тиках")),
                JProperty.optional(IName.raw("up"), IJElement.bool(), IComment.text("Требуется ли поднимать игрока")),
                JProperty.optional(IName.raw("fixLegs"), IJElement.bool(), IComment.text("Требуется ли восстанавливать перелом ноги")),
                JProperty.optional(IName.raw("prefix"), IJElement.raw("PREFIX TEXT")
                        .or(JObject.of(
                                JProperty.require(IName.raw("self"), IJElement.raw("PREFIX TEXT")),
                                JProperty.optional(IName.raw("target"), IJElement.raw("PREFIX TEXT")),
                                JProperty.optional(IName.raw("cooldown"), IJElement.raw("PREFIX TEXT"))
                        )), IComment.text("Отображаемый префикс перетаймеров использования"))
        ), IComment.text("Предмет востанавливающий здоровье. После использования возможен вызов ").append(IComment.link(docs.settingsLink(NextSetting.class))));
    }
}