package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.JObject;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;

@Step(name = "none")
public final class NoneStep implements IStep<NoneStep> {
    public static final NoneStep Instance = new NoneStep();
    public static NoneStep instance() { return Instance; }

    private NoneStep() {}
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) { }
    @Override public NoneStep parse(JsonObject json) { return Instance; }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(), IComment.text("Ничего не делает"));
    }
}
