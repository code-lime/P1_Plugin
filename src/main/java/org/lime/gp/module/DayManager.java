package org.lime.gp.module;

import com.google.gson.JsonObject;

import java.util.Calendar;

import org.bukkit.GameRule;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.system;

public class DayManager {
    private static double timeSlownessMultiplier;
    private static int rts_rain;
    private static int rts_default;
    public static core.element create() {
        return core.element.create(DayManager.class)
                .<JsonObject>addConfig("config", v -> v
                        .withParent("time")
                        .withDefault(system.json.object()
                                .add("cycle", 40 * 60)
                                .build()
                        )
                        .withInvoke(json -> {
                            timeSlownessMultiplier = json.get("cycle").getAsInt() / 20;
                        })
                )
                .<JsonObject>addConfig("config", v -> v
                        .withParent("weather")
                        .withDefault(system.json.object()
                                .add("random_tick_speed_rain", 10)
                                .add("random_tick_speed_default", 3)
                                .build()
                        )
                        .withInvoke(json -> {
                            rts_rain = json.get("random_tick_speed_rain").getAsInt();
                            rts_default = json.get("random_tick_speed_default").getAsInt();
                        })
                )
                .withInit(DayManager::init);
    }
    private static boolean DDC;
    public static void init() {
        lime.MainWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        lime.LoginWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        DDC = false;
        lime.repeat(() -> {
            DDC = lime.MainWorld.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
            int rts_old = lime.MainWorld.getGameRuleValue(GameRule.RANDOM_TICK_SPEED);
            int rts_new = lime.MainWorld.isClearWeather() ? rts_default : rts_rain;
            if (rts_old != rts_new) lime.MainWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, rts_new);
        }, 1);
        lime.once(DayManager::next, 1);
    }
    private static void next() {
        if (!DDC) lime.MainWorld.setFullTime(lime.MainWorld.getFullTime() + 1L);
        lime.LoginWorld.setFullTime(lime.MainWorld.getFullTime());
        lime.once(DayManager::next, timeSlownessMultiplier / 20.0);
    }

    public static Calendar now() {
        double hours = (lime.MainWorld.getFullTime() + 6000) / 1000.0;
        Calendar calendar = new Calendar.Builder().setDate(1183, 1, 1).build();
        calendar.add(Calendar.SECOND, (int)(hours * 60 * 60));
        return calendar;
    }
}
