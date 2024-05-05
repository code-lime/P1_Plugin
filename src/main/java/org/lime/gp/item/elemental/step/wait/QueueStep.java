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

import java.util.List;

@Step(name = "queue")
public record QueueStep(List<? extends IStep<?>> steps, double sec) implements IStep<QueueStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        int sec = 0;
        for (IStep<?> step : steps) {
            lime.once(() -> step.execute(target, context, location), sec);
            sec += this.sec;
        }
    }

    public QueueStep parse(JsonObject json) {
        return new QueueStep(
                json.getAsJsonArray("steps").asList().stream().map(IStep::parse).toList(),
                json.get("sec").getAsDouble()
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("sec"), IJElement.raw(1.0), IComment.text("Время в секундах")),
                JProperty.require(IName.raw("steps"), IJElement.anyList(IJElement.linkParent()), IComment.text("Вызываемые элементы"))
        ), IComment.text("Вызывает элементы последовательно с промежутком в ").append(IComment.field("sec")));
    }
}
