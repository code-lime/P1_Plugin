package org.lime.gp.player.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.lime.gp.database.Methods;
import org.lime.gp.module.ConfirmCommand;
import org.lime.gp.module.Discord;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action1;

import java.io.File;
import java.util.UUID;

public class ResetUser {
    public static CoreElement create() {
        return CoreElement.create(ResetUser.class)
                .addCommand("reset.user", v -> ConfirmCommand.setup(v, "сброс", ResetUser::resetUser));
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
                        Advancements.clearPlayerData(uuid);

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

















