package org.lime.gp.player.module;

import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.Methods;
import org.lime.gp.database.MySql;
import org.lime.gp.lime;
import org.lime.packetwrapper.WrapperPlayServerPlayerInfo;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.scoreboard.ScoreboardManager;
import org.lime.core;
import org.lime.system;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.lime.gp.chat.ChatHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TabManager implements Listener {
    private final static TabManager Instance = new TabManager();

    private static double tab_update_single;
    private static double tab_update_wait;
    private static boolean tab_debug;
    private static String sql_query;
    private static final List<Integer> see_ids = new ArrayList<>();
    public static core.element create() {
        return core.element.create(TabManager.class)
                .withInit(TabManager::init)
                .withInstance(Instance)
                .<JsonObject>addConfig("config", v -> v
                        .withParent("tab")
                        .withDefault(system.json.object().add("debug", new JsonPrimitive(false)).add("update_single", 1.0).add("update_wait", 5.0).build())
                        .withInvoke(json -> {
                            tab_update_single = json.get("update_single").getAsDouble();
                            tab_update_wait = json.get("update_wait").getAsDouble();
                            tab_debug = json.has("debug") && json.get("debug").getAsBoolean();
                            see_ids.clear();
                        })
                )
                .addFile("tab.sql", "tab.sql", core.element.data.text().withInvoke(sql -> sql_query = sql))
                .addCommand("id", _v -> _v.withCheck(v -> v instanceof Player).withExecutor(v -> {
                    Player player = (Player)v;
                    Integer id = getPayerIDorNull(player.getUniqueId());
                    if (id == null) return true;
                    LangMessages.Message.Chat_MyID.sendMessage(player, Apply.of().add("id", String.valueOf(id)));
                    return true;
                }));
    }

    public static ConcurrentHashMap<UUID, Integer> static_ids = new ConcurrentHashMap<>();
    public static boolean hasDonateID(UUID uuid) {
        return static_ids.containsKey(uuid);
    }

    public static Integer getPayerIDorNull(UUID uuid) {
        PlayerData dat = PlayerData.getPlayerDataOrNull(uuid);
        if (dat == null) return null;
        return dat.index;
    }
    public static int getPayerIDorDefault(UUID uuid, int def) {
        PlayerData dat = PlayerData.getPlayerDataOrNull(uuid);
        if (dat == null) return def;
        return dat.index;
    }
    public static synchronized UUID getUUIDorNull(int timed_id) {
        for (Map.Entry<UUID, PlayerData> kv : PlayerData.displayIndexing.entrySet()) {
            if (kv.getValue().index == timed_id)
                return kv.getKey();
        }
        return null;
    }
    public static Map<Integer, String> getPlayers() {
        return PlayerData.displayIndexing.entrySet().stream().collect(Collectors.toMap(kv -> kv.getValue().index, kv -> Bukkit.getOfflinePlayer(kv.getKey()).getName()));
    }
    public static Map<String, UUID> getUUIDs() {
        return PlayerData.displayIndexing.entrySet().stream().collect(Collectors.toMap(kv -> String.valueOf(kv.getValue().index), Map.Entry::getKey));
    }

    private static final ConcurrentHashMap<Integer, Team> sortTeams = new ConcurrentHashMap<>();
    private static Team getSortTeam(int index) {
        Team team = sortTeams.getOrDefault(index, null);
        if (team == null) {
            String key = StringUtils.leftPad(String.valueOf(index), 3, '0');
            team = scoreboard.getTeam(key);
            if (team != null) team.unregister();
            team = scoreboard.registerNewTeam(key);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            sortTeams.put(index, team);
        }
        return team;
    }

    private static int getNewIndex(UUID uuid) {
        Integer static_id = static_ids.getOrDefault(uuid, null);
        if (static_id != null && !PlayerData.isExistIndex(static_id)) return static_id;
        int index = 1;
        while (PlayerData.isExistIndex(index) || static_ids.containsValue(index)) index++;
        return index;
    }
    public static class PlayerData {
        private final static ConcurrentHashMap<UUID, PlayerData> displayIndexing = new ConcurrentHashMap<>();
        private static synchronized boolean isExistIndex(int index) {
            for (Map.Entry<UUID, PlayerData> kv : displayIndexing.entrySet()) {
                if (kv.getValue().index == index)
                    return true;
            }
            return false;
        }
        public static synchronized PlayerData getPlayerData(UUID uuid) {
            PlayerData data = displayIndexing.getOrDefault(uuid, null);
            if (data == null) displayIndexing.put(uuid, data = new PlayerData(uuid));
            return data;
        }
        public static synchronized PlayerData getPlayerDataOrNull(UUID uuid) {
            return displayIndexing.getOrDefault(uuid, null);
        }

        public Long exitDate = null;
        public final int index;
        public final UUID uuid;
        public final Team team;
        public PlayerData(UUID uuid) {
            this.uuid = uuid;
            this.index = getNewIndex(uuid);
            this.team = getSortTeam(this.index);
            this.team.addEntry(Bukkit.getOfflinePlayer(uuid).getName());
        }
        public boolean isOnline() {
            return Bukkit.getPlayer(uuid) != null;
        }
        public boolean isRemove() {
            boolean online = isOnline();
            if (online) {
                exitDate = null;
                return false;
            }
            long current = System.currentTimeMillis();
            if (exitDate == null) exitDate = current + 30 * 60 * 1000;
            return exitDate < current;
        }
    }

    private static Scoreboard scoreboard;
    public static Team NoCollision;

    public static class BufferData {
        public static final BufferData EMPTY = new BufferData(Component.text(" ..."), Collections.singletonList(Component.text("...")), -1);

        public final WrappedChatComponent tab;
        public final List<Component> nick;
        public final int id;

        public BufferData(Component tab, List<Component> nick, int id) {
            this.tab = ChatHelper.toWrapped(tab);
            this.nick = nick;
            this.id = id;
        }

        public static BufferData of(UUID owner, UUID uuid) {
            Map<UUID, BufferData> map = bufferTab.getOrDefault(owner, null);
            return map == null ? EMPTY : map.getOrDefault(uuid, EMPTY);
        }
    }

    private static final ConcurrentHashMap<UUID, Map<UUID, BufferData>> bufferTab = new ConcurrentHashMap<>();

    public static void init() {
        AnyEvent.addEvent("update.tab", AnyEvent.type.owner, player -> {
            TabManager.regen();
        });
        AnyEvent.addEvent("see_ids", AnyEvent.type.owner_console, b->b.createParam(Integer::parseInt, "[id]"), (player, id) -> {
            if (see_ids.contains(id)) see_ids.remove(id);
            else see_ids.add(id);
        });

        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        scoreboard.getTeams().forEach(Team::unregister);

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getMainScoreboard();
        NoCollision = scoreboard.getTeam("hide");
        if (NoCollision != null) NoCollision.unregister();
        NoCollision = scoreboard.registerNewTeam("hide");
        NoCollision.addEntry("");
        NoCollision.addEntry(".");
        NoCollision.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        NoCollision.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(lime._plugin, PacketType.Play.Server.PLAYER_INFO) {
            @Override public void onPacketSending(PacketEvent event) {
                WrapperPlayServerPlayerInfo playerInfo = new WrapperPlayServerPlayerInfo(event.getPacket());
                if (playerInfo.getAction() == EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
                    List<PlayerInfoData> data = new ArrayList<>();
                    playerInfo.getData().forEach(info -> data.add(new PlayerInfoData(info.getProfile(), info.getLatency(), info.getGameMode(), WrappedChatComponent.fromText(" ..."))));
                    playerInfo.setData(data);
                }
                /*UUID owner = event.getPlayer().getUniqueId();
                Map<UUID, BufferData> map = bufferTab.getOrDefault(owner, null);
                if (map == null) return;
                List<PlayerInfoData> data = new ArrayList<>();
                playerInfo.getData().forEach(info -> {
                    UUID uuid = info.getProfile().getUUID();
                    BufferData buff = map.getOrDefault(uuid, BufferData.EMPTY);
                    WrappedChatComponent tab = buff.tab;
                    if (see_ids.contains(buff.id)) {
                        lime.NextTick(() -> lime.LogOP("S:"+buff.id+":" + puuid(owner) + "/" + puuid(uuid) + ">"+ChatHelper.getLegacyText(ChatHelper.fromJson(system.json.parse(tab.getJson())))));
                    }
                    data.add(new PlayerInfoData(info.getProfile(), info.getLatency(), info.getGameMode(), tab));
                });
                playerInfo.setData(data);*/
            }
        });
        static_ids.clear();
        Methods.donateStaticID(list -> {
            static_ids.putAll(list);
            lime.once(TabManager::regen, 5);
            update();
            lime.repeat(() -> {
                bufferTab.entrySet().removeIf(kv -> Bukkit.getPlayer(kv.getKey()) == null);
                Methods.donateStaticID(_list -> {
                    static_ids.putAll(_list);
                    static_ids.entrySet().removeIf(kv -> !_list.containsKey(kv.getKey()));
                });
            }, 60);
        });
    }
    private static void regen() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        scoreboard.getTeams().forEach(team -> {
            if (team.equals(NoCollision)) return;
            team.unregister();
        });
        bufferTab.clear();

        sortTeams.clear();
        PlayerData.displayIndexing.clear();
    }
    private static Component header;
    private static Component footer;
    private static String puuid(UUID uuid) {
        return uuid.toString().substring(0, 5);
    }
    public static synchronized void update() {
        lime.once(TabManager::update, tab_update_wait);

        PlayerData.displayIndexing.entrySet().removeIf(v -> v.getValue().isRemove());

        HashMap<String, String> args = system.map.<String, String>of()
                .add("tps", String.valueOf(Bukkit.getTPS()[0]))
                .add("online", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .build();
        header = LangMessages.Message.Tab_Header.getSingleMessage(Apply.of().add(args));
        footer = LangMessages.Message.Tab_Footer.getSingleMessage(Apply.of().add(args));

        static_ids.forEach((uuid, static_id) -> {
            if (PlayerData.isExistIndex(static_id) || PlayerData.displayIndexing.remove(uuid) == null) return;
            PlayerData.getPlayerData(uuid);
        });

        Methods.SQL.Async.rawSqlQuery(sql_query, set -> system.toast(
                UUID.fromString(MySql.readObject(set, "owner", String.class)),
                UUID.fromString(MySql.readObject(set, "uuid", String.class)),
                MySql.readObject(set, "tab", String.class),
                MySql.readObject(set, "name", String.class),
                MySql.readObject(set, "id", Integer.class)
        ), lines -> {
            lime.invokeAsync(() -> {
                HashMap<UUID, Map<UUID, BufferData>> buffer = new HashMap<>();
                lines.forEach(line -> {
                    UUID owner = line.val0;
                    UUID uuid = line.val1;
                    String tab = line.val2;
                    String name = line.val3;
                    if (tab == null) tab = " ...";
                    if (name == null) name = "...";
                    int id = line.val4;
                    Map<UUID, BufferData> _map = buffer.getOrDefault(owner, null);
                    if (_map == null) _map = new HashMap<>();
                    _map.put(uuid, new BufferData(ChatHelper.formatComponent(tab),
                            Arrays
                                    .stream(name.split("\n"))
                                    .filter(v -> !v.isEmpty())
                                    .map(ChatHelper::formatComponent)
                                    .collect(Collectors.toList()),
                            id)
                    );
                    buffer.put(owner, _map);
                });
                bufferTab.putAll(buffer);
                bufferTab.entrySet().removeIf(kv -> !buffer.containsKey(kv.getKey()));
                buffer.forEach((owner,tab) -> {
                    Player player = Bukkit.getPlayer(owner);
                    if (player == null) return;
                    WrapperPlayServerPlayerInfo wpspi = new WrapperPlayServerPlayerInfo();
                    wpspi.setAction(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);
                    List<PlayerInfoData> list = new ArrayList<>();
                    tab.forEach((uuid, dat) -> list.add(new PlayerInfoData(
                            new WrappedGameProfile(uuid, "1"),
                            0,
                            EnumWrappers.NativeGameMode.SURVIVAL,
                            dat.tab
                    )));
                    wpspi.setData(list);
                    wpspi.sendPacket(player);
                    TabManager.updatePlayer(player, false);
                });
            }, () -> {
                /*WrapperPlayServerPlayerInfo wpspi = new WrapperPlayServerPlayerInfo();
                wpspi.setAction(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);
                List<PlayerInfoData> list = new ArrayList<>();
                WrappedChatComponent chat = ChatHelper.toWrapped(Component.text(" ..."));
                Bukkit.getOnlinePlayers().forEach(p -> {
                    UUID uuid = p.getUniqueId();
                    list.add(new PlayerInfoData(
                            new WrappedGameProfile(uuid, "1"),
                            0,
                            EnumWrappers.NativeGameMode.SURVIVAL,
                            chat
                    ));
                });
                wpspi.setData(list);
                Packet<?> packet = (Packet<?>)wpspi.getHandle().getHandle();

                Bukkit.getOnlinePlayers().forEach(p -> {
                    TabManager.OnUpdatePlayer(p, false);
                    PacketManager.SendPacket(p, packet);
                });*/
            });
        });
        /*for (int i = 0; i < length; i++) {
            Player player = players.get(i);
            UUID uuid = player.getUniqueId();
            String user_uuid = uuid.toString();
            PlayerData.getPlayerData(uuid);

            lime.Once(() -> DataReader.SQL.Async.RawSqlQuery(sql_query.replace("{user_uuid}", user_uuid), set -> system.toast(MySql.ReadObject(set, "uuid", String.class), MySql.ReadObject(set, "text", String.class)), lines -> {
                bufferTab.compute(uuid, (k, v) -> {
                    ConcurrentHashMap<UUID, Component> __v = v == null ? new ConcurrentHashMap<>() : v;
                    lines.forEach(kv -> __v.put(UUID.fromString(kv.val0), ChatHelper.FormatComponent(kv.val1)));
                    return __v;
                });
                OnUpdatePlayer(player);
            }), tab_update_single * i);*/
    }
    private static void updatePlayer(Player player, boolean send) {
        player.sendPlayerListHeaderAndFooter(header, footer);
        PlayerData.getPlayerData(player.getUniqueId());
        if (send) player.playerListName(Component.text(" ..."));
    }
    @EventHandler private static void on(PlayerJoinEvent e) {
        updatePlayer(e.getPlayer(), true);
    }
}
















