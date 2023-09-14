package org.lime.gp.module;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.Streams;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.gp.admin.AnyEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PacketLogger {
    public static String key(PacketContainer container) { return container.getHandle().getClass().getName(); }
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> logger = new ConcurrentHashMap<>();
    private static final PacketAdapter adapter = new PacketAdapter(lime._plugin, Streams.stream(PacketType.values()).filter(PacketType::isSupported).collect(Collectors.toList())) {
        @Override public void onPacketReceiving(PacketEvent event) { call(event); }
        @Override public void onPacketSending(PacketEvent event) { call(event); }
        public void call(PacketEvent event) {
            String uuid = event.getPlayer().getUniqueId().toString();
            PacketContainer packet = event.getPacket();
            String key = key(packet);
            logger.compute(key, (k,v) -> {
                v = v == null ? new ConcurrentHashMap<>() : v;
                v.compute("total", (_k,_v) -> (_v == null ? 0 : _v) + 1);
                v.compute(uuid, (_k,_v) -> (_v == null ? 0 : _v) + 1);
                return v;
            });
        }
    };
    private static boolean isAdded = false;
    public static CoreElement create() {
        return CoreElement.create(PacketLogger.class)
                .withInit(PacketLogger::init);
    }
    public static void init() {
        AnyEvent.addEvent("packet.logger", AnyEvent.type.owner_console, b -> b.createParam("play", "pause", "save", "reset"), (p, state) -> {
            switch (state) {
                case "play": {
                    if (isAdded) return;
                    ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
                    isAdded = true;
                    lime.logOP("PLAYED PACKET LOGGER");
                    return;
                }
                case "pause": {
                    if (!isAdded) return;
                    ProtocolLibrary.getProtocolManager().removePacketListener(adapter);
                    isAdded = false;
                    lime.logOP("PAUSED PACKET LOGGER");
                    return;
                }
                case "save": {
                    List<Toast2<String, List<Toast2<String, Integer>>>> data = new ArrayList<>();
                    logger.forEach((k,v) -> {
                        List<Toast2<String, Integer>> users = new ArrayList<>();
                        v.forEach((_k,_v) -> users.add(Toast.of(_k, _v)));
                        users.sort(Comparator.comparingInt(__v -> __v.val1));
                        data.add(Toast.of(k, users));
                    });
                    data.sort(Comparator.comparingInt(v -> v.val1.get(v.val1.size() - 1).val1));
                    lime.writeAllConfig("packet_logger", json.format(json.object().add(data, kv -> kv.val0, kv -> json.object().add(kv.val1, _kv -> _kv.val0, _kv -> _kv.val1)).build()));
                    lime.logOP("SAVED PACKET LOGGER");
                    return;
                }
                case "reset": {
                    logger.clear();
                    lime.logOP("RESETTED PACKET LOGGER");
                    return;
                }
            }
        });
    }
}


























































