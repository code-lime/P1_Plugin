package org.lime.gp.item.elemental;

import com.caoccao.javet.values.reference.V8ValueObject;
import org.lime.gp.module.JavaScript;

import java.util.Map;

public class DataContext {
    private final V8ValueObject context = JavaScript.createNative();//.invoke("var tmp = {}; tmp", Collections.emptyMap()).orElseThrow();

    public void addContext(Map<String, Object> args) {
        args.put("context", context);
    }

    /*public void part() {

    }

    @Override public void close() {
        try {
            context.close();
        } catch (JavetException e) {
            throw new RuntimeException(e);
        }
    }*/
}
