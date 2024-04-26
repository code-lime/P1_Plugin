package org.lime.gp.player.ui.respurcepack;

import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.ResourcePackInfo;

public class ResourcePackVersionInfo extends ResourcePackInfo {
    public static final MetadataSectionType<ResourcePackVersionInfo> VERSION = new ResourcePackVersionInfoDeserializer();

    private final String packVersion;

    public ResourcePackVersionInfo(String version, IChatBaseComponent description, int format) {
        super(description, format);
        this.packVersion = version;
    }
    public String getVersion() {
        return this.packVersion;
    }
}
