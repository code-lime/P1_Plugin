package org.lime.gp.module;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootSerialization;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lime.core;
import org.lime.system;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModifyLootTable implements Listener {
    public static core.element create() {
        return core.element.create(ModifyLootTable.class)
                .withInstance()
                .<JsonObject>addConfig("loottable", v -> v.withInvoke(ModifyLootTable::config).withDefault(new JsonObject()));
    }
    private static final HashMap<MinecraftKey, system.Toast2<net.minecraft.world.level.storage.loot.LootTable, Boolean>> lootTables = new HashMap<>();
    private static final Gson GSON = LootSerialization.createLootTableSerializer().create();
    public static void config(JsonObject json) {
        HashMap<MinecraftKey, system.Toast2<net.minecraft.world.level.storage.loot.LootTable, Boolean>> lootTables = new HashMap<>();
        json.entrySet().forEach(kv -> {
            net.minecraft.world.level.storage.loot.LootTable lootTable = GSON.fromJson(kv.getValue(), net.minecraft.world.level.storage.loot.LootTable.class);
            Arrays.stream(kv.getKey().split(" "))
                    .forEach(key -> {
                        system.Toast2<net.minecraft.world.level.storage.loot.LootTable, Boolean> value;
                        if (key.startsWith("+")) {
                            key = key.substring(1);
                            value = system.toast(lootTable, true);
                        } else {
                            value = system.toast(lootTable, false);
                        }
                        lootTables.put(new MinecraftKey(key), value);
                    });
        });
        ModifyLootTable.lootTables.clear();
        ModifyLootTable.lootTables.putAll(lootTables);
    }
    @EventHandler public static void on(PopulateLootEvent e) {
        Optional.ofNullable(lootTables.get(e.getKey())).ifPresent(v -> v.invoke((lootTable, append) -> {
            if (lootTable == null || e.isReplaced()) return;
            if (append) {
                List<ItemStack> items = e.getVanillaItems();
                items.addAll(lootTable.getRandomItems(e.getContext(true)));
                e.setItems(items.stream().map(CraftItemStack::asCraftMirror).collect(Collectors.toList()));
            } else {
                e.setItems(lootTable.getRandomItems(e.getContext(true)).stream().map(CraftItemStack::asCraftMirror).collect(Collectors.toList()));
            }
        }));
    }
}















