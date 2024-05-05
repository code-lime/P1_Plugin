package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.util.Vector;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.gp.item.settings.use.target.PlayerTarget;
import org.lime.gp.sound.Sounds;
import org.lime.system.utils.MathUtils;

@Step(name = "sound")
public record SoundStep(String sound, boolean self) implements IStep<SoundStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        Vector point = MathUtils.convert(location.getTranslation());
        if (self) target.castToPlayer().map(PlayerTarget::getPlayer).ifPresent(player -> Sounds.playSound(sound, player, point));
        else Sounds.playSound(sound, point.toLocation(target.getWorld()));
    }

    public SoundStep parse(JsonObject json) {
        return new SoundStep(
                json.get("sound").getAsString(),
                json.get("self").getAsBoolean()
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("sound"), IJElement.link(docs.sound()), IComment.text("Звук")),
                JProperty.require(IName.raw("self"), IJElement.bool(), IComment.text("Воспроизводится ли звук только текущему игроку"))
        ), IComment.text("Воспроизводит звук"));
    }
}
