package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPosition;
import org.bukkit.Location;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.component.list.LimitComponent;
import org.lime.gp.database.rows.HouseRow;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.lime;
import org.lime.gp.module.TimeoutData;
import org.lime.plugin.CoreElement;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import java.util.*;

@Setting(name = "block_limit") public class BlockLimitSetting extends ItemSetting<JsonObject> {
    private static boolean ENABLE = true;
    public static CoreElement create() {
        return CoreElement.create(BlockLimitSetting.class)
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("block_limit_enable")
                        .withDefault(new JsonPrimitive(ENABLE))
                        .withInvoke(_v -> ENABLE = _v.getAsBoolean()));
    }

    public final String type;
    public final int limit;
    public final Map<String, Integer> privates = new HashMap<>();

    public BlockLimitSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        type = json.get("type").getAsString();
        limit = json.get("limit").getAsInt();
        if (json.has("private"))
            json.getAsJsonObject("private")
                    .entrySet()
                    .forEach(kv -> privates.put(kv.getKey(), kv.getValue().getAsInt()));
    }

    public Optional<Toast2<Integer, Integer>> getLimit(Location location) {
        return getLimit(location.getWorld().getUID(), new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }
    public Optional<Toast2<Integer, Integer>> getLimit(UUID world, BlockPosition position) {
        if (!ENABLE)
            return Optional.empty();

        List<HouseRow> houses = null;
        if (!privates.isEmpty()) {
            houses = world == lime.MainWorld.getUID()
                    ? HouseRow.getInHouse(new Location(lime.MainWorld, position.getX(), position.getY(), position.getZ()))
                    : new ArrayList<>();
            houses.removeIf(v -> !privates.containsKey(v.rawType));

            for (var kv : privates.entrySet()) {
                for (HouseRow house : houses) {
                    if (!kv.getKey().equals(house.rawType))
                        continue;
                    int count = TimeoutData.count(new LimitComponent.HouseGroup(world, house.id, house.rawType, type), LimitComponent.LimitElement.class);
                    if (count >= kv.getValue())
                        return Optional.of(Toast.of(count, kv.getValue()));
                }
            }
        }
        if (houses != null && !houses.isEmpty())
            return Optional.empty();
        int count = TimeoutData.count(new LimitComponent.ChunkGroup(world, position, type), LimitComponent.LimitElement.class);
        return count >= limit ? Optional.of(Toast.of(count, limit)) : Optional.empty();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("type"), IJElement.raw("LIMIT_TYPE"), IComment.empty()
                        .append(IComment.text("Тип указанный в "))
                        .append(IComment.link(docs.componentsLink(LimitComponent.class)))),
                JProperty.require(IName.raw("limit"), IJElement.raw(10), IComment.empty()
                        .append(IComment.text("Максимальное количество блоков с типом "))
                        .append(IComment.raw("LIMIT_TYPE")))
        ), IComment.text("Блокирует возможность ставить блок если количество блоков данного типа больше лимита"));
    }
}
