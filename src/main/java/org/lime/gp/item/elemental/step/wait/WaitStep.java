package org.lime.gp.item.elemental.step.wait;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.gp.lime;

@Step(name = "wait")
public record WaitStep(IStep<?> step, double sec) implements IStep<WaitStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        lime.once(() -> step.execute(target, context, location), sec);
    }

    public WaitStep parse(JsonObject json) {
        return new WaitStep(
                IStep.parse(json.get("step")),
                json.get("sec").getAsDouble()
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("sec"), IJElement.raw(1.0), IComment.text("Время в секундах")),
                JProperty.require(IName.raw("step"), IJElement.linkParent(), IComment.text("Вызываемый элемент"))
        ), IComment.text("Вызывает элемент через ").append(IComment.field("sec")));
    }
}
