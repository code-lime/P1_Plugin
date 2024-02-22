package org.lime.gp.block.component.list;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.lime.display.models.ExecutorJavaScript;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.display.partial.Partial;
import org.lime.gp.block.component.display.partial.PartialLoader;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.Items;
import org.lime.gp.module.JavaScript;

import java.util.*;

@InfoComponent.Component(name = "display")
public final class DisplayComponent extends ComponentDynamic<JsonObject, DisplayInstance> {
    public final Map<Integer, Partial> partials;
    public final Map<UUID, Partial> partialMap = new HashMap<>();
    public final double maxDistanceSquared;

    public final ExecutorJavaScript animation;

    public void animationTick(Map<String, String> variable, Map<String, String> display_variable, Map<String, Object> data) {
        animation.execute(Map.of("variable", variable, "display_variable", display_variable, "data", data));
    }

    public DisplayComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        if (json.has("animation")) {
            JsonObject animation = json.getAsJsonObject("animation");
            this.animation = new ExecutorJavaScript("tick", animation, JavaScript.js);
        } else {
            this.animation = ExecutorJavaScript.empty();
        }
        LinkedList<Partial> partials = new LinkedList<>();
        maxDistanceSquared = PartialLoader.load(info, json, partials, this.partialMap);
        this.partials = createPartials(info, partials);
    }
    public DisplayComponent(BlockInfo info, List<Partial> partials) {
        super(info);
        this.animation = ExecutorJavaScript.empty();
        LinkedList<Partial> _partials = new LinkedList<>();
        maxDistanceSquared = PartialLoader.loadStatic(info, partials, _partials, this.partialMap);
        this.partials = createPartials(info, _partials);
    }

    private static Map<Integer, Partial> createPartials(BlockInfo info, LinkedList<Partial> partials) {
        int length = partials.size();
        if (length == 0) return Collections.emptyMap();
        HashMapWithDefault<Integer, Partial> outPartials = new HashMapWithDefault<>(partials.get(0));
        Partial last = null;
        for (int i = length - 1; i >= 0; i--) {
            Partial partial = partials.get(i);
            if (last != null) {
                int lastDistanceChunk = last.distanceChunk();
                int delta = partial.distanceChunk() - lastDistanceChunk;
                for (int _i = 1; _i < delta; _i++) {
                    outPartials.put(lastDistanceChunk + _i, last);
                }
            }
            if (outPartials.putIfAbsent(partial.distanceChunk(), partial) == null) last = partial;
        }
        return outPartials;
    }
    
    @Override public DisplayInstance createInstance(CustomTileMetadata metadata) { return new DisplayInstance(this, metadata); }
    @Override public Class<DisplayInstance> classInstance() { return DisplayInstance.class; }

    private static class HashMapWithDefault<Key,Value> extends HashMap<Key, Value> {
        private final Value defaultValue;

        public HashMapWithDefault(Value defaultValue) {
            this.defaultValue = defaultValue;
        }
        @Override public Value get(Object key) {
            return super.getOrDefault(key, defaultValue);
        }
    }

    private static final String AIR_KEY = Items.getMaterialKey(Material.AIR);

    public static void putItem(Map<String, String> map, String prefixKey, ItemStack item) {
        if (item == null) item = new ItemStack(Material.AIR);
        map.put(prefixKey + ".key", Items.getGlobalKeyByItem(item).orElse(AIR_KEY));
        map.put(prefixKey + ".id", String.valueOf(Items.getIDByItem(item).orElse(0)));
        Color color = null;
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof PotionMeta potion && potion.hasColor()) color = potion.getColor();
            if (meta instanceof LeatherArmorMeta leather) color = leather.getColor();
        }
        map.put(prefixKey + ".color", color == null ? "#FFFFFF" : ChatColorHex.toHex(color));
        map.put(prefixKey + ".type", item.getType().name());
        map.put(prefixKey + ".count", String.valueOf(item.getAmount()));
    }
    public static void putItem(Map<String, String> map, String prefixKey, net.minecraft.world.item.ItemStack item) {
        putItem(map, prefixKey, CraftItemStack.asCraftMirror(item));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup animation_tick = ExecutorJavaScript.docs("ANIMATION_TICK", "animation_tick", docs.js(), docs.json(), "tick");
        IIndexGroup arg_field = JsonEnumInfo.of("ARG_FIELD", ImmutableList.of(
                IJElement.text("ARG_KEY").concat("=", IJElement.concat(",",
                        IJElement.text("ARG_VALUE"),
                        IJElement.text("ARG_VALUE"),
                        IJElement.any(),
                        IJElement.text("ARG_VALUE")
                ))
        ));
        IIndexGroup distance_key = JsonEnumInfo.of("DISTANCE_KEY", ImmutableList.of(
                IJElement.raw(10),
                IJElement.raw(10).concat("?", IJElement.concat("&",
                        IJElement.link(arg_field),
                        IJElement.link(arg_field),
                        IJElement.any(),
                        IJElement.link(arg_field)
                ))
        ));
        IIndexGroup partial = Partial.allDocs("PARTIAL", docs);
        JProperty part = JProperty.optional(IName.link(distance_key), IJElement.link(partial));
        return JsonGroup.of(index, JObject.of(
                JProperty.optional(IName.raw("animation"), IJElement.link(animation_tick), IComment.empty()
                        .append(IComment.text("JavaScript метод вызываемый каждый тик. Передаваемые параметры: "))
                        .append(IComment.raw("variable"))
                        .append(IComment.text(", "))
                        .append(IComment.raw("display_variable"))
                        .append(IComment.text(", "))
                        .append(IComment.raw("data"))),
                part,
                part,
                IJProperty.any()
        ), IComment.text("Отображает элемент в зависимости от параметров")).withChilds(arg_field, distance_key, animation_tick, partial);
    }
}
