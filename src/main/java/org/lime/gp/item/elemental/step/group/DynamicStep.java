package org.lime.gp.item.elemental.step.group;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.gp.item.settings.use.target.PlayerTarget;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.module.TabManager;
import org.lime.json.JsonObjectOptional;
import org.lime.system.utils.MathUtils;

import java.util.HashMap;
import java.util.Map;

@Step(name = "dynamic")
public record DynamicStep(String js, JsonObjectOptional args) implements IStep<DynamicStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        Map<String, Object> args = this.args.createObject();
        Map<String, Object> data = new HashMap<>();

        target.castToPlayer()
                .map(PlayerTarget::getPlayer)
                .ifPresent(player -> {
                    UserRow.getBy(player).ifPresent(row -> data.putAll(row.appendToReplace(new HashMap<>())));
                    data.put("uuid", player.getUniqueId().toString());
                    data.put("timed_id", TabManager.getPayerIDorDefault(player.getUniqueId(), -1));
                });
        Vector2f yawPitch = MathUtils.getYawPitch(location);
        data.put("yaw", yawPitch.x);
        data.put("pitch", yawPitch.y);
        Vector3f pos = location.getTranslation();
        data.put("pos_x", pos.x);
        data.put("pos_y", pos.y);
        data.put("pos_z", pos.z);
        data.put("world", target.getWorld().getName());

        args.put("data", data);
        context.addContext(args);

        JavaScript.getJsJson(js, args)
                .map(IStep::parse)
                .ifPresent(step -> step.execute(target, context, location));
    }

    public DynamicStep parse(JsonObject json) {
        return new DynamicStep(
                json.get("js").getAsString(),
                json.has("args")
                        ? JsonObjectOptional.of(json.getAsJsonObject("args"))
                        : new JsonObjectOptional()
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("js"), IJElement.link(docs.js()), IComment.text("Вызываемый JS код")),
                JProperty.optional(IName.raw("args"), IJElement.anyObject(
                        JProperty.require(IName.raw("KEY"), IJElement.link(docs.json()))
                ), IComment.text("Передаваемые аргументы"))
        ), IComment.text("Вызывает JS который должен возвращать JS объект являющимся ").append(IComment.linkParent()));
    }
}
