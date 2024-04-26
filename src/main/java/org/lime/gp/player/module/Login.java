package org.lime.gp.player.module;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minecraft.server.level.LightEngineThreaded;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.lime.gp.admin.Administrator;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.CityRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.plugin.CoreElement;
import org.lime.system.Time;
import org.lime.system.map;
import org.lime.system.toast.Toast;
import org.lime.system.utils.MathUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Login implements Listener {
    private static Location login;
    private static Set<ChunkCoordIntPair> login_sync_chunks;
    private static Set<ChunkCoordIntPair> login_unlock_chunks;
    private static Location main;
    private static boolean only_op;

    public static CoreElement create() {
        return CoreElement.create(Login.class)
                .withInstance()
                .withInit(Login::init)
                .addCommand("sync.login", v -> v.withCheck(ServerOperator::isOp).withTab().withExecutor(sender -> {
                    WorldServer main = lime.MainWorld.getHandle();
                    WorldServer login = lime.LoginWorld.getHandle();

                    int sections = Math.min(main.getSectionsCount(), login.getSectionsCount());

                    int iterator = 0;
                    int total = login_sync_chunks.size();
                    int lastPersent = -1;

                    HashSet<ChunkCoordIntPair> chunks = new HashSet<>();
                    IBlockData BARRIER = Blocks.BARRIER.defaultBlockState();

                    for (ChunkCoordIntPair chunkPos : login_sync_chunks) {
                        iterator++;

                        boolean locked = !login_unlock_chunks.contains(chunkPos);

                        int persent = iterator * 100 / total;
                        if (persent != lastPersent) {
                            lastPersent = persent;
                            lime.logOP("[SyncBlocks] " + persent + "% | " + iterator + " / " + total + "...");
                        }

                        Chunk mainChunk = main.getChunk(chunkPos.x, chunkPos.z);
                        Chunk loginChunk = login.getChunk(chunkPos.x, chunkPos.z);
                        chunks.add(chunkPos);

                        ChunkSection[] mainSections = mainChunk.getSections();
                        ChunkSection[] loginSections = loginChunk.getSections();

                        for (int y = 0; y < sections; y++) {
                            ChunkSection mainSection = mainSections[y];
                            ChunkSection loginSection = loginSections[y];

                            mainSection.acquire();
                            loginSection.acquire();
                            try {
                                for (int _x = 0; _x < 16; _x++)
                                    for (int _y = 0; _y < 16; _y++)
                                        for (int _z = 0; _z < 16; _z++) {
                                            IBlockData data = mainSection.getBlockState(_x,_y,_z);
                                            if (data.isAir() && locked) data = BARRIER;
                                            loginSection.setBlockState(_x,_y,_z,data);
                                        }
                            }
                            finally {
                                mainSection.release();
                                loginSection.release();
                            }
                        }
                    }

                    lime.logOP("[SyncBlocks] 100% | " + lastPersent + " / " + lastPersent + "...OK!");
                    lime.logOP("[SyncLightAsync] 0%...");
                    LightEngineThreaded lightEngine = (LightEngineThreaded)login.getLightEngine();
                    lightEngine.relight(chunks, coord -> {}, count -> lime.logOP("[SyncLightAsync] 100% | Relight chunks: "+count+"...OK!"));
                    return true;
                }))
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("main")
                        .withDefault(new JsonPrimitive(MathUtils.getString(new Vector(0, 70, 0))))
                        .withInvoke(json -> main = MathUtils.getLocation(lime.MainWorld, json.getAsString()))
                )
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("login")
                        .withDefault(new JsonPrimitive(MathUtils.getString(new Vector(0, 70, 0))))
                        .withInvoke(json -> login = MathUtils.getLocation(lime.LoginWorld, json.getAsString()))
                )
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("login_sync_chunks")
                        .withDefault(new JsonPrimitive("-10:-10..10:10"))
                        .withInvoke(json -> login_sync_chunks = (json.isJsonArray()
                                        ? Streams.stream(json.getAsJsonArray().iterator()).map(JsonElement::getAsString)
                                        : Stream.of(json.getAsString()))
                                .map(_v -> _v.replace("..", ":").split(":"))
                                .map(_args -> Toast.of(Integer.parseInt(_args[0]),Integer.parseInt(_args[1]),Integer.parseInt(_args[2]),Integer.parseInt(_args[3])))
                                .map(_v -> Toast.of(Math.min(_v.val0, _v.val2), Math.min(_v.val1, _v.val3), Math.max(_v.val0, _v.val2), Math.max(_v.val1, _v.val3)))
                                .flatMap(_v -> IntStream.range(_v.val0, _v.val2 + 1)
                                        .boxed()
                                        .flatMap(x -> IntStream.range(_v.val1, _v.val3 + 1).mapToObj(z -> new ChunkCoordIntPair(x,z)))
                                )
                                .collect(Collectors.toSet())
                        )
                )
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("login_unlock_chunks")
                        .withDefault(new JsonPrimitive("-5:-5..5:5"))
                        .withInvoke(json -> login_unlock_chunks = (json.isJsonArray()
                                ? Streams.stream(json.getAsJsonArray().iterator()).map(JsonElement::getAsString)
                                : Stream.of(json.getAsString()))
                                .map(_v -> _v.replace("..", ":").split(":"))
                                .map(_args -> Toast.of(Integer.parseInt(_args[0]),Integer.parseInt(_args[1]),Integer.parseInt(_args[2]),Integer.parseInt(_args[3])))
                                .map(_v -> Toast.of(Math.min(_v.val0, _v.val2), Math.min(_v.val1, _v.val3), Math.max(_v.val0, _v.val2), Math.max(_v.val1, _v.val3)))
                                .flatMap(_v -> IntStream.range(_v.val0, _v.val2 + 1)
                                        .boxed()
                                        .flatMap(x -> IntStream.range(_v.val1, _v.val3 + 1).mapToObj(z -> new ChunkCoordIntPair(x,z)))
                                )
                                .collect(Collectors.toSet())
                        )
                )
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("only_op")
                        .withDefault(new JsonPrimitive(true))
                        .withInvoke(json -> only_op = json.getAsBoolean())
                );
    }

    public static Location getLoginLocation() {
        return login;
    }
    public static Location getMainLocation(UUID uuid) {
        return UserRow.getBy(uuid)
                .flatMap(UserRow::getCityID)
                .flatMap(CityRow::getBy)
                .flatMap(v -> v.posMain)
                .map(v -> v.toLocation(lime.MainWorld))
                .orElse(main);
    }
    public static Location getMainLocation(Player player) {
        return getMainLocation(player.getUniqueId());
    }

    public static void init() {
        AnyEvent.addEvent("only_op", AnyEvent.type.owner_console, v -> v.createParam("enable", "disable"), (s,v) -> {
            switch (v) {
                case "enable" -> {
                    only_op = true;
                    lime.logOP("Enabled only_op");
                }
                case "disable" -> {
                    only_op = false;
                    lime.logOP("Disabled only_op");
                }
            }
        });
        lime.repeat(Login::update, 0.1);
    }
    private static final HashMap<Player, BukkitTask> teleportToMain = new HashMap<>();
    private static final HashMap<UUID, Long> timeTitleLogin = new HashMap<>();

    private static final Component single = ImageBuilder.of(0xEff9, 2000).withColor(NamedTextColor.BLACK).build();

    public static void update() {
        if (!Tables.USER_TABLE.isInit()) return;
        long now = System.currentTimeMillis();
        timeTitleLogin.entrySet().removeIf(kv -> kv.getValue() < now);
        teleportToMain.entrySet().removeIf(kv -> {
            if (kv.getKey().isOnline()) return false;
            kv.getValue().cancel();
            return true;
        });
        Bukkit.getOnlinePlayers().forEach(player -> {
            boolean isInit = UserRow.getBy(player.getUniqueId()).isPresent() && !player.getScoreboardTags().contains("can_in_login");
            if (player.getWorld() == lime.LoginWorld) {
                if (player.getGameMode() == GameMode.SURVIVAL) player.setGameMode(GameMode.ADVENTURE);
                if (isInit) {
                    if (!teleportToMain.containsKey(player)) {
                        teleportToMain.put(player, lime.once(() -> player.teleport(getMainLocation(player)), 5));
                        timeTitleLogin.put(player.getUniqueId(), now + 13 * 1000);
                        player.showTitle(Title.title(single, Component.empty(), Title.Times.times(Duration.ofSeconds(5), Duration.ofSeconds(2), Duration.ofSeconds(5))));
                    }
                }
            } else {
                if (!isInit) {
                    player.teleport(getLoginLocation());
                }
                else {
                    BukkitTask task = teleportToMain.remove(player);
                    if (task != null) {
                        if (player.getGameMode() == GameMode.ADVENTURE) player.setGameMode(GameMode.SURVIVAL);
                        task.cancel();
                    }
                }
            }
        });
    }

    public static boolean isLogin(Player player) {
        return player.getWorld() == lime.LoginWorld;
    }
    public static boolean isTitleLogin(Player player) {
        return timeTitleLogin.containsKey(player.getUniqueId());
    }

    private static final Map<UUID, Map<UUID, String>> multiList = map.<UUID, Map<UUID, String>>of()
            .add(UUID.fromString("ce6e763f-a669-40eb-866d-019e6ddca12c"),
                    map.<UUID, String>of(true)
                        .add(UUID.fromString("00000000-0000-1000-0000-000000000000"), "Code_Lime.2")
                        .add(UUID.fromString("00000000-0000-1000-0001-000000000001"), "Code_Lime.3")
                        .build()
            )
            .add(UUID.fromString("f76d2058-a107-413c-973e-101fb20c9fdb"),
                    map.<UUID, String>of(true)
                            .add(UUID.fromString("00000000-0000-1000-0000-000000000001"), "Aleks_Bur.2")
                            .build()
            )
            .add(UUID.fromString("fb87fd2f-620d-464e-a7dd-6e2ee1b38e33"),
                    map.<UUID, String>of(true)
                            .add(UUID.fromString("00000000-0000-1000-0000-000000000002"), "mihannik.2")
                            .add(UUID.fromString("00000000-0000-1000-0000-300000000002"), "mihannik.3")
                            .add(UUID.fromString("00000000-0000-1000-0000-400000000002"), "mihannik.4")
                            .add(UUID.fromString("00000000-0000-1000-0000-500000000002"), "mihannik.5")
                            .add(UUID.fromString("00000000-0000-1000-0000-600000000002"), "mihannik.6")
                            .add(UUID.fromString("00000000-0000-1000-0000-700000000002"), "mihannik.7")
                            .add(UUID.fromString("00000000-0000-1000-0000-800000000002"), "mihannik.8")
                            .add(UUID.fromString("00000000-0000-1000-0000-900000000002"), "mihannik.9")
                            .add(UUID.fromString("00000000-0000-1000-0000-A00000000002"), "mihannik.A")
                            .add(UUID.fromString("00000000-0000-1000-0000-B00000000002"), "mihannik.B")
                            .build()
            )
            .build();

    @EventHandler public static void on(AsyncPlayerPreLoginEvent e) {
        PlayerProfile profile = e.getPlayerProfile();
        if (profile.getId() == null) return;
        Map<UUID, String> dats = multiList.getOrDefault(profile.getId(), null);
        if (dats == null) return;
        UUID uuid = profile.getId();
        if (Bukkit.getPlayer(uuid) == null) return;
        Map.Entry<UUID, String> dat = dats.entrySet().stream().filter(kv -> Bukkit.getPlayer(kv.getKey()) == null).findFirst().orElse(null);
        if (dat == null) return;
        PlayerProfile new_profile = new CraftPlayerProfile(dat.getKey(), dat.getValue());
        new_profile.setProperties(profile.getProperties());
        e.setPlayerProfile(new_profile);
    }
    @EventHandler public static void on(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        Optional
                .ofNullable(player.getAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .ifPresent(ip -> Methods.addIP(ip, uuid));
        Methods.updateUser(uuid, player.getName(), Time.moscowNow());
    }
    @EventHandler public static void on(PlayerLoginEvent e) {
        Player player = e.getPlayer();
        if (e.getResult() == PlayerLoginEvent.Result.KICK_FULL) {
            if (player.isOp()
                    || TabManager.donates.containsKey(player.getUniqueId())
                    || Administrator.Permissions.IGNORE_FULL.check(player)
                    || Administrator.Permissions.IGNORE_FULL.checkOffline(player.getUniqueId())
            ) e.setResult(PlayerLoginEvent.Result.ALLOWED);
            return;
        }
        if (!only_op) return;
        if (player.isOp()) return;
        e.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Component.text("Временно недоступно"));
    }
}



















