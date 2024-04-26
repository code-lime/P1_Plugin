package org.lime.gp.module.worlds;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;

import java.util.Map;
import java.util.stream.Collectors;

public class SubWorldService implements IWorldService {
    private final String prefix;
    private final IWorldService parent;

    public SubWorldService(String prefix, IWorldService parent) {
        this.prefix = prefix;
        this.parent = parent;
    }

    private String withPrefix(String sourceName) {
        return prefix + "/" + sourceName;
    }

    @Override public String load(String sourceName) {
        return parent.load(withPrefix(sourceName));
    }
    @Override public String unload(String sourceName, boolean save) {
        return parent.unload(withPrefix(sourceName), save);
    }
    @Override public String copy(String sourceName, String targetName, Action1<String> callback) {
        return parent.copy(withPrefix(sourceName), withPrefix(targetName), callback);
    }
    @Override public String delete(String sourceName) {
        return parent.delete(withPrefix(sourceName));
    }
    @Override public World rawWorld(String sourceName) {
        return parent.rawWorld(withPrefix(sourceName));
    }
    @Override public Map<String, Boolean> list() {
        String replacePrefix = prefix + "/";
        return parent.list()
                .entrySet()
                .stream()
                .filter(v -> v.getKey().startsWith(replacePrefix))
                .collect(Collectors.toMap(v -> v.getKey().substring(replacePrefix.length()), Map.Entry::getValue));
    }
}
