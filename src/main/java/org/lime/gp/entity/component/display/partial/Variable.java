package org.lime.gp.entity.component.display.partial;

import com.google.gson.JsonObject;
import org.lime.gp.lime;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import java.util.*;
import java.util.stream.Collectors;

public final class Variable {
    public final List<Toast2<String, List<String>>> values = new ArrayList<>();
    public final Partial partial;

    public Variable(int distanceChunk, JsonObject owner, JsonObject child) {
        child.entrySet().forEach(kv -> {
            if (kv.getKey().equals("result")) return;
            values.add(Toast.of(kv.getKey(), Collections.singletonList(kv.getValue().getAsString())));
        });
        partial = Partial.parse(distanceChunk, lime.combineJson(owner, child.get("result"), false).getAsJsonObject());
    }

    public Variable(Partial partial, List<Toast2<String, List<String>>> variable) {
        this.partial = partial;
        this.values.addAll(variable);
    }

    public boolean is(Map<String, String> values) {
        for (Toast2<String, List<String>> kv : this.values) {
            String str = values.getOrDefault(kv.val0, null);
            if (!kv.val1.contains(str)) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return values.stream().map(kv -> kv.val0 + "=" + String.join(",", kv.val1)).collect(Collectors.joining(","));
    }
}
