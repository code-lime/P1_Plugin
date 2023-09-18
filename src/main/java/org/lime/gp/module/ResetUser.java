package org.lime.gp.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_20_R1.CraftOfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.ServerOperator;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.player.module.PlayerData;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.RandomUtils;

import java.io.File;
import java.util.*;

public class ResetUser {
    private static final HashMap<String, Toast2<UUID, Integer>> cacheValues = new HashMap<>();

    private static String getNextCacheValue() {
        String value;

        do value = "confirm#" + RandomUtils.rand(1000, 9999);
        while (cacheValues.containsKey(value));

        return value;
    }

    public static CoreElement create() {
        return CoreElement.create(ResetUser.class)
                .withInit(ResetUser::init)
                .addCommand("reset.user", v -> v
                        .withUsage("/reset.user [uuid or dsid]")
                        .withCheck(ServerOperator::isOp)
                        .withTab((s,args) -> args.length == 1 ? List.of("[uuid]", "[dsid]") : Collections.emptyList())
                        .withExecutor((s,args) -> {
                            if (args.length != 1) return false;
                            String key = args[0];
                            if (key.startsWith("confirm#")) {
                                Toast2<UUID, Integer> cacheValue = cacheValues.remove(key);
                                if (cacheValue != null) {
                                    resetUser(cacheValue.val0, s::sendMessage);
                                    return true;
                                }
                                s.sendMessage(Component.empty()
                                        .append(Component.text("Номер сброса не найден, либо недействитетен"))
                                        .color(NamedTextColor.RED));
                                return true;
                            }

                            Action1<UserRow> onUser = user -> {
                                String value = getNextCacheValue();
                                cacheValues.put(value, Toast.of(user.uuid, 60));
                                s.sendMessage(Component.empty()
                                        .append(Component.text("Найден игрок для сброса:")
                                                .append(Component.newline())
                                                .append(Component.text("  Аккаунт Mojang: ")
                                                        .append(Component.text(user.uuid.toString()).color(NamedTextColor.GOLD))
                                                        .append(Component.text(" (").color(NamedTextColor.GRAY))
                                                        .append(Component.text(user.userName).color(NamedTextColor.GOLD))
                                                        .append(Component.text(")").color(NamedTextColor.GRAY))
                                                )
                                                .append(Component.newline())
                                                .append(Component.text("  Аккаунт GP: ")
                                                        .append(Component.text(user.firstName + " " + user.lastName).color(NamedTextColor.GOLD))
                                                )
                                                .append(Component.newline())
                                                .append(Component.text("Сброс загеристророван в системе. Для подтверждения сброса требуется написать команду ")
                                                        .append(Component.text("/reset.user " + value)
                                                                .color(NamedTextColor.AQUA)
                                                                .hoverEvent(HoverEvent.showText(Component.text("Нажмите для выполнения")
                                                                        .color(NamedTextColor.GREEN)))
                                                                .clickEvent(ClickEvent.suggestCommand("/reset.user " + value))
                                                        )
                                                        .append(Component.text(" в течении 10 минут"))
                                                )
                                        )
                                        .color(NamedTextColor.YELLOW));
                            };
                            Action0 onNotFound = () -> {
                                s.sendMessage(Component.empty()
                                        .color(NamedTextColor.RED)
                                        .append(Component.text("Игрок ")
                                                .append(Component.text(key).color(NamedTextColor.GOLD))
                                                .append(Component.text(" не найден!"))
                                        ));
                            };

                            ExtMethods.parseUUID(key)
                                    .flatMap(UserRow::getBy)
                                    .ifPresentOrElse(onUser, () -> ExtMethods.parseLong(key)
                                            .ifPresentOrElse(dsid -> Methods.discordFind(dsid, uuid -> Optional.ofNullable(uuid)
                                                    .flatMap(UserRow::getBy)
                                                    .ifPresentOrElse(onUser, onNotFound)), onNotFound));
                            return true;
                        })
                );
    }

    private static void init() {
        lime.repeat(ResetUser::update, 10);
    }
    private static void update() {
        cacheValues.values().removeIf(kv -> {
            kv.val1--;
            return kv.val1 <= 0;
        });
    }

    private static void resetUser(UUID uuid, Action1<Component> callback) {
        Methods.SQL.Async.rawSqlOnce("SELECT users.id FROM users WHERE users.uuid = '"+uuid+"'", Integer.class, out_id -> {
            if (out_id == null) {
                callback.invoke(Component.empty()
                        .color(NamedTextColor.RED)
                        .append(Component.text("Игрок ")
                                .append(Component.text(uuid.toString()).color(NamedTextColor.GOLD))
                                .append(Component.text(" не найден!"))
                        ));
                return;
            }
            Methods.discordFind(uuid, dsid -> Methods.SQL.Async.rawSql(
                    "SELECT ResetUserByUUID('"+uuid+"')",
                    () -> Methods.SQL.Async.rawSqlOnce("SELECT users.id FROM users WHERE users.uuid = '"+uuid+"'", Integer.class, out2_id -> {
                        if (out2_id != null) {
                            callback.invoke(Component.empty()
                                    .color(NamedTextColor.RED)
                                    .append(Component.text("Произошла ошибка при сбросе игрока ")
                                            .append(Component.text(uuid.toString()).color(NamedTextColor.GOLD))
                                            .append(Component.text("! Аккаунт был сброшен частично!"))
                                    ));
                            return;
                        }

                        if (dsid != null) Discord.reset(dsid);

                        try {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                            if (player instanceof Player onlinePlayer) onlinePlayer.kick(Component.text("Ваш аккаунт был сброшен!"));
                            new File(MinecraftServer.getServer().playerDataStorage.getPlayerDir(), uuid + ".dat").delete();
                        } catch (Exception ignored) {}

                        PlayerData.clearPlayerData(uuid);

                        callback.invoke(Component.empty()
                                .color(NamedTextColor.GREEN)
                                .append(Component.text("Аккаунт игрока ")
                                        .append(Component.text(uuid.toString()).color(NamedTextColor.GOLD))
                                        .append(Component.text(" был успешно сброшен!"))
                                ));
                    })));
        });
    }
}

















