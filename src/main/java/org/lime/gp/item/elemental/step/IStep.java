package org.lime.gp.item.elemental.step;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.Items;
import org.lime.gp.item.elemental.step.action.*;
import org.lime.gp.item.elemental.step.group.*;
import org.lime.gp.item.elemental.step.wait.QueueStep;
import org.lime.gp.item.elemental.step.wait.WaitStep;
import org.lime.json.JsonObjectOptional;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.MathUtils;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public interface IStep {
    void execute(Player player, Transformation location);

    static IStep parse(JsonElement raw) {
        if (raw.isJsonNull()) return NoneStep.Instance;
        else if (raw.isJsonObject()) {
            JsonObject json = raw.getAsJsonObject();
            String type = json.get("type").getAsString();
            return switch (type) {
                case "delta" -> new DeltaStep(
                        parse(json.get("step")),
                        MathUtils.getVector(json.get("delta").getAsString()),
                        json.get("count").getAsInt()
                );
                case "function" -> new FunctionStep(
                        parse(json.get("step")),
                        json.get("js").getAsString(),
                        json.has("args")
                                ? JsonObjectOptional.of(json.getAsJsonObject("args"))
                                : new JsonObjectOptional()
                );
                case "list" -> new ListStep(
                        json.getAsJsonArray("steps").asList().stream().map(IStep::parse).toList()
                );
                case "offset" -> new OffsetStep(
                        parse(json.get("step")),
                        MathUtils.transformation(json.get("offset"))
                );
                case "palette" -> new PaletteStep(
                        json.getAsJsonObject("palette")
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, kv -> parse(kv.getValue()))),
                        json.getAsJsonArray("map")
                                .asList()
                                .stream()
                                .map(v -> v.getAsJsonArray()
                                        .asList()
                                        .stream()
                                        .map(JsonElement::getAsString)
                                        .toList()
                                )
                                .toList()
                );

                case "queue" -> new QueueStep(
                        json.getAsJsonArray("steps").asList().stream().map(IStep::parse).toList(),
                        json.get("sec").getAsDouble()
                );
                case "wait" -> new WaitStep(
                        IStep.parse(json.get("step")),
                        json.get("sec").getAsDouble()
                );

                case "block.fake" -> new FakeBlockStep(
                        Material.valueOf(json.get("material").getAsString()),
                        json.has("states")
                                ? Collections.emptyMap()
                                : json.getAsJsonObject("states")
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().getAsString())),
                        MathUtils.getVector(json.get("radius").getAsString()),
                        json.get("self").getAsBoolean()
                );
                case "none" -> NoneStep.Instance;
                case "other" -> new OtherStep(json.get("other").getAsString());
                case "potion" -> new PotionStep(
                        Items.parseEffect(json.getAsJsonObject("potion")),
                        MathUtils.getVector(json.get("radius").getAsString()),
                        json.get("self").getAsBoolean()
                );
                case "particle" -> new ParticleStep(
                        ParticleStep.parseParticle(json.get("particle").getAsJsonObject()),
                        MathUtils.getVector(json.get("radius").getAsString()),
                        json.get("self").getAsBoolean()
                );
                case "block.set" -> new SetBlockStep(
                        Material.valueOf(json.get("material").getAsString()),
                        json.has("states")
                                ? Collections.emptyMap()
                                : json.getAsJsonObject("states")
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().getAsString())),
                        json.get("force").getAsBoolean()
                );
                case "sound" -> new SoundStep(
                        json.get("sound").getAsString(),
                        json.get("self").getAsBoolean()
                );
                default -> throw new IllegalArgumentException("Not supported type of step: " + type);
            };
        } else if (raw.isJsonArray()) return new ListStep(raw.getAsJsonArray().asList().stream().map(IStep::parse).toList());
        else return new OtherStep(raw.getAsString());
    }
}