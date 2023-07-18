package org.lime.gp.module;

import com.google.gson.JsonObject;

import java.util.Calendar;

import org.bukkit.GameRule;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.system;

public class DayManager {
    private static double timeSlownessMultiplier;
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
                .withInit(DayManager::init);
    }
    private static boolean DDC;
    public static void init() {
        lime.MainWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        lime.LoginWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        DDC = false;
        lime.repeat(() -> DDC = lime.MainWorld.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE), 1);
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
