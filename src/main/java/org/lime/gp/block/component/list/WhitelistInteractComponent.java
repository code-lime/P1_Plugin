package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.BottleInstance;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.player.level.LevelModule;
import org.lime.system.range.IRange;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import javax.annotation.Nullable;

@InfoComponent.Component(name = "whitelist_interact")
public final class WhitelistInteractComponent extends ComponentStatic<JsonObject> implements CustomTileMetadata.Interactable {
    public final @Nullable IRange works;
    public final @Nullable IRange levels;

    public WhitelistInteractComponent(BlockInfo info, JsonObject json) {
        super(info);
        this.works = json.has("works") ? IRange.parse(json.get("works").getAsString()) : null;
        this.levels = json.has("levels") ? IRange.parse(json.get("levels").getAsString()) : null;
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        return UserRow.getBy(event.player().getUUID())
                .filter(v -> works == null || works.inRange(v.work, 100))
                .filter(v -> levels == null || LevelModule.getLevel(v.id, v.work)
                        .map(l -> levels.inRange(l, 100))
                        .orElse(false)
                )
                .map(v -> EnumInteractionResult.PASS)
                .orElse(EnumInteractionResult.CONSUME);
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
