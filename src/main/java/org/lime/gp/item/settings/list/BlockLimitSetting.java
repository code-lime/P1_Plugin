package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPosition;
import org.bukkit.Location;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.component.list.LimitComponent;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.module.TimeoutData;

import java.util.Optional;

@Setting(name = "block_limit") public class BlockLimitSetting extends ItemSetting<JsonObject> {
    public final String type;
    public final int limit;

    public BlockLimitSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        type = json.get("type").getAsString();
        limit = json.get("limit").getAsInt();
    }

    public boolean isLimit(Location location) {
        return isLimit(new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }
    public boolean isLimit(BlockPosition position) {
        return TimeoutData.count(new LimitComponent.ChunkGroup(position, type), LimitComponent.LimitElement.class) >= limit;
    }
    public Optional<Integer> isLimitWithGet(Location location) {
        return isLimitWithGet(new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }
    public Optional<Integer> isLimitWithGet(BlockPosition position) {
        int count = TimeoutData.count(new LimitComponent.ChunkGroup(position, type), LimitComponent.LimitElement.class);
        return count >= limit ? Optional.of(count) : Optional.empty();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("type"), IJElement.raw("LIMIT_TYPE"), IComment.empty()
                        .append(IComment.text("Тип указанный в "))
                        .append(IComment.link(docs.componentsLink(LimitComponent.class)))),
                JProperty.require(IName.raw("limit"), IJElement.raw(10), IComment.empty()
                        .append(IComment.text("Максимальное количество блоков с типом "))
                        .append(IComment.raw("LIMIT_TYPE")))
        ), "Блокирует возможность ставить блок если количество блоков данного типа больше лимита");
    }
}
