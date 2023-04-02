package org.lime.gp.module;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsMobs;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.database.Rows;
import org.lime.gp.lime;
import org.lime.system;

public class BiomeMobs implements Listener {
    public static core.element create() {
        ReflectionAccess.category_EntityTypes.set(EntityTypes.WOLF, EnumCreatureType.MONSTER);
        return core.element.create(BiomeMobs.class)
                .withInstance()
                .withInit(BiomeMobs::init)
                .<JsonObject>addConfig("biomes", v -> v
                        .withInvoke(BiomeMobs::config)
                        .withDefault(new JsonObject())
                );
    }
    public static void init() {
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.UNIVERSAL_ANGER, true));
        lime.repeat(BiomeMobs::update, 1);
    }
    public static void config(JsonObject json) {
        MinecraftServer.getServer()
                .registryAccess()
                .registryOrThrow(Registries.BIOME)
                .entrySet()
                .forEach(kv -> {
                    ResourceKey<BiomeBase> resourceKey = kv.getKey();
                    String key = resourceKey.location().toString();
                    BiomeBase base = kv.getValue();
                    BiomeSettingsMobs mobs = base.getMobSettings();
                    if (json.has(key)) writeTo(mobs, json.getAsJsonObject(key));
                    else json.add(key, readFrom(mobs));
                });
        lime.writeAllConfig("biomes", system.toFormat(json));
    }
    public static void update() {
        Bukkit.getWorlds().forEach(BiomeMobs::updateWorld);
    }
    @SuppressWarnings("deprecation")
    public static void updateWorld(World world) {
        world.getEntitiesByClasses(Wolf.class, Silverfish.class, Spider.class).forEach(entity -> {
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
            if (entity instanceof Mob mob && mob.getTarget() == null) {
                double maxHealth = mob.getMaxHealth();
                if (mob.getLocation().getBlock().getLightFromSky() > 2) {
                    maxHealth -= 0.1;
                    if (maxHealth <= 0) mob.remove();
                    else mob.setMaxHealth(maxHealth);
                }
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
        return system.json.object()
                .add("probability", ReflectionAccess.creatureGenerationProbability_BiomeSettingsMobs.get(mobs))
                .addObject("spawners", v -> v
                        .add(ReflectionAccess.spawners_BiomeSettingsMobs.get(mobs), EnumCreatureType::getName, _v -> system.json.array()
                                .add(ReflectionAccess.items_WeightedRandomList.get(_v), item -> system.json.object()
                                        .add("type", BuiltInRegistries.ENTITY_TYPE.getKey(item.type).toString())
                                        .add("weight", item.getWeight().asInt())
                                        .add("group_size", item.minCount + "-" + item.maxCount)
                                )
                        )
                )
                .addObject("spawn_costs", v -> v
                        .add(ReflectionAccess.mobSpawnCosts_BiomeSettingsMobs.get(mobs), k -> BuiltInRegistries.ENTITY_TYPE.getKey(k).toString(), item -> system.json.object()
                                .add("energy_budget", item.energyBudget())
                                .add("charge", item.charge())
                        )
                )
                .build();
    }

    @EventHandler public static void on(CreatureSpawnEvent e) {
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            if (!Rows.HouseRow.getInHouse(e.getLocation()).isEmpty()) {
                e.setCancelled(true);
                return;
            }
            if (e.getEntity().getType() == EntityType.HUSK) {
                e.setCancelled(true);
                Wolf wolf = e.getLocation().getWorld().spawn(e.getLocation(), Wolf.class, CreatureSpawnEvent.SpawnReason.NATURAL);
                if (system.rand_is(0.8)) wolf.addScoreboardTag("angry");
            }
        }
    }
}


















