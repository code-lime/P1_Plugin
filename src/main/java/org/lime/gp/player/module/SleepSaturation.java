package org.lime.gp.player.module;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.gp.admin.Administrator;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.admin.AnyEvent.type;
import org.lime.gp.lime;
import org.lime.gp.module.SingleModules;
import org.lime.gp.player.module.needs.NeedSystem;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.CustomUI.IType;
import org.lime.gp.player.ui.CustomUI.IUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.plugin.CoreElement;
import org.lime.system.json;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class SleepSaturation implements IUI {
    private static final SleepSaturation instance = new SleepSaturation();

    public static CoreElement create() {
        return CoreElement.create(SleepSaturation.class)
                .withInstance(instance)
                .withInit(SleepSaturation::init)
                .<JsonObject>addConfig("sleep_saturation", v -> v
                        .withDefault(json.object()
                                .add("enable", false)
                                .add("total_sec", 10 * 60)
                                .add("reset_sec", 2 * 60)
                                .add("color", "#FFFFFF")
                                .addObject("death", _v -> _v
                                        .add("color", "#0000FF")
                                        .add("ticks_begin", 0)
                                        .add("ticks_end", 100)
                                )
                                .build())
                        .withInvoke(SleepSaturation::config)
                );
    }
    
    private static void init() {
        AnyEvent.addEvent("sleep.saturation", type.other, v -> v.createParam(Double::parseDouble, "1", "[0.0..1.0]"), (player, value) -> {
            setValue(player.getPersistentDataContainer(), value);
        });
        lime.repeat(SleepSaturation::update, 1);
        CustomUI.addListener(instance);
    }
    private static boolean enable = false;
    private static double deltaTotalSec = 0;
    private static double deltaResetSec = 0;
    private static int ticksBegin = 0;
    private static int ticksEnd = 0;
    private static TextColor color = NamedTextColor.WHITE;
    private static TextColor deathColor = NamedTextColor.WHITE;
    private static void config(JsonObject json) {
        boolean enable = json.get("enable").getAsBoolean();
        double delta_total_sec = json.get("total_sec").getAsDouble();
        double delta_reset_sec = json.get("reset_sec").getAsDouble();
        TextColor color = TextColor.fromHexString(json.get("color").getAsString());
        if (delta_total_sec != 0) delta_total_sec = 1 / delta_total_sec;
        if (delta_reset_sec != 0) delta_reset_sec = 1 / delta_reset_sec;
        JsonObject death = json.getAsJsonObject("death");
        TextColor deathColor = TextColor.fromHexString(death.get("color").getAsString());
        ticksBegin = death.get("ticks_begin").getAsInt();
        ticksEnd = death.get("ticks_end").getAsInt();

        SleepSaturation.enable = enable;
        SleepSaturation.deltaTotalSec = delta_total_sec;
        SleepSaturation.deltaResetSec = delta_reset_sec;
        SleepSaturation.color = color;
        SleepSaturation.deathColor = deathColor;
    }
    private static final PotionEffect DARKNESS_EFFECT = PotionEffectType.DARKNESS.createEffect(40, 1)
        .withIcon(false)
        .withParticles(false);
    private static final PotionEffect SLOW_EFFECT = PotionEffectType.SLOW.createEffect(40, 1)
            .withIcon(false)
            .withParticles(false);
    private static final PotionEffect SLOW_DIGGING = PotionEffectType.SLOW_DIGGING.createEffect(40, 1)
            .withIcon(false)
            .withParticles(false);
    private static void update() {
        if (!enable) return;
        Bukkit.getOnlinePlayers().forEach(player -> {
            switch (player.getGameMode()) {
                case ADVENTURE:
                case SURVIVAL: break;
                default: return;
            }
            UUID uuid = player.getUniqueId();
            if (Administrator.inABan(uuid)) return;
            PersistentDataContainer data = player.getPersistentDataContainer();
            boolean isSleep = lime.isLay(player) && SingleModules.isInBed(uuid);
            if (modifyValue(data, isSleep ? deltaResetSec : -(deltaTotalSec * NeedSystem.getSleepMutate(player))) == 0) {
                player.addPotionEffect(DARKNESS_EFFECT);
                player.addPotionEffect(SLOW_EFFECT);
                player.addPotionEffect(SLOW_DIGGING);
            }
        });
    }

    private static final NamespacedKey SLEEP_TIME = new NamespacedKey(lime._plugin, "sleep_time");
    private static double getValue(PersistentDataContainer data) {
        return data.getOrDefault(SLEEP_TIME, PersistentDataType.DOUBLE, 1.0);
    }
    private static void setValue(PersistentDataContainer data, double value) {
        if (value < 0) value = 0;
        else if (value > 1) value = 1;
        data.set(SLEEP_TIME, PersistentDataType.DOUBLE, value);
    }
    private static double modifyValue(PersistentDataContainer data, double delta) {
        double value = data.getOrDefault(SLEEP_TIME, PersistentDataType.DOUBLE, 1.0);
        value += delta;
        if (value < 0) value = 0;
        else if (value > 1) value = 1;
        data.set(SLEEP_TIME, PersistentDataType.DOUBLE, value);
        return value;
    }

    public static void reset(Player player) {
        setValue(player.getPersistentDataContainer(), 1.0);
    }

    @Override public Collection<ImageBuilder> getUI(Player player) {
        if (!enable) return Collections.emptyList();
        switch (player.getGameMode()) {
            case ADVENTURE:
            case SURVIVAL: break;
            default: return Collections.emptyList();
        }
        double value = getValue(player.getPersistentDataContainer());
        ImageBuilder img = ImageBuilder.of(0xE630 + (int)Math.round(value * 182), 182);
        float deathDelta;
        if (ticksEnd == 0 || (deathDelta = DeathGame.getDelta(player)) <= 0) {
            img = img.withColor(color);
        } else {
            int ticks = Math.round((ticksEnd - ticksBegin) * deathDelta + ticksBegin);
            float halfTicks = ticks / 2f;

            float deltaTick = Math.abs((MinecraftServer.currentTick % ticks - halfTicks) / halfTicks);
            img = img.withColor(TextColor.lerp(deltaTick, color, deathColor));
        }
        return Collections.singletonList(img);
    }

    @Override public IType getType() { return IType.ACTIONBAR; }
}
