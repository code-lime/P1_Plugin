package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.item.settings.use.ITimeUse;
import org.lime.gp.item.settings.use.target.IPlayerTarget;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.item.settings.use.target.SelfTarget;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.player.module.needs.INeedEffect;
import org.lime.gp.player.module.needs.NeedSystem;
import org.lime.plugin.CoreElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Setting(name = "use_needs") public class UseNeedsSetting extends ItemSetting<JsonObject> implements ITimeUse<IPlayerTarget> {
    public static CoreElement create() {
        return CoreElement.create(UseNeedsSetting.class)
                .withInit(UseNeedsSetting::init);
    }
    private static void init() {
        NeedSystem.register(UseNeedsSetting::getNeeds);
    }

    private record UserGroup(UUID uuid) implements TimeoutData.TKeyedGroup<UUID> {
        @Override public UUID groupID() { return uuid; }
    }
    private static class NeedTime extends TimeoutData.IGroupTimeout {
        public final UseNeedsSetting setting;
        public NeedTime(UseNeedsSetting setting) {
            super(setting.needsTime);
            this.setting = setting;
        }
    }

    private static Stream<INeedEffect<?>> getNeeds(Player player) {
        return TimeoutData.values(new UserGroup(player.getUniqueId()), NeedTime.class)
                .flatMap(v -> v.setting.needs.stream());
    }
    private void applyNeed(Player player) {
        TimeoutData.put(new UserGroup(player.getUniqueId()), UUID.randomUUID(), NeedTime.class, new NeedTime(this));
    }

    public final List<INeedEffect<?>> needs = new ArrayList<>();
    public final int needsTime;

    public final int time;
    public final int cooldown;

    public final boolean onlySelf;

    public final String prefixSelf;
    public final String prefixTarget;
    public final String prefixCooldown;
    private final boolean silent;

    public UseNeedsSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        json.getAsJsonArray("needs")
                .forEach(item -> needs.add(INeedEffect.parse(item.getAsJsonObject())));
        this.needsTime = json.get("needs_time").getAsInt();
        this.onlySelf = json.has("only_self") && json.get("only_self").getAsBoolean();
        this.time = json.has("time") ? json.get("time").getAsInt() : 0;
        this.cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;
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
        if (shift || onlySelf) return Optional.of(SelfTarget.Instance);
        else if (!target.isSelf()) return target.castToPlayer().map(v -> v);
        else return Optional.empty();
    }
    @Override public boolean timeUse(Player player, IPlayerTarget target, ItemStack item) {
        applyNeed(player);
        return true;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("needs"), IJElement.anyList(IJElement.link(docs.need())),  IComment.text("Добавляет потребность при использовании")),
                JProperty.optional(IName.raw("needs_time"), IJElement.raw(10), IComment.text("Время активности потребности")),
                JProperty.optional(IName.raw("time"), IJElement.raw(10), IComment.text("Время использования предмета в тиках")),
                JProperty.optional(IName.raw("silent"), IJElement.bool(), IComment.text("Будет ли проигран звук ломания предмета")),
                JProperty.optional(IName.raw("cooldown"), IJElement.raw(10), IComment.text("Время между использованиями предмета в тиках")),
                JProperty.optional(IName.raw("only_self"), IJElement.bool(), IComment.text("Возможно ли применить на другого игрока")),
                JProperty.optional(IName.raw("prefix"), IJElement.raw("PREFIX TEXT")
                        .or(JObject.of(
                                JProperty.require(IName.raw("self"), IJElement.raw("PREFIX TEXT")),
                                JProperty.optional(IName.raw("target"), IJElement.raw("PREFIX TEXT")),
                                JProperty.optional(IName.raw("cooldown"), IJElement.raw("PREFIX TEXT"))
                        )), IComment.text("Отображаемый префикс перетаймеров использования"))
        ), IComment.text("Предмет добавляющий потребности при использовании. После использования возможен вызов ").append(IComment.link(docs.settingsLink(NextSetting.class))));
    }
}