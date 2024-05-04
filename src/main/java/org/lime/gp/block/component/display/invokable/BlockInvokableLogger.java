package org.lime.gp.block.component.display.invokable;

import net.minecraft.core.BlockPosition;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.json;

import java.util.concurrent.ConcurrentHashMap;

public class BlockInvokableLogger {
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> logs = new ConcurrentHashMap<>();

    public static CoreElement create() {
        return CoreElement.create(BlockInvokableLogger.class)
                .withInit(BlockInvokableLogger::init);
    }

    public static void log(String name, String key, BlockPosition position) {
        //logs.computeIfAbsent(name + "." + key, v -> new ConcurrentHashMap<>())
        //        .compute(position.toShortString(), (k,v) -> (v == null ? 0 : v) + 1);
        //if (position.equals(new BlockPosition(-473, 65, -149)))
        //    lime.logOP("BP: UPDATE: " + position);
    }

    private static void init() {
        AnyEvent.addEvent("invokable.logs", AnyEvent.type.owner_console, v -> v.createParam("get", "clear"), (v, a) -> {
            switch (a) {
                case "get" -> lime.writeAllConfig("data/invokable.logs", json.format(json.by(logs).build()));
                case "clear" -> logs.clear();
            }
        });
    }
}
