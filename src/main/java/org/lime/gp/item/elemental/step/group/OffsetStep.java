package org.lime.gp.item.elemental.step.group;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.system.utils.MathUtils;

@Step(name = "offset")
public record OffsetStep(IStep<?> step, Transformation offset, boolean onlyYaw) implements IStep<OffsetStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        if (onlyYaw) location = MathUtils.onlyYaw(location);
        location = MathUtils.transform(offset, location);
        step.execute(target, context, location);
    }

    public OffsetStep parse(JsonObject json) {
        return new OffsetStep(
                IStep.parse(json.get("step")),
                MathUtils.transformation(json.get("offset")),
                json.has("only_yaw") && json.get("only_yaw").getAsBoolean()
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("offset"), IJElement.link(docs.transform()), IComment.text("Сдвиг")),
                JProperty.optional(IName.raw("only_yaw"), IJElement.bool(), IComment.text("Оставляет поворот только по оси YAW")),
                JProperty.require(IName.raw("step"), IJElement.linkParent(), IComment.text("Вызываемый элемент"))
        ), IComment.text("Вызывает элемент со сдвигом"));
    }
}
