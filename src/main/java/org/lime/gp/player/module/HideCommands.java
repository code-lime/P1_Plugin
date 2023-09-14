package org.lime.gp.player.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;
import org.lime.system.json;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HideCommands implements Listener {
    public static CoreElement create() {
        return CoreElement.create(HideCommands.class)
                .withInstance()
                .<JsonObject>addConfig("commands", v -> v
                        .withInvoke(j -> lime.nextTick(() -> HideCommands.config(j)))
                        .withDefault(json.object().add("hide", new JsonObject()).add("remove", new JsonArray()).build())
                );
    }

    private static final List<String> hides = new ArrayList<>();
    public static void config(JsonObject _json) {
        JsonObject hide = _json.get("hide").getAsJsonObject();
        JsonArray remove = _json.get("remove").getAsJsonArray();

        Map<String, Command> commands = Bukkit.getCommandMap().getKnownCommands();
        Map<String, String[]> aliases = Bukkit.getServer().getCommandAliases();
        Map<String, Command> replace_command = Stream.of("ban", "ban-ip", "pardon", "pardon-ip", "banlist").collect(Collectors.toMap(cmd -> cmd, cmd -> commands.get("lime:" + cmd)));
        hides.clear();
        commands.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new))
                .forEach((command, cmd) -> {
                    Optional.ofNullable(replace_command.get(cmd.getName()))
                            .ifPresent(_cmd -> commands.put(command, _cmd));

                    if (!hide.has(command)) hide.add(command, new JsonPrimitive(false));
                    if (hide.get(command).getAsBoolean()) hides.add(command);

                    if (remove.contains(new JsonPrimitive(command))) {
                        hides.add(command);
                        cmd.unregister(Bukkit.getCommandMap());
                        aliases.remove(command);
                    }
                });
        lime.writeAllConfig("commands", json.format(_json));

        //MinecraftServer.getServer().vanillaCommandDispatcher.getDispatcher()
        /*lime.logOP(Bukkit.getServer().reloadCommandAliases()
                ? (ChatColor.GREEN + "Command aliases successfully reloaded.")
                : (ChatColor.RED + "An error occurred while trying to reload command aliases."));*/

    }

    @EventHandler public static void onCommand(PlayerCommandSendEvent e) {
        if (e.getPlayer().isOp()) return;
        e.getCommands().removeIf(hides::contains);
    }
}




























