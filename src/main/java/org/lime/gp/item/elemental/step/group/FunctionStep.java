package org.lime.gp.item.elemental.step.group;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record FunctionStep(IStep step, String js, JsonObjectOptional args) implements IStep {
    private @Nullable Vector tryConvert(Iterable<?> i) {
        double x = 0;
        double y = 0;
        double z = 0;
        int index = 0;
        for (Object item : i) {
            if (!(item instanceof Number num)) return null;
            switch (index) {
                case 0 -> x = num.doubleValue();
                case 1 -> y = num.doubleValue();
                case 2 -> z = num.doubleValue();
                default -> { return null; }
            }
            index++;
        }
        return new Vector(x,y,z);
    }
    @Override public void execute(Player player, Transformation location) {
        Map<String, Object> args = this.args.createObject();
        args.put("uuid", player.getUniqueId().toString());

        List<Transformation> points = new ArrayList<>();
        if (!JavaScript.invoke(js, args)
                .map(v -> v instanceof Iterable<?> i ? i : null)
                .map(i -> {
                    for (Object item : i) {
                        JsonElement element = json.by(item).build();
                        if (element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            points.add(location.compose(new Transformation(
                                    new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()),
                                    null,
                                    null,
                                    null
                            )));
                        } else {
                            points.add(location.compose(MathUtils.transformation(element)));
                        }
                    }
                    return true;
                })
                .orElse(false)
        )
            lime.logOP("Error execute JS '" + js + "'. Return format of execute not equals [TRANSFORMATION or [x,y,z],TRANSFORMATION or [x,y,z],...,TRANSFORMATION or [x,y,z]]");

        points.forEach(point -> step.execute(player, point));
    }
}
