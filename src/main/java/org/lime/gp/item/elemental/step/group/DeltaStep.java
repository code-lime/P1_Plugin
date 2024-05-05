package org.lime.gp.item.elemental.step.group;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.system.utils.MathUtils;

@Step(name = "delta")
public record DeltaStep(IStep<?> step, Vector delta, int count) implements IStep<DeltaStep> {
    public Transformation toRandom(Transformation location) {
        Vector offset = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).multiply(2).multiply(delta);
        return MathUtils.transform(location, new Transformation(new Vector3f((float) offset.getX(), (float) offset.getY(), (float) offset.getZ()), null, null, null));
    }
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        for (int i = 0; i < count; i++)
            step.execute(target, context, toRandom(location));
    }

    public DeltaStep parse(JsonObject json) {
        return new DeltaStep(
                IStep.parse(json.get("step")),
                MathUtils.getVector(json.get("delta").getAsString()),
                json.get("count").getAsInt()
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("delta"), IJElement.link(docs.vector()), IComment.text("Разброс вызова")),
                JProperty.require(IName.raw("count"), IJElement.raw(5), IComment.text("Количество вызовов")),
                JProperty.require(IName.raw("step"), IJElement.linkParent(), IComment.text("Вызываемый элемент"))
        ), IComment.text("Вызывает определенное количество с разбросом сдвига"));
    }
}
