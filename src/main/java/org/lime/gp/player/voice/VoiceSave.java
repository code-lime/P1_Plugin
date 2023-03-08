package org.lime.gp.player.voice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.player.voice.Radio.RadioElement;
import org.lime.gp.player.voice.Radio.SenderInfo;
import org.lime.gp.player.voice.Wave.WavFile;

import com.google.gson.JsonObject;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;

public class VoiceSave implements RadioElement {
    private static final VoiceSave instance = new VoiceSave();
    public static org.lime.core.element create() {
        return org.lime.core.element.create(VoiceSave.class)
                .disable()
                .withInstance(instance)
                .withInit(VoiceSave::init)
                .<JsonObject>addConfig("voice_save", v -> v
                        .withDefault(system.json.object().add("levels", system.json.array()).build())
                        .withInvoke(VoiceSave::config)
                );
    }
    
    public static void init() {
        Radio.addListener(() -> Stream.of(instance));
        AnyEvent.addEvent("tmp.audio", AnyEvent.type.owner, p -> {
            saveQueue.entrySet().forEach(kv -> {
                lime.logOP("T.A.0");
                VoiceFrame lastFrame = kv.getValue().peek();
                lime.logOP("T.A.1");
                if (lastFrame == null) return;
                lime.logOP("T.A.2");
                try {
                    lime.logOP("T.A.3");
                    Files.write(lime.getConfigFile("tmp.wav").toPath(), saveFrames(kv.getValue().toArray(VoiceFrame[]::new)));
                    lime.logOP("T.A.4");
                } catch (IOException e) {
                    lime.logStackTrace(e);
                }
                kv.getValue().clear();
            });
        });
        /*lime.timer()
            .setAsync()
            .withLoop(1.0)
            .withCallback(() -> {
                long saveTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
                saveQueue.entrySet().forEach(kv -> {
                    VoiceFrame lastFrame = kv.getValue().peek();
                    if (lastFrame == null) return;
                    if (lastFrame.initTime >= saveTime) return;
                    saveFrames(kv.getValue().toArray(VoiceFrame[]::new));
                    kv.getValue().clear();
                });
            })
            .run();*/
    }


    private static long FRAME_LENGTH = 20000000L;
    private static byte[] saveFrames(VoiceFrame[] frames) {
        HashMap<UUID, system.Toast2<short[][], OpusDecoder>> frameMap = new HashMap<>();
        int startIndex = (int)(frames[0].initTime / FRAME_LENGTH);
        int endIndex = (int)(frames[frames.length-1].initTime / FRAME_LENGTH);

        int length = endIndex - startIndex + 1;
        for (VoiceFrame frame : frames) {
            int index = (int)(frame.initTime / FRAME_LENGTH) - startIndex;
            if (index >= length || index < 0) continue;
            system.Toast2<short[][], OpusDecoder> memory = frameMap.computeIfAbsent(frame.uuid, uuid -> {
                short[][] arr = new short[length][];
                Arrays.fill(arr, new short[960]);
                OpusDecoder decoder = Voice.API.createDecoder();
                return system.toast(arr, decoder);
            });
            memory.val0[index] = memory.val1.decode(frame.data);
        }
        long[][] preResultMemory = new long[length][];
        for (int i = 0; i < length; i++) preResultMemory[i] = new long[960];

        for (system.Toast2<short[][], OpusDecoder> memory : frameMap.values())
            for (int i = 0; i < length; i++)
                for (int f = 0; f < 960; f++)
                    preResultMemory[i][f] += memory.val0[i][f];

        int count = frameMap.size();
        short[] resultMemory = new short[length * 960];
        for (int i = 0; i < length; i++) {
            long[] preMemory = preResultMemory[i];
            for (int f = 0; f < 960; f++)
                resultMemory[i * 960 + f] = (short)(preMemory[f] / count);
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WavFile writeWavFile = WavFile.newWavFile(out, 1, length, 16, 48000);
            writeWavFile.writeFrames(resultMemory, length);
            writeWavFile.close();
            return out.toByteArray();
        } catch (Throwable e) {
            lime.logStackTrace(e);
        }
        

        /*byte[] audio = Voice.API.getAudioConverter().shortsToBytes(resultMemory);
        try (ByteArrayInputStream file = new ByteArrayInputStream(audio)) {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, out);
                return out.toByteArray();
            }
        } catch (Throwable e) {
            lime.logStackTrace(e);
        }*/
        return null;
    }

    private record VoiceFrame(long initTime, UUID uuid, byte[] data) {
    }

    private static final UUID uniqueID = UUID.randomUUID();
    private static ConcurrentHashMap<Integer, ConcurrentLinkedQueue<VoiceFrame>> saveQueue = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<VoiceFrame>>();

    public static void config(JsonObject json) {
        HashMap<Integer, ConcurrentLinkedQueue<VoiceFrame>> saveQueue = new HashMap<Integer, ConcurrentLinkedQueue<VoiceFrame>>();
        json.getAsJsonArray("levels").forEach(level -> saveQueue.put(level.getAsInt(), new ConcurrentLinkedQueue<VoiceFrame>()));
        VoiceSave.saveQueue.clear();
        VoiceSave.saveQueue.putAll(saveQueue);
    }

    @Override public boolean hasLevel(int level) {
        return saveQueue.containsKey(level);
    }
    @Override public UUID unique() { return uniqueID; }
    @Override public boolean isDistance(Location location, double total_distance) { return true; }
    @Override public short distance() { return 0; }

    @Override public void play(SenderInfo info, byte[] data, int level, double total_distance) {
        lime.logOP("P.0: " + level);
        ConcurrentLinkedQueue<VoiceFrame> queue = saveQueue.get(level);
        if (queue == null) return;
        queue.add(new VoiceFrame(System.currentTimeMillis(), info.uuid(), data));
        lime.logOP("P.1: " + level + " > " + queue.size());
    }
}
