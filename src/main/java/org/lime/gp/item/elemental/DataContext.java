package org.lime.gp.item.elemental;

import org.lime.gp.module.JavaScript;

import java.util.Collections;
import java.util.Map;

public class DataContext {
    private final Object context = JavaScript.invoke("var tmp = {}; tmp", Collections.emptyMap()).orElseThrow();

    public void addContext(Map<String, Object> args) {
        args.put("context", context);
    }
}
