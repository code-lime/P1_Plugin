package org.lime.gp.player.module.needs.thirst;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.TextColor;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;

public class StateColor {
    public final List<ImageBuilder> whole;
    public final List<ImageBuilder> half;
    public final List<ImageBuilder> empty;

    private static List<ImageBuilder> parsePart(JsonObject json, int offset) {
        int baseOffset = json.has("offset") ? json.get("offset").getAsInt() : offset;
        TextColor color = TextColor.fromHexString("#" + json.get("color").getAsString());
        List<ImageBuilder> list = new ArrayList<>();
        json.get("image").getAsJsonArray().forEach(v -> {
            String[] args = v.getAsString().split(":");
            int image = Integer.parseInt(args[0], 16);
            int size = Integer.parseInt(args[1]);
            list.add(ImageBuilder.of(image, size).withColor(color).addOffset(baseOffset));
        });
        return list;
    }

    public static StateColor parse(JsonObject json) {
        int offset = json.has("offset") ? json.get("offset").getAsInt() : 0;
        return new StateColor(
                parsePart(json.get("whole").getAsJsonObject(), offset),
                parsePart(json.get("half").getAsJsonObject(), offset),
                parsePart(json.get("empty").getAsJsonObject(), offset)
        );
    }

    public enum Type {
        Whole,
        Half,
        Empty
    }

    public ImageBuilder get(Type type, int level) {
        List<ImageBuilder> list;
        switch (type) {
            case Whole:
                list = whole;
                break;
            case Half:
                list = half;
                break;
            case Empty:
                list = empty;
                break;
            default:
                return ImageBuilder.empty;
        }
        return system.getOrDefault(list, level, ImageBuilder.empty);
    }

    public StateColor(List<ImageBuilder> whole, List<ImageBuilder> half, List<ImageBuilder> empty) {
        this.whole = whole;
        this.half = half;
        this.empty = empty;
    }
}
