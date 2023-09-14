package org.lime.gp.player.level;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.resources.MinecraftKey;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
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
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.LevelFoodMutateSetting;
import org.lime.gp.lime;
import org.lime.gp.database.rows.LevelRow;
import org.lime.gp.database.rows.UserRow;

import com.google.gson.JsonObject;
import org.lime.gp.player.module.PlayerData;
import org.lime.gp.player.module.TabManager;
import org.lime.plugin.CoreElement;
import org.lime.system.json;

public class LevelModule implements Listener {
    public static CoreElement create() {
        return CoreElement.create(LevelModule.class)
                .withInstance()
                .withInit(LevelModule::init)
                .<JsonObject>addConfig("level", v -> v
                        .withDefault(new JsonObject())
                        .withInvoke(LevelModule::config)
                );
    }
    
    private static final HashMap<Integer, LevelData> workData = new HashMap<>();

    public static float levelMutate(UUID uuid) {
        float mutate = 1;
        if (TabManager.hasDonate(uuid)) mutate += 0.25;
        PlayerData.JsonPersistentDataContainer data = PlayerData.getPlayerData(uuid);
        if (data.has(LEVEL_FOOD_MUTATE_KEY)) {
            boolean delete = true;
            if (data.getJson(LEVEL_FOOD_MUTATE_KEY) instanceof JsonObject json) {
                if (json.has("end_ms")) {
                    if (json.get("end_ms").getAsLong() > System.currentTimeMillis()) {
                        mutate += 0.25;
                        delete = false;
                    }
                }
            }
            if (delete) data.remove(LEVEL_FOOD_MUTATE_KEY);
        }
        return mutate;
    }

    private static final HashMap<String, JsonObject> cache = new HashMap<>();
    private static void init() {
        lime.repeat(LevelModule::update, 0.75);
        AnyEvent.addEvent("spawn.export", AnyEvent.type.owner_console, v -> v.createParam(_v -> _v, cache::keySet), (p, type) -> {
            String text = cache.get(type).toString();
            lime.logConsole("Loot export: " + text);
        });
    }
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(LevelModule::updateDisplay);
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

        HashMap<String, JsonObject> cache = new HashMap<>();
        json = lime.combineParent(json, true, false);
        HashMap<Integer, LevelData> workData = new HashMap<>();
        json.entrySet().forEach(kv -> {
            int work = Integer.parseInt(kv.getKey());
            LevelData data = new LevelData(work, kv.getValue().getAsJsonObject());
            workData.put(work, data);
            data.cache.forEach((level, raw) -> cache.put(work + "." + level, raw));
        });
        LevelModule.workData.clear();
        LevelModule.workData.putAll(workData);
        LevelModule.cache.clear();
        LevelModule.cache.putAll(cache);
    }

    public static Optional<Integer> getLevel(int userID, int workID) {
        LevelData data = workData.get(workID);
        if (data == null) return Optional.empty();
        int level = LevelRow.getBy(userID, workID).map(value -> value.level).orElse(0);
        return Optional.of(level);
    }
    public static Optional<Integer> getCurrentLevel(UUID uuid) {
        return UserRow.getBy(uuid)
                .map(v -> LevelRow.getBy(v.id, v.work).map(value -> value.level).orElse(0));
    }
    public static Optional<Integer> getCurrentLevel(UUID uuid, int workID) {
        LevelData data = workData.get(workID);
        if (data == null) return Optional.empty();
        return UserRow.getBy(uuid)
                .filter(v -> v.work == workID)
                .map(v -> LevelRow.getBy(v.id, workID).map(value -> value.level).orElse(0));
    }
    public static Optional<LevelStep> getLevelStep(int userID, int workID) {
        LevelData data = workData.get(workID);
        if (data == null) return Optional.empty();
        int level = LevelRow.getBy(userID, workID).map(value -> value.level).orElse(0);
        return Optional.ofNullable(data.levels.get(level));
    }
    public static Optional<LevelData> getLevelData(UUID uuid) {
        return UserRow.getBy(uuid).map(user -> workData.get(user.work));
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
        if (!(e.getRecipe() instanceof Keyed keyed)) return;
        getLevelStep(uuid).ifPresent(step -> step.deltaExp(uuid, ExperienceAction.CRAFT, keyed.getKey().getKey()));
    }
    public static void onCraft(UUID uuid, MinecraftKey key) {
        getLevelStep(uuid).ifPresent(step -> step.deltaExp(uuid, ExperienceAction.CRAFT, key.getPath()));
    }
    public static void onHarvest(UUID uuid, String key) {
        getLevelStep(uuid).ifPresent(step -> step.deltaExp(uuid, ExperienceAction.HARVEST, key));
    }

    public static void dieAction(Player player) {
        UUID uuid = player.getUniqueId();
        getLevelStep(uuid).ifPresent(step -> step.deltaExp(uuid, ExperienceAction.SINGLE, ExperienceAction.SingleValue.DIE));
    }

    @EventHandler private static void onExpChange(PlayerExpChangeEvent e) {
        if (workData.size() == 0) return;
        e.setAmount(0);
    }


    private static final NamespacedKey LEVEL_FOOD_MUTATE_KEY = new NamespacedKey(lime._plugin, "level_food_mutate");
    @EventHandler(ignoreCancelled = true) public static void on(PlayerItemConsumeEvent e) {
        Items.getOptional(LevelFoodMutateSetting.class, e.getItem())
                .ifPresent(mutate -> {
                    long now = System.currentTimeMillis();
                    long endMs = 0;
                    PlayerData.JsonPersistentDataContainer data = PlayerData.getPlayerData(e.getPlayer().getUniqueId());
                    if (data.getJson(LEVEL_FOOD_MUTATE_KEY) instanceof JsonObject json) {
                        if (json.has("end_ms")) {
                            endMs = json.get("end_ms").getAsLong();
                        }
                    }
                    endMs = Math.max(endMs, now + (long)(mutate.sec * 1000));
                    data.setJson(LEVEL_FOOD_MUTATE_KEY, json.object().add("end_ms", endMs).build());
                });
    }
}
