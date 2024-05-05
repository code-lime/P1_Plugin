package org.lime.gp.item.elemental.step;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.lime.docs.IIndexGroup;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Elemental;
import org.lime.gp.item.elemental.step.action.NoneStep;
import org.lime.gp.item.elemental.step.action.OtherStep;
import org.lime.gp.item.elemental.step.group.ListStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.unsafe;

public interface IStep<T extends IStep<T>> {
    void execute(ILocationTarget target, DataContext context, Transformation location);
    T parse(JsonObject json);
    IIndexGroup docs(String index, IDocsLink docs);

    static IStep<?> parse(JsonElement raw) {
        if (raw.isJsonNull()) return NoneStep.instance();
        else if (raw.isJsonObject()) {
            JsonObject json = raw.getAsJsonObject();
            String type = json.get("type").getAsString();
            Class<? extends IStep<?>> stepClass = Elemental.stepsClasses.get(type);
            if (stepClass == null) throw new IllegalArgumentException("Not supported type of step: " + type);
            return unsafe.createInstance(stepClass).parse(json);
        } else if (raw.isJsonArray()) return new ListStep(raw.getAsJsonArray().asList().stream().map(IStep::parse).toList());
        else return new OtherStep(raw.getAsString());
    }
}