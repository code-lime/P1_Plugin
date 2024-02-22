package org.lime.gp.item.elemental.step.action;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.json.JsonObjectOptional;
import org.lime.system.utils.MathUtils;

@Step(name = "particle")
public record ParticleStep(ParticleBuilder particle, Vector radius, boolean self) implements IStep<ParticleStep> {
    @Override public void execute(Player player, DataContext context, Transformation position) {
        Location location = MathUtils.convert(position.getTranslation()).toLocation(player.getWorld());
        ParticleBuilder particle = this.particle.source(player).location(location);
        if (radius.isZero()) {
            if (!self) return;
            particle.receivers(player).spawn();
            return;
        }
        particle.receivers(location.getNearbyPlayers(radius.getX(), radius.getY(), radius.getZ())).spawn();
    }

    public static ParticleBuilder parseParticle(JsonObject json) {
        Particle particle = Particle.valueOf(json.get("type").getAsString());
        ParticleBuilder builder = particle.builder();
        JsonObjectOptional config = JsonObjectOptional.of(json);

        config.getAsInt("count").ifPresent(builder::count);
        config.getAsString("offset").map(MathUtils::getVector).ifPresent(v -> builder.offset(v.getX(), v.getY(), v.getZ()));
        config.getAsDouble("extra").ifPresent(builder::extra);
        config.getAsBoolean("force").ifPresent(builder::force);
        config.getAsJsonObject("data").map(data -> {
            Class<?> dataType = particle.getDataType();
            if (dataType == Void.class) return null;
            else if (dataType == Particle.DustOptions.class) return new Particle.DustOptions(data.getAsString("color")
                    .map(v -> TextColor.fromHexString("#" + v))
                    .map(v -> Color.fromRGB(v.value()))
                    .orElse(Color.WHITE),
                    data.getAsFloat("size").orElse(1.0f)
            );
            else if (dataType == ItemStack.class) {
                ItemStack item = new ItemStack(data.getAsString("type").map(Material::valueOf).orElse(Material.AIR));
                data.getAsInt("id").ifPresent(id -> {
                    ItemMeta meta = item.getItemMeta();
                    meta.setCustomModelData(id);
                    item.setItemMeta(meta);
                });
                return item;
            }
            else if (dataType == BlockData.class) return data.getAsString("type").map(Material::valueOf).orElse(Material.AIR).createBlockData();
            else if (dataType == Particle.DustTransition.class) return new Particle.DustTransition(data.getAsString("from_color")
                    .map(v -> TextColor.fromHexString("#" + v))
                    .map(v -> Color.fromRGB(v.value()))
                    .orElse(Color.WHITE),
                    data.getAsString("to_color")
                            .map(v -> TextColor.fromHexString("#" + v))
                            .map(v -> Color.fromRGB(v.value()))
                            .orElse(Color.WHITE),
                    data.getAsFloat("size").orElse(1.0f)
            );
            else if (dataType == Float.class) return data.getAsFloat("value").orElse(0.0f);
            else if (dataType == Integer.class) return data.getAsInt("value").orElse(0);
            else throw new IllegalArgumentException("Not supported particle type: " + dataType);
        }).ifPresent(builder::data);

        return builder;
    }
    public ParticleStep parse(JsonObject json) {
        return new ParticleStep(
                ParticleStep.parseParticle(json.get("particle").getAsJsonObject()),
                MathUtils.getVector(json.get("radius").getAsString()),
                json.get("self").getAsBoolean()
        );
    }
    /*@Override public JObject docs(IDocsLink docs) {
        return JObject.of(
                JProperty.require(IName.raw("material"), IJElement.link(docs.vanillaMaterial()), IComment.text("Тип блока")),
                JProperty.optional(IName.raw("states"), IJElement.anyObject(
                        JProperty.require(IName.raw("KEY"), IJElement.raw("VALUE"))
                ), IComment.text("Параметры блока")),
                JProperty.require(IName.raw("radius"), IJElement.link(docs.vector()), IComment.join(
                        IComment.text("Игроки, находящиеся в данном радиус увидят влияние. Если радиус равен "),
                        IComment.raw("0 0 0"),
                        IComment.text(" то влияние увидит только текущий игрок")
                )),
                JProperty.require(IName.raw("self"), IJElement.bool(), IComment.text("Видит ли текущий игрок влияние")),
                JProperty.require(IName.raw("undo_sec"), IJElement.raw(5.5), IComment.text("Время, через которое влияние пропадет")),
                JProperty.require(IName.raw("force"), IJElement.bool(), IComment.text("Влияет ли влияние на незаменяемые блоки (камень, земля)"))
        );
    }*/
}
