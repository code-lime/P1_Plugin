package org.lime.gp.module.timeline;

import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.lime.gp.module.ChunkForceView;
import org.lime.system.Time;
import org.lime.system.toast.Toast2;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class RegionTimeline {
    private final int regionX;
    private final int regionZ;

    private boolean enable = false;
    private int timePerSec = 1;
    private @Nullable Calendar current = null;
    private record FrameData(Calendar time, long tick){}
    private @Nullable FrameData lastFrameData = null;

    private final LinkedHashMap<Calendar, HashMap<ChunkCoordIntPair, ProtoChunk>> frames = new LinkedHashMap<>();

    public RegionTimeline(int regionX, int regionZ, Map<Calendar, Map<ChunkCoordIntPair, ProtoChunk>> frames) {
        this.regionX = regionX;
        this.regionZ = regionZ;

        frames.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(kv -> this.frames.put(kv.getKey(), new HashMap<>(kv.getValue())));
    }
    public RegionTimeline(int regionX, int regionZ, Stream<Toast2<Calendar, Map<ChunkCoordIntPair, ProtoChunk>>> frames) {
        this.regionX = regionX;
        this.regionZ = regionZ;

        frames
                .sorted(Comparator.comparing(Toast2::get0))
                .forEach(kv -> this.frames.put(kv.val0, new HashMap<>(kv.val1)));
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
    public void setFrame(WorldServer world, Calendar calendar) {
        current = (Calendar)calendar.clone();
        Calendar currentFrame = getNextTime(current);
        if (lastFrameData == null || !lastFrameData.time.equals(currentFrame))
            loadTime(world, currentFrame);
    }
    public void addFrame(WorldServer world, int timeSec) {
        if (current == null) current = getFirstTime();
        current.add(Calendar.SECOND, timeSec);
        setFrame(world, current);
    }
    public void setTimePerSec(int timePerSec) {
        this.timePerSec = timePerSec;
    }
    public void onTick(WorldServer world) {
        if (!enable) return;
        if (current == null) current = getFirstTime();

        Calendar currentFrame = getNextTime(current);
        if (lastFrameData == null || !lastFrameData.time.equals(currentFrame))
            loadTime(world, currentFrame);
        current.add(Calendar.SECOND, timePerSec);
    }

    public void loadTime(WorldServer world, Calendar calendar) {
        Map<ChunkCoordIntPair, ProtoChunk> map = getByTime(calendar);
        load(world, getByTime(calendar));
        lastFrameData = new FrameData(calendar, map.values().stream().mapToLong(TempRegionFileCache::getLastWorldSaveTime).max().orElse(0));
    }
    public Map<ChunkCoordIntPair, ProtoChunk> getByTime(Calendar calendar) {
        Map<ChunkCoordIntPair, ProtoChunk> last = frames.get(calendar);
        return Collections.unmodifiableMap(Objects.requireNonNullElseGet(last, () -> frames.get(getNextTime(calendar))));
    }
    public boolean isEmpty() {
        return frames.isEmpty();
    }
    public int getCount() {
        return frames.size();
    }

    public int getX() {
        return regionX;
    }
    public int getZ() {
        return regionZ;
    }

    public Set<Calendar> getTimes() {
        return Collections.unmodifiableSet(frames.keySet());
    }
    public Calendar getNextTime(Calendar calendar) {
        long time = calendar.getTimeInMillis();
        Calendar last = null;
        for (Calendar frame : frames.keySet()) {
            if (last != null && frame.getTimeInMillis() > time) break;
            last = frame;
        }
        return (Calendar)Objects.requireNonNull(last).clone();
    }
    public Calendar getFirstTime() {
        Calendar calendar = null;
        for (Calendar frame : frames.keySet()) {
            calendar = frame;
            break;
        }
        return (Calendar)Objects.requireNonNull(calendar).clone();
    }
    public Calendar getLastTime() {
        Calendar calendar = null;
        for (Calendar frame : frames.keySet()) calendar = frame;
        return (Calendar)Objects.requireNonNull(calendar).clone();
    }

    public String drawInfo() {
        if (current == null) current = getFirstTime();

        return (enable ? "[PLAYING] " : "[PAUSE] ")
                + "Region r." + regionX + "." + regionZ + ".mca "
                + "Now: " + Time.formatCalendar(current, true) + " "
                + "Frame: " + (lastFrameData == null ? "NULL" : Time.formatCalendar(lastFrameData.time, true)) + " "
                + "Tick: " + (lastFrameData == null ? "NULL" : lastFrameData.tick) + " "
                + "(" + Time.formatTotalTime(timePerSec, Time.Format.FORMATTED) + " f/s)";
    }

    public static void load(WorldServer world, Map<ChunkCoordIntPair, ProtoChunk> map) {
        ProtoChunkUtils.forceUpdateChunks(world, map);
        ChunkForceView.update();
    }
}










