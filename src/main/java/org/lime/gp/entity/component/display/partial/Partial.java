package org.lime.gp.entity.component.display.partial;

import com.google.gson.JsonObject;
import org.lime.gp.entity.component.display.partial.list.JoinPartial;
import org.lime.gp.entity.component.display.partial.list.ModelPartial;
import org.lime.gp.entity.component.display.partial.list.NonePartial;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Partial {
    private final UUID uuid;
    private final int distanceChunk;
    private final List<Variable> variables = new LinkedList<>();

    public UUID uuid() { return uuid; }
    public UUID unique() { return uuid; }
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

    public abstract PartialEnum type();

    public List<Partial> partials() {
        if (variables.size() == 0) return Collections.singletonList(this);
        List<Partial> partials = new LinkedList<>();
        partials.add(this);
        variables.forEach(variable -> partials.addAll(variable.partial.partials()));
        return partials;
    }

    public Partial partial(Map<String, String> values) {
        for (Variable variable : variables) {
            if (variable.is(values)) {
                return variable.partial.partial(values);
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return "[distance=" + distanceChunk + (variables.size() == 0 ? "" : ":") + variables.stream().map(Object::toString).collect(Collectors.joining(",")) + "]";
    }

    public static Partial parse(int distanceChunk, JsonObject json) {
        if (json.has("model")) return new ModelPartial(distanceChunk, json);
        else if (json.has("join")) return new JoinPartial(distanceChunk, json);
        else return new NonePartial(distanceChunk, json);
    }
}
