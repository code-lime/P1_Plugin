package org.lime.gp.module;

import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.core;
import org.lime.gp.item.settings.list.BlockEyesSetting;
import org.lime.gp.lime;

public class BlockEyes {
    public static core.element create() {
        return core.element.create(BlockEyes.class)
                .withInit(BlockEyes::init);
    }

    private static void init() {
        lime.repeat(BlockEyes::update, 1);
    }
    private static final PotionEffect BLINDNESS_EFFECT = PotionEffectType.BLINDNESS.createEffect(40, 255)
            .withAmbient(false)
            .withParticles(false)
            .withIcon(false);
    private static final PotionEffect DARKNESS_EFFECT = PotionEffectType.DARKNESS.createEffect(40, 255)
            .withAmbient(false)
            .withParticles(false)
            .withIcon(false);
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!BlockEyesSetting.isBlock(player)) return;
            player.addPotionEffect(BLINDNESS_EFFECT);
            player.addPotionEffect(DARKNESS_EFFECT);
        });
    }
}
