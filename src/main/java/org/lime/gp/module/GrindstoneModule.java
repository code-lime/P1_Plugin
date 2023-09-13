package org.lime.gp.module;

import com.google.gson.JsonPrimitive;
import org.lime.core;
import org.lime.plugin.CoreElement;

public class GrindstoneModule {
    private static boolean grindstoneEnable = true;
    public static boolean isEnable() {
        return grindstoneEnable;
    }
    public static CoreElement create() {
        return CoreElement.create(GrindstoneModule.class)
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("grindstone")
                        .withDefault(new JsonPrimitive(grindstoneEnable))
                        .withInvoke(json -> grindstoneEnable = json.getAsBoolean())
                );
    }
}
