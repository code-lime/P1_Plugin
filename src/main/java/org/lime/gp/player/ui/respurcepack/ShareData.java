package org.lime.gp.player.ui.respurcepack;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.lime.gp.block.Blocks;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.entity.Entities;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.list.RemoteExecuteSetting;
import org.lime.gp.lime;
import org.lime.system.json;
import org.lime.system.map;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;
import org.lime.web;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ShareData(
        String url,
        String urlLogs,
        String owner,
        String repo,
        String branch,
        Map<String, String> headers) {
    private static final String LOG_INDEX_TEMPLATE = "{INDEX}";

    public static ShareData parse(JsonObject json) {
        String url = json.get("url").getAsString();
        return new ShareData(
                url,
                url.replace("/share", "/logs/" + LOG_INDEX_TEMPLATE),
                json.get("owner").getAsString(),
                json.get("repo").getAsString(),
                json.get("branch").getAsString(),
                json.get("headers")
                        .getAsJsonObject()
                        .asMap()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().getAsString()))
        );
    }

    private static Stream<IRemoteExecute> executeElements() {
        return Streams.concat(
                        Items.creatorIDs.values()
                                .stream()
                                .map(v -> v instanceof ItemCreator c ? c : null)
                                .filter(Objects::nonNull)
                                .flatMap(v -> Items.getAll(RemoteExecuteSetting.class, v).stream()),
                        Blocks.creators.values()
                                .stream()
                                .flatMap(v -> v.components.values().stream())
                                .map(v -> v instanceof org.lime.gp.block.component.list.RemoteExecuteComponent c ? c : null),
                        Entities.creators.values()
                                .stream()
                                .flatMap(v -> v.components.values().stream())
                                .map(v -> v instanceof org.lime.gp.entity.component.list.RemoteExecuteComponent c ? c : null)
                )
                .filter(Objects::nonNull);
    }
    private static Stream<String> executeLines() {
        return executeElements().flatMap(IRemoteExecute::executeLines);
    }

    public String replace(String text) {
        return text
                .replace("{owner}", owner)
                .replace("{repo}", repo)
                .replace("{branch}", branch);
    }

    private static final Map<String, TextColor> levelColors = map.<String, TextColor>of()
            .add("trace", NamedTextColor.WHITE)
            .add("debug", NamedTextColor.WHITE)
            .add("information", NamedTextColor.WHITE)
            .add("warning", NamedTextColor.YELLOW)
            .add("error", NamedTextColor.RED)
            .add("critical", NamedTextColor.DARK_RED)
            .add("none", NamedTextColor.WHITE)
            .build();

    private static void logLoggerLine(String level, String line) {
        if (!line.isBlank()) {
            TextColor color = levelColors.getOrDefault(level, NamedTextColor.WHITE);
            if (line.startsWith("ADMIN:")) {
                Component component = Component.text("[Share]  - ").color(color).append(ChatHelper.formatComponent(line.substring(6)));
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (!player.isOp()) return;
                    player.sendMessage(component);
                });
            } else if (line.startsWith("CONSOLE:")) {
                Component component = Component.text("[Share]  - ").color(color).append(ChatHelper.formatComponent(line.substring(8)));
                Bukkit.getConsoleSender().sendMessage(component);
            } else {
                lime.logOP(Component.text("[Share]  - ").color(color).append(Component.text(line)));
            }
        }
    }
    public void share() {
        lime.logOP("[Share] Setup remote generator...");
        String jsonString = json.object()
                .add("owner", owner)
                .add("repo", repo)
                .add("branch", branch)
                .addArray("execute", _v -> _v.add(executeLines().iterator()))
                .build()
                .toString();
        lime.logOP("[Share] Execute remote generator...");
        web.method.POST
                .create(url, jsonString)
                .expectContinue(true)
                .headers(headers)
                .json()
                .map(JsonElement::getAsJsonObject)
                .executeAsync((logger, _code) -> {
                    String index = logger.get("index").getAsString();
                    Toast2<BukkitTask, Integer> task = Toast.of(null, 5);
                    lime.logOP("[Share] Remote generator logs:");
                    task.val0 = lime.repeat(() -> {
                        if (task.val1 <= 0) {
                            lime.logOP("[Share] Remote generator closed");
                            task.val0.cancel();
                            return;
                        }
                        task.val1--;
                        web.method.GET
                                .create(urlLogs.replace(LOG_INDEX_TEMPLATE, index))
                                .expectContinue(true)
                                .headers(headers)
                                .json()
                                .map(JsonElement::getAsJsonObject)
                                .executeAsync((log, __code) -> {
                                    log.getAsJsonArray("logs")
                                            .forEach(frameRaw -> {
                                                JsonObject frame = frameRaw.getAsJsonObject();
                                                logLoggerLine(frame.get("level").getAsString().toLowerCase(), frame.get("message").getAsString());
                                            });

                                    boolean isEnd = log.get("is_end").getAsBoolean();
                                    task.val1 = 5;
                                    if (isEnd) {
                                        task.val0.cancel();
                                        lime.logOP("[Share] Remote generator closed");
                                    }
                                });
                    }, 1);
                });
    }
}