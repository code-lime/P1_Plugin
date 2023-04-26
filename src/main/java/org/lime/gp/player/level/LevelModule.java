package org.lime.gp.player.level;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.lime.display.lime;
import org.lime.gp.database.rows.LevelRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.module.PopulateLootEvent;

import com.google.gson.JsonObject;

public class LevelModule implements Listener {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(LevelModule.class)
                .withInstance()
                .<JsonObject>addConfig("level", v -> v
                        .withDefault(new JsonObject())
                        .withInvoke(LevelModule::config)
                );
    }
    
    private static final HashMap<Integer, LevelData> workData = new HashMap<>();

    private static void config(JsonObject json) {
        json = lime.combineParent(json, true, false);
        HashMap<Integer, LevelData> workData = new HashMap<>();
        json.entrySet().forEach(kv -> {
            int work = Integer.parseInt(kv.getKey());
            workData.put(work, new LevelData(work, kv.getValue().getAsJsonObject()));
        });
        LevelModule.workData.clear();
        LevelModule.workData.putAll(workData);
    }

    public static Optional<LevelData> getLevelData(UUID uuid) {
        return UserRow.getBy(uuid).map(user -> {
            int work = user.work;
            LevelData data = workData.get(work);
            if (data == null) return null;
            return data;
        });
    }
    public static Optional<LevelStep> getLevelStep(UUID uuid) {
        return UserRow.getBy(uuid).map(user -> {
            int work = user.work;
            LevelData data = workData.get(work);
            if (data == null) return null;
            int level = LevelRow.getBy(user.id, work).map(value -> value.level).orElse(0);
            return data.levels.get(level);
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) private static void onKill(EntityDeathEvent e) {
        if (!(e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damageByEntityEvent)) return;
        if (!(damageByEntityEvent.getDamager() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        getLevelStep(uuid).ifPresent(step -> step.appendExp(uuid, ExperienceAction.KILL, e.getEntity()));
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) private static void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        UUID uuid = e.getPlayer().getUniqueId();
        getLevelStep(uuid).ifPresent(step -> step.appendExp(uuid, ExperienceAction.BREAK, block));
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) private static void onBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        getLevelStep(uuid).ifPresent(step -> step.appendExp(uuid, ExperienceAction.FARM, e.getMother()));
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) private static void onCraft(CraftItemEvent e) {
        UUID uuid = e.getWhoClicked().getUniqueId();
        getLevelStep(uuid).ifPresent(step -> step.appendExp(uuid, ExperienceAction.CRAFT, e.getCurrentItem()));
    }
    
    @EventHandler private static void onLoot(PopulateLootEvent e) {
        e.getOptional(PopulateLootEvent.Parameters.KillerEntity)
            .or(() -> e.getOptional(PopulateLootEvent.Parameters.ThisEntity))
            .map(v -> v.getBukkitEntity() instanceof CraftPlayer cp ? cp : null)
            .flatMap(player -> getLevelStep(player.getUniqueId()))
            .ifPresent(step -> step.tryModifyLoot(e));
    }
}
