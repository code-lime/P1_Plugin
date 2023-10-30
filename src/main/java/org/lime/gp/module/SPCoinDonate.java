package org.lime.gp.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import net.minecraft.world.level.block.BlockFire;
import org.apache.http.client.utils.URIBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.extension.ExtMethods;

import com.google.gson.JsonObject;

import org.lime.gp.lime;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SPCoinDonate {
    public static final UUID Code_Lime = UUID.fromString("ce6e763f-a669-40eb-866d-019e6ddca12c");
    public static CoreElement create() {
        return CoreElement.create(SPCoinDonate.class)
                .<JsonObject>addConfig("spcoin", v -> v
                        .withDefault(json.object()
                                .add("api", "https://admingp.spworlds.org/spcoin")
                                .add("ws", "WS")
                                .add("token", "TOKEN")
                                .addNull("webhook_logs")
                                .build()
                        )
                        .withInvoke(SPCoinDonate::config)
                )
                .addCommand("spcoin", v -> v
                        .withUsage(String.join("\n",
                            "/spcoin convert [count] - Конвертировать SPCoin в донатную валюту ГП (Трефы)",
                            "/spcoin balance - Узнать текущий баланс SPCoin"
                        ))
                        .withCheckCast(Player.class)
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> List.of("convert", "balance");
                            case 2 -> args[0].equals("convert")
                                ? List.of("[count]")
                                : Collections.emptyList();
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> switch (args.length) {
                            case 1 -> switch (args[0]) {
                                case "balance" -> {
                                    UUID uuid = ((Player)sender).getUniqueId();
                                    balanceGet(uuid, balance -> sender.sendMessage(Component.text("[SPCoin] ")
                                        .color(NamedTextColor.GREEN)
                                        .append(balance
                                            .map(_balance -> Component.text("Количество SPCoin's для конвертации: " + _balance)
                                                .color(NamedTextColor.GREEN))
                                            .orElse(Component.text("Ошибка! Проблема на стороне сервера! Собщите в тикет!")
                                                .color(NamedTextColor.RED))
                                        ))
                                    );
                                    yield true;
                                }
                                default -> false;
                            };
                            case 2 -> switch (args[0]) {
                                case "convert" -> {
                                    UUID uuid = ((Player)sender).getUniqueId();
                                    ExtMethods.parseUnsignedInt(args[1])
                                            .filter(count -> count > 0)
                                            .ifPresentOrElse((count) -> balanceDel(uuid, "Buy trefs in gp", count, state -> {
                                                if (state) {
                                                    lime.logToFile("spcoin", "[{time}] User '"+uuid+"' convert " + count + " SPCoin's");
                                                    if (webhook_logs != null) {
                                                        try {
                                                            Discord.sendMessageToWebhook(webhook_logs, String.join("\n",
                                                                    "Аккаунт Mojang: **" + uuid + " (" + ((Player)sender).getName() + ")**",
                                                                    "Аккаунт GP: **" + UserRow.getBy(uuid).map(_v -> _v.firstName + " " + _v.lastName).orElse("Не зарегистрирован") + "**",
                                                                    "Сумма: **" + count + " RUB**"
                                                            ));
                                                        } catch (Throwable ignored) { }
                                                    }
                                                    Methods.addDonateSPCoin(uuid, count);
                                                    sender.sendMessage(Component.text("[SPCoin] ")
                                                        .color(NamedTextColor.GREEN)
                                                        .append(Component.text(count + " SPCoin's конвертировано успешно в " + count + "♣!").color(NamedTextColor.GREEN))
                                                    );
                                                } else {
                                                    balanceGet(uuid, balance -> sender.sendMessage(Component.text("[SPCoin] ")
                                                        .color(NamedTextColor.GREEN)
                                                        .append(Component.text("Ошибка конвертации! "
                                                            + balance
                                                                .map(_balance -> 
                                                                    "Для совершения данной операции пополните баланс на сайте на "
                                                                        + (count - _balance)
                                                                        + " СПКоинов. Текущий баланс: "
                                                                        + _balance)
                                                                .orElse(
                                                                    "Проблема на стороне сервера! Собщите в тикет!")
                                                        ).color(NamedTextColor.RED)))
                                                    );
                                                }
                                            }), () -> sender.sendMessage(Component.text("[SPCoin] ")
                                                    .color(NamedTextColor.GREEN)
                                                    .append(Component.text("Ошибка! '"+args[1]+"' - не число!").color(NamedTextColor.RED))));
                                    yield true;
                                }
                                default -> false;
                            };
                            default -> false;
                        })
                );
    }

    private static String apiBuilder(FuncEx1<URIBuilder, URIBuilder> setup, String... path) {
        try {
            URIBuilder builder = new URIBuilder(api);
            List<String> segments = builder.getPathSegments();
            segments.addAll(Arrays.asList(path));
            builder.setPathSegments(segments);
            return setup.invoke(builder).build().toURL().toString();
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    private static String api;
    private static String ws;
    private static String token;
    private static String webhook_logs;
    private static void config(JsonObject json) {
        api = json.get("api").getAsString();
        ws = json.get("ws").getAsString();
        token = json.get("token").getAsString();
        webhook_logs = json.has("webhook_logs") && !json.get("webhook_logs").isJsonNull() ? json.get("webhook_logs").getAsString() : null;
    }

    public static void balanceGet(UUID uuid, Action1<Optional<Integer>> state) {
        web.method.GET.create(apiBuilder(
                v -> v
                    .addParameter("uuid", uuid.toString()), 
                "proxy", "balance", "get"
            ))
            .header("ws", ws)
            .header("token", token)
            .expectContinue(true)
            .json()
            .executeAsync((data, code) -> {
                if (code != 200) {
                    lime.log("[BalanceGET] Error " + code + ": " + data);
                    state.invoke(Optional.empty());
                    return;
                }
                lime.log("[BalanceGET] OK " + code + ": " + data);
                state.invoke(Optional.of(data.getAsInt()));
            });
    }
    /*private static void balanceAdd(UUID uuid, String reason, int value, Action1<Boolean> state) {
        web.method.POST.create(apiBuilder(
                v -> v
                    .addParameter("uuid", uuid.toString())
                    .addParameter("reason", reason)
                    .addParameter("value", value + ""), 
                "proxy", "balance", "increase"
            ))
            .header("ws", ws)
            .header("token", token)
            .expectContinue(true)
            .json()
            .executeAsync((data, code) -> {
                if (code != 200) {
                    lime.log("[BalanceADD] Error " + code + ": " + data);
                    state.invoke(false);
                    return;
                }
                lime.log("[BalanceADD] OK " + code + ": " + data);
                state.invoke(data.getAsBoolean());
            });
    }*/
    private static void balanceDel(UUID uuid, String reason, int value, Action1<Boolean> state) {
        web.method.POST.create(apiBuilder(
                v -> v
                    .addParameter("uuid", uuid.toString())
                    .addParameter("reason", reason)
                    .addParameter("value", value + ""), 
                "proxy", "balance", "decrease"
            ))
            .header("ws", ws)
            .header("token", token)
            .expectContinue(true)
            .json()
            .executeAsync((data, code) -> {
                if (code != 200) {
                    lime.log("[BalanceDEL] Error " + code + ": " + data);
                    state.invoke(false);
                    return;
                }
                lime.log("[BalanceDEL] OK " + code + ": " + data);
                state.invoke(data.getAsBoolean());
            });
    }

    public static void convert(UUID uuid, int count, Action1<Integer> callback) {
        if (count <= 0) return;
        balanceDel(uuid, "Buy trefs in gp", count, state -> {
            if (state) {
                lime.logToFile("spcoin", "[{time}] User '"+uuid+"' convert " + count + " SPCoin's");
                if (webhook_logs != null) {
                    try {
                        Discord.sendMessageToWebhook(webhook_logs, String.join("\n",
                                "Аккаунт Mojang: **" + uuid + " (" + Optional.ofNullable(Bukkit.getOfflinePlayer(uuid)).map(OfflinePlayer::getName).orElse("NONE") + ")**",
                                "Аккаунт GP: **" + UserRow.getBy(uuid).map(_v -> _v.firstName + " " + _v.lastName).orElse("Не зарегистрирован") + "**",
                                "Сумма: **" + count + " RUB**"
                        ));
                    } catch (Throwable ignored) { }
                }
                Methods.addDonateSPCoin(uuid, count);
                callback.invoke(count);
            } else {
                balanceGet(uuid, balance -> callback.invoke(count - balance.orElse(0)));
            }
        });
    }
}

















