package org.lime.gp.block.component.display.partial;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.component.display.partial.list.*;

import com.google.gson.JsonObject;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.toast.*;

public abstract class Partial {
    private final UUID uuid;
    private final int distanceChunk;
    private final List<Variable> variables = new LinkedList<>();

    public UUID uuid() { return uuid; }
    public int distanceChunk() { return distanceChunk; }
    public List<Variable> variables() { return variables; }

    public Partial(int distanceChunk, JsonObject json) {
        this.uuid = UUID.randomUUID();
        this.distanceChunk = Math.max(distanceChunk, 0);

        if (json.has("variable")) json.getAsJsonArray("variable").forEach(variable -> {
            JsonObject owner = json.deepCopy();
            owner.remove("variable");
            variables.add(new Variable(distanceChunk, owner, variable.getAsJsonObject()));
        });
    }

    public UUID unique() { return uuid; }

    public abstract PartialEnum type();
    public List<Partial> partials() {
        if (variables.size() == 0) return Collections.singletonList(this);
        List<Partial> partials = new LinkedList<>();
        partials.add(this);
        variables.forEach(variable -> partials.addAll(variable.partial.partials()));
        return partials;
    }
    public Partial addVariable(Variable variable) {
        variables.add(variable);
        return this;
    }
    public Partial partial(Map<String, String> values) {
        for (Variable variable : variables) {
            if (variable.is(values))
                return variable.partial.partial(values);
        }
        return this;
    }
    @Override public String toString() {
        return "[distance=" + distanceChunk + (variables.size() == 0 ? "" : ":") + variables.stream().map(Object::toString).collect(Collectors.joining(",")) + "]";
    }

    public static Partial parse(int distanceChunk, JsonObject json) {
        if (json.has("view")) return new ViewPartial(distanceChunk, json);
        else if (json.has("model")) return new ModelPartial(distanceChunk, json);
        else if (json.has("item")) return new FramePartial(distanceChunk, json);
        else if (json.has("material")) return new BlockPartial(distanceChunk, json);
        else return new NonePartial(distanceChunk, json);
    }

    public static JObject docs(IDocsLink docs, IIndexDocs variable) {
        return JObject.of(
                JProperty.optional(IName.raw("variable"), IJElement.anyList(IJElement.link(variable)), IComment.text("Список вариантов в зависимости от значений"))
        );
    }
    public static IIndexGroup allDocs(String title, IDocsLink docs) {
        Toast1<IIndexGroup> partial = Toast.of(null);
        IIndexGroup variable = JsonGroup.of("VARIABLE", Variable.docs(docs, IIndexDocs.remote(() -> partial.val0)));

        IIndexGroup viewPartial = JsonGroup.of("VIEW_PARTIAL", ViewPartial.docs(docs, variable));
        IIndexGroup modelPartial = JsonGroup.of("MODEL_PARTIAL", ModelPartial.docs(docs, variable));
        IIndexGroup framePartial = JsonGroup.of("FRAME_PARTIAL", FramePartial.docs(docs, variable));
        IIndexGroup blockPartial = JsonGroup.of("BLOCK_PARTIAL", BlockPartial.docs(docs, variable));
        IIndexGroup nonePartial = JsonGroup.of("NONE_PARTIAL", NonePartial.docs(docs, variable));

        return partial.val0 = JsonGroup.of(title, IJElement.or(
                IJElement.link(viewPartial),
                IJElement.link(modelPartial),
                IJElement.link(framePartial),
                IJElement.link(blockPartial),
                IJElement.link(nonePartial)
        )).withChilds(variable, viewPartial, modelPartial, framePartial, blockPartial, nonePartial);
    }
}










