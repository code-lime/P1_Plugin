package org.lime.gp.admin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.module.Discord;

import com.google.gson.JsonObject;

public class AutoRestart {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(AutoRestart.class)
                .withInit(AutoRestart::init)
                .<JsonObject>addConfig("autorestart", v -> v
                        .withDefault(system.json.object()
                                .add("alert_channel", 1082817786818080838L)
                                .add("alert_message", "Alert")
                                .add("min_tps", 4.9)
                                .addArray("commands", _v -> _v.add("restart.timed sec 5"))
                                .build()
                        )
                        .withInvoke(AutoRestart::config)
                );
    }
    
    private static boolean DONE = false;
    private static long ALERT_CHANNEL = 0L;
    private static String ALERT_MESSAGE = "Alert";
    private static double MIN_TPS = 1.0;
    private static final List<String> COMMANDS = new ArrayList<String>();

    public static void init() {
        lime.repeat(AutoRestart::update, 2);
    }
    public static void config(JsonObject json) {
        DONE = false;
        ALERT_CHANNEL = json.get("alert_channel").getAsLong();
        ALERT_MESSAGE = json.get("alert_message").getAsString();
        MIN_TPS = json.get("min_tps").getAsDouble();
        COMMANDS.clear();
        json.get("commands").getAsJsonArray().forEach(cmd -> COMMANDS.add(cmd.getAsString()));
    }
    public static void update() {
        if (DONE) return;
        double tps = Bukkit.getTPS()[0];
        if (tps >= MIN_TPS) return;
        DONE = true;
        Discord.sendMessageToChannel(ALERT_CHANNEL, ALERT_MESSAGE
            .replace("{min_tps}", system.getDouble(MIN_TPS))
            .replace("{tps}", system.getDouble(tps)));
        COMMANDS.forEach(cmd -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd
            .replace("{min_tps}", system.getDouble(MIN_TPS))
            .replace("{tps}", system.getDouble(tps))));
    }
}
