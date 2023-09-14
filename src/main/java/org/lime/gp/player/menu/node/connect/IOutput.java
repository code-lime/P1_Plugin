package org.lime.gp.player.menu.node.connect;

import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class IOutput {
    public final String key;
    public final List<Toast2<Integer, String>> target;
    public IOutput(String key, List<Toast2<Integer, String>> target) {
        this.key = key;
        this.target = target;
    }
    public void setNext(Map<Integer, Map<String, Object>> data, Object value) {
        this.target.forEach(kv -> {
            Map<String, Object> variables = data.computeIfAbsent(kv.val0, k -> new HashMap<>());
            variables.put(kv.val1, value);
        });
    }
}
