package org.lime.gp.item.elemental.step.action;

import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.sound.Sounds;
import org.lime.system.utils.MathUtils;

public record SoundStep(String sound, boolean self) implements IStep {
    @Override public void execute(Player player, Transformation location) {
        Vector point = MathUtils.convert(location.getTranslation());
        if (self) Sounds.playSound(sound, player, point);
        else Sounds.playSound(sound, point.toLocation(player.getWorld()));
    }
}
