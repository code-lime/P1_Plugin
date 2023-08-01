package org.lime.gp.module.loot;

import com.google.gson.JsonObject;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.FishHook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lime.core;
import org.lime.gp.item.loot.ILoot;
import org.lime.gp.player.level.LevelModule;
import org.lime.system;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class ModifyLootTable implements Listener {
    public static core.element create() {
        return core.element.create(ModifyLootTable.class)
                .withInstance()
                .<JsonObject>addConfig("loottable", v -> v.withInvoke(ModifyLootTable::config).withDefault(new JsonObject()));
    }
    private static final HashMap<String, system.Toast2<ILoot, LootModifyAction>> modifyLootTable = new HashMap<>();
    public static void config(JsonObject json) {
        HashMap<String, system.Toast2<ILoot, LootModifyAction>> lootTables = new HashMap<>();
        json.entrySet().forEach(kv -> LootModifyAction.parse(kv.getKey(), kv.getValue())
                .invoke((key, loot, action) -> lootTables.put(key, system.toast(loot, action))));
        ModifyLootTable.modifyLootTable.clear();
        ModifyLootTable.modifyLootTable.putAll(lootTables);
    }

    @EventHandler private static void onLoot(PopulateLootEvent e) {
        if (e.getOptional(Parameters.KillerEntity)
                .or(() -> e.getOptional(Parameters.ThisEntity))
                .map(net.minecraft.world.entity.Entity::getBukkitEntity)
                .map(v -> v instanceof CraftPlayer cp
                        ? cp
                        : v instanceof FishHook hook && hook.getShooter() instanceof CraftPlayer cp
                        ? cp
                        : null
                )
                .flatMap(player -> LevelModule.getLevelStep(player.getUniqueId()))
                .filter(step -> step.tryModifyLoot(e))
                .isEmpty()
        )
            tryModifyLoot(e);
    }
    public static ILoot getLoot(UUID uuid, String key, ILoot loot, IPopulateLoot variable) {
        return LevelModule.getLevelStep(uuid)
                .flatMap(step -> step.tryChangeLoot(key, loot, variable))
                .or(() -> tryChangeLoot(key, loot, variable))
                .orElse(loot);
    }


    private static void tryModifyLoot(PopulateLootEvent e) {
        String key = e.getKey().getPath();
        system.Toast2<ILoot, LootModifyAction> loot = null;
        for (var kv : modifyLootTable.entrySet()) {
            if (!system.compareRegex(key, kv.getKey())) continue;
            loot = kv.getValue();
            break;
        }
        if (loot == null) return;
        loot.val1.modifyLoot(e, loot.val0);
    }
    private static Optional<ILoot> tryChangeLoot(String key, ILoot base, IPopulateLoot variable) {
        system.Toast2<ILoot, LootModifyAction> loot = null;
        for (var kv : modifyLootTable.entrySet()) {
            if (!system.compareRegex(key, kv.getKey())) continue;
            loot = kv.getValue();
            break;
        }
        return loot == null ? Optional.empty() : Optional.of(loot.val1.changeLoot(base, loot.val0));
    }
}















