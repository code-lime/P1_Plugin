package org.lime.gp.player.module;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.JsonListEntry;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.WhiteList;
import net.minecraft.server.players.WhiteListEntry;
import org.apache.commons.lang.StringUtils;
import org.lime.core;
import org.lime.gp.database.Methods;
import org.lime.gp.database.Rows;
import org.lime.gp.database.Tables;
import org.lime.gp.lime;
import org.lime.system;
import org.lime.web;

import java.util.*;
import java.util.stream.Collectors;

public class PredonateWhitelist {
    public static core.element create() {
        return core.element.create(PredonateWhitelist.class)
                .disable()
                .withInit(PredonateWhitelist::init);
    }
    public static boolean sync = false;
    public static void init() {
        sync = true;
        lime.repeat(PredonateWhitelist::sync, 10);
    }
    private static final Set<String> ignored = new HashSet<>();
    private static final Set<String> appended = new HashSet<>();
    public static void sync() {
        if (!sync) return;
        Map<String, Boolean> sets = Tables.PREDONATE_TABLE.getRows()
                .stream()
                .filter(v -> v.whitelist == Rows.PreDonateRow.State.NONE)
                .filter(system.distinctBy(v -> v.name))
                .collect(Collectors.toMap(v -> v.name, v -> switch (v.type) {
                    case QIWI, TRADEMC -> Arrays.asList(300.0, 700.0, 1500.0, 4000.0, 10000.0).contains(v.amount);
                    case DIAKA -> Arrays.asList(190.0, 440.0, 940.0, 2500.0, 6200.0).contains(v.amount);
                }));

        WhiteList whiteList = getWhitelist();
        Set<String> exist = whiteList.getEntries().stream().map(JsonListEntry::getUser).map(GameProfile::getName).collect(Collectors.toSet());
        sets.forEach((set, good) -> {
            if (exist.contains(set)) return;
            if (!good) {
                ignored.add(set);
                return;
            }
            getByNameUUID(set, (uuid, name) -> {
                if (uuid == null) {
                    lime.logOP("[PreWhitelist] User '"+set+"' not founded!");
                    ignored.add(set);
                }
                else {
                    lime.logOP("[PreWhitelist] User '"+name+"' added to whitelist!");
                    whiteList.add(new WhiteListEntry(new GameProfile(uuid, name)));
                    appended.add(set);
                }
            });
        });
        lime.once(() -> {
            if (!ignored.isEmpty() || !appended.isEmpty()) {
                Methods.predonateWhitelist(appended, ignored);
                ignored.clear();
                appended.clear();
            }
        }, 2);
    }
    private static WhiteList getWhitelist() {
        return getPlayerList().getWhiteList();
    }
    private static PlayerList getPlayerList() {
        return MinecraftServer.getServer().getPlayerList();
    }
    public static void onUpdate(Rows.PreDonateRow row, Tables.KeyedTable.Event event) {
        sync();
    }
    public static void getByNameUUID(String user, system.Action2<UUID, String> callback) {
        try {
            String[] split = user.split("-");
            String uuid = split.length > 1 ? ("0".repeat(8) + "-" + "0".repeat(4) + "-" + "0".repeat(4) + "-" + "0".repeat(4)  + "-" + StringUtils.leftPad(split[split.length - 1], 12, '0')) : user;
            UUID _uuid = UUID.fromString(uuid);
            try { callback.invoke(_uuid, split.length > 1 ? user : null); }
            catch (Exception e) { lime.logStackTrace(e); }
            return;
        } catch (Throwable ignored) { }

        try {
            web.method.GET.create("https://api.mojang.com/users/profiles/minecraft/" + user).json().executeAsync((json, code) -> {
                try {
                    JsonObject _json = json.getAsJsonObject();
                    callback.invoke(
                            UUID.fromString(_json.get("id").getAsString().replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")),
                            _json.get("name").getAsString()
                    );
                } catch (Exception e) {
                    callback.invoke(null, null);
                }
            });
        } catch (Exception e) {
            callback.invoke(null, null);
        }
    }
}
