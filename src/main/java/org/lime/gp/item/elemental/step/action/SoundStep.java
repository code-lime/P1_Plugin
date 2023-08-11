package org.lime.gp.item.elemental.step.action;

import org.bukkit.entity.Player;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.sound.Sounds;

public record SoundStep(String sound, boolean self) implements IStep {
    @Override public void execute(Player player, LocalLocation location) {
        if (self) Sounds.playSound(sound, player, location.position());
        else Sounds.playSound(sound, location.position().toLocation(player.getWorld()));
    }
}
