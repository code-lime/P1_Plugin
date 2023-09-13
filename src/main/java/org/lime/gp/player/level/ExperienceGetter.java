package org.lime.gp.player.level;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.display.models.ExecutorJavaScript;
import org.lime.gp.lime;
import org.lime.system;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public record ExperienceGetter<TValue, TCompare>(boolean duplicate, ExperienceAction<TValue, TCompare> action, @Nullable String regex, TCompare compare, system.IRange range, ExecutorJavaScript js) {
    public double execute(UUID uuid, TValue value, double total) {
        double exp = range.getValue(total);
        js.execute(Map.of(
                "uuid", uuid.toString(),
                "action", action.key(),
                "regexValue", regex == null ? "" : regex,
                "realValue", action.debug(value),
                "total", total,
                "exp", exp
        ));
        return exp;
    }

    private static <TValue, TCompare>ExperienceGetter<TValue, TCompare> parse(boolean duplicate, ExperienceAction<TValue, TCompare> action, @Nullable String regex, JsonObject json) {
        return new ExperienceGetter<>(duplicate, action, regex,
                action.parse(regex),
                json.has("exp")
                        ? system.IRange.parse(json.get("exp").getAsString())
                        : new system.OnceRange(0),
                new ExecutorJavaScript("execute", json, lime._plugin.js())
        );
    }
    private static <TValue, TCompare>ExperienceGetter<TValue, TCompare> parse(boolean duplicate, ExperienceAction<TValue, TCompare> action, @Nullable String regex, JsonPrimitive json) {
        return new ExperienceGetter<>(duplicate, action, regex, action.parse(regex), system.IRange.parse(json.getAsString()), ExecutorJavaScript.empty());
    }
    public static <TValue, TCompare>ExperienceGetter<TValue, TCompare> parse(ExperienceAction<TValue, TCompare> action, @Nullable String regex, JsonElement json) {
        boolean duplicate = false;
        if (regex != null && regex.endsWith("#duplicate")) {
            regex = regex.substring(0, regex.length() - 10);
            duplicate = true;
        }
        return json.isJsonObject() ? ExperienceGetter.parse(duplicate, action, regex, json.getAsJsonObject()) : ExperienceGetter.parse(duplicate, action, regex, json.getAsJsonPrimitive());
    }
}
