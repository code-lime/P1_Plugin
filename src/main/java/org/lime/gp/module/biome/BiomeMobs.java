package org.lime.gp.module.biome;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.lime.plugin.CoreElement;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.database.rows.HouseRow;
import org.lime.gp.lime;
import org.lime.gp.module.biome.weather.Weather;
import org.lime.gp.module.biome.weather.WeatherType;
import org.lime.gp.module.mobs.DespawnData;
import org.lime.gp.module.mobs.IPopulateSpawn;
import org.lime.gp.module.mobs.Parameters;
import org.lime.gp.module.mobs.spawn.ISpawn;
import org.lime.system.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BiomeMobs implements Listener {
    public static CoreElement create() {
        ReflectionAccess.category_EntityTypes.set(EntityTypes.WOLF, EnumCreatureType.MONSTER);
        return CoreElement.create(BiomeMobs.class)
                .withInstance()
                .withInit(BiomeMobs::init)
                .<JsonObject>addConfig("biomes", v -> v
                        .withInvoke(BiomeMobs::config)
                        .withDefault(new JsonObject())
                )
                .<JsonObject>addConfig("spawntable", v -> v
                        .withInvoke(BiomeMobs::configTable)
                        .withDefault(new JsonObject())
                );
    }
    public static void init() {
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.UNIVERSAL_ANGER, true));
        lime.repeat(BiomeMobs::update, 1);
    }
    public static void config(JsonObject _json) {
        MinecraftServer.getServer()
                .registryAccess()
                .registryOrThrow(Registries.BIOME)
                .entrySet()
                .forEach(kv -> {
                    ResourceKey<BiomeBase> resourceKey = kv.getKey();
                    String key = resourceKey.location().toString();
                    BiomeBase base = kv.getValue();
                    BiomeSettingsMobs mobs = base.getMobSettings();
                    if (_json.has(key)) writeTo(mobs, _json.getAsJsonObject(key));
                    else _json.add(key, readFrom(mobs));
                });
        lime.writeAllConfig("biomes", json.format(_json));
    }
    private static final HashMap<EntityType, ISpawn> entitySpawns = new HashMap<>();
    public static void configTable(JsonObject json) {
        HashMap<EntityType, ISpawn> entitySpawns = new HashMap<>();
        lime.combineParent(json, true, false).entrySet().forEach(kv -> entitySpawns.put(EntityType.valueOf(kv.getKey()), ISpawn.parse(kv.getValue())));
        BiomeMobs.entitySpawns.clear();
        BiomeMobs.entitySpawns.putAll(entitySpawns);
    }
    public static void update() {
        Bukkit.getWorlds().forEach(BiomeMobs::updateWorld);
    }
    @SuppressWarnings("deprecation")
    public static void updateWorld(World world) {
        world.getEntitiesByClass(CraftEntity.class).forEach(entity -> {
            if (entity instanceof Wolf wolf) {
                if (!wolf.getScoreboardTags().contains("angry")) return;
                wolf.setAngry(true);
                if (wolf.getTarget() == null) {
                    wolf.getLocation()
                            .getNearbyPlayers(4)
                            .stream()
                            .filter(v -> switch (v.getGameMode()) { case SURVIVAL, ADVENTURE -> true; default -> false; })
                            .findFirst()
                            .ifPresent(wolf::setTarget);
                }
            }
            if (entity instanceof Damageable damageable && (!(entity instanceof Mob mob) || mob.getTarget() == null)) {
                PersistentDataContainer container = entity.getPersistentDataContainer();
                DespawnData.tickSecond(container)
                        .filter(v -> v <= 0)
                        .ifPresent(sec -> {
                            if (DespawnData.isOnlyLight(container) && entity.getLocation().getBlock().getLightFromSky() <= 2) return;
                            DespawnData.getDeltaHealth(container)
                                    .ifPresentOrElse(delta -> {
                                        double maxHealth = damageable.getMaxHealth() - delta;
                                        if (maxHealth <= 0) entity.remove();
                                        else damageable.setMaxHealth(maxHealth);
                                    }, entity::remove);
                        });
            }
        });
    }

    public static void writeTo(BiomeSettingsMobs mobs, JsonObject json) {
        BiomeSettingsMobs.a builder = new BiomeSettingsMobs.a().creatureGenerationProbability(json.get("probability").getAsFloat());
        json.getAsJsonObject("spawners").entrySet().forEach(kv -> {
            kv.getValue().getAsJsonArray().forEach(_item -> {
                JsonObject item = _item.getAsJsonObject();
                String[] group = item.get("group_size").getAsString().split("-");
                builder.addSpawn(Arrays.stream(EnumCreatureType.values()).filter(v -> v.getName().equals(kv.getKey())).findAny().get(), new BiomeSettingsMobs.c(
                        BuiltInRegistries.ENTITY_TYPE.get(new MinecraftKey(item.get("type").getAsString())),
                        item.get("weight").getAsInt(),
                        Integer.parseInt(group[0]),
                        Integer.parseInt(group[1])
                ));
            });
        });
        json.getAsJsonObject("spawn_costs").entrySet().forEach(kv -> {
            JsonObject item = kv.getValue().getAsJsonObject();
            builder.addMobCharge(BuiltInRegistries.ENTITY_TYPE.get(new MinecraftKey(kv.getKey())), item.get("charge").getAsDouble(), item.get("energy_budget").getAsDouble());
        });
        BiomeSettingsMobs buffer = builder.build();

        ReflectionAccess.creatureGenerationProbability_BiomeSettingsMobs.set(mobs, ReflectionAccess.creatureGenerationProbability_BiomeSettingsMobs.get(buffer));
        ReflectionAccess.spawners_BiomeSettingsMobs.set(mobs, ReflectionAccess.spawners_BiomeSettingsMobs.get(buffer));
        ReflectionAccess.mobSpawnCosts_BiomeSettingsMobs.set(mobs, ReflectionAccess.mobSpawnCosts_BiomeSettingsMobs.get(buffer));
    }
    public static JsonObject readFrom(BiomeSettingsMobs mobs) {
        return json.object()
                .add("probability", ReflectionAccess.creatureGenerationProbability_BiomeSettingsMobs.get(mobs))
                .addObject("spawners", v -> v
                        .add(ReflectionAccess.spawners_BiomeSettingsMobs.get(mobs), EnumCreatureType::getName, _v -> json.array()
                                .add(ReflectionAccess.items_WeightedRandomList.get(_v), item -> json.object()
                                        .add("type", BuiltInRegistries.ENTITY_TYPE.getKey(item.type).toString())
                                        .add("weight", item.getWeight().asInt())
                                        .add("group_size", item.minCount + "-" + item.maxCount)
                                )
                        )
                )
                .addObject("spawn_costs", v -> v
                        .add(ReflectionAccess.mobSpawnCosts_BiomeSettingsMobs.get(mobs), k -> BuiltInRegistries.ENTITY_TYPE.getKey(k).toString(), item -> json.object()
                                .add("energy_budget", item.energyBudget())
                                .add("charge", item.charge())
                        )
                )
                .build();
    }

    @EventHandler public static void on(CreatureSpawnEvent e) {
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.NATURAL && !HouseRow.getInHouse(e.getLocation()).isEmpty()) {
            e.setCancelled(true);
            return;
        }
        if (!(e.getEntity() instanceof CraftEntity entity)) return;
        if (entity.getScoreboardTags().contains("spawn#generic")) return;
        EntityType type = entity.getType();
        ISpawn spawn = entitySpawns.get(type);
        if (spawn == null) return;
        Entity handle = entity.getHandle();
        if (!(handle.level() instanceof WorldServer world)) return;
        Location location = entity.getLocation();
        Vector pos = location.toVector();
        IPopulateSpawn populate = IPopulateSpawn.of(world, List.of(
                IPopulateSpawn.var(Parameters.ThisEntity, handle),
                IPopulateSpawn.var(Parameters.Origin, pos),
                IPopulateSpawn.var(Parameters.Weather, WeatherType.getBy(handle.level())),
                IPopulateSpawn.var(Parameters.SeasonKey, Weather.getCurrentSeason()),
                IPopulateSpawn.var(Parameters.SpawnReason, reason),
                IPopulateSpawn.var(Parameters.FloorBlock, handle.getFeetBlockState())
        ));

        spawn.generateMob(populate).ifPresent(creator -> lime.nextTick(() -> {
            if (e.isCancelled()) return;
            Entity spawned = creator.spawn(world, new Vec3D(pos.getX(), pos.getY(), pos.getZ()));
            if (spawned != null) spawned.spawnReason = handle.spawnReason;
            e.getEntity().remove();
        }));
    }
}


















