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

@Step(name = "home")
public record HomeStep(IStep<?> step) implements IStep<HomeStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        step.execute(target, context, MathUtils.transformation(target.getLocation()));
    }

    public HomeStep parse(JsonObject json) {
        return new HomeStep(
                IStep.parse(json.get("step"))
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("step"), IJElement.linkParent(), IComment.text("Вызываемый элемент"))
        ), IComment.text("Вызывает элемент со сдвигом в координате вызывателя"));
    }
}
