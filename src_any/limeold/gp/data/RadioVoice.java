package org.lime.gp.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.gp.player.voice.Voice;
import org.lime.system;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RadioVoice implements Listener {
    private static class PlayData {
        private final static ConcurrentHashMap<String, PlayData> playDatas = new ConcurrentHashMap<>();

        private final List<system.Toast4<Integer, byte[], Long, Long>> frames = new ArrayList<>();
        private final int total;
        private PlayData(JsonArray json) {
            system.Toast1<Integer> total = system.toast(0);
            json.getAsJsonArray().forEach(v -> {
                JsonObject item = v.getAsJsonObject();
                long time = item.get("time").getAsLong();
                long value = item.get("value").getAsLong();
                byte[] frame = Base64.getDecoder().decode(item.get("frame").getAsString());
                int ticks = (int)((time / 1000.0)*20);
                frames.add(system.toast(ticks, frame, value, time));
                total.val0 = Math.max(total.val0, ticks);
            });
            this.total = total.val0;
        }
        public void play(system.Action2<byte[], Long> frame, system.Action0 end) {
            lime.logOP("PLAY.0: " + total);
            frames.forEach(f -> lime.onceTicks(() -> frame.invoke(f.val1, f.val2), f.val0));
            lime.onceTicks(end, total);
        }
        public void play(system.Action3<byte[], Long, Long> frame, system.Action0 end) {
            lime.logOP("PLAY.1: " + total);
            frames.forEach(f -> lime.onceTicks(() -> frame.invoke(f.val1, f.val2, f.val3), f.val0));
            lime.onceTicks(end, total);
        }

        public static boolean reload() {
            HashMap<String, PlayData> playDatas = new HashMap<>();
            for (File file : lime.getConfigFile("voice").listFiles())
                playDatas.put(FilenameUtils.removeExtension(file.getName()), new PlayData(system.json.parse(lime.readAllText(file)).getAsJsonArray()));
            PlayData.playDatas.clear();
            PlayData.playDatas.putAll(playDatas);
            return true;
        }
        public static void play(String key, system.Action2<byte[], Long> frame, system.Action0 end) {
            lime.logOP("PRE.PLAY.0: " + key);
            playDatas.forEach((k,v) -> lime.logOP("VOICE: " + k));
            PlayData data = playDatas.getOrDefault(key, null);
            lime.logOP("PRE.PLAY.1: " + (data == null ? "NULL" : (data.frames.size() + " frames")));
            if (data == null) return;
            lime.logOP("PRE.PLAY.2: " + key);
            data.play(frame, end);
        }
        public static void play(String key, system.Action3<byte[], Long, Long> frame, system.Action0 end) {
            PlayData data = playDatas.getOrDefault(key, null);
            if (data == null) return;
            data.play(frame, end);
        }
        public static boolean play(String key, Player player, boolean log) {
            return play(key, player, -1, -1, log);
        }
        public static boolean play(String key, Player player, long from, long to, boolean log) {
            Voice.getConnectionOf(player.getUniqueId())
                    .ifPresent(connection -> {
                        Optional.ofNullable(playDatas.get(key)).ifPresent(data -> {
                            int delta = (int)((from / 1000.0) * 20);
                            UUID uuid = player.getUniqueId();
                            if (from < 0) from = 0;
                            if (to < 0) to = Long.MAX_VALUE;
                            long _from = from;
                            long _to = to;
                        })
                    });
            /*TODO*/
            /*SocketClientUDP socket = SocketServerUDP.clients.getOrDefault(player, null);
            if (socket == null) return true;
            PlayData data = playDatas.getOrDefault(key, null);
            if (data == null) return true;
            int delta = (int)((from / 1000.0) * 20);
            UUID uuid = player.getUniqueId();
            if (from < 0) from = 0;
            if (to < 0) to = Long.MAX_VALUE;
            long _from = from;
            long _to = to;
            data.frames.forEach(f -> {
                if (f.val3 < _from || f.val3 > _to) return;
                lime.onceTicks(() -> {
                    if (log) player.sendMessage(Component.text("[Log] Index: " + f.val3));
                    try { SocketServerUDP.sendTo(PacketUDP.write(new VoiceServerPacket(f.val1, uuid, f.val2, (short)8)), socket); } catch (Exception ignore) { }
                }, f.val0 - delta);
            });*/
            return true;
        }
    }
    public static core.element create() {
        return core.element.create(RadioVoice.class)
                .withInstance()
                .withInit(RadioVoice::init)
                .addCommand("voice.record", _v -> _v
                        .withCheck(v -> v.isOp() && v instanceof Player)
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> Arrays.asList("start", "save", "cancel", "play", "reload");
                            case 2 -> switch (args[0]) {
                                case "save" -> Collections.singletonList("[name]");
                                case "play", "part" -> PlayData.playDatas.keySet();
                                default -> Collections.emptyList();
                            };
                            case 3 -> switch (args[0]) {
                                case "play" -> Collections.singletonList("FROM:TO");
                                default -> Collections.emptyList();
                            };
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> switch (args.length) {
                            case 1 -> switch (args[0]) {
                                case "start" -> savedData.put(((Player)sender).getUniqueId(), new RecordData()) == null ? true : true;
                                case "cancel" -> savedData.remove(((Player)sender).getUniqueId()) == null ? true : true;
                                case "reload" -> PlayData.reload();
                                default -> false;
                            };
                            case 2 -> switch (args[0]) {
                                case "save" -> RecordData.save(savedData.remove(((Player)sender).getUniqueId()), args[1]) || PlayData.reload();
                                case "play" -> PlayData.play(args[1], (Player)sender, true);
                                default -> false;
                            };
                            case 3 -> switch (args[0]) {
                                case "play" -> PlayData.play(
                                        args[1],
                                        (Player)sender,
                                        Long.parseUnsignedLong(args[2].split(":")[0]),
                                        Long.parseUnsignedLong(args[2].split(":")[1]),
                                        true);
                                default -> false;
                            };
                            default -> false;
                        })
                );
    }
    private static ConcurrentHashMap<UUID, RecordData> savedData = new ConcurrentHashMap<>();
    private static class RecordData {
        public Long startTime = null;
        public final List<system.Toast3<Long, Long, byte[]>> frames = new LinkedList<>();
        public final void frame(long value, byte[] data) {
            long now = System.currentTimeMillis();
            if (startTime == null) startTime = now;
            long delta = now - startTime;
            frames.add(system.toast(delta, value, data));
        }

        public static boolean save(RecordData data, String name) {
            if (data == null) return true;
            lime.writeAllConfig("voice/" + name, system.json.array().add(data.frames, v -> system.json.object().add("time", v.val0).add("value", v.val1).add("frame", Base64.getEncoder().encodeToString(v.val2)).build()).build().toString());
            return true;
        }
    }
    public static void init() {
        lime.repeat(() -> savedData.entrySet().removeIf(kv -> Bukkit.getPlayer(kv.getKey()) == null), 1);
        PlayData.reload();
    }

    public enum Records {
        RADIO_NOISE("radio_noise"),
        RADIO_ON_NOISE("radio_on_noise"),
        RADIO_NOTIFY_NOISE("radio_notify_noise"),
        RADIO_OFF_NOISE("radio_off_noise");

        private final String name;
        Records(String name) {
            this.name = name;
        }

        public void play(system.Action2<byte[], Long> frame, system.Action0 end) { PlayData.play(name, frame, end); }
        public void play(system.Action2<byte[], Long> frame) { PlayData.play(name, frame, () -> {}); }
        public void play(Player player) { PlayData.play(name, player, false); }
    }

    /*@EventHandler public static void on(PlayerSpeakEvent e) {
        RecordData data = savedData.getOrDefault(e.getPlayer().getUniqueId(), null);
        if (data == null) return;
        data.frame(e.getSequenceNumber(), e.getData());
    }*/
}




















