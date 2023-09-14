package org.lime.gp.module;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import org.bukkit.GameRule;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.HashMap;
import java.util.regex.Pattern;

public class RandomTickSpeed {
    private static int value = 3;
    private static boolean fraction = false;

    public static CoreElement create() {
        return CoreElement.create(RandomTickSpeed.class)
                .withInit(RandomTickSpeed::init)
                .<JsonObject>addConfig("randomTickSpeed", v -> v
                        .withDefault(json.object()
                                .add("value", 3)
                                .add("fraction", false)
                                .build())
                        .withInvoke(json -> {
                            setValue(json.get("value").getAsInt(), json.get("fraction").getAsBoolean());
                        })
                );
    }


    private static void init() {
        /*
        int rts_old = lime.MainWorld.getGameRuleValue(GameRule.RANDOM_TICK_SPEED);
        int rts_new = lime.MainWorld.isClearWeather() ? rts_default : rts_rain;
        if (rts_old != rts_new) lime.MainWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, rts_new);
        */
        AnyEvent.addEvent("randomTickSpeed", AnyEvent.type.owner_console, player -> {
            if (fraction) lime.logOP("RandomTickSpeed value: 1/" + value + " (" + (1.0 / value) + ")");
            else lime.logOP("RandomTickSpeed value: " + value);
        });
        AnyEvent.addEvent("randomTickSpeed", AnyEvent.type.owner_console, v -> v.createParam("[round_value:double]", "[1/value:int]"), (player, value) -> {
            String[] arr = value.split(Pattern.quote("/"));
            switch (arr.length) {
                case 1 -> {
                    double rts = Double.parseDouble(arr[0]);
                    setRoundValue(rts);
                    if (fraction)
                        lime.logOP("Set randomTickSpeed value: 1/" + RandomTickSpeed.value + " (" + (1.0 / RandomTickSpeed.value) + ")");
                    else lime.logOP("Set randomTickSpeed value: " + RandomTickSpeed.value);
                    return;
                }
                case 2 -> {
                    if ("1".equals(arr[0])) {
                        int rts = Integer.parseInt(arr[1]);
                        setValue(rts, true);
                        if (rts == 0) lime.logOP("Set randomTickSpeed value: 0");
                        else lime.logOP("Set randomTickSpeed value: 1/" + rts + " (" + (1.0 / rts) + ")");
                        return;
                    }
                }
            }
            lime.logOP("Not supported randomTickSpeed value: " + value);
        });
        lime.repeatTicks(RandomTickSpeed::tick, 1);
    }

    private static int tickIndex = 0;
    private static void tick() {
        final int randomTickSpeedValue;
        if (fraction) {
            tickIndex++;
            if (tickIndex % value == 0) {
                randomTickSpeedValue = 1;
                tickIndex = 0;
            } else {
                randomTickSpeedValue = 0;
            }
        } else {
            tickIndex = 0;
            randomTickSpeedValue = value;
        }
        MinecraftServer.getServer()
                .getAllLevels()
                .forEach(level -> level.getGameRules()
                        .getRule(GameRules.RULE_RANDOMTICKING)
                        .set(randomTickSpeedValue, null)
                );
    }

    public static int getValue() { return value; }
    public static boolean isFraction() { return fraction; }
    public static double finalValue() { return fraction ? (1.0 / value) : value; }

    public static void setRoundValue(double value) {
        if (value >= 1 || value == 0) setValue((int)Math.round(value), false);
        else setValue((int)Math.round(1/value), true);
    }
    public static void setValue(int value, boolean fraction) {
        if (value == 0 && fraction) fraction = false;
        if (RandomTickSpeed.value == value && RandomTickSpeed.fraction == fraction) return;
        RandomTickSpeed.value = value;
        RandomTickSpeed.fraction = fraction;

        lime.writeAllConfig("randomTickSpeed", json.object()
                .add("value", value)
                .add("fraction", fraction)
                .build()
                .toString()
        );
    }
}
