package org.lime.gp.module.worlds;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.lime.system.execute.Action1;

import java.util.Map;

public interface IWorldService {
    String load(String sourceName);
    default String unload(String sourceName) {
        return unload(sourceName, true);
    }
    String unload(String sourceName, boolean save);
    default String copy(String sourceName, String targetName) {
        return copy(sourceName, targetName, v -> {});
    }
    String copy(String sourceName, String targetName, Action1<String> callback);
    String delete(String sourceName);
    World rawWorld(String sourceName);
    //IWorldBorder border(String sourceName);

    Map<String, Boolean> list();

    default IWorldService sub(String prefix) {
        return new SubWorldService(prefix, this);
    }
}
