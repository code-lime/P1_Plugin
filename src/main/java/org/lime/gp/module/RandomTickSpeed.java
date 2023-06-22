package org.lime.gp.module;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.system;

import java.util.regex.Pattern;

public class RandomTickSpeed {
    private static int value = 3;
    private static boolean fraction = false;
    public static core.element create() {
        return core.element.create(RandomTickSpeed.class)
                .withInit(RandomTickSpeed::init)
                .<JsonObject>addConfig("randomTickSpeed", v -> v
                        .withDefault(system.json.object()
                                .add("value", 3)
                                .add("fraction", false)
                                .build())
                        .withInvoke(json -> {
                            setValue(json.get("value").getAsInt(), json.get("fraction").getAsBoolean());
                        })
                );
    }

    private static void init() {
        AnyEvent.addEvent("randomTickSpeed", AnyEvent.type.owner_console, v -> v.createParam("[value:int]", "[1/value:int]"), (player, value) -> {
            String[] arr = value.split(Pattern.quote("/"));
            switch (arr.length) {
                case 1 -> {
                    setValue(Integer.parseInt(arr[0]), false);
                    return;
                }
                case 2 -> {
                    if ("1".equals(arr[0])) {
                        setValue(Integer.parseInt(arr[1]), true);
                        return;
                    }
                }
            }
            lime.logOP("Not supported `RTS` value: " + value);
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

    public static void setValue(int value, boolean fraction) {
        if (value == 0 && fraction) fraction = false;
        if (RandomTickSpeed.value == value && RandomTickSpeed.fraction == fraction) return;
        RandomTickSpeed.value = value;
        RandomTickSpeed.fraction = fraction;

        lime.writeAllConfig("randomTickSpeed", system.json.object()
                .add("value", value)
                .add("fraction", fraction)
                .build()
                .toString()
        );
    }
}
