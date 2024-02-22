package org.lime.gp.player.module;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.collect.Streams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.ReJoinRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.system.json;

import java.util.*;
import java.util.stream.Stream;

public class ReJoin implements Listener {
    public static CoreElement create() {
        return CoreElement.create(ReJoin.class)
                .withInit(ReJoin::init)
                .withInstance()
                .addCommand("rejoin", v -> v
                        .withUsage(String.join("\n",
                                "/rejoin create [name] - Создать аккаунт",
                                "/rejoin delete [identifier] - Удалить аккаунт",
                                "/rejoin info [identifier] - Информациа об аккаунте",
                                "/rejoin set [identifier] [uuid or user_name] - Установить владельца аккаунта",
                                "/rejoin rename [identifier] [name] - Переименовать аккаунт",
                                "/rejoin select [identifier] - Выбрать аккаунт",
                                "/rejoin skin set/reset - Сохранить текущий скин / Удалить сохраненный скин (работает только на выбранном аккаунте)"
                        ))
                        .withTab((s,a) -> switch (a.length) {
                            case 1 -> Streams.concat(
                                    s.isOp() ? Stream.of("skin", "create", "delete", "info", "set", "rename") : Stream.empty(),
                                    s instanceof Player ? Stream.of("select") : Stream.empty()
                                    ).toList();
                            case 2 -> switch (a[0]) {
                                case "skin" -> List.of("set", "reset");
                                case "create" -> s.isOp() ? Collections.singletonList("[name]") : Collections.emptyList();
                                case "delete", "info", "set", "rename" -> s.isOp()
                                        ? Tables.REJOIN_TABLE.getRows()
                                            .stream()
                                            .map(ReJoinRow::identifier)
                                            .toList()
                                        : Collections.emptyList();
                                case "select" -> s instanceof Player p
                                        ? Streams.concat(
                                                Tables.REJOIN_TABLE.getRows()
                                                    .stream()
                                                    .filter(_v -> p.getUniqueId().equals(_v.owner.orElse(null)))
                                                    .map(ReJoinRow::identifier),
                                                Stream.of("default")
                                        ).toList()
                                        : Collections.singletonList("default");
                                default -> Collections.emptyList();
                            };
                            case 3 -> switch (a[0]) {
                                case "set" -> s.isOp() ? Bukkit.getOnlinePlayers().stream().flatMap(_v -> Stream.of(_v.getName(), _v.getUniqueId().toString())).toList() : Collections.emptyList();
                                case "rename" -> s.isOp() ? Collections.singletonList("[name]") : Collections.emptyList();
                                default -> Collections.emptyList();
                            };
                            default -> Collections.emptyList();
                        })
                        .withExecutor((s, a) -> switch (a.length) {
                            case 2 -> switch (a[0]) {
                                case "skin" -> {
                                    if (!(s instanceof Player player)) yield false;
                                    UUID uuid = player.getUniqueId();
                                    yield Tables.REJOIN_TABLE
                                            .getBy(_v -> _v.genUUID().equals(uuid))
                                            .map(row -> switch (a[1]) {
                                                case "set" -> {
                                                    Methods.rejoinSkin(row.identifier(), Skins.getProperty(player).toJson().toString(), () -> s.sendMessage("Skin set!"));
                                                    yield true;
                                                }
                                                case "reset" -> {
                                                    Methods.rejoinSkin(row.identifier(), null, () -> s.sendMessage("Skin reset!"));
                                                    yield true;
                                                }
                                                default -> false;
                                            })
                                            .orElse(false);
                                }
                                case "create" -> {
                                    if (!s.isOp()) yield false;
                                    Methods.rejoinCreate(a[1], s instanceof Player p ? p.getUniqueId() : null, () -> s.sendMessage("Created!"));
                                    yield true;
                                }
                                case "delete" -> {
                                    if (!s.isOp()) yield false;
                                    Methods.rejoinDelete(a[1], () -> s.sendMessage("Deleted!"));
                                    yield true;
                                }
                                case "info" -> {
                                    if (!s.isOp()) yield false;
                                    Tables.REJOIN_TABLE.getBy(_v -> _v.identifier().equals(a[1])).ifPresentOrElse(row -> {
                                        s.sendMessage(Component.join(JoinConfiguration.newlines(),
                                                Component.text("ReJoin info:"),
                                                Component.text(" - Name: ")
                                                        .append(Component.text(row.name)
                                                                .color(NamedTextColor.YELLOW)
                                                                .clickEvent(ClickEvent.copyToClipboard(row.name))
                                                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                                        ),
                                                Component.text(" - Index: ")
                                                        .append(Component.text(row.index)
                                                                .color(NamedTextColor.YELLOW)
                                                                .clickEvent(ClickEvent.copyToClipboard(String.valueOf(row.index)))
                                                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                                        ),
                                                Component.text( " - Identifier: ")
                                                        .append(Component.text(row.identifier())
                                                                .color(NamedTextColor.YELLOW)
                                                                .clickEvent(ClickEvent.copyToClipboard(row.identifier()))
                                                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                                        ),
                                                Component.text( " - Owner: ")
                                                        .append(Component.text(row.owner.map(UUID::toString).orElse("NotSet"))
                                                                .color(NamedTextColor.YELLOW)
                                                                .clickEvent(ClickEvent.copyToClipboard(row.owner.map(UUID::toString).orElse("NotSet")))
                                                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                                        ),
                                                Component.text( " - Generic user UUID: ")
                                                        .append(Component.text(row.genUUID().toString())
                                                                .color(NamedTextColor.YELLOW)
                                                                .clickEvent(ClickEvent.copyToClipboard(row.genUUID().toString()))
                                                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                                        ),
                                                Component.text( " - Generic user Name: ")
                                                        .append(Component.text(row.genName())
                                                                .color(NamedTextColor.YELLOW)
                                                                .clickEvent(ClickEvent.copyToClipboard(row.genName()))
                                                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                                        )
                                        ));
                                    }, () -> s.sendMessage("ReJoin info of '"+a[1]+"' not founded"));
                                    yield true;
                                }
                                case "select" -> {
                                    if (!(s instanceof Player player)) yield false;
                                    UUID uuid = player.getUniqueId();
                                    if (a[1].equals("default")) {
                                        UUID ownerUUID = Tables.REJOIN_TABLE
                                                .getBy(_v -> _v.genUUID().equals(uuid))
                                                .flatMap(_v -> _v.owner)
                                                .or(() -> {
                                                    for (String tag : player.getScoreboardTags())
                                                        if (tag.startsWith("gen_owner:"))
                                                            return Optional.of(UUID.fromString(tag.substring(10)));
                                                    return Optional.empty();
                                                })
                                                .orElse(uuid);
                                        Methods.rejoinSelect(ownerUUID, null, () -> s.sendMessage("ReJoin selected"));
                                        yield true;
                                    }
                                    Tables.REJOIN_TABLE.getBy(_v -> uuid.equals(_v.owner.orElse(null)) && _v.identifier().equals(a[1]))
                                            .ifPresentOrElse(
                                                    row -> Methods.rejoinSelect(uuid, row.identifier(), () -> s.sendMessage("ReJoin selected")),
                                                    () -> s.sendMessage("ReJoin info of '"+a[1]+"' not founded"));
                                    yield true;
                                }
                                default -> false;
                            };
                            case 3 -> switch (a[0]) {
                                case "set" -> {
                                    if (!s.isOp()) yield false;
                                    ExtMethods.parseUUID(a[2])
                                            .or(() -> Optional.ofNullable(Bukkit.getPlayer(a[2])).map(Entity::getUniqueId))
                                            .ifPresentOrElse(
                                                    owner -> Methods.rejoinSet(a[1], owner, () -> s.sendMessage("Set!")),
                                                    () -> s.sendMessage("Player '"+a[2]+"' not founded")
                                            );
                                    yield true;
                                }
                                case "rename" -> {
                                    if (!s.isOp()) yield false;
                                    Methods.rejoinRename(a[1], a[2], () -> s.sendMessage("Renamed!"));
                                    yield true;
                                }
                                default -> false;
                            };
                            default -> false;
                        })
                );
    }

    private static boolean ENABLE_REJOIN = true;
    private static boolean DEBUG_REJOIN = false;
    private static void init() {
        AnyEvent.addEvent("rejoin.var", AnyEvent.type.owner_console, v -> v.createParam("debug", "enable").createParam(Boolean::parseBoolean, "true", "false"), (v, name, value) -> {
            switch (name) {
                case "enable" -> ENABLE_REJOIN = value;
                case "debug" -> DEBUG_REJOIN = value;
            }
        });
        PacketManager.adapter().add(ServerboundChatSessionUpdatePacket.class, (packet, event) -> {
            if (!ENABLE_REJOIN) return;
            if (DEBUG_REJOIN) lime.logOP("Cancel: " + packet.chatSession().sessionId());
            event.setCancelled(true);
        }).listen();
    }

    @EventHandler public static void on(AsyncPlayerPreLoginEvent e) {
        PlayerProfile profile = e.getPlayerProfile();
        UUID uuid = profile.getId();
        if (uuid == null) return;
        Tables.REJOIN_TABLE.getBy(v -> v.select && uuid.equals(v.owner.orElse(null))).ifPresent(row -> {
            CraftPlayerProfile newProfile = new CraftPlayerProfile(row.genUUID(), row.genName());
            newProfile.setProperties(profile.getProperties());
            newProfile.setProperty(new ProfileProperty("gen_owner", uuid.toString()));
            row.skin
                    .map(v -> new Skins.Property(json.parse(v).getAsJsonObject()))
                    .ifPresent(v -> Skins.setProfile(newProfile.getGameProfile(), v, false));
            e.setPlayerProfile(newProfile);
        });
    }
    @EventHandler public static void on(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        PlayerProfile profile = player.getPlayerProfile();
        Set<String> tags = player.getScoreboardTags();
        tags.removeIf(v -> v.startsWith("gen_owner:"));
        profile.getProperties().removeIf(prop -> {
            if (!prop.getName().equals("gen_owner")) return false;
            tags.add("gen_owner:"+prop.getValue());
            return true;
        });
    }
}
