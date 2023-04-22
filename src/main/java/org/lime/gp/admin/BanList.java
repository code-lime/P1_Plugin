package org.lime.gp.admin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.lime.core;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.BanListRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.KeyedTable;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.system;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BanList implements Listener {
    public static core.element create() {
        return core.element.create(BanList.class)
                .withInstance()
                .withInit(BanList::init)
                .addCommand("ban", v -> v
                        .withUsage("/ban [user_name/user_uuid] [reason]")
                        .withCheck(CommandSender::isOp)
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> Bukkit.getOnlinePlayers().stream().flatMap(_v -> Stream.of(_v.getUniqueId().toString(), _v.getName())).toList();
                            default -> Collections.singletonList("reason");
                        })
                        .withExecutor((sender, args) -> switch (args.length) {
                            case 1 -> false;
                            default -> {
                                system.funcEx(UUID::fromString)
                                        .optional()
                                        .invoke(args[0])
                                        .ifPresentOrElse(uuid -> Methods.banUser(uuid,
                                                Stream.of(args).skip(1).collect(Collectors.joining(" ")),
                                                sender.getName(),
                                                () -> sender.sendMessage("User '"+uuid+"' banned with reason '"+Stream.of(args).skip(1).collect(Collectors.joining(" "))+"'")
                                        ), () -> Tables.USER_TABLE.getRowsBy(row -> row.userName.equalsIgnoreCase(args[0])).forEach(row -> Methods.banUser(row.uuid,
                                                Stream.of(args).skip(1).collect(Collectors.joining(" ")),
                                                sender.getName(),
                                                () -> sender.sendMessage("User '"+row.uuid+"'("+row.userName+") banned with reason '"+Stream.of(args).skip(1).collect(Collectors.joining(" "))+"'")
                                        )));
                                yield true;
                            }
                        })
                )
                .addCommand("ban-ip", v -> v
                        .withUsage("/ban-ip [user_name/user_uuid/ip] [reason]")
                        .withCheck(CommandSender::isOp)
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> Bukkit.getOnlinePlayers().stream().flatMap(_v -> Stream.of(_v.getUniqueId().toString(), _v.getName())).toList();
                            default -> Collections.singletonList("reason");
                        })
                        .withExecutor((sender, args) -> switch (args.length) {
                            case 1 -> false;
                            default -> {
                                system.<String, InetAddress>funcEx(InetAddress::getByName)
                                        .optional()
                                        .invoke(args[0])
                                        .ifPresentOrElse(ip -> Methods.banUser(ip,
                                                Stream.of(args).skip(1).collect(Collectors.joining(" ")),
                                                sender.getName(),
                                                () -> sender.sendMessage("IP '"+ip.getHostAddress()+"' banned with reason '"+Stream.of(args).skip(1).collect(Collectors.joining(" "))+"'")
                                        ), () -> Methods.ipListByUUIDs(
                                                system.funcEx(UUID::fromString)
                                                        .optional()
                                                        .invoke(args[0])
                                                        .map(Stream::of)
                                                        .orElseGet(() -> Tables.USER_TABLE.getRowsBy(row -> row.userName.equalsIgnoreCase(args[0]))
                                                                .stream()
                                                                .map(_v -> _v.uuid)
                                                        )
                                                        .collect(Collectors.toSet()),
                                                ips -> ips.forEach(ip -> Methods.banUser(ip,
                                                        Stream.of(args).skip(1).collect(Collectors.joining(" ")),
                                                        sender.getName(),
                                                        () -> sender.sendMessage("IP '"+ip.getHostAddress()+"' banned with reason '"+Stream.of(args).skip(1).collect(Collectors.joining(" "))+"'")
                                                ))
                                        ));
                                yield true;
                            }
                        })
                )
                .addCommand("pardon", v -> v
                        .withUsage("/pardon [user_name/user_uuid]")
                        .withCheck(CommandSender::isOp)
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> Tables.BANLIST_TABLE.getRows()
                                    .stream()
                                    .filter(_v -> _v.type == BanListRow.Type.UUID)
                                    .map(row -> UUID.fromString(row.user))
                                    .flatMap(uuid -> Stream.concat(Stream.of(uuid.toString()), UserRow.getBy(uuid).map(_v -> _v.userName).stream()))
                                    .toList();
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> {
                            system.funcEx(UUID::fromString)
                                    .optional()
                                    .invoke(args[0])
                                    .ifPresentOrElse(uuid -> Methods.pardonUser(uuid,
                                            () -> sender.sendMessage("User '"+uuid+"' unbanned")
                                    ), () -> Tables.USER_TABLE.getRowsBy(row -> row.userName.equalsIgnoreCase(args[0])).forEach(row -> Methods.pardonUser(row.uuid,
                                            () -> sender.sendMessage("User '"+row.uuid+"'("+row.userName+") unbanned")
                                    )));
                            return true;
                        })
                )
                .addCommand("pardon-ip", v -> v
                        .withUsage("/pardon-ip [user_name/user_uuid/ip]")
                        .withCheck(CommandSender::isOp)
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> Tables.BANLIST_TABLE.getRows()
                                    .stream()
                                    .filter(_v -> _v.type == BanListRow.Type.IP)
                                    .map(row -> row.user)
                                    .toList();
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> {
                            system.<String, InetAddress>funcEx(InetAddress::getByName)
                                    .optional()
                                    .invoke(args[0])
                                    .ifPresentOrElse(ip -> Methods.pardonUser(ip,
                                            () -> sender.sendMessage("IP '"+ip.getHostAddress()+"' unbanned")
                                    ), () -> Methods.ipListByUUIDs(
                                            system.funcEx(UUID::fromString)
                                                    .optional()
                                                    .invoke(args[0])
                                                    .map(Stream::of)
                                                    .orElseGet(() -> Tables.USER_TABLE.getRowsBy(row -> row.userName.equalsIgnoreCase(args[0]))
                                                            .stream()
                                                            .map(_v -> _v.uuid)
                                                    )
                                                    .collect(Collectors.toSet()),
                                            ips -> ips.forEach(ip -> Methods.pardonUser(ip,
                                                    () -> sender.sendMessage("IP '"+ip.getHostAddress()+"' unbanned")
                                            ))
                                    ));
                            return true;
                        })
                )
                .addCommand("banlist", v -> v
                        .withUsage("/banlist")
                        .withCheck(CommandSender::isOp)
                        .withTab("")
                        .withExecutor((sender, args) -> {
                            sender.sendMessage(Component.text("Список банов:"));
                            Tables.BANLIST_TABLE.getRows()
                                    .stream()
                                    .sorted(Comparator.comparing(_v -> _v.type))
                                    .map(row -> Component.text()
                                            .append(Component.text(" - " + row.displayName()).color(NamedTextColor.GRAY))
                                            .append(sender instanceof Player
                                                    ? Component.text()
                                                    .append(Component.text(" [").append(Component.text("?").color(NamedTextColor.YELLOW)).append(Component.text("]")))
                                                    .clickEvent(ClickEvent.copyToClipboard(
                                                            String.join("\n",
                                                                    row.user,
                                                                    "Тип: " + row.type.name(),
                                                                    "Дата: " + system.formatCalendar(row.createTime, true),
                                                                    "Выдал: " + row.owner,
                                                                    "Причина: " + row.reason
                                                            )
                                                    ))
                                                    .hoverEvent(HoverEvent.showText(Component.text("Информация о бане:")
                                                            .append(Component.text("\n Тип: ").color(NamedTextColor.GRAY).append(Component.text(row.type.name()).color(NamedTextColor.YELLOW)))
                                                            .append(Component.text("\n Дата: ").color(NamedTextColor.GRAY).append(Component.text(system.formatCalendar(row.createTime, true)).color(NamedTextColor.YELLOW)))
                                                            .append(Component.text("\n Выдал: ").color(NamedTextColor.GRAY).append(Component.text(row.owner).color(NamedTextColor.YELLOW)))
                                                            .append(Component.text("\n Причина: ").color(NamedTextColor.GRAY).append(Component.text(row.reason).color(NamedTextColor.YELLOW)))
                                                            .append(Component.text("\n\nНажмите чтобы скопировать...").color(NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC))
                                                    ))
                                                    : Component.empty()
                                            )
                                    ).forEach(sender::sendMessage);
                            return true;
                        })
                );
    }
    public static void init() {
        sync();
    }
    public static void onBanUpdate(BanListRow row, KeyedTable.Event event) {
        lime.once(BanList::sync, 1);
    }
    public static void sync() {
        Bukkit.getOnlinePlayers().forEach(player -> BanListRow.getBy(player).ifPresent(ban -> player.kick(Component.text(ban.reason), PlayerKickEvent.Cause.BANNED)));
    }
    @EventHandler public static void on(PlayerLoginEvent e) {
        Player player = e.getPlayer();
        BanListRow.getBy(player)
                .ifPresent(ban -> e.disallow(PlayerLoginEvent.Result.KICK_BANNED, Component.text(ban.reason)));
    }
}





















