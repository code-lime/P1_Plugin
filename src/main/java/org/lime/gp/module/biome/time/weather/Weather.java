package org.lime.gp.module.biome.time.weather;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.papermc.paper.chunk.PlayerChunkLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.PlayerChunkMap;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.gp.module.HasteDonate;
import org.lime.gp.module.RandomTickSpeed;
import org.lime.gp.module.biome.BiomeModify;
import org.lime.gp.module.biome.SnowModify;
import org.lime.gp.module.biome.time.DateTime;
import org.lime.gp.module.biome.time.DayManager;
import org.lime.gp.module.biome.time.SeasonKey;
import org.lime.gp.player.api.ViewDistance;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Weather {
    private static double initValue = 2;
    private static double stepValue = 1.5;
    private static boolean seasons = false;

    private static BiomeModify.ActionCloseable generateActionCloseable = null;

    public static core.element create() {
        return core.element.create(Weather.class)
                .withInit(Weather::init)
                .<JsonObject>addConfig("config", v -> v
                        .withParent("weather")
                        .withDefault(system.json.object()
                                .addObject("random_tick_speed", _v -> _v
                                        .add("init", initValue)
                                        .add("step", stepValue)
                                )
                                .add("seasons", seasons)
                                .build()
                        )
                        .withInvoke(json -> {
                            JsonObjectOptional _json = JsonObjectOptional.of(json);
                            _json.getAsJsonObject("random_tick_speed").ifPresentOrElse(rts -> {
                                initValue = rts.getAsDouble("init").orElse(2.0);
                                stepValue = rts.getAsDouble("step").orElse(1.5);
                            }, () -> {
                                initValue = 2.0;
                                stepValue = 1.5;
                            });
                            seasons = _json.getAsBoolean("seasons").orElse(false);
                            lastCounter = -1;
                            lastSeasonKey = null;

                            if (generateActionCloseable != null) generateActionCloseable.close();
                            generateActionCloseable = seasons ? BiomeModify.appendGenerate(Weather::generate) : null;
                        })
                );
    }

    private static void init() {
        AnyEvent.addEvent("biome.raw", AnyEvent.type.owner_console, v -> v, v -> {
            HashMap<BiomeColors, system.Toast2<String, Integer>> pattern = new HashMap<>();
            Map<String, BiomeColors> biomeMap = BiomeModify.getRawVanillaBiomes()
                    .collect(Collectors.toMap(_v -> _v.getString("name"), _v -> BiomeColors.parseElement(_v.getCompound("element"))));
            biomeMap.forEach((key, value) -> pattern.compute(value, (_k, _v) -> {
                if (_v == null) {
                    String[] values = NamespacedKey.fromString(key).getKey().split("_");
                    String patternValue = "_" + values[values.length - 1];

                    return pattern.values().stream().anyMatch(__v -> patternValue.equals(__v.val0))
                            ? system.toast(patternValue + "#" + pattern.size(), 1)
                            : system.toast(patternValue, 1);
                }
                _v.val1++;
                return _v;
            }));
            pattern.values().removeIf(_v -> _v.val1 < 2);
            JsonObject raw = system.json.object()
                    .addObject("Pattern", _v -> _v
                            .add(pattern.entrySet().stream().sorted(Comparator.comparing(kv -> kv.getValue().val0)).iterator(),
                                    kv -> kv.getValue().val0,
                                    kv -> kv.getKey().saveJson())
                    )
                    .addObject("All", _v -> _v
                            .add(biomeMap.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).iterator(),
                                    Map.Entry::getKey,
                                    kv -> Optional.ofNullable(pattern.get(kv.getValue()))
                                        .<JsonElement>map(__v -> new JsonPrimitive(__v.val0))
                                        .orElseGet(() -> kv.getValue().saveJson().build())
                            )
                    )
                    .build();
            lime.writeAllConfig("biome.raw", system.toFormat(raw));
        });
        AnyEvent.addEvent("recalculate.view", AnyEvent.type.owner, player -> {
            lime.logOP("Recalculate view: " + ViewDistance.clearPlayerView(player));
            /*lime.logOP("Player view:\n" + String.join("\n",
                "View distance: " + player.getViewDistance(),
                "Client view distance: " +  player.getClientViewDistance(),
                "Send view distance: " +   player.getSendViewDistance()
            ));*/
        });
        lime.repeat(Weather::update, 1);
    }
    /*
    private static void remapBiomes(DataPaletteBlock<Holder<BiomeBase>> biomes) {
        biomes.acquire();
        try {
            reflection.dynamic<?> _this = reflection.dynamic.of(biomes, DataPaletteBlock.class);
            reflection.dynamic<?> _data = _this.getMojang("data");
            DataPalette<Holder<BiomeBase>> _palette = (DataPalette<Holder<BiomeBase>>)_data.getMojang("palette").value; //DataPalette<Holder<BiomeBase>>
            DataBits _storage = (DataBits)_data.getMojang("storage").value; //DataBits

            int i2 = biomes.data.palette.idFor(value);
            biomes.data.storage.set(index, i2);
            biomes.set(biomes.strategy.getIndex(x2, y2, z2), value);
        }
        finally {
            biomes.release();
        }
    }
    private static void replaceBiomes(DataPaletteBlock<Holder<BiomeBase>> biomes) {
        for (int x = 0; x < 16; x += 4)
            for (int y = 0; y < 4; y += 4)
                for (int z = 0; z < 16; z += 4) {
                    biomes.set(x, y, z, biome);
                }
    }
*/
    private static int lastCounter = -1;
    private static SeasonKey lastSeasonKey = null;
    private static void update() {
        int counter = 0;
        if (HasteDonate.isHaste()) counter++;
        if (seasons) {
            DateTime now = DayManager.now();
            SeasonKey seasonKey = now.getSeasonKey();
            counter += switch (seasonKey) {
                case Sunny -> 2;
                case Frosty -> 0;
                case Rainy -> 1;
            };
            if (lastSeasonKey != seasonKey) {
                lastSeasonKey = seasonKey;
                changeWeather(seasonKey);
            }
        } else if (lime.MainWorld.isClearWeather()) counter++;

        if (counter == lastCounter) return;
        lastCounter = counter;

        double roundValue = initValue;
        for (int i = 0; i < counter; i++) roundValue *= stepValue;
        RandomTickSpeed.setRoundValue(roundValue);
    }

    public static SeasonKey getCurrentSeason() {
        return lastSeasonKey;
    }

    private static void changeWeather(SeasonKey seasonKey) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!(player instanceof CraftPlayer cplayer)) return;
            PlayerChunkMap chunkMap = cplayer.getHandle().getLevel().getChunkSource().chunkMap;
            PlayerChunkLoader.PlayerLoaderData data = chunkMap.playerChunkManager.getData(cplayer.getHandle());
            ReflectionAccess.sentChunks_PlayerLoaderData_PlayerChunkLoader.get(data).clear();
            ReflectionAccess.lastLocX_PlayerLoaderData_PlayerChunkLoader.set(data, Double.NEGATIVE_INFINITY);
            ViewDistance.clearPlayerView(player);
        });
        SnowModify.setSnow(seasonKey == SeasonKey.Frosty);
    }

    private static Stream<system.Toast3<Integer, NBTTagCompound, String>> generate(int id, String key, NBTTagCompound element) {
        return WeatherBiomes.selectBiomes(key).map(biomeHolder -> {
            NBTTagCompound seasonElement = element.copy();
            biomeHolder.biomeColors.modify(seasonElement);
            biomeHolder.seasonKey.modify(seasonElement);
            return system.toast(biomeHolder.index, seasonElement, biomeHolder.seasonKey.key);
        });
    }
}

















