package org.lime.gp.module.timeline;

import com.google.gson.JsonObject;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.minecraft.server.level.WorldServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.http.client.utils.URIBuilder;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.plugin.CoreElement;
import org.lime.system.ProgressInputStream;
import org.lime.system.Time;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Action2;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;
import org.lime.zip;

import javax.annotation.Nullable;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RegionDownloader {
    private static String URL;
    private static String PREFIX;
    private static String FOLDER;

    public static CoreElement create() {
        return CoreElement.create(RegionDownloader.class)
                .withInit(RegionDownloader::init)
                .withUninit(RegionDownloader::uninit)
                .<JsonObject>addConfig("timeline", v -> v
                        .withParent("download")
                        .withInvoke(json -> {
                            URL = json.get("url").getAsString();
                            PREFIX = json.get("prefix").getAsString();
                            FOLDER = json.get("folder").getAsString();
                        })
                        .withDefault(json.object()
                                .add("url", "http://127.0.0.1:80/timeline")
                                .add("prefix", "gpANY")
                                .add("folder", "gpANY/world/region/")
                                .build())
                );
    }

    private static final File tmpRegionFolder = lime.getConfigFile("tmp/timeline_region");
    private static final File lastRegionFile = lime.getConfigFile("last_timeline_region.zip");
    private static void deleteDirectory(File dir) {
        try { FileUtils.deleteDirectory(dir); }
        catch (Exception ignored) {}
    }

    private static void init() {
        if (tmpRegionFolder.exists()) deleteDirectory(tmpRegionFolder);
        tmpRegionFolder.mkdirs();

        CustomUI.addListener(new CustomUI.GUI(CustomUI.IType.ACTIONBAR) {
            @Override public Collection<ImageBuilder> getUI(Player player) {
                ProgressInfo info = progressInfoMap.get(player.getUniqueId());
                if (info == null) return Collections.emptyList();
                return Collections.singletonList(ImageBuilder.of(player, info.text()));
            }
        });
    }
    private static void uninit() {
        if (tmpRegionFolder.exists()) deleteDirectory(tmpRegionFolder);
    }

    public static boolean hasLast() {
        return lastRegionFile.exists();
    }

    private record ProgressInfo(int current, int total, String postfix) {
        public ProgressInfo(int current, int total) {
            this(current, total, "B");
        }

        public String text() {
            return total > 0 && total >= current
                    ? String.format("%d%s / %d%s (%d%%)", current, postfix, total, postfix, current * 100 / total)
                    : String.format("%d%s / ???%s (???%%)", current, postfix, postfix);
        }
    }
    private static final ConcurrentHashMap<UUID, ProgressInfo> progressInfoMap = new ConcurrentHashMap<>();

    private static Optional<Calendar> tryExportInfo(String fileName) {
        String[] parts = FilenameUtils.removeExtension(FilenameUtils.getName(fileName)).split("\\.");
        if (parts.length != 2) {
            lime.log("Try '" + fileName + "': ERROR IN PARTS");
            return Optional.empty();
        }
        try {
            Calendar calendar = Time.parseCalendar(parts[1], true);
            lime.log("Try '" + fileName + "': " + Time.formatCalendar(calendar, true));
            return Optional.of(calendar);
        } catch (Exception e) {
            lime.log("Try '" + fileName + "': ERROR " + e.getMessage());
            return Optional.empty();
        }
    }

    private static HttpResponse.BodyHandler<InputStream> ofStreamReader(Action2<HttpResponse.ResponseInfo, InputStream> beginReader) {
        return info -> {
            HttpResponse.BodySubscriber<InputStream> subscriber = HttpResponse.BodyHandlers.ofInputStream().apply(info);
            beginReader.invoke(info, (InputStream)subscriber);
            return subscriber;
        };
    }

    private static ProgressInputStream progressOf(InputStream base, @Nullable UUID owner) {
        return new ProgressInputStream(base) {
            @Override public void onProgress(int current, int total) {
                ProgressInfo info = new ProgressInfo(current, total);
                lime.log("[STREAM] Progress: " + info.text());
                if (owner != null) progressInfoMap.put(owner, info);
            }
            @Override public boolean isCanceled() {
                return false;
            }
            @Override public void close() throws IOException {
                super.close();
                lime.log("[STREAM] Progress ended");
                if (owner != null) progressInfoMap.remove(owner);
            }
        };
    }

    private static Optional<RegionTimeline> throwableDownloadRegion(WorldServer worldLoader, int regionX, int regionZ, @Nullable UUID owner) throws Throwable {
        String regionFileName = "r."+regionX+"."+regionZ+".mca";
        URI url = new URIBuilder(URL)
                .addParameter("prefix", PREFIX)
                .addParameter("file", FOLDER + regionFileName)
                .build();

        HttpURLConnection connection = (HttpURLConnection)url.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.connect();
        try {
            if (connection.getResponseCode() != 200)
                throw new IllegalArgumentException("Error! Response code: " + HttpResponseStatus.valueOf(connection.getResponseCode()));
            int zipCountFiles = Integer.parseInt(connection.getHeaderField("Zip-Count-Files"));
            return throwableReadRegion(connection.getInputStream(), zipCountFiles, worldLoader, regionX, regionZ, owner, true);
        } finally {
            connection.disconnect();
        }
    }
    private static Optional<RegionTimeline> throwableLoadLastRegion(WorldServer worldLoader, @Nullable UUID owner) throws Throwable {
        try (FileInputStream input = new FileInputStream(lastRegionFile)) {
            Path path = lastRegionFile.toPath();
            int regionCount = Integer.parseInt(Files.getAttribute(path, "user:region_count").toString());
            int regionX = Integer.parseInt(Files.getAttribute(path, "user:region_x").toString());
            int regionZ = Integer.parseInt(Files.getAttribute(path, "user:region_z").toString());
            return throwableReadRegion(input, regionCount, worldLoader, regionX, regionZ, owner, false);
        }
    }
    private static Optional<RegionTimeline> throwableReadRegion(InputStream input, int zipCountFiles, WorldServer worldLoader, int regionX, int regionZ, @Nullable UUID owner, boolean saveToLast) throws Throwable {
        FileOutputStream lastFileStream = saveToLast ? new FileOutputStream(lastRegionFile) : null;
        int regionCount = 0;
        try {
            try (TempRegionFileCache cache = TempRegionFileCache.overrideFolder(tmpRegionFolder.toPath())) {
                Toast1<Integer> progress = Toast.of(0);
                if (lastFileStream != null) input = new TeeInputStream(input, lastFileStream);
                RegionTimeline timeline = new RegionTimeline(regionX, regionZ, zip.unzipStream(input)
                        .flatMap(entry -> {
                            progress.val0++;
                            ProgressInfo info = new ProgressInfo(progress.val0, zipCountFiles, "f");
                            if (owner != null) progressInfoMap.put(owner, info);
                            lime.log("Progress entry '" + entry.name() + "' | " + info.text());
                            return tryExportInfo(entry.name()).map(v -> Toast.of(v, entry)).stream();
                        })
                        .map(v -> {
                            try {
                                return Toast.of(v.val0, cache.appendAndReadRawRegion(v.val1.stream(), worldLoader, regionX, regionZ));
                            } catch (Exception e) {
                                throw new IllegalArgumentException(e);
                            }
                        }));
                regionCount = timeline.getCount();
                return Optional.of(timeline);
            } catch (Throwable e) {
                lime.logStackTrace(e);
                return Optional.empty();
            }
        } finally {
            if (lastFileStream != null) {
                lastFileStream.close();
                if (regionCount > 0) {
                    Path path = lastRegionFile.toPath();
                    Files.setAttribute(path, "user:region_count", String.valueOf(regionCount));
                    Files.setAttribute(path, "user:region_x", String.valueOf(regionX));
                    Files.setAttribute(path, "user:region_z", String.valueOf(regionZ));
                } else {
                    lastRegionFile.delete();
                }
            }
            if (owner != null)
                progressInfoMap.remove(owner);
        }
    }

    public static void loadLastRegion(WorldServer worldLoader, @Nullable UUID owner, Action1<Optional<RegionTimeline>> callback) {
        lime.invokeAsync(() -> {
            try {
                return throwableLoadLastRegion(worldLoader, owner);
            } catch (Throwable e) {
                lime.logStackTrace(e);
                return Optional.empty();
            }
        }, callback);
    }
    public static void downloadRegion(WorldServer worldLoader, int regionX, int regionZ, @Nullable UUID owner, Action1<Optional<RegionTimeline>> callback) {
        lime.invokeAsync(() -> {
            try {
                return throwableDownloadRegion(worldLoader, regionX, regionZ, owner);
            } catch (Throwable e) {
                lime.logStackTrace(e);
                return Optional.empty();
            }
        }, callback);
    }
}












