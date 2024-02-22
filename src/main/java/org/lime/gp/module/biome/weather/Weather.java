package org.lime.gp.module.biome.weather;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.valueproviders.UniformInt;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.lime.gp.module.ChunkForceView;
import org.lime.plugin.CoreElement;
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
import org.lime.system.json;
import org.lime.system.toast.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Weather {
    private static double initValue = 2;
    private static double stepValue = 1.5;
    private static boolean seasons = false;

    private static BiomeModify.ActionCloseable generateActionCloseable = null;

    public static CoreElement create() {
        return CoreElement.create(Weather.class)
                .withInit(Weather::init)
                .<JsonObject>addConfig("config", v -> v
                        .withParent("weather")
                        .withDefault(json.object()
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
            HashMap<BiomeData, Toast2<String, Integer>> pattern = new HashMap<>();
            Map<String, BiomeData> biomeMap = BiomeModify.getRawVanillaBiomes()
                    .collect(Collectors.toMap(_v -> _v.getString("name"), _v -> BiomeData.parseElement(_v.getCompound("element"))));
            biomeMap.forEach((key, value) -> pattern.compute(value, (_k, _v) -> {
                if (_v == null) {
                    String[] values = NamespacedKey.fromString(key).getKey().split("_");
                    String patternValue = "_" + values[values.length - 1];

                    return pattern.values().stream().anyMatch(__v -> patternValue.equals(__v.val0))
                            ? Toast.of(patternValue + "#" + pattern.size(), 1)
                            : Toast.of(patternValue, 1);
                }
                _v.val1++;
                return _v;
            }));
            pattern.values().removeIf(_v -> _v.val1 < 2);
            JsonObject raw = json.object()
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
            lime.writeAllConfig("biome.raw", json.format(raw));
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

    public static boolean isSeasons() {
        return seasons;
    }
    public static SeasonKey getCurrentSeason() {
        return lastSeasonKey;
    }

    private static void changeWeather(SeasonKey seasonKey) {
        ChunkForceView.update();
        SnowModify.setBiome(seasonKey);

        int modifyRain = switch (seasonKey) {
            case Frosty -> 2;
            case Rainy -> 4;
            default -> 1;
        };
        ReflectionAccess.RAIN_DELAY_WorldServer.set(null, UniformInt.of(12000 / modifyRain, 180000 / modifyRain));
        ReflectionAccess.RAIN_DURATION_WorldServer.set(null, UniformInt.of(12000 * modifyRain, 24000 * modifyRain));
        Bukkit.getWorlds().forEach(world -> world.setStorm(seasonKey != SeasonKey.Sunny));
    }

    private static Stream<Toast3<Integer, NBTTagCompound, String>> generate(int id, String key, NBTTagCompound element) {
        return WeatherBiomes.selectBiomes(key).map(biomeHolder -> {
            NBTTagCompound seasonElement = element.copy();
            biomeHolder.seasonKey.modify(seasonElement);
            biomeHolder.biomeData.modify(seasonElement);
            return Toast.of(biomeHolder.index, seasonElement, biomeHolder.seasonKey.key);
        });
    }
}

















