package org.lime.gp.player.level;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.lime.gp.lime;
import org.lime.gp.database.rows.LevelRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.module.PopulateLootEvent;

import com.google.gson.JsonObject;

public class LevelModule implements Listener {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(LevelModule.class)
                .withInstance()
                .withInit(LevelModule::init)
                .<JsonObject>addConfig("level", v -> v
                        .withDefault(new JsonObject())
                        .withInvoke(LevelModule::config)
                );
    }
    
    private static final HashMap<Integer, LevelData> workData = new HashMap<>();

    private static void init() {
        lime.repeat(LevelModule::update, 0.75);
    }
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> updateDisplay(player));
    }
    private static void updateDisplay(Player player) {
        int level;
        double exp;
        Optional<LevelRow> row = LevelRow.getActiveBy(player.getUniqueId());
        if (row.isEmpty()) {
            if (workData.size() == 0) return;
            level = 0;
            exp = 0;
        } else {
            LevelRow _row = row.get();
            level = _row.level;
            exp = _row.exp;
        }
        player.setExp((float)exp);
        player.setLevel(level);
    }

    public static boolean DEBUG = false;

    private static void config(JsonObject json) {
        DEBUG = json.has("DEBUG") && json.remove("DEBUG").getAsBoolean();

        json = lime.combineParent(json, true, false);
        HashMap<Integer, LevelData> workData = new HashMap<>();
        json.entrySet().forEach(kv -> {
            int work = Integer.parseInt(kv.getKey());
            workData.put(work, new LevelData(work, kv.getValue().getAsJsonObject()));
        });
        LevelModule.workData.clear();
        LevelModule.workData.putAll(workData);
    }

    public static Optional<LevelStep> getLevelStep(int userID, int workID) {
        LevelData data = workData.get(workID);
        if (data == null) return Optional.empty();
        int level = LevelRow.getBy(userID, workID).map(value -> value.level).orElse(0);
        return Optional.of(data.levels.get(level));
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

    private static Optional<Player> getPlayerOwner(Entity entity) {
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player)
            return Optional.of(player);
        return entity instanceof Player player ? Optional.of(player) : Optional.empty();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) private static void onKill(EntityDeathEvent e) {
        if (!(e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damageByEntityEvent)) return;
        getPlayerOwner(damageByEntityEvent.getDamager()).ifPresent(player -> {
            UUID uuid = player.getUniqueId();
            getLevelStep(uuid).ifPresent(step -> step.deltaExp(uuid, ExperienceAction.KILL, e.getEntity()));
        });
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) private static void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        UUID uuid = e.getPlayer().getUniqueId();
        getLevelStep(uuid).ifPresent(step -> step.deltaExp(uuid, ExperienceAction.BREAK, block));
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) private static void onBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        getLevelStep(uuid).ifPresent(step -> step.deltaExp(uuid, ExperienceAction.FARM, e.getMother()));
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) private static void onCraft(CraftItemEvent e) {
        UUID uuid = e.getWhoClicked().getUniqueId();
        getLevelStep(uuid).ifPresent(step -> step.deltaExp(uuid, ExperienceAction.CRAFT, e.getCurrentItem()));
    }

    public static void dieAction(Player player) {
        UUID uuid = player.getUniqueId();
        getLevelStep(uuid).ifPresent(step -> step.deltaExp(uuid, ExperienceAction.DIE, null));
    }
    
    @EventHandler private static void onLoot(PopulateLootEvent e) {
        e.getOptional(PopulateLootEvent.Parameters.KillerEntity)
            .or(() -> e.getOptional(PopulateLootEvent.Parameters.ThisEntity))
            .map(v -> v.getBukkitEntity() instanceof CraftPlayer cp ? cp : null)
            .flatMap(player -> getLevelStep(player.getUniqueId()))
            .ifPresent(step -> step.tryModifyLoot(e));
    }
    @EventHandler private static void onExpChange(PlayerExpChangeEvent e) {
        if (workData.size() == 0) return;
        e.setAmount(0);
    }
}
