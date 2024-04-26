package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.bukkit.Location;
import org.lime.Position;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.database.rows.HouseRow;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.list.BlockLimitSetting;
import org.lime.gp.module.TimeoutData;
import org.lime.system.toast.*;

import java.util.UUID;
import java.util.stream.Stream;

@InfoComponent.Component(name = "limit") public class LimitComponent extends ComponentStatic<JsonObject> implements CustomTileMetadata.Tickable {
    public final String type;

    public LimitComponent(BlockInfo creator, JsonObject json) {
        super(creator, json);
        type = json.get("type").getAsString();
    }
    public record ChunkGroup(UUID world, long chunk, String type) implements TimeoutData.TKeyedGroup<Toast3<UUID, Long, String>> {
        public ChunkGroup(UUID world, BlockPosition pos, String type) { this(world, ChunkCoordIntPair.asLong(pos), type); }
        @Override public Toast3<UUID, Long, String> groupID() { return Toast.of(world, chunk, type); }
    }
    public record HouseGroup(UUID world, int house, String houseType, String type) implements TimeoutData.TKeyedGroup<Toast4<UUID, Integer, String, String>> {
        public static Stream<HouseGroup> groups(Location location, String type) {
            UUID world = location.getWorld().getUID();
            return HouseRow.getInHouse(location).stream().map(v -> new HouseGroup(world, v.id, v.rawType, type));
        }
        @Override public Toast4<UUID, Integer, String, String> groupID() { return Toast.of(world, house, houseType, type); }
    }
    public static class LimitElement extends TimeoutData.IGroupTimeout {
        public final Position position;
        public LimitElement(Position position) { this.position = position; }
    }

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        UUID uuid = metadata.key.uuid();
        LimitElement element = new LimitElement(metadata.position());
        TimeoutData.put(new ChunkGroup(event.getWorld().getWorld().getUID(), event.getPos(), type), uuid, LimitElement.class, element);
        HouseGroup.groups(metadata.position().getLocation(), type)
                .forEach(v -> TimeoutData.put(v, uuid, LimitElement.class, element));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("type"), IJElement.raw("LIMIT_TYPE"), IComment.empty()
                        .append(IComment.text("Пользовательский тип. Используется в "))
                        .append(IComment.link(docs.settingsLink(BlockLimitSetting.class))))
        ), IComment.text("Записывает информацию об количестве блоков в текущем чанке"));
    }
}