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

import java.util.List;

@Step(name = "list")
public record ListStep(List<? extends IStep<?>> steps) implements IStep<ListStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        steps.forEach(step -> step.execute(target, context, location));
    }

    public ListStep parse(JsonObject json) {
        return new ListStep(
                json.getAsJsonArray("steps").asList().stream().map(IStep::parse).toList()
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("steps"), IJElement.anyList(IJElement.linkParent()), IComment.text("Набор вызываемых элементов"))
        ), IComment.text("Вызывает набор элементов"));
    }
}
