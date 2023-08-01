package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.gp.module.loot.PopulateLootEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@InfoComponent.Component(name = "loot")
public final class LootComponent extends ComponentStatic<JsonObject> implements CustomTileMetadata.Lootable {
    public final Map<String, Integer> items;
    public final Map<String, String> args = new HashMap<>();

    public LootComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        Map<String, Integer> items = new HashMap<>();
        json.getAsJsonObject("items").entrySet().forEach(kv -> {
            items.put(kv.getKey(), kv.getValue().getAsInt());
            if (Items.getItemCreator(kv.getKey()).isPresent()) return;
            lime.logOP("[Warning] Key of item in spawn '" + kv.getKey() + "' not founded!");
        });
        this.items = items;
        if (json.has("args"))
            json.getAsJsonObject("args").entrySet().forEach(kv -> args.put(kv.getKey(), kv.getValue().getAsString()));
    }

    public LootComponent(BlockInfo info, List<Material> items) {
        super(info);
        this.items = items.stream().collect(Collectors.toMap(Enum::name, v -> 1));
    }

    public LootComponent(BlockInfo info, Map<Material, Integer> items) {
        super(info);
        this.items = items.entrySet().stream().collect(Collectors.toMap(kv -> kv.getKey().name(), Map.Entry::getValue));
    }

    @Override
    public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        event.addItems(generateItems(metadata.list(DisplayInstance.class)
                .findFirst()
                .map(DisplayInstance::getAll)
                .map(v -> Apply.of().add(v))
                .orElseGet(Apply::of)
        ).collect(Collectors.toList()));
    }

    public Stream<ItemStack> generateItems(Apply apply) {
        Apply modify = Apply.of();
        args.forEach((key, value) -> modify.add(ChatHelper.formatText(key, apply), ChatHelper.formatText(value, apply)));
        return this.items
                .entrySet()
                .stream()
                .map(kv -> Items.getItemCreator(kv.getKey())
                        .map(v -> v.createItem(kv.getValue(), modify))
                        .or(() -> {
                            lime.logOP("[Warning] Key of item in spawn '" + kv.getKey() + "' not founded!");
                            return Optional.empty();
                        })
                )
                .flatMap(Optional::stream);
    }
}
