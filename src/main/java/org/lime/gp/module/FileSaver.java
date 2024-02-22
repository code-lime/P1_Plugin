package org.lime.gp.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.json;
import org.lime.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileSaver {
    private static String url;
    private static String owner;
    private static String repo;
    private static String branch;
    private static Map<String, List<String>> files;

    public static CoreElement create() {
        return CoreElement.create(FileSaver.class)
                .withInit(FileSaver::init)
                .<JsonObject>addConfig("saver", v -> v
                        .withDefault(json.object()
                                .add("url", "URL")
                                .add("owner", "OWNER")
                                .add("repo", "REPO")
                                .add("branch", "BRANCH")
                                .build())
                        .withInvoke(_v -> {
                            url = _v.get("url").getAsString();
                            owner = _v.get("owner").getAsString();
                            repo = _v.get("repo").getAsString();
                            branch = _v.get("branch").getAsString();
                        }))
                .<JsonObject>addConfig("saver_files", v -> v
                        .withDefault(new JsonObject())
                        .withInvoke(_v -> files = lime.combineParent(_v, true, false).entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue()
                                        .getAsJsonArray()
                                        .asList()
                                        .stream()
                                        .map(JsonElement::getAsString)
                                        .toList()
                                ))));
    }
    private static void init() {
        AnyEvent.addEvent("saver.sync", AnyEvent.type.owner_console, v -> v.createParam(_v -> _v, () -> files.keySet()), (p, file) -> sync(file, files.get(file)));
    }

    private static void sync(String name, List<String> path) {
        lime.logOP("[Save] Execute save...");
        web.method.POST
                .create(url.replace("{name}", name), json.object()
                        .add("owner", owner)
                        .add("repo", repo)
                        .add("branch", branch)
                        .add("path", path)
                        .build()
                        .toString())
                .expectContinue(true)
                .headers(Map.of("Content-Type", "application/json"))
                .json()
                .map(JsonElement::getAsJsonObject)
                .executeAsync((logger, _code) -> {
                    if (logger.has("error")) {
                        lime.logOP("[Save] Error: " + logger.get("error").getAsString());
                        return;
                    }
                    lime.logOP("[Save] Status: " + logger.get("status").getAsString());
                });
    }
}
















