package org.lime.gp.player.module;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.EnumGamemode;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.FakeUserRow;
import org.lime.gp.lime;
import org.lime.gp.module.npc.EPlayerModule;
import org.lime.gp.module.npc.eplayer.RawEPlayer;
import org.lime.plugin.CoreElement;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import java.util.*;
import java.util.stream.Stream;

public class FakeUsers implements Listener {
    public static final String serverIndex = UUID.randomUUID().toString();
    private static final String fakeUserPrefix = "9473fe09-635a-3000-0000-";

    public static CoreElement create() {
        return CoreElement.create(FakeUsers.class)
                .withInit(FakeUsers::init)
                .withUninit(FakeUsers::uninit)
                .withInstance();
    }
    private static final HashMap<Integer, Toast2<FakeUserRow, RawEPlayer>> eplayers = new HashMap<>();
    private static final HashMap<UUID, HashSet<Integer>> tabShow = new HashMap<>();

    private static void init() {
        AnyEvent.addEvent("fake.clone", AnyEvent.type.other, p -> Methods.createFakeUser(p, serverIndex));
        lime.repeat(FakeUsers::update, 5);
        EPlayerModule.registry(() -> eplayers.values().stream().map(v -> v.val1));
    }
    private static void uninit() {

    }
    private static void onClick(Player player, boolean isShift, UUID target) {
        lime.logOP("OC: " + player.getUniqueId() + " -> " + target + " " + (isShift ? "with" : "without") + " shift");
    }

    public static Stream<UUID> getFakeUsers(UUID uuid) {
        return eplayers
                .values()
                .stream()
                .filter(v -> v.val0.uuid.equals(uuid))
                .map(v -> v.val0.unique);
    }
    private static ClientboundPlayerInfoUpdatePacket createFakeUserPacket(FakeUserRow row) {
        UUID uuid = getFakeUserUid(id);
        return new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.a.ADD_PLAYER),
                new ClientboundPlayerInfoUpdatePacket.b(
                        uuid,
                        new GameProfile(uuid, "FAKE#" + id),
                        true,
                        0,
                        EnumGamemode.ADVENTURE,
                        null,
                        null));
    }
    private static ClientboundPlayerInfoRemovePacket createFakeUserPacketRemove(int id) {
        UUID uuid = getFakeUserUid(id);
        return new ClientboundPlayerInfoRemovePacket(List.of(uuid));
    }

    private static void update() {
        Methods.fakeUsers(rows -> {
            HashMap<Integer, UUID> existList = new HashMap<>();
            HashSet<Integer> removeList = new HashSet<>(eplayers.keySet());
            rows.forEach(row -> {
                if (!serverIndex.equals(row.serverIndex))
                    return;
                existList.put(row.id, row.unique);
                removeList.remove(row.id);
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
            removeList.forEach(eplayers::remove);

            HashSet<UUID> removeTabs = new HashSet<>(tabShow.keySet());
            Bukkit.getOnlinePlayers().forEach(player -> {
                removeTabs.remove(player.getUniqueId());
                HashSet<Integer> shows = tabShow.computeIfAbsent(player.getUniqueId(), v -> new HashSet<>());
                existList.forEach((id, unique) -> {
                    if (shows.add(id))
                        ((CraftPlayer)player).getHandle().connection.send(createFakeUserPacket(unique));
                });
                shows.removeIf(id -> {
                    if (existList.containsKey(id))
                        return false;
                    ((CraftPlayer)player).getHandle().connection.send(createFakeUserPacketRemove(id));
                    return true;
                });
            });

            removeTabs.forEach(tabShow::remove);
        });
    }

    @EventHandler public static void on(PlayerQuitEvent e) {
        tabShow.remove(e.getPlayer().getUniqueId());
    }
}
