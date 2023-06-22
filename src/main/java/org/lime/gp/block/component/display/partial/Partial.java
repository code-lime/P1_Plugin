package org.lime.gp.block.component.display.partial;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.lime.gp.block.component.display.partial.list.BlockPartial;
import org.lime.gp.block.component.display.partial.list.FramePartial;
import org.lime.gp.block.component.display.partial.list.ModelPartial;
import org.lime.gp.block.component.display.partial.list.NonePartial;
import org.lime.gp.block.component.display.partial.list.ViewPartial;

import com.google.gson.JsonObject;

public abstract class Partial {
    public final UUID uuid;
    public final int distanceChunk;
    public final List<Variable> variables = new LinkedList<>();

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
        if (json.has("model")) return new ModelPartial(distanceChunk, json);
        else if (json.has("view")) return new ViewPartial(distanceChunk, json);
        else if (json.has("item")) return new FramePartial(distanceChunk, json);
        else if (json.has("material")) return new BlockPartial(distanceChunk, json);
        else return new NonePartial(distanceChunk, json);
    }
}