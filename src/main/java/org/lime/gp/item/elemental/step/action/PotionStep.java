package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.Items;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.gp.item.settings.use.target.PlayerTarget;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;

@Step(name = "potion")
public record PotionStep(PotionEffect effect, Vector radius, boolean self) implements IStep<PotionStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        @Nullable Player player = target.castToPlayer().map(PlayerTarget::getPlayer).orElse(null);

        if (radius.isZero()) {
            if (!self || player == null) return;
            player.addPotionEffect(effect);
            return;
        }
        World world = target.getWorld();
        world.getNearbyPlayers(MathUtils.convert(location.getTranslation()).toLocation(world), radius.getX(), radius.getY(), radius.getZ()).forEach(other -> {
            if (!self && player == other) return;
            other.addPotionEffect(effect);
        });
    }

    public PotionStep parse(JsonObject json) {
        return new PotionStep(
                Items.parseEffect(json.getAsJsonObject("potion")),
                MathUtils.getVector(json.get("radius").getAsString()),
                json.get("self").getAsBoolean()
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("potion"), IJElement.link(docs.potionEffect()), IComment.text("Эффект")),
                JProperty.require(IName.raw("radius"), IJElement.link(docs.vector()), IComment.join(
                        IComment.text("Игроки, находящиеся в данном радиусе почувствуют эффект. Если радиус равен "),
                        IComment.raw("0 0 0"),
                        IComment.text(" то эффект примениется только на текущего игрока")
                )),
                JProperty.require(IName.raw("self"), IJElement.bool(), IComment.text("Применяется ли эффект на текущего игрока"))
        ), IComment.text("Вызывает новый элементаль с существующим контентом"));
    }
}
