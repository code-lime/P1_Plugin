package org.lime.gp.module;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.lime.core;
import org.lime.gp.lime;

public class CommandLogger implements Listener {
    public static core.element create() {
        return core.element.create(CommandLogger.class)
                .withInstance();
    }
    @EventHandler(ignoreCancelled = true) public static void on(PlayerCommandPreprocessEvent event) {
        lime.logToFile("commands", "[{time}] " + event.getPlayer().getUniqueId() + "(" + event.getPlayer().getName() + "):" + event.getMessage());
    }
    @EventHandler(ignoreCancelled = true) public static void on(ServerCommandEvent event) {
        if (event.getSender() instanceof BlockCommandSender) return;
        lime.logToFile("commands", "[{time}] " + event.getSender().getName() + ":" + event.getSender().getClass().getName() + ":" + event.getCommand());
    }
}
