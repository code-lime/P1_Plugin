package org.lime.gp.block.component.data.voice;

import com.google.common.primitives.Ints;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import org.apache.commons.lang.StringUtils;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.RecorderComponent;
import org.lime.gp.database.Tables;
import org.lime.gp.extension.MapUUID;
import org.lime.gp.lime;
import org.lime.gp.module.Discord;
import org.lime.gp.module.ThreadPool;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.player.voice.Radio;
import org.lime.gp.player.voice.Voice;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class RecorderInstance extends BlockComponentInstance<RecorderComponent> implements CustomTileMetadata.Tickable, CustomTileMetadata.Removeable, CustomTileMetadata.FirstTickable {
    public static core.element create() {
        return core.element.create(RecorderInstance.class)
                .withInit(RecorderInstance::init);
    }
    public static final system.LockToast2<Long, Long> nextAsyncTimes = system.toast(0L, 0L).lock();
    public static void init() {
        AnyEvent.addEvent("convert.bifs", AnyEvent.type.owner_console, p -> convertAllToBIFs());
        AnyEvent.addEvent("recorder.play", AnyEvent.type.other, v -> v
                        .createParam(UUID::fromString, "[block_uuid:uuid]")
                        .createParam(Integer::parseInt, "[x:int]")
                        .createParam(Integer::parseInt, "[y:int]")
                        .createParam(Integer::parseInt, "[z:int]")
                        .createParam(UUID::fromString, "[sound:uuid]"),
                (p, block_uuid, x, y, z, sound) -> org.lime.gp.block.Blocks.of(p.getWorld().getBlockAt(x,y,z))
                        .flatMap(org.lime.gp.block.Blocks::customOf)
                        .filter(v -> v.key.uuid().equals(block_uuid))
                        .flatMap(v -> v.list(RecorderInstance.class).findAny())
                        .ifPresent(instance -> instance.startPlay(sound))
        );
        AnyEvent.addEvent("recorder.stop", AnyEvent.type.other, v -> v
                        .createParam(UUID::fromString, "[block_uuid:uuid]")
                        .createParam(Integer::parseInt, "[x:int]")
                        .createParam(Integer::parseInt, "[y:int]")
                        .createParam(Integer::parseInt, "[z:int]"),
                (p, block_uuid, x, y, z) -> org.lime.gp.block.Blocks.of(p.getWorld().getBlockAt(x,y,z))
                        .flatMap(org.lime.gp.block.Blocks::customOf)
                        .filter(v -> v.key.uuid().equals(block_uuid))
                        .flatMap(v -> v.list(RecorderInstance.class).findAny())
                        .ifPresent(RecorderInstance::stopPlay)
        );
        AnyEvent.addEvent("recorder.discord", AnyEvent.type.other,
                (p) -> Tables.DISCORD_TABLE.getBy(v -> v.uuid.equals(p.getUniqueId()))
                        .ifPresent(row -> Discord.sendRecord(row.discordID))
        );

        AnyEvent.addEvent("delta.show.recorder", AnyEvent.type.owner_console, p -> lime.logOP("[Recorder] DeltaShow:\n" + nextAsyncTimes.call(v -> String.join("\n",
                " - Last call: " + system.formatCalendar(system.getMoscowTime(v.val0), true),
                " - Delta call: " + v.val1 + "ms"
        ))));
        ThreadPool.Type.Async.executeRepeat(() -> TimeoutData.values(MusicPlayer.class).forEach(MusicPlayer::nextFrame), nextAsyncTimes);
    }

    public RecorderInstance(RecorderComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    @Override public void onFirstTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        syncDisplayVariable();
        saveData();
    }

    /*private static class MusicPlayer extends TimeoutData.ITimeout {
        public final UUID sound;
        public final Radio.SenderInfo sender;
        public final long startTime;
        public final AudioSupplier supplier;
        public final OpusEncoder encoder;
        public final system.LockToast1<UUID> connection = system.<UUID>toast(null).lock();
        public final system.LockToast1<Boolean> active = system.toast(true).lock();
        public int framePosition = 0;
        public long waitFrame = 0;

        public MusicPlayer(UUID sound, Radio.SenderInfo sender, short[] audio) {
            this.sound = sound;
            this.sender = sender;
            this.supplier = new AudioSupplier(audio);
            this.encoder = Voice.API.createEncoder();
            this.startTime = System.nanoTime();
        }

        public boolean isActive() { return active.get0(); }
        public boolean nextFrame() {
            if (!active.get0()) return false;
            UUID connection = this.connection.get0();
            while (System.nanoTime() >= waitFrame) {
                short[] frame;
                if ((frame = supplier.get()) == null) {
                    active.set0(false);
                    return false;
                }

                if (frame.length != 960) {
                    lime.logOP("Got invalid audio frame size " + frame.length + " != " + 960);
                    continue;
                }

                if (connection != null) {
                    try {
                        byte[] bytes = encoder.encode(frame);
                        TimeoutData.values(RadioInstance.RadioVoiceData.class)
                                .filter(v -> connection.equals(v.unique))
                                .forEach(v -> {
                                    if (v.state.isOutput) v.play(sender, bytes);
                                    if (v.state.isInput) Radio.playRadio(sender, v.location, v.total_distance, v.level, Voice.modifyVolume(bytes, v.volume));
                                });
                    } catch (Throwable e) {
                        active.set0(false);
                        return false;
                    }
                }

                framePosition++;
                waitFrame = startTime + (long)framePosition * 20000000L;
            }
            return true;
        }
    }*/
    private static class MusicPlayer extends TimeoutData.ITimeout {
        public final UUID sound;
        public final Radio.SenderInfo sender;
        public final long startTime;
        public final byte[][] bifs;
        public final system.LockToast1<UUID> connection = system.<UUID>toast(null).lock();
        public final system.LockToast1<Boolean> active = system.toast(true).lock();
        public int position = 0;
        public long waitFrame = 0;

        public MusicPlayer(UUID sound, Radio.SenderInfo sender, byte[][] bifs) {
            this.sound = sound;
            this.sender = sender;
            this.bifs = bifs;
            this.startTime = System.nanoTime();
        }

        public boolean isActive() { return active.get0(); }
        public boolean nextFrame() {
            if (!active.get0()) return false;
            UUID connection = this.connection.get0();
            while (System.nanoTime() >= waitFrame) {
                if (position >= bifs.length) {
                    active.set0(false);
                    return false;
                }

                if (connection != null) {
                    try {
                        TimeoutData.values(RadioInstance.RadioVoiceData.class)
                                .filter(v -> connection.equals(v.unique))
                                .forEach(v -> {
                                    if (v.state.isOutput) v.play(sender, bifs[position], v.level, v.total_distance);
                                    if (v.state.isInput) Radio.playRadio(sender, v.location, v.total_distance, v.level, Voice.modifyVolume(sender, MapUUID.of("radio.block.recorder", sender.uuid(), v.unique), bifs[position], v.volume, 0));
                                });
                    } catch (Throwable e) {
                        active.set0(false);
                        return false;
                    }
                }

                position++;
                waitFrame = startTime + (long)position * 20000000L;
            }
            return true;
        }
    }

    @Nullable private UUID connectionUUID;
    @Nullable private MusicPlayer musicPlayer;

    @Override public void read(JsonObjectOptional json) {
        syncDisplayVariable();
    }
    @Override public system.json.builder.object write() {
        return system.json.object();
    }

    private int tick = 0;
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        tick = (tick + 1) % 20;
        if (tick == 0) onConnectionTick(metadata);
        if (connectionUUID != null) TimeoutData.put(connectionUUID, Radio.RadioLockTimeout.class, new Radio.RadioLockTimeout());
        if (musicPlayer != null) {
            if (musicPlayer.isActive()) TimeoutData.put(unique(), MusicPlayer.class, musicPlayer);
            else {
                musicPlayer = null;
                TimeoutData.remove(unique(), MusicPlayer.class);
                syncDisplayVariable();
            }
        }
    }
    @Override public void onRemove(CustomTileMetadata metadata, TileEntitySkullEventRemove event) {
        TimeoutData.remove(unique(), MusicPlayer.class);
    }
    public void startPlay(UUID sound) {
        if (musicPlayer != null) {
            musicPlayer = null;
            TimeoutData.remove(unique(), MusicPlayer.class);
        }
        /*musicPlayer = new MusicPlayer(sound, Radio.SenderInfo.block(unique(), metadata().position()), Voice.API.getAudioConverter().bytesToShorts(system.funcEx(Files::readAllBytes)
                .throwable()
                .invoke(lime.getConfigFile("sounds/" + sound + ".bin").toPath())));*/
        musicPlayer = new MusicPlayer(sound, Radio.SenderInfo.block(unique(), metadata().position()), readBIFs(system.funcEx(Files::readAllBytes)
                .throwable()
                .invoke(lime.getConfigFile("sounds/" + sound + ".bif").toPath())));
        musicPlayer.connection.set0(connectionUUID);
        syncDisplayVariable();
    }
    public void stopPlay() {
        if (musicPlayer == null) return;
        musicPlayer = null;
        TimeoutData.remove(unique(), MusicPlayer.class);
        syncDisplayVariable();
    }

    public static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000.0f, 16, 1, 2, 48000.0f, false);

    public enum AudioType {
        MP3,
        WAV
    }
    
    private static byte[][] generateFrames(byte[] sound, system.Action2<Integer, Integer> progress) {
        short[] shorts = Voice.API.getAudioConverter().bytesToShorts(sound);
        int shortLength = shorts.length;
        int frameCount = (int)Math.ceil(shorts.length / 960.0);
        byte[][] frames = new byte[frameCount][];
        OpusEncoder encoder = Voice.API.createEncoder();
        for (int i = 0; i < frameCount; i++) {
            progress.invoke(i, frameCount);
            int framePosition = i * 960;
            short[] frame = new short[960];
            System.arraycopy(shorts, framePosition, frame, 0, Math.min(960, shortLength - framePosition));
            frames[i] = encoder.encode(frame);
        }
        progress.invoke(frameCount, frameCount);
        return frames;
    }

    public interface ISoundOut {
        default void goodOrError(system.Action1<GoodOut> good, system.Action1<ErrorOut> error) {
            if (this instanceof GoodOut _good) good.invoke(_good);
            else if (this instanceof ErrorOut _error) error.invoke(_error);
        }
    }
    public record GoodOut(UUID soundUUID, double totalSec) implements ISoundOut {}
    public record ErrorOut(String text) implements ISoundOut {}

    public static ISoundOut createSoundFile(AudioType audioType, system.Action2<Integer, Integer> bifProgress, byte[] audio) {
        if (audio.length > 10 * 1024 * 1024) return new ErrorOut("Максимальный размер файла - 10MB");
        try (ByteArrayInputStream file = new ByteArrayInputStream(audio)) {
            AudioInputStream finalInputStream = switch (audioType) {
                case WAV -> {
                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
                    yield AudioSystem.getAudioInputStream(FORMAT, inputStream);
                }
                case MP3 -> {
                    AudioInputStream inputStream = new MpegAudioFileReader().getAudioInputStream(file);

                    AudioFormat baseFormat = inputStream.getFormat();

                    AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getFrameRate(), false);
                    AudioInputStream convertedInputStream = new MpegFormatConversionProvider().getAudioInputStream(decodedFormat, inputStream);
                    yield AudioSystem.getAudioInputStream(FORMAT, convertedInputStream);
                }
                default -> throw new IllegalArgumentException("Формат '" + audioType + "' не поддерживается!");
            };

            byte[] bytes = finalInputStream.readAllBytes();

            byte[] bifs = writeBIFs(generateFrames(bytes, bifProgress));

            UUID uuid = UUID.randomUUID();
            Files.write(lime.getConfigFile("sounds/" + uuid + ".bif").toPath(), bifs);
            return new GoodOut(uuid, -1);
        } catch (AssertionError e) {
            String message = e.getMessage();
            return new ErrorOut(message == null ? "Ошибка чтения файла! Данный файл не поддерживается либо содержит ошибку!" : message);
        } catch (Throwable e) {
            String message = e.getMessage();
            return new ErrorOut(message == null ? "Неизвестная ошибка" : message);
        } 
    }

    private static byte[] writeBIFs(byte[][] frames) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (byte[] frame : frames) {
            stream.writeBytes(Ints.toByteArray(frame.length));
            stream.writeBytes(frame);
        }
        return stream.toByteArray();
    }
    private static byte[][] readBIFs(byte[] bytes) {
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        List<byte[]> frames = new ArrayList<>();
        byte[] fLength = new byte[4];
        while (stream.read(fLength, 0, 4) != -1) {
            byte[] frame = new byte[Ints.fromByteArray(fLength)];
            stream.read(frame, 0, frame.length);
            frames.add(frame);
        }
        return frames.toArray(byte[][]::new);
    }
    private static void convertAllToBIFs() {
        try {
            HashSet<UUID> files = new HashSet<>();
            HashSet<UUID> conterted = new HashSet<>();
            for (File file : Objects.requireNonNull(lime.getConfigFile("sounds/").listFiles())) {
                String[] fileName = file.getName().split("\\.", 2);
                if (fileName.length != 2) continue;
                UUID uuid;
                try { uuid = UUID.fromString(fileName[0]); } catch (Exception e) { continue; }
                switch (fileName[1]) {
                    case "bin" -> files.add(uuid);
                    case "bif" -> conterted.add(uuid);
                }
            }
            files.removeAll(conterted);
            files.forEach(uuid -> {
                lime.invokeAsync(() -> {
                    lime.logOP("Start convert file '" + uuid + ".bin'");
                    try {
                        byte[] bytes = Files.readAllBytes(lime.getConfigFile("sounds/" + uuid + ".bin").toPath());
                        system.Toast1<Integer> progress = system.toast(-1);
                        byte[][] frames = generateFrames(bytes, (current, total) -> {
                            int newProgress = current * 100 / total;
                            if (progress.val0 == newProgress) return;
                            progress.val0 = newProgress;
                            lime.logOP("["+ StringUtils.leftPad(newProgress + "", 3, '_') +"%] Converted file to '" + uuid + ".bif': " + current + " / " +total + "...");
                        });
                        byte[] bifs = writeBIFs(frames);
                        Files.write(lime.getConfigFile("sounds/" + uuid + ".bif").toPath(), bifs);
                        lime.logOP("Converted file to '" + uuid + ".bif'");
                    } catch (Exception e) {
                        lime.logOP("Error convert file '" + uuid + ".bin'");
                        lime.logStackTrace(e);
                    }
                }, () -> {});
            });
        } catch (Exception e) {
            lime.logStackTrace(e);
        }
    }

    public void onConnectionTick(CustomTileMetadata metadata) {
        TileEntityLimeSkull skull = metadata.skull;
        skull.getLevel().getBlockEntity(skull.getBlockPos().above(), TileEntityTypes.SKULL)
                .flatMap(Blocks::customOf)
                .flatMap(target -> target.list(RadioInstance.class).findAny())
                .map(BlockInstance::unique)
                .ifPresentOrElse(target -> {
                    if (connectionUUID == target) return;
                    connectionUUID = target;
                    if (musicPlayer != null) musicPlayer.connection.set0(connectionUUID);
                    syncDisplayVariable();
                }, () -> {
                    if (connectionUUID == null) return;
                    TimeoutData.remove(connectionUUID, Radio.RadioLockTimeout.class);
                    connectionUUID = null;
                    if (musicPlayer != null) musicPlayer.connection.set0(null);
                    syncDisplayVariable();
                });
    }
    private void syncDisplayVariable() {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> {
            display.set("recorder_connected", connectionUUID == null ? "false" : "true");
            display.set("recorder_sound", musicPlayer == null ? "null" : musicPlayer.sound + "");
        });
    }
}



















