package org.lime.gp.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.gp.database.Methods;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.system;
import ru.deelter.mylinker.MyLinker;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class SPCoinDonate {
    public static final UUID Code_Lime = UUID.fromString("ce6e763f-a669-40eb-866d-019e6ddca12c");
    public static core.element create() {
        return core.element.create(SPCoinDonate.class)
                .addCommand("spcoin", v -> v
                        .withUsage("/spcoin convert [count] - Конвертировать SPCoin в Днатную валюту ГП (Трефы)")
                        .withCheck(_v -> _v instanceof Player)
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> Stream.concat(Stream.of("convert"), ((Player)sender).getUniqueId().equals(Code_Lime) ? Stream.of("modify") : Stream.empty()).toList();
                            case 2 -> List.of("[count]");
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> switch (args.length) {
                            case 2 -> switch (args[0]) {
                                case "convert" -> {
                                    UUID uuid = ((Player)sender).getUniqueId();
                                    ExtMethods.parseUnsignedInt(args[1])
                                            .filter(count -> count > 0)
                                            .ifPresentOrElse((count) -> convert(uuid, count, "Buy trefs in gp", state -> {
                                                if (state) {
                                                    lime.logToFile("spcoin", "[{time}] User '"+uuid+"' convert " + count + " SPCoin's");
                                                    Methods.addDonateSPCoin(uuid, count);
                                                }
                                                sender.sendMessage(Component.text("[SPCoin] ")
                                                        .color(NamedTextColor.GREEN)
                                                        .append(state
                                                                ? Component.text(count + " SPCoin's конвертировано успешно в " + count + "♣!").color(NamedTextColor.GREEN)
                                                                : Component.text("Ошибка конвертации! Не хватает баланса либо проблемы на стороне сервера!").color(NamedTextColor.RED)
                                                        ));
                                            }), () -> sender.sendMessage(Component.text("[SPCoin] ")
                                                    .color(NamedTextColor.GREEN)
                                                    .append(Component.text("Ошибка! '"+args[1]+"' - не число!").color(NamedTextColor.RED))));
                                    yield true;
                                }
                                case "modify" -> {
                                    UUID uuid = ((Player)sender).getUniqueId();
                                    if (!Code_Lime.equals(uuid)) yield false;
                                    ExtMethods.parseUnsignedInt(args[1])
                                            .ifPresentOrElse((count) -> convert(uuid, count, "Admin modify (test)", state -> sender.sendMessage(Component.text("[SPCoin] ")
                                                            .color(NamedTextColor.GREEN)
                                                            .append(state
                                                                    ? Component.text(count + " Изменение: " + count).color(NamedTextColor.GREEN)
                                                                    : Component.text("Ошибка! Проблемы на стороне сервера!").color(NamedTextColor.RED)
                                                            ))),
                                                    () -> sender.sendMessage(Component.text("[SPCoin] ")
                                                            .color(NamedTextColor.GREEN)
                                                            .append(Component.text("Ошибка! '"+args[1]+"' - не число!").color(NamedTextColor.RED)))
                                            );
                                    yield true;
                                }
                                default -> false;
                            };
                            default -> false;
                        })
                );
    }
    public static void convert(UUID uuid, int count, String message, system.Action1<Boolean> state) {
        MyLinker.getInstance()
                .getSocketClient()
                .subtractBalance(uuid, count, message)
                .queue(state::invoke);
    }
}

















