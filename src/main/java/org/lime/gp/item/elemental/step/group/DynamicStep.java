package org.lime.gp.item.elemental.step.group;

import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.joml.Vector2f;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.module.TabManager;
import org.lime.json.JsonObjectOptional;
import org.lime.system.utils.MathUtils;

import java.util.HashMap;
import java.util.Map;

public record DynamicStep(String js, JsonObjectOptional args) implements IStep {
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

        JavaScript.getJsJson(js, args)
                .map(IStep::parse)
                .ifPresent(step -> step.execute(player, context, location));
    }
}
