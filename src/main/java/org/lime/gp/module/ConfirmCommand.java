package org.lime.gp.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.permissions.ServerOperator;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.plugin.CoreCommand;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Action2;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;
import org.lime.system.utils.RandomUtils;

import java.util.*;

public class ConfirmCommand {
    private static final HashMap<String, Toast2<UUID, Integer>> cacheValues = new HashMap<>();

    private static String getNextCacheValue() {
        String value;

        do value = "confirm#" + RandomUtils.rand(1000, 9999);
        while (cacheValues.containsKey(value));

        return value;
    }

    public static CoreCommand<?> setup(CoreCommand<?> command, String actionName, Action2<UUID, Action1<Component>> action) {
        String commandName = command.cmd;
        return command
                .withUsage("/"+commandName+" [uuid or user_name or dsid]")
                .withCheck(ServerOperator::isOp)
                .withTab((s,args) -> args.length == 1 ? List.of("[uuid]", "[user_name]", "[dsid]") : Collections.emptyList())
                .withExecutor((s,args) -> {
                    if (args.length != 1) return false;
                    String key = args[0];
                    if (key.startsWith("confirm#")) {
                        Toast2<UUID, Integer> cacheValue = cacheValues.remove(key);
                        if (cacheValue != null) {
                            action.invoke(cacheValue.val0, s::sendMessage);
                            return true;
                        }
                        s.sendMessage(Component.empty()
                                .append(Component.text("Номер подтверждения не найден, либо недействитетен"))
                                .color(NamedTextColor.RED));
                        return true;
                    }

                    Action1<UserRow> onUser = user -> {
                        String value = getNextCacheValue();
                        cacheValues.put(value, Toast.of(user.uuid, 60));
                        s.sendMessage(Component.empty()
                                .append(Component.text("Найден игрок:")
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
                                        .append(Component.text("Действие '"+actionName+"' загеристроровано в системе. Для подтверждения требуется написать команду ")
                                                .append(Component.text("/"+commandName+" " + value)
                                                        .color(NamedTextColor.AQUA)
                                                        .hoverEvent(HoverEvent.showText(Component.text("Нажмите для выполнения")
                                                                .color(NamedTextColor.GREEN)))
                                                        .clickEvent(ClickEvent.suggestCommand("/"+commandName+" " + value))
                                                )
                                                .append(Component.text(" в течении 10 минут"))
                                        )
                                )
                                .color(NamedTextColor.YELLOW));
                    };
                    Action0 onNotFound = () -> s.sendMessage(Component.empty()
                            .color(NamedTextColor.RED)
                            .append(Component.text("Игрок ")
                                    .append(Component.text(key).color(NamedTextColor.GOLD))
                                    .append(Component.text(" не найден!"))
                            ));

                    ExtMethods.parseUUID(key)
                            .flatMap(UserRow::getBy)
                            .or(() -> UserRow.getByName(key))
                            .ifPresentOrElse(onUser, () -> ExtMethods.parseLong(key)
                                    .ifPresentOrElse(dsid -> Methods.discordFind(dsid, uuid -> Optional.ofNullable(uuid)
                                            .flatMap(UserRow::getBy)
                                            .ifPresentOrElse(onUser, onNotFound)), onNotFound));
                    return true;
                });
    }

    public static CoreElement create() {
        return CoreElement.create(ConfirmCommand.class)
                .withInit(ConfirmCommand::init);
    }

    private static void init() {
        lime.repeat(ConfirmCommand::update, 10);
    }
    private static void update() {
        cacheValues.values().removeIf(kv -> {
            kv.val1--;
            return kv.val1 <= 0;
        });
    }
}
