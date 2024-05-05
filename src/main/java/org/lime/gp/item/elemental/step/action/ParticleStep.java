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
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.gp.item.settings.use.target.PlayerTarget;
import org.lime.json.JsonObjectOptional;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;

@Step(name = "particle")
public record ParticleStep(ParticleBuilder particle, Vector radius, boolean self) implements IStep<ParticleStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation position) {
        Location location = MathUtils.convert(position.getTranslation()).toLocation(target.getWorld());
        @Nullable Player player = target.castToPlayer().map(PlayerTarget::getPlayer).orElse(null);

        ParticleBuilder particle = this.particle.source(player).location(location);
        if (radius.isZero()) {
            if (!self) return;
            particle.receivers(player).spawn();
            return;
        }
        particle.receivers(location.getNearbyPlayers(radius.getX(), radius.getY(), radius.getZ())
                .stream()
                .filter(v -> self || v != player)
                .toList()).spawn();
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
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup color = JsonGroup.of("COLOR", JObject.of(
                JProperty.optional(IName.raw("color"), IJElement.raw("#FFFFFF"), IComment.text("HEX цвет")),
                JProperty.optional(IName.raw("size"), IJElement.raw(1.0))
        ), IComment.text("Particle.DustOptions"));
        IIndexGroup item = JsonGroup.of("ITEM", JObject.of(
                JProperty.optional(IName.raw("type"), IJElement.link(docs.vanillaMaterial())),
                JProperty.optional(IName.raw("id"), IJElement.raw(10), IComment.text("CustomModelData"))
        ), IComment.text("ItemStack"));
        IIndexGroup block = JsonGroup.of("BLOCK", JObject.of(
                JProperty.optional(IName.raw("type"), IJElement.link(docs.vanillaMaterial()))
        ), IComment.text("BlockData"));
        IIndexGroup deltaColor = JsonGroup.of("DELTA_COLOR", JObject.of(
                JProperty.optional(IName.raw("from_color"), IJElement.raw("#FFFFFF"), IComment.text("HEX цвет")),
                JProperty.optional(IName.raw("to_color"), IJElement.raw("#FFFFFF"), IComment.text("HEX цвет")),
                JProperty.optional(IName.raw("size"), IJElement.raw(1.0))
        ), IComment.text("Particle.DustTransition"));
        IIndexGroup floatValue = JsonGroup.of("FLOAT", JObject.of(
                JProperty.require(IName.raw("value"), IJElement.raw(0.0))
        ), IComment.text("Float value"));
        IIndexGroup intValue = JsonGroup.of("INTEGER", JObject.of(
                JProperty.require(IName.raw("value"), IJElement.raw(0))
        ), IComment.text("Integer value"));

        IIndexGroup DATA = JsonEnumInfo.of("DATA")
                .add(IJElement.link(color))
                .add(IJElement.link(item))
                .add(IJElement.link(block))
                .add(IJElement.link(deltaColor))
                .add(IJElement.link(floatValue))
                .add(IJElement.link(intValue))
                .withChilds(color, item, block, deltaColor, floatValue, intValue);

        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("radius"), IJElement.link(docs.vector()), IComment.join(
                        IComment.text("Игроки, находящиеся в данном радиус увидят влияние. Если радиус равен "),
                        IComment.raw("0 0 0"),
                        IComment.text(" то влияние увидит только текущий игрок")
                )),
                JProperty.require(IName.raw("self"), IJElement.bool(), IComment.text("Видит ли текущий игрок частицы")),
                JProperty.require(IName.raw("particle"), JObject.of(
                        JProperty.require(IName.raw("type"), IJElement.link(docs.particleType()), IComment.text("Название частицы")),
                        JProperty.optional(IName.raw("count"), IJElement.raw(10), IComment.text("Количество частиц")),
                        JProperty.optional(IName.raw("offset"), IJElement.link(docs.vector()), IComment.text("Разброс частиц")),
                        JProperty.optional(IName.raw("extra"), IJElement.raw(2.0), IComment.text("Скорость частиц")),
                        JProperty.optional(IName.raw("force"), IJElement.bool(), IComment.text("Отобразить частицы дальше 32х блоков")),
                        JProperty.optional(IName.raw("data"), IJElement.link(DATA), IComment.text("Дополнительные параметры частиц"))
                ), IComment.text("Настройка частиц"))
        ), IComment.text("Вызывает новый элементаль с существующим контентом"))
                .withChilds(DATA);
    }
}
