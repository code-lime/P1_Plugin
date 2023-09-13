package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.lime.Position;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.list.BlockLimitSetting;
import org.lime.gp.module.TimeoutData;
import org.lime.system;

@InfoComponent.Component(name = "limit") public class LimitComponent extends ComponentStatic<JsonObject> implements CustomTileMetadata.Tickable {
    public final String type;

    public LimitComponent(BlockInfo creator, JsonObject json) {
        super(creator, json);
        type = json.get("type").getAsString();
    }
    public record ChunkGroup(long chunk, String type) implements TimeoutData.TKeyedGroup<system.Toast2<Long, String>> {
        public ChunkGroup(BlockPosition pos, String type) { this(ChunkCoordIntPair.asLong(pos), type); }
        @Override public system.Toast2<Long, String> groupID() { return system.toast(chunk, type); }
    }
    public static class LimitElement extends TimeoutData.IGroupTimeout {
        public final Position position;
        public LimitElement(Position position) { this.position = position; }
    }

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        TimeoutData.put(new ChunkGroup(event.getPos(), type), metadata.key.uuid(), LimitElement.class, new LimitElement(metadata.position()));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("type"), IJElement.raw("LIMIT_TYPE"), IComment.empty()
                        .append(IComment.text("Пользовательский тип. Используется в "))
                        .append(IComment.link(docs.settingsLink(BlockLimitSetting.class))))
        ), "Записывает информацию об количестве блоков в текущем чанке");
    }
}
