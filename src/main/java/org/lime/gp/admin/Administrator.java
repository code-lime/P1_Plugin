package org.lime.gp.admin;

import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.core;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Methods;
import org.lime.gp.database.Rows;
import org.lime.gp.database.Tables;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.module.*;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.voice.Voice;
import org.lime.system;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.Thirst;
import org.lime.gp.chat.ChatHelper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Administrator implements Listener {
    public static Location center;
    public static double distance;
    public enum Permissions {
        ABAN("lime.admin.aban"),
        AWARN("lime.admin.awarn"),
        ATP("lime.admin.atp"),
        ATP_TO("lime.admin.atp.to"),
        AGM("lime.admin.agm"),
        ATARGET("lime.admin.target"),
        AMUTE("lime.admin.amute"),
        IGNORE_FULL("lime.admin.ignore_full");

        public boolean check(Permissible permissible) {
            return permissible.hasPermission(permission());
        }
        public boolean checkOffline(UUID uuid) {
            return Tables.PERMISSIONS_TABLE.get(uuid.toString()).filter(v -> v.permissions.contains(permission())).isPresent();
        }

        private final String perm;
        public String permission() {
            return perm;
        }

        Permissions(String perm) {
            this.perm = perm;
        }
    }
    public static final ConcurrentLinkedQueue<system.Toast3<Double, Location, String>> rooms = new ConcurrentLinkedQueue<>();
    public static final ConcurrentHashMap<UUID, Integer> target_list = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Location> back_locations = new ConcurrentHashMap<>();

    public static core.element create() {
        return core.element.create(Administrator.class)
                .withInstance()
                .<JsonObject>addConfig("config", v -> v
                        .withParent("aban")
                        .withDefault(system.json.object()
                                .add("center", "0 10 0")
                                .add("distance", 5.0)
                                .add("world", 0)
                                .addObject("rooms", _v -> _v
                                        .addObject("0 10 0", __v -> __v
                                                .add("distance", 5.0)
                                                .add("world", 0)
                                                .add("name", "Комната")
                                        )
                                )
                                .build()
                        )
                        .withInvoke(json -> {
                            distance = json.get("distance").getAsDouble();
                            center = system.getLocation(Bukkit.getWorlds().get(json.get("world").getAsInt()), json.get("center").getAsString());
                            List<system.Toast3<Double, Location, String>> list = new ArrayList<>();
                            json.get("rooms").getAsJsonObject().entrySet().forEach(kv -> {
                                JsonObject _json = kv.getValue().getAsJsonObject();
                                list.add(system.toast(
                                        _json.get("distance").getAsDouble(),
                                        system.getLocation(Bukkit.getWorlds().get(_json.get("world").getAsInt()), kv.getKey()),
                                        _json.has("name") && !_json.get("name").isJsonNull() ? _json.get("name").getAsString() : null
                                ));
                            });
                            rooms.clear();
                            rooms.addAll(list);
                        })
                )
                .addCommand("aban", _v -> _v
                        .withCheck(v -> v.isOp() || Permissions.ABAN.check(v))
                        .withUsage("/aban [id_or_nick] [time_or_null] {reason}")
                        .withTab((s,args) -> switch (args.length) {
                            case 1 -> Stream.concat(
                                    TabManager.getPlayers().keySet().stream().map(String::valueOf),
                                    Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            ).collect(Collectors.toList());
                            case 2 -> Collections.singletonList("[time_or_null]");
                            default -> Collections.singletonList("{reason}");
                        })
                        .withExecutor((s,args) -> {
                            if (args.length < 2) return false;
                            UUID uuid = TabManager.getUUIDs().getOrDefault(args[0], null);
                            if (uuid == null) {
                                OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(args[0]);
                                uuid = player == null ? null : player.getUniqueId();
                            }
                            if (uuid == null) {
                                s.sendMessage("User '"+args[0]+"' not founded!");
                                return true;
                            }
                            Integer time = "null".equals(args[1]) ? null : system.formattedTime(args[1]);
                            String reason = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
                            String userDisplay = Rows.UserRow.getBy(uuid)
                                    .map(row -> row.firstName + " " + row.lastName + " (" + row.userName + ")")
                                    .orElseGet(uuid::toString);
                            Methods.aBanAdd(uuid, reason, time, s instanceof Player sp ? sp.getUniqueId() : null,
                                    () -> s.sendMessage("User '"+ userDisplay +"' was sent to aban for " + (time == null ? "∞" : system.formatTotalTime(time, system.FormatTime.HOUR_TIME)) + " with reason: " + reason));
                            return true;
                        })
                )
                .addCommand("awarn", _v -> _v
                        .withCheck(v -> v.isOp() || Permissions.AWARN.check(v))
                        .withUsage("/awarn [id_or_nick] {reason}\n/awarn list [id_or_nick]")
                        .withTab((s,args) -> switch (args.length) {
                            case 1 -> Stream.concat(Stream.of("list"), Stream.concat(
                                    TabManager.getPlayers().keySet().stream().map(String::valueOf),
                                    Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            )).collect(Collectors.toList());
                            default -> args[0].equals("list")
                                    ? Stream.concat(
                                            TabManager.getPlayers().keySet().stream().map(String::valueOf),
                                            Bukkit.getOnlinePlayers().stream().map(Player::getName)
                                    ).collect(Collectors.toList())
                                    : Collections.singletonList("{reason}");
                        })
                        .withExecutor((s,args) -> {
                            if (args.length < 2) return false;
                            boolean list = args[0].equals("list");
                            String find_user = args[list ? 1 : 0];
                            UUID uuid = TabManager.getUUIDs().getOrDefault(find_user, null);
                            if (uuid == null) {
                                OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(find_user);
                                uuid = player == null ? null : player.getUniqueId();
                            }
                            if (uuid == null) {
                                s.sendMessage("User '"+find_user+"' not founded!");
                                return true;
                            }
                            system.Action1<List<Methods.WarnInfo>> callback_list = warns -> s.sendMessage(Component
                                    .text("Список варнов игрока ("+warns.size()+" шт.):\n")
                                    .append(Component.join(JoinConfiguration.separator(Component.text("\n")), warns.stream().map(v -> v.toLine(" - ")).toList()))
                            );
                            if (list) {
                                Methods.aWarnList(uuid, callback_list);
                                return true;
                            }
                            String reason = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
                            s.sendMessage("Варн выдан!");
                            Methods.aWarnAdd(uuid, reason, s instanceof Player sp ? sp.getUniqueId() : null, callback_list);
                            return true;
                        })
                )
                .addCommand("amute", _v -> _v
                        .withCheck(v -> v.isOp() || Permissions.AMUTE.check(v))
                        .withUsage("/amute [id_or_nick] [time_or_null] {reason}")
                        .withTab((s,args) -> switch (args.length) {
                            case 1 -> Stream.concat(
                                    TabManager.getPlayers().keySet().stream().map(String::valueOf),
                                    Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            ).collect(Collectors.toList());
                            case 2 -> Collections.singletonList("[time_or_null]");
                            default -> Collections.singletonList("{reason}");
                        })
                        .withExecutor((s,args) -> {
                            if (args.length < 2) return false;
                            UUID uuid = TabManager.getUUIDs().getOrDefault(args[0], null);
                            if (uuid == null) {
                                OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(args[0]);
                                uuid = player == null ? null : player.getUniqueId();
                            }
                            if (uuid == null) {
                                s.sendMessage("User '"+args[0]+"' not founded!");
                                return true;
                            }
                            Integer time = "null".equals(args[1]) ? null : system.formattedTime(args[1]);
                            String reason = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
                            String userDisplay = Rows.UserRow.getBy(uuid)
                                    .map(row -> row.firstName + " " + row.lastName + " (" + row.userName + ")")
                                    .orElseGet(uuid::toString);
                            Methods.aMuteAdd(uuid, reason, time, s instanceof Player sp ? sp.getUniqueId() : null,
                                    () -> s.sendMessage("User '"+ userDisplay +"' was muted for " + (time == null ? "∞" : system.formatTotalTime(time, system.FormatTime.HOUR_TIME)) + " with reason: " + reason));
                            return true;
                        })
                )
                .addCommand("atp", _v -> _v
                        .withCheck(v -> v.isOp() || Permissions.ATP.check(v))
                        .withUsage("/atp [id_or_nick] {id_or_nick}")
                        .withTab((s,args) -> switch (args.length) {
                            case 1, 2 -> Stream.concat(
                                    TabManager.getPlayers().keySet().stream().map(String::valueOf),
                                    Bukkit.getOnlinePlayers().stream().map(Player::getName)
                            ).collect(Collectors.toList());
                            default -> Collections.emptyList();
                        })
                        .withExecutor((s,args) -> switch (args.length) {
                            case 1 -> {
                                if (!(s instanceof Player player)) yield false;
                                Player target = getOnlineUser(args[0]);
                                if (target == null) {
                                    s.sendMessage("Player '"+args[0]+"' not founded!");
                                    yield false;
                                }
                                s.sendMessage("You teleported to '"+args[0]+"'");
                                yield player.teleport(target);
                            }
                            case 2 -> {
                                if (!s.isOp() || !Permissions.ATP_TO.check(s)) {
                                    s.sendMessage("You don't have permissions!");
                                    yield false;
                                }
                                Player player = getOnlineUser(args[0]);
                                if (player == null) {
                                    s.sendMessage("Player '"+args[0]+"' not founded!");
                                    yield false;
                                }
                                Player target = getOnlineUser(args[1]);
                                if (target == null) {
                                    s.sendMessage("Player '"+args[1]+"' not founded!");
                                    yield false;
                                }
                                s.sendMessage("Player '"+args[0]+"' teleported to '"+args[1]+"'");
                                yield player.teleport(target);
                            }
                            default -> false;
                        })
                )
                .addCommand("agm", _v -> _v
                        .withCheck(v -> v.isOp() || Permissions.AGM.check(v))
                        .addCheck(v -> v instanceof Player)
                        .withUsage("/agm [survival_or_spectator_or_immortality]")
                        .withTab((s,args) -> switch (args.length) {
                            case 1 -> Arrays.asList("survival", "spectator", "immortality");
                            default -> Collections.emptyList();
                        })
                        .withExecutor((s,args) -> switch (args.length) {
                            case 1 -> {
                                Player player = (Player)s;
                                player.setGameMode(switch (args[0]) {
                                    case "survival" -> { player.removeScoreboardTag("immortality"); yield GameMode.SURVIVAL; }
                                    case "spectator" -> { player.removeScoreboardTag("immortality"); yield GameMode.SPECTATOR; }
                                    case "immortality" -> { player.addScoreboardTag("immortality"); yield GameMode.SURVIVAL; }
                                    default -> player.getGameMode();
                                });
                                yield true;
                            }
                            default -> false;
                        })
                )
                /*.addCommand("report", _v -> _v
                        .withCheck(__v -> __v instanceof Player)
                        .withUsage("/report [Сообщение]")
                        .withTab((s,args) -> Collections.singletonList("[сообщение]"))
                        .withExecutor((s,args) -> {
                            Player player = (Player)s;
                            String message = String.join(" ", args).trim();
                            if (message.length() == 0) return false;
                            return Rows.UserRow.getBy(player.getUniqueId()).map(user -> {
                                ChatMessages.smsFast(player, user, 777, Methods.CallType.ADMIN, message);
                                return true;
                            }).orElse(false);
                        })
                )*/
                .addCommand("get_name", _v -> _v
                        .withCheck(ServerOperator::isOp)
                        .withTab((sender) -> TabManager.getPlayers().keySet().stream().map(String::valueOf).collect(Collectors.toList()))
                        .withExecutor((sender, args) -> {
                            if (args.length == 0) return false;
                            String userName = TabManager.getPlayers().getOrDefault(Integer.parseInt(args[0]), null);
                            if (userName == null) return true;
                            sender.sendMessage(ChatHelper.formatComponent("Игрок: <HOVER_TEXT:<GRAY>Копировать><CLICK_COPY:" + userName + ">" + userName + "</CLICK></HOVER>"));
                            return true;
                        })
                )
                .addCommand("get_uuid", _v -> _v
                        .withCheck(ServerOperator::isOp)
                        .withTab((sender) -> TabManager.getUUIDs().keySet().stream().map(String::valueOf).collect(Collectors.toList()))
                        .withExecutor((sender, args) -> {
                            if (args.length == 0) return false;
                            UUID uuid = TabManager.getUUIDs().getOrDefault(args[0], null);
                            if (uuid == null) return true;
                            sender.sendMessage(ChatHelper.formatComponent("Игрок (uuid): <HOVER_TEXT:<GRAY>Копировать><CLICK_COPY:" + uuid + ">" + uuid + "</CLICK></HOVER>"));
                            return true;
                        })
                )
                .addCommand("tell.all", _v -> _v
                        .withCheck(v -> v.isOp() && v instanceof Player)
                        .withTab("[сообщение]")
                        .withExecutor((sender, args) -> {
                            MenuCreator.show((Player) sender, "lang.chat.tell_all", Apply.of().add("text", String.join(" ", args)));
                            return true;
                        })
                )
                .addCommand("rename.user", _v -> _v
                        .withCheck(ServerOperator::isOp)
                        .withUsage("/rename.user [uuid] [first_name] [last_name]")
                        .withTab((s,args) -> switch (args.length) {
                            case 1 -> Bukkit.getOnlinePlayers().stream().map(HumanEntity::getUniqueId).map(UUID::toString).collect(Collectors.toList());
                            case 2 -> Collections.singletonList("[first_name]");
                            case 3 -> Collections.singletonList("[last_name]");
                            default -> Collections.emptyList();
                        })
                        .withExecutor((s,args) -> switch (args.length) {
                            case 3 -> {
                                Methods.renameUser(UUID.fromString(args[0]), args[1], args[2], () -> s.sendMessage("User renamed!"));
                                yield true;
                            }
                            default -> false;
                        })
                )
                .withInit(Administrator::init);
    }

    private static Player getOnlineUser(String key) {
        UUID uuid = TabManager.getUUIDs().getOrDefault(key, null);
        if (uuid != null) return Bukkit.getPlayer(uuid);
        try { return Bukkit.getPlayer(UUID.fromString(key)); }
        catch (Exception e) { return Bukkit.getPlayer(key); }
    }

    public static void init() {
        AnyEvent.addEvent("aban.target", AnyEvent.type.owner_console, builder -> builder.createParam(UUID::fromString, "[uuid]").createParam(v->v, () -> Streams.concat(target_list.values().stream().map(Object::toString), Stream.of("exit")).collect(Collectors.toList())), (player, uuid, state) -> {
            if ("exit".equals(state)) target_list.remove(uuid);
            else target_list.put(uuid, Integer.parseInt(state));
        });
        lime.repeat(Administrator::update, 0.5);
    }

    public static void update() {
        abans.clear();

        Tables.AMUTE_TABLE.getRows().forEach(row -> {
            UUID uuid = row.uuid;
            if (Bukkit.getPlayer(uuid) == null) return;
            Voice.mute(uuid);
        });
        Tables.ABAN_TABLE.getRows().forEach(row -> {
            abans.add(row.uuid);
            Player player = Bukkit.getPlayer(row.uuid);
            if (player == null) return;
            effects.forEach(player::addPotionEffect);
            Thirst.thirstReset(player);
            CustomUI.TextUI.show(player, "До выхода: " + (row.timeToEnd == null ? "Неограничено" : system.formatTotalTime(row.timeToEnd, system.FormatTime.DAY_TIME)) + (isNullOrEmpty(row.reason) ? "" : (" | Причина: " + row.reason)), 15);
            Location playerLoc = player.getLocation();
            if (lime.isLay(player)) {
                Death.up(player);
                lime.unLay(player);
            }
            else if (lime.isLay(player)) {
                Knock.unKnock(player);
                lime.unSit(player);
            }

            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20);
            Thirst.thirstValue(player, 20);

            HandCuffs.unLock(player);
            if (!target_list.containsKey(row.uuid) && (playerLoc.getWorld() != center.getWorld() || playerLoc.distance(center) > distance)) {
                player.teleport(center);
            }
            if (row.timeToEnd != null && row.timeToEnd < 0) {
                Methods.aBanDel(row.uuid, () -> {
                    for (int i = 0; i < 3; i++)
                        lime.once(() -> player.teleport(Login.getMainLocation()), i);
                });
            }
        });
        sync = !sync;
        if (sync) Methods.aAnyUpdate(EntityPosition.onlinePlayers.keySet());
        Bukkit.getOnlinePlayers().forEach(player -> {
            UUID uuid = player.getUniqueId();
            Set<String> tags = player.getScoreboardTags();
            if (tags.contains("immortality")) {
                if (player.getGameMode() != GameMode.SURVIVAL || (!player.isOp() && !Permissions.AGM.check(player))) tags.remove("immortality");
                else {
                    tags.remove("leg.broken");
                    CustomUI.TextUI.show(player, "[Бессмертие]", 15);
                    player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    ProxyFoodMetaData.ofPlayer(player)
                            .ifPresent(food -> {
                                food.setFoodLevel(20);
                                food.setSaturation(20);
                            });
                    Thirst.thirstValue(player, 20);
                }
            }
            Integer room = target_list.getOrDefault(uuid, null);
            List<Integer> rooms = getRooms(player.getLocation());
            if (room == null) {
                if (rooms.size() > 0) {
                    Location back = back_locations.remove(uuid);
                    if (back == null) back = Login.getMainLocation();
                    player.teleport(back);
                }
                return;
            }

            player.setHealth(20);
            ProxyFoodMetaData.ofPlayer(player)
                    .ifPresent(food -> {
                        food.setFoodLevel(20);
                        food.setSaturation(20);
                    });
            HandCuffs.unLock(player);
            Thirst.thirstReset(player);

            if (lime.isLay(player)) {
                Death.up(player);
                lime.unLay(player);
            }
            else if (lime.isLay(player)) {
                Knock.unKnock(player);
                lime.unSit(player);
            }

            if (rooms.contains(room)) return;
            Location old = player.getLocation();
            if (old.getWorld() == lime.MainWorld) back_locations.put(uuid, old);
            player.teleport(new ArrayList<>(Administrator.rooms).get(room).val1);
        });
    }
    private static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }
    private static final List<UUID> abans = new ArrayList<>();
    public static void aban(UUID uuid, String reason, Integer time_sec) {
        aban(uuid, reason, time_sec, null);
    }
    public static void aban(UUID uuid, String reason, Integer time_sec, UUID owner) {
        Methods.aBanAdd(uuid, reason, time_sec, owner, () -> { });
    }
    public static boolean inABan(UUID uuid) {
        return abans.contains(uuid) || target_list.containsKey(uuid);
    }
    private static final List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.REGENERATION, 20, 255, false, false, false),
            new PotionEffect(PotionEffectType.SATURATION, 20, 255, false, false, false)
    );
    private static boolean sync = false;
    private static List<Integer> getRooms(Location location) {
        List<Integer> rooms = new ArrayList<>();
        List<system.Toast3<Double, Location, String>> _rooms = new ArrayList<>(Administrator.rooms);
        int size = _rooms.size();
        for (int i = 0; i < size; i++) {
            system.Toast3<Double, Location, String> kv = _rooms.get(i);
            if (kv.val1.getWorld() != location.getWorld()) continue;
            if (kv.val1.distance(location) < kv.val0) rooms.add(i);
        }
        return rooms;
    }
    @EventHandler public static void on(PlayerInteractEvent e) {
        if (inABan(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerInteractEntityEvent e) {
        if (inABan(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler public static void on(BlockDamageEvent e) {
        if (inABan(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler public static void on(BlockPlaceEvent e) {
        if (inABan(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }
    @EventHandler public static void on(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player player && inABan(player.getUniqueId())) e.setCancelled(true);
    }
    @EventHandler public static void on(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (player.getScoreboardTags().contains("immortality")) e.setCancelled(true);
    }
}
















