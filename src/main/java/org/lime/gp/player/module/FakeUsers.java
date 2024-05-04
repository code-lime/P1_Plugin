package org.lime.gp.player.module;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.EnumGamemode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.FakeUserRow;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.gp.module.npc.EPlayerModule;
import org.lime.gp.module.npc.eplayer.RawEPlayer;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.plugin.CoreElement;
import org.lime.skin;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class FakeUsers implements Listener {
    public static final String serverIndex = UUID.randomUUID().toString();
    private static final Object noneObject = new Object();

    public static CoreElement create() {
        return CoreElement.create(FakeUsers.class)
                .withInit(FakeUsers::init)
                .withUninit(FakeUsers::uninit)
                .withInstance();
    }
    private static final HashMap<Integer, Toast2<FakeUserRow, RawEPlayer>> eplayers = new HashMap<>();
    private static final HashMap<UUID, HashSet<UUID>> tabShow = new HashMap<>();
    private static final ConcurrentHashMap<UUID, Object> ownerFakes = new ConcurrentHashMap<>();

    private static void init() {
        AnyEvent.addEvent("fake.clone", AnyEvent.type.other,
                p -> {
                    Methods.createFakeUser(p, serverIndex, 10, false);
                    update();
                });
        AnyEvent.addEvent("fake.clone", AnyEvent.type.other, v -> v
                        .createParam(Integer::parseInt, "[lifeTime]"),
                (p, v) -> {
                    Methods.createFakeUser(p, serverIndex, v, false);
                    update();
                });
        AnyEvent.addEvent("fake.clone", AnyEvent.type.other, v -> v
                        .createParam(Integer::parseInt, "[lifeTime]")
                        .createParam(_v -> _v.equalsIgnoreCase("true") || _v.equalsIgnoreCase("yes"), "[autoRemove]"),
                (p, a, b) -> {
                    Methods.createFakeUser(p, serverIndex, a, b);
                    update();
                });
        lime.repeat(FakeUsers::update, 5);
        EPlayerModule.registry(() -> eplayers.values().stream().map(v -> v.val1));
    }
    private static void uninit() {
        tabShow.forEach((uuid, target) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || target.isEmpty()) return;
            PacketManager.sendPackets(player, target.stream().map(FakeUsers::createFakeUserPacketRemove));
        });
    }
    private static void onClick(Player player, boolean isShift, UUID target) {
        MenuCreator.show(player, "fake.user", Apply.of()
                .add("shift", isShift ? "true" : "false")
                .add("other_uuid", target.toString()));
    }

    public static boolean hasFakeUser(UUID owner) {
        return ownerFakes.containsKey(owner);
    }
    public static Stream<UUID> getFakeUsers(UUID uuid) {
        return eplayers
                .values()
                .stream()
                .filter(v -> v.val0.uuid.equals(uuid))
                .map(v -> v.val0.unique);
    }
    public static void setRemoteStatus(UUID owner, String status) {
        Methods.setFakeUserStatus(owner, status);
    }

    private static ClientboundPlayerInfoUpdatePacket createFakeUserPacket(UUID uuid, FakeUserRow row) {
        return new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(
                        ClientboundPlayerInfoUpdatePacket.a.ADD_PLAYER,
                        ClientboundPlayerInfoUpdatePacket.a.INITIALIZE_CHAT,
                        ClientboundPlayerInfoUpdatePacket.a.UPDATE_GAME_MODE,
                        ClientboundPlayerInfoUpdatePacket.a.UPDATE_LISTED,
                        ClientboundPlayerInfoUpdatePacket.a.UPDATE_LATENCY,
                        ClientboundPlayerInfoUpdatePacket.a.UPDATE_DISPLAY_NAME),
                new ClientboundPlayerInfoUpdatePacket.b(
                        uuid,
                        Skins.setProfile(new GameProfile(uuid, row.userName), new Skins.Property(skin.uploaded.None), false),
                        true,
                        0,
                        EnumGamemode.ADVENTURE,
                        null,
                        null));
    }
    private static ClientboundPlayerInfoRemovePacket createFakeUserPacketRemove(UUID uuid) {
        return new ClientboundPlayerInfoRemovePacket(List.of(uuid));
    }

    public static int getCount() {
        return eplayers.size();
    }

    private static void update() {
        Methods.updateFakeUsers(serverIndex, rows -> {
            HashMap<UUID, FakeUserRow> existList = new HashMap<>();
            HashSet<Integer> removeList = new HashSet<>(eplayers.keySet());
            HashSet<UUID> removeUUIDs = new HashSet<>(ownerFakes.keySet());
            rows.forEach(row -> {
                if (!serverIndex.equals(row.serverIndex))
                    return;
                existList.put(row.unique, row);
                removeList.remove(row.id);
                removeUUIDs.remove(row.uuid);
                ownerFakes.put(row.uuid, noneObject);
                eplayers.computeIfAbsent(row.id, k -> Toast.of(row, new RawEPlayer(
                        "fake#" + row.id,
                        row.location.clone(),
                        null,
                        row.pose,
                        (p,s) -> onClick(p,s,row.uuid),
                        LangMessages.Message.Entity_FakeUser.getMessages(),
                        DeathGame.loadEquipment(row.equipment).val0,
                        row.skin == null ? null : new Skins.Property(json.parse(row.skin).getAsJsonObject()))));
            });
            removeUUIDs.forEach(ownerFakes::remove);
            removeList.forEach(eplayers::remove);

            HashSet<UUID> removeTabs = new HashSet<>(tabShow.keySet());
            Bukkit.getOnlinePlayers().forEach(player -> {
                removeTabs.remove(player.getUniqueId());
                HashSet<UUID> shows = tabShow.computeIfAbsent(player.getUniqueId(), v -> new HashSet<>());
                existList.forEach((unique, row) -> {
                    if (!shows.add(unique)) return;
                    PacketManager.sendPacket(player, createFakeUserPacket(unique, row));
                });
                shows.removeIf(unique -> {
                    if (existList.containsKey(unique))
                        return false;
                    PacketManager.sendPacket(player, createFakeUserPacketRemove(unique));
                    return true;
                });
            });

            removeTabs.forEach(tabShow::remove);
        });
    }

    @EventHandler public static void on(PlayerQuitEvent e) {
        tabShow.remove(e.getPlayer().getUniqueId());
    }
    @EventHandler public static void on(PlayerJoinEvent e) {
        Methods.removeFakeUsers(e.getPlayer().getUniqueId(), true, v -> {
            if (!v) return;
            update();
        });
    }
}
