package org.lime.gp.module;

import com.google.gson.JsonPrimitive;
import org.bukkit.Bukkit;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;

public class MaxPlayers {
    public static int TO_MAX_PLAYERS = 0;
    public static CoreElement create() {
        return CoreElement.create(MaxPlayers.class)
                .<JsonPrimitive>addConfig("config", v -> v.withParent("max_players").withDefault(new JsonPrimitive(50)).withInvoke(j -> TO_MAX_PLAYERS = j.getAsInt()))
                .withInit(MaxPlayers::init);
    }
    public static void init() {
        lime.repeat(MaxPlayers::update, 1);
    }
    public static void update() {
        int max_players = Bukkit.getServer().getMaxPlayers();
        if (max_players == TO_MAX_PLAYERS) return;
        if (max_players > TO_MAX_PLAYERS) Bukkit.getServer().setMaxPlayers(TO_MAX_PLAYERS);
        else Bukkit.getServer().setMaxPlayers(max_players + 1);
    }
}
