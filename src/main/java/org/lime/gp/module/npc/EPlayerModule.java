package org.lime.gp.module.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.world.EnumHand;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.display.Displays;
import org.lime.gp.lime;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.npc.display.EPlayerDisplay;
import org.lime.gp.module.npc.display.EPlayerManager;
import org.lime.gp.module.npc.eplayer.ConfigEPlayer;
import org.lime.gp.module.npc.eplayer.IEPlayer;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Func0;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EPlayerModule implements Listener {
    public static UUID createUUID() {
        String uuid = UUID.randomUUID().toString();
        return UUID.fromString(uuid.substring(0,14)+'1'+uuid.substring(15));
    }

    public static CoreElement create() {
        return CoreElement.create(EPlayerModule.class)
                .withInit(EPlayerModule::init)
                .<JsonObject>addConfig("npc", v -> v.withInvoke(EPlayerModule::config).withDefault(new JsonObject()));
    }
    private static void init() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(lime._plugin, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketPlayInUseEntity use = (PacketPlayInUseEntity)event.getPacket().getHandle();
                use.dispatch(new PacketPlayInUseEntity.c() {
                    @Override public void onAttack() { onInteraction(EnumHand.MAIN_HAND); }
                    @Override public void onInteraction(EnumHand enumHand) { onInteraction(enumHand, Vec3D.ZERO); }
                    @Override public void onInteraction(EnumHand enumHand, Vec3D vec3D) {
                        PlayerUseUnknownEntityEvent e = new PlayerUseUnknownEntityEvent(
                                event.getPlayer(),
                                use.getEntityId(),
                                use.getActionType() == PacketPlayInUseEntity.b.ATTACK,
                                enumHand == EnumHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND
                        );
                        lime.invokeSync(() -> click(e));
                    }
                });
            }
        });
        DrawText.load(NPC_MANAGER);
        registry(npcData::stream);
    }

    public static HashMap<UUID, Set<String>> npc_show = new HashMap<>();
    public static void show(UUID uuid, String npc) {
        Set<String> npcList = npc_show.getOrDefault(uuid, null);
        if (npcList == null) npc_show.put(uuid, npcList = new HashSet<>());
        npcList.add(npc);
    }
    public static void hide(UUID uuid, String npc) {
        Set<String> npcList = npc_show.getOrDefault(uuid, null);
        if (npcList == null) return;
        npcList.remove(npc);
    }
    public static List<String> shows(UUID uuid) {
        List<String> list = new ArrayList<>();
        Set<String> npcList = npc_show.getOrDefault(uuid, null);
        if (npcList != null) list.addAll(npcList);
        return list;
    }

    public static void config(JsonObject json) {
        List<ConfigEPlayer> npcData = new ArrayList<>();

        lime.combineParent(json, true, true)
                .entrySet()
                .forEach(kv -> npcData.add(new ConfigEPlayer(kv.getKey(), kv.getValue().getAsJsonObject())));

        EPlayerModule.npcData.clear();
        EPlayerModule.npcData.addAll(npcData);

        Displays.uninitDisplay(NPC_MANAGER);
        Displays.initDisplay(NPC_MANAGER);
    }
    public static void click(PlayerUseUnknownEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.isAttack()) return;
        EPlayerDisplay display = getClick(event.getEntityId());
        if (display == null) return;
        Player player = event.getPlayer();
        boolean isShift = event.getPlayer().isSneaking();
        display.click(player, isShift);
    }
    private static final List<Func0<Stream<? extends IEPlayer>>> dataList = new ArrayList<>();
    public static void registry(Func0<Stream<? extends IEPlayer>> data) { dataList.add(data); }

    public static Map<String, IEPlayer> createData() {
        return dataList.stream()
                .flatMap(Func0::invoke)
                .collect(Collectors.toMap(IEPlayer::key, v -> v));
    }

    private static final EPlayerManager NPC_MANAGER = new EPlayerManager();
    private static final List<IEPlayer> npcData = new ArrayList<>();
    private static EPlayerDisplay getClick(int entityID) {
        for (EPlayerDisplay display : NPC_MANAGER.getDisplays().values()) {
            if (display.entityID == entityID)
                return display;
        }
        return null;
    }
}
