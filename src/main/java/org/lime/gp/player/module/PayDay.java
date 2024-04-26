package org.lime.gp.player.module;

import com.google.gson.JsonPrimitive;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.Methods;
import org.lime.gp.lime;

import java.util.HashMap;
import java.util.UUID;

public class PayDay {
    private static boolean ENABLE = true;

    public static CoreElement create() {
        return CoreElement.create(PayDay.class)
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("payday")
                        .withDefault(new JsonPrimitive(ENABLE))
                        .withInvoke(_v -> ENABLE = _v.getAsBoolean()))
                .withInit(PayDay::init);
    }

    private static final HashMap<UUID, Integer> playTime = new HashMap<>();
    public static void init() {
        AnyEvent.addEvent("payday.call", AnyEvent.type.owner, player -> doThis());
        long min = 60 - (System.currentTimeMillis() / 60000) % 60;
        lime.repeat(PayDay::doThis, min * 60, 60 * 60);
        lime.repeat(() -> Bukkit.getOnlinePlayers().forEach(player -> playTime.put(player.getUniqueId(), playTime.getOrDefault(player.getUniqueId(), 0) + 1)), 60);
    }
    public static void doThis() {
        if (!ENABLE)
            return;
        HashMap<UUID, Methods.PayDayInput> payDayInput = new HashMap<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (playTime.getOrDefault(player.getUniqueId(), 0) < 30) {
                LangMessages.Message.PayDay_Error.sendMessage(player);
                return;
            }
            payDayInput.put(player.getUniqueId(), Methods.PayDayInput.create());
        });
        Methods.payDay(payDayInput, out -> payDayInput.keySet().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            Methods.PayDayOutput _out = out.getOrDefault(uuid, null);
            if (_out != null) LangMessages.Message.PayDay_Done.sendMessage(player, Apply.of().add(_out.args()));
            else LangMessages.Message.PayDay_Error.sendMessage(player);
        }));
        playTime.clear();
    }
}






























