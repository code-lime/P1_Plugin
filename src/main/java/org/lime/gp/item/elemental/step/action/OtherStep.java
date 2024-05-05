package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Elemental;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;

@Step(name = "other")
public record OtherStep(String step) implements IStep<OtherStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        Elemental.execute(target, context, step, location);
    }

    public OtherStep parse(JsonObject json) {
        return new OtherStep(json.get("other").getAsString());
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("other"), IJElement.link(docs.elementalName()), IComment.text("Вызываемый элементаль"))
        ), IComment.text("Вызывает новый элементаль с существующим контентом"));
    }
}
