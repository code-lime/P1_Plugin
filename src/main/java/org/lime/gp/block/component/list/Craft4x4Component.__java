package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.Craft4x4Action;
import org.lime.gp.chat.ChatHelper;

@InfoComponent.Component(name = "craft4x4")
public final class Craft4x4Component extends ComponentStatic<JsonObject> implements CustomTileMetadata.Interactable {
    public final Component title;
    public final String vanillaType;

    public Craft4x4Component(BlockInfo creator, JsonObject json) {
        super(creator, json);
        title = ChatHelper.formatComponent(json.get("title").getAsString());
        vanillaType = json.has("vanilla_type") ? json.get("vanilla_type").isJsonNull() ? null : json.get("vanilla_type").getAsString() : null;
    }

    @Override
    public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        return Craft4x4Action.open(this, metadata, event);
    }
}
