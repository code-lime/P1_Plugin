package org.lime.gp.player.ui.respurcepack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ChatDeserializer;
import org.jetbrains.annotations.NotNull;

public class ResourcePackVersionInfoDeserializer implements MetadataSectionType<ResourcePackVersionInfo> {
    @Override public @NotNull ResourcePackVersionInfo fromJson(JsonObject jsonObject) {
        IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(jsonObject.get("description"));
        if (component == null)
            throw new JsonParseException("Invalid/missing description!");
        int i = ChatDeserializer.getAsInt(jsonObject, "pack_format");
        String version = ChatDeserializer.getAsString(jsonObject, "version");
        return new ResourcePackVersionInfo(version, component, i);
    }
    @Override public @NotNull JsonObject toJson(ResourcePackVersionInfo metadata) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("description", IChatBaseComponent.ChatSerializer.toJsonTree(metadata.getDescription()));
        jsonObject.addProperty("pack_format", metadata.getPackFormat());
        jsonObject.addProperty("version", metadata.getVersion());
        return jsonObject;
    }
    @Override public @NotNull String getMetadataSectionName() { return "pack"; }
}
