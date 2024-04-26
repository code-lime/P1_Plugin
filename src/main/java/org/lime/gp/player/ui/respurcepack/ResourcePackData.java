package org.lime.gp.player.ui.respurcepack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.server.packs.ResourcePackFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.system.file;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

public record ResourcePackData(
        @Nullable String url,
        @Nonnull String hash,
        @Nonnull String index,
        @Nonnull String version,
        @Nonnull Component notSupported) {
    private static String createUrl(String url, String index) {
        try {
            return new URIBuilder(url).addParameter("u", index).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ResourcePackData createEmpty() {
        return new ResourcePackData(null, "", "none", "0.0.0", Component.empty());
    }

    public ResourcePackData updateIndex(@Nullable String index) {
        return new ResourcePackData(url, hash, index == null ? "none" : index, version, notSupported);
    }
    public ResourcePackData update(String url, String version) {
        String checkUrl = url;
        boolean isAuto = false;
        if (version.startsWith("auto")) {
            String[] autoVersionArgs = version.split("#", 2);
            if (autoVersionArgs.length == 2 && !autoVersionArgs[1].isBlank())
                checkUrl = autoVersionArgs[1];
            isAuto = true;
        }
        String hash = "";
        try (var tmpFile = file.temp()) {
            File file = tmpFile.file();
            IOUtils.copy(new URIBuilder(checkUrl)
                    .addParameter("u", UUID.randomUUID().toString())
                    .build()
                    .toURL(), file);
            try (InputStream raw = new FileInputStream(file)) {
                hash = DigestUtils.sha1Hex(raw);
            }

            if (isAuto) {
                try (ResourcePackFile raw = new ResourcePackFile("tmp", file, true)) {
                    ResourcePackVersionInfo versionInfo = raw.getMetadataSection(ResourcePackVersionInfo.VERSION);
                    version = versionInfo.getVersion();
                    lime.logOP("Analyze resourcepack version: " + version);
                } catch (Exception e) {
                    lime.logOP("Error analyze resourcepack");
                    lime.logStackTrace(e);
                    version = "?.?.?";
                }
            }
        } catch (Exception e) {
            lime.logOP("Error check resourcepack");
            lime.logStackTrace(e);
        }

        Component notSupportedPart = Component.translatable("ЭТОТ РЕСУРСПАК НЕ ПОДДЕРЖИВАЕТСЯ! v"+version+" | THIS RESOURCEPACK IS NOT SUPPORTED! v"+version+" | ", TextColor.color(255, 0, 0));
        Component notSupported = Component.empty();
        for (int i = 0; i < 4; i++)
            notSupported = notSupported.append(notSupportedPart);

        return new ResourcePackData(url, hash, index, version, notSupported);
    }
    public ResourcePackData empty() {
        return new ResourcePackData(null, "", index, "0.0.0", Component.empty());
    }

    public void send(Player player, @Nullable ShareData shareData) {
        if (this.url == null)
            return;
        String url = createUrl(shareData == null ? this.url : (shareData.replace(this.url)), index);
        player.setResourcePack(url, hash);
    }
}
