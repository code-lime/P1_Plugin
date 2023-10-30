package org.lime.gp.item.elemental.step.group;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.module.TabManager;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record FunctionStep(IStep step, String js, JsonObjectOptional args) implements IStep {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        Map<String, Object> args = this.args.createObject();
        Map<String, Object> data = new HashMap<>();

        UserRow.getBy(player).ifPresent(row -> data.putAll(row.appendToReplace(new HashMap<>())));
        data.put("uuid", player.getUniqueId().toString());
        data.put("timed_id", TabManager.getPayerIDorDefault(player.getUniqueId(), -1));
        Vector2f yawPitch = MathUtils.getYawPitch(location);
        data.put("yaw", yawPitch.x);
        data.put("pitch", yawPitch.y);

        args.put("data", data);
        context.addContext(args);

        List<Transformation> points = new ArrayList<>();
        if (!JavaScript.getJsJson(js, args)
                .filter(JsonElement::isJsonArray)
                .map(JsonElement::getAsJsonArray)
                .map(items -> {
                    items.getAsJsonArray().forEach(item -> {
                        if (item.isJsonArray()) {
                            JsonArray array = item.getAsJsonArray();
                            points.add(MathUtils.transform(location, new Transformation(
                                    new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()),
                                    null,
                                    null,
                                    null
                            )));
                        } else {
                            points.add(MathUtils.transform(location, MathUtils.transformation(item)));
                        }
                    });
                    return true;
                })
                .orElse(false)
        )
            lime.logOP("Error execute JS '" + js + "'. Return format of execute not equals [TRANSFORMATION or [x,y,z],TRANSFORMATION or [x,y,z],...,TRANSFORMATION or [x,y,z]]");

        points.forEach(point -> step.execute(player, context, point));
    }
}
