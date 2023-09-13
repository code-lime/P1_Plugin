package org.lime.gp.module;

import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.database.Methods;
import org.lime.gp.lime;

public class HasteDonate {
    public static CoreElement create() {
        return CoreElement.create(HasteDonate.class)
                .withInit(HasteDonate::init);
    }

    private static long endTime = 0;
    public static boolean isHaste() {
        long now = System.currentTimeMillis();
        return endTime > now;
    }

    private static void init() {
        lime.repeat(HasteDonate::update, 10);
    }
    private static void update() {
        Methods.hasteDonate(calendar -> {
            long now = System.currentTimeMillis();
            endTime = calendar == null ? 0 : calendar.getTimeInMillis();
            PotionEffect haste_effect;
            if (endTime > now) {
                int deltaTicks = (int)((endTime - now) / 50.0);
                haste_effect = PotionEffectType.FAST_DIGGING
                        .createEffect(deltaTicks, 0)
                        .withAmbient(false)
                        .withParticles(false);
            } else {
                haste_effect = null;
            }
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.removePotionEffect(PotionEffectType.FAST_DIGGING);
                if (haste_effect != null) player.addPotionEffect(haste_effect);
            });
        });
    }
}









