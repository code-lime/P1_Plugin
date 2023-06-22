package org.lime.gp.block.component.display.partial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.lime.system;
import org.lime.gp.lime;

import com.google.gson.JsonObject;

public final class Variable {
    public final List<system.Toast2<String, List<String>>> values = new ArrayList<>();
    public final Partial partial;

    public Variable(int distanceChunk, JsonObject owner, JsonObject child) {
        child.entrySet().forEach(kv -> {
            if (kv.getKey().equals("result")) return;
            values.add(system.toast(kv.getKey(), Collections.singletonList(kv.getValue().getAsString())));
        });
        partial = Partial.parse(distanceChunk, lime.combineJson(owner, child.get("result"), false).getAsJsonObject());
    }
    public Variable(Partial partial, List<system.Toast2<String, List<String>>> variable) {
        this.partial = partial;
        this.values.addAll(variable);
    }
    public Variable(Partial partial) {
        this.partial = partial;
    }
    public Variable add(String key, String value) {
        this.values.add(system.toast(key, new ArrayList<>(List.of(value))));
        return this;
    }
    public boolean is(Map<String, String> values) {
        for (system.Toast2<String, List<String>> kv : this.values) {
            String str = values.getOrDefault(kv.val0, null);
            if (!kv.val1.contains(str)) return false;
        }
        return true;
    }
    @Override public String toString() {
        return values.stream().map(kv -> kv.val0+"="+String.join(",",kv.val1)).collect(Collectors.joining(","));
    }
}