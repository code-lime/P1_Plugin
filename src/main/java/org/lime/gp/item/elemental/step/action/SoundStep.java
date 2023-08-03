package org.lime.gp.item.elemental.step.action;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.sound.Sounds;

public record SoundStep(String sound, boolean self) implements IStep {
    @Override public void execute(Player player, Vector position) {
        if (self) Sounds.playSound(sound, player, position);
        else Sounds.playSound(sound, position.toLocation(player.getWorld()));
    }
}
