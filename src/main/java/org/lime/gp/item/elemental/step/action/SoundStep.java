package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.sound.Sounds;
import org.lime.system.utils.MathUtils;

@Step(name = "sound")
public record SoundStep(String sound, boolean self) implements IStep<SoundStep> {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        Vector point = MathUtils.convert(location.getTranslation());
        if (self) Sounds.playSound(sound, player, point);
        else Sounds.playSound(sound, point.toLocation(player.getWorld()));
    }

    public SoundStep parse(JsonObject json) {
        return new SoundStep(
                json.get("sound").getAsString(),
                json.get("self").getAsBoolean()
        );
    }
}
