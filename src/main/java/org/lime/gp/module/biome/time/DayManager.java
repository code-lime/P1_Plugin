package org.lime.gp.module.biome.time;

import com.google.gson.JsonObject;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.system.json;

import java.util.Collection;
import java.util.Collections;

public class DayManager {
    private static double timeSlownessMultiplier;
    public static CoreElement create() {
        return CoreElement.create(DayManager.class)
                .<JsonObject>addConfig("config", v -> v
                        .withParent("time")
                        .withDefault(json.object()
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
        lime.nextTick(() -> CustomUI.addListener(new CustomUI.GUI(CustomUI.IType.ACTIONBAR) {
            @Override public Collection<ImageBuilder> getUI(Player player) {
                if (player.getInventory().getItemInMainHand().getType() != Material.CLOCK) return Collections.emptyList();
                DateTime now = now();
                return Collections.singleton(ImageBuilder.of(player, now.toFormat("dd.yyyySS HH:mm:ss"))
                        .withColor(switch (now.getSeasonIndex()) {
                            case 1 -> TextColor.color(0xF0F329);
                            case 2 -> TextColor.color(0xE7E7E7);
                            case 3 -> TextColor.color(0x31ABF9);
                            default -> null;
                        })
                );
            }
        }));
    }
    private static void next() {
        if (!DDC) lime.MainWorld.setFullTime(lime.MainWorld.getFullTime() + 1L);
        lime.LoginWorld.setFullTime(lime.MainWorld.getFullTime());
        lime.once(DayManager::next, timeSlownessMultiplier / 20.0);
    }

    public static DateTime now() {
        double hours = (lime.MainWorld.getFullTime() + 6000) / 1000.0;
        return DateTime.START_TIME.addHours(hours);
    }
}
