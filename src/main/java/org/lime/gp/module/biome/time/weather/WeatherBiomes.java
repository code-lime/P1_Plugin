package org.lime.gp.module.biome.time.weather;

import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkSection;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.gp.module.biome.BiomeModify;
import org.lime.gp.module.biome.CustomRegistry;
import org.lime.gp.module.biome.time.SeasonKey;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WeatherBiomes {
    public static core.element create() {
        return core.element.create(WeatherBiomes.class)
                .withInit(WeatherBiomes::init)
                .<JsonObject>addConfig("weather_biomes", v -> v
                        .withDefault(() -> {
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
                            return system.json.object()
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
                        })
                        .withInvoke(WeatherBiomes::config)
                );
    }

    private static final HashBiMap<Integer, BiomeHolder> customBiomeMap = HashBiMap.create();
    private static final HashMap<system.Toast2<String, SeasonKey>, Integer> seasonToBiomeID = new HashMap<>();

    private static Holder<BiomeBase> BIOME_PLAINS;
    private static int BIOME_COUNT;
    private static CustomRegistry<Holder<BiomeBase>> BIOME_REGISTRY;

    private static void init() {
        BIOME_PLAINS = MinecraftServer.getServer().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        BIOME_COUNT = BiomeModify.getRawVanillaBiomes().mapToInt(v -> v.getInt("id")).max().orElse(-1) + 1;
        BIOME_REGISTRY = CustomRegistry.createBiomeRegistry(customBiomeMap);
        PacketManager.adapter()
                .add(ClientboundLevelChunkWithLightPacket.class, WeatherBiomes::onPacket)
                .listen();
    }

    private static void config(JsonObject json) {
        List<String> whitelist = JsonElementOptional.of(json.remove("whitelist"))
                .getAsJsonArray()
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsString)
                .flatMap(Optional::stream)
                .toList();

        system.json.builder.object sourceConfig = system.json.object();
        json.entrySet().forEach(group -> group.getValue().getAsJsonObject().entrySet().forEach(dat -> {
            boolean isWhitelist = whitelist.contains(dat.getKey());

            JsonElement value = dat.getValue();
            if (isWhitelist) lime.logOP("See raw " + dat.getKey() + ": " + value);

            if (value.isJsonPrimitive()) dat.setValue(value = system.json.object().add("parent", value.getAsString()).build());

            JsonObject element = value.getAsJsonObject();
            boolean isSeasons = false;
            for (SeasonKey key : SeasonKey.values()) {
                String name = key.key;
                if (element.has(name)) {
                    isSeasons = true;
                    JsonObject raw = element.get(name).deepCopy().getAsJsonObject();
                    sourceConfig.add(dat.getKey() + "#" + name, raw);
                    JsonElement parent = raw.remove("parent");
                    if (parent != null) raw.addProperty("parent", parent.getAsString() + "#" + name);
                }
            }
            if (isSeasons) {
                if (isWhitelist) lime.logOP("Contains seasons " + dat.getKey());
                return;
            }
            if (isWhitelist) lime.logOP("Add seasons " + dat.getKey());
            for (SeasonKey key : SeasonKey.values()) {
                String name = key.key;
                JsonObject raw = element.deepCopy();
                JsonElement parent = raw.remove("parent");
                if (parent != null) raw.addProperty("parent", parent.getAsString() + "#" + name);
                sourceConfig.add(dat.getKey() + "#" + name, raw);
            }
        }));
        HashMap<system.Toast2<String, SeasonKey>, BiomeColors> biomeColorMap = new HashMap<>();
        lime.combineParent(sourceConfig.build(), false, false)
                .entrySet()
                .forEach(kv -> {
                    String[] args = kv.getKey().split("#");
                    if (!whitelist.contains(args[0])) return;
                    lime.logOP("Setup biome settings: " + system.toast(args[0], SeasonKey.byKey(args[1])));
                    biomeColorMap.put(system.toast(args[0], SeasonKey.byKey(args[1])), BiomeColors.parseJson(JsonObjectOptional.of(kv.getValue().getAsJsonObject())));
                });

        system.Toast1<Integer> iterator = system.toast(BIOME_COUNT*2);
        HashMap<Integer, BiomeHolder> customBiomeList = new HashMap<>();
        HashMap<system.Toast2<String, SeasonKey>, Integer> seasonToBiomeID = new HashMap<>();
        biomeColorMap.forEach((kk, v) -> kk.invoke((biomeName, seasonKey) -> {
            int index = iterator.val0;
            customBiomeList.put(index, new BiomeHolder(index, biomeName, seasonKey, v));
            seasonToBiomeID.put(system.toast(biomeName, seasonKey), index);
            lime.logOP("Settings of " + biomeName + "#"+seasonKey.key+": " + v);
            iterator.val0++;
        }));
        WeatherBiomes.customBiomeMap.clear();
        WeatherBiomes.customBiomeMap.putAll(customBiomeList);

        WeatherBiomes.seasonToBiomeID.clear();
        WeatherBiomes.seasonToBiomeID.putAll(seasonToBiomeID);
    }

    private static ByteBuf getWriteBuffer(byte[] buffer) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
        byteBuf.writerIndex(0);
        return byteBuf;
    }
    public static void onPacket(ClientboundLevelChunkWithLightPacket packet, PacketEvent event) {
        if (!Weather.isSeasons()) return;
        //boolean isDebug = packet.getX() == 0 && packet.getZ() == 0;
        //if (isDebug) lime.logOP("Begin update chunk");
        ClientboundLevelChunkPacketData change = packet.getChunkData();
        PacketDataSerializer buffer = change.getReadBuffer();
        WorldServer world = ((CraftWorld)event.getPlayer().getWorld()).getHandle();
        List<ChunkSection> sections = CustomRegistry.readSections(buffer, world, BIOME_REGISTRY, BIOME_PLAINS).peek(section -> {
            for (int x = 0; x < 4; x++)
                for (int y = 0; y < 4; y++)
                    for (int z = 0; z < 4; z++) {
                        //boolean isDebugSection = isDebug && section.bottomBlockY() == ChunkSection.getBottomBlockY(4) && x == 0 && y == 0 && z == 0;
                        BiomeHolder holder = section.getNoiseBiome(x,y,z).unwrapKey()
                                //.filter(isDebugSection ? ExtMethods.filterLog("Full key: {0}") : v -> true)
                                .map(v -> v.location().toString())
                                //.filter(isDebugSection ? ExtMethods.filterLog("Location: {0} / Season: " + Weather.getCurrentSeason().key) : v -> true)
                                .map(biomeName -> seasonToBiomeID.get(system.toast(biomeName, Weather.getCurrentSeason())))
                                //.filter(isDebugSection ? ExtMethods.filterLog("Biome index: {0}") : v -> true)
                                .map(customBiomeMap::get)
                                //.filter(isDebugSection ? ExtMethods.filterLog("Biome: {0}") : v -> true)
                                .orElse(null);
                        if (holder == null) continue;
                        //if (isDebugSection) lime.logOP("Holder: " + holder.vanillaKey + "#" + holder.seasonKey.key);
                        section.setBiome(x, y, z, holder);
                    }
        }).toList();
        byte[] bytes = new byte[sections.stream().mapToInt(ChunkSection::getSerializedSize).sum()];
        PacketDataSerializer serializer = new PacketDataSerializer(getWriteBuffer(bytes));
        for (ChunkSection section : sections) section.write(serializer, null);
        ReflectionAccess.buffer_ClientboundLevelChunkPacketData.set(change, bytes);
        //if (isDebug) lime.logOP("End update chunk");
    }

    private static JsonElement colorsToHex(JsonElement json) {
        if (json.isJsonArray()) json.getAsJsonArray().forEach(WeatherBiomes::colorsToHex);
        else if (json.isJsonObject()) {
            json.getAsJsonObject().entrySet().forEach(kv -> {
                JsonElement value = kv.getValue();
                if (!kv.getKey().endsWith("color") || !value.isJsonPrimitive()) {
                    colorsToHex(value);
                    return;
                }
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                if (!primitive.isNumber()) {
                    colorsToHex(value);
                    return;
                }
                kv.setValue(new JsonPrimitive(TextColor.color(primitive.getAsInt()).asHexString().toUpperCase()));
            });
        }
        return json;
    }
    private static JsonElement colorsToIndex(JsonElement json) {
        if (json.isJsonArray()) json.getAsJsonArray().forEach(WeatherBiomes::colorsToHex);
        else if (json.isJsonObject()) {
            json.getAsJsonObject().entrySet().forEach(kv -> {
                JsonElement value = kv.getValue();
                if (!kv.getKey().endsWith("color") || !value.isJsonPrimitive()) {
                    colorsToHex(value);
                    return;
                }
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                TextColor color;
                if (!primitive.isString() || (color = TextColor.fromHexString(primitive.getAsString())) == null) {
                    colorsToHex(value);
                    return;
                }
                kv.setValue(new JsonPrimitive(color.value()));
            });
        }
        return json;
    }

    public static Stream<BiomeHolder> selectBiomes(String key) {
        return Stream.of(SeasonKey.values())
                .map(seasonKey -> seasonToBiomeID.get(system.toast(key, seasonKey)))
                .filter(Objects::nonNull)
                .map(customBiomeMap::get);
    }
}
















