package org.lime.gp.item.elemental.step.action;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.gson.JsonObject;
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
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

public record ParticleStep(ParticleBuilder particle, Vector radius, boolean self) implements IStep {
    @Override public void execute(Player player, LocalLocation position) {
        Location location = position.position().toLocation(player.getWorld());
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
        config.getAsString("offset").map(system::getVector).ifPresent(v -> builder.offset(v.getX(), v.getY(), v.getZ()));
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
}
