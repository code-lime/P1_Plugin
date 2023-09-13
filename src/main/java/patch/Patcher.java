package patch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.Main;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.lime.json.JsonElementOptional;
import org.lime.reflection;
import org.lime.system;

import java.io.Closeable;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

public class Patcher {
    public static void patch(URL url) {
        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            throwablePatch(connection.getInputStream().readAllBytes());
        }
        catch (Throwable e) { throw new IllegalArgumentException(e); }
    }
    public static void patch(byte[] resource) {
        try { throwablePatch(resource); }
        catch (Throwable e) { throw new IllegalArgumentException(e); }
    }

    private static Path of(Class<?> tClass) throws Throwable {
        return new File(tClass.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath();
    }
    private static Path loadBaseFile(Path base, boolean resetCacheOriginal) throws Throwable {
        Path dir = base.getParent();
        Path orig = Paths.get(
                dir.toString(),
                FilenameUtils.getBaseName(base.toString()) + "-orig." + FilenameUtils.getExtension(base.toString())
        );
        if (resetCacheOriginal || !Files.exists(orig)) Files.copy(base, orig, StandardCopyOption.REPLACE_EXISTING);
        return orig;
    }

    private static String miniSha1(byte[]... parts) throws Throwable {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        for (byte[] part : parts) md.update(part);
        return hex(md.digest()).substring(5, 15);
    }
    private static String miniSha1(Stream<byte[]> parts) throws Throwable {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        parts.forEach(md::update);
        return hex(md.digest()).substring(5, 15);
    }

    private static String hex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) formatter.format("%02x", b);
        return formatter.toString();
    }

    private static system.Toast2<String, String> calculateVersion(JarArchive plugin_archive, JsonObject patch_data) throws Throwable {
        Native.log("Calculate patch version...");
        List<system.Toast3<String, byte[], Boolean>> checkFiles = new ArrayList<>();
        try (var ignored = Native.subLog()) {
            patch_data.getAsJsonArray("append").forEach(append -> {
                String name = append.getAsString();
                checkFiles.add(system.toast(name, plugin_archive.entries.get(name), true));
            });
            Stream.concat(
                    patch_data.getAsJsonArray("append_regex").asList().stream().map(JsonElement::getAsString).map(v -> system.toast(v, true)),
                    Stream.of(system.toast("patch\\/.*", false))
            ).forEach(v -> v.invoke((regex, patch) -> Native.subLog("Append group '"+regex+"':", () -> plugin_archive.entries
                    .entrySet()
                    .stream()
                    .filter(kv -> system.compareRegex(kv.getKey(), regex))
                    .forEach(kv -> checkFiles.add(system.toast(kv.getKey(), kv.getValue(), patch))))));
        }
        checkFiles.sort(Comparator.comparing(v -> v.val0));
        return system.toast(
                miniSha1(checkFiles.stream().flatMap(v -> Stream.of(v.val0.getBytes(), v.val1))),
                miniSha1(checkFiles.stream().filter(v -> v.val2).flatMap(v -> Stream.of(v.val0.getBytes(), v.val1)))
        );
    }

    private static void throwablePatch(byte[] resource) throws Throwable {
        Native.log("Check patch...");
        Path paper_path = Paths.get(ManagementFactory.getRuntimeMXBean().getClassPath());
        JarArchive paper_archive = JarArchive.of("paper", paper_path);

        Native.log("Read plugin jar...");
        JarArchive plugin_archive = JarArchive.of("plugin", of(patch.class));

        JsonObject patch_data = system.json.parse(new String(resource)).getAsJsonObject();

        boolean resetCacheOriginal = !paper_archive.entries.containsKey("lime-patch.json");

        system.Toast2<String, String> current_version = Optional.ofNullable(paper_archive.entries.get("lime-patch.json"))
                .map(String::new)
                .map(system.json::parse)
                .map(JsonElementOptional::of)
                .flatMap(JsonElementOptional::getAsJsonObject)
                .flatMap(v -> v.getAsJsonObject("version"))
                .flatMap(v -> v.getAsString("patch")
                        .flatMap(patch -> v.getAsString("append")
                                .map(append -> system.toast(patch, append))))
                .orElse(null);

        system.Toast2<String, String> patch_version = calculateVersion(plugin_archive, patch_data);

        Native.log("Current patch version: " + (current_version == null ? "Not patched" : current_version));
        Native.log("New patch version:     " + patch_version);

        if (patch_version.equals(current_version)) return;

        boolean isOnlyAppendPart = current_version == null || !current_version.val1.equals(patch_version.val1);

        Native.log("Patch...");
        paper_archive.entries.put("lime-patch.json", system.toFormat(system.json.object()
                .addObject("version", v -> v
                        .add("patch", isOnlyAppendPart ? "??????????" : patch_version.val0)
                        .add("append", patch_version.val1)
                ).build()
        ).getBytes());

        Path version_base = of(Main.class);
        String sha256_old = Native.sha256(Files.readAllBytes(version_base));
        Path bukkit_base = of(Bukkit.class);
        String sha256_bukkit_old = Native.sha256(Files.readAllBytes(bukkit_base));

        Native.log("Getting original version jar...");
        Path version_orig = loadBaseFile(version_base, resetCacheOriginal);
        Path bukkit_orig = loadBaseFile(bukkit_base, resetCacheOriginal);

        Native.log("Read version jar...");
        JarArchive version_archive = JarArchive.of("version", version_orig);
        Native.subLog("Append:", () -> {
            patch_data.getAsJsonArray("append").forEach(append -> {
                String name = append.getAsString();
                Native.log("Append '"+name+"'...");
                version_archive.entries.put(name, plugin_archive.entries.get(name));
            });
            patch_data.getAsJsonArray("append_regex").forEach(append_regex -> {
                String regex = append_regex.getAsString();
                Native.log("Append group '"+regex+"':");
                plugin_archive.entries
                        .entrySet()
                        .stream()
                        .filter(v -> system.compareRegex(v.getKey(), regex))
                        .forEach(kv -> {
                            Native.log(" - '"+kv.getKey()+"'...");
                            version_archive.entries.put(kv.getKey(), kv.getValue());
                        });
            });
        });

        if (isOnlyAppendPart) {
            Native.log("Save version jar...");
        } else {
            Native.log("Read bukkit jar...");
            JarArchive bukkit_archive = JarArchive.of("bukkit", bukkit_orig);

            Native.log("Read deobf file...");
            try (Closeable ignored = Native.loadDeobf()) {
                MutatePatcher.patch(version_archive, bukkit_archive, plugin_archive);
            }
            Native.log("Save version jar...");
            bukkit_archive.toFile(bukkit_base);
        }
        version_archive.toFile(version_base);

        String sha256_base = Native.sha256(Files.readAllBytes(version_base));
        String sha256_orig = Native.sha256(Files.readAllBytes(version_orig));
        String sha256_bukkit_base = Native.sha256(Files.readAllBytes(bukkit_base));
        String sha256_bukkit_orig = Native.sha256(Files.readAllBytes(bukkit_orig));

        Native.log("Apply paper jar...");
        paper_archive.entries.put("META-INF/versions.list", new String(paper_archive.entries.get("META-INF/versions.list"))
                .replace(sha256_old, sha256_base)
                .replace(sha256_orig, sha256_base)
                .replace(sha256_bukkit_old, sha256_bukkit_base)
                .replace(sha256_bukkit_orig, sha256_bukkit_base)
                .getBytes());
        paper_archive.entries.put("META-INF/patches.list", new String(paper_archive.entries.get("META-INF/patches.list"))
                .replace(sha256_old, sha256_base)
                .replace(sha256_orig, sha256_base)
                .replace(sha256_bukkit_old, sha256_bukkit_base)
                .replace(sha256_bukkit_orig, sha256_bukkit_base)
                .getBytes());

        Native.log("Save paper jar...");
        paper_archive.toFile(paper_path);

        Native.log("Patch status: OK");
        Native.log("Exit...");

        Runtime.getRuntime().halt(0);
    }
}


















