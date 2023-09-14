package org.lime.gp.module.biome.weather;

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
import net.minecraft.util.DataBits;
import net.minecraft.util.RegistryID;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.*;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.lime.gp.admin.AnyEvent;
import org.lime.plugin.CoreElement;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.gp.module.biome.BiomeModify;
import org.lime.gp.module.biome.CustomRegistry;
import org.lime.gp.module.biome.time.SeasonKey;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.reflection;
import org.lime.system.json;
import org.lime.system.range.IRange;
import org.lime.system.range.OnceRange;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WeatherBiomes {
    public static CoreElement create() {
        return CoreElement.create(WeatherBiomes.class)
                .withInit(WeatherBiomes::init)
                .<JsonObject>addConfig("weather_biomes", v -> v
                        .withDefault(() -> {
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
                            return json.object()
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
    private static final HashMap<Toast2<String, SeasonKey>, Integer> seasonToBiomeID = new HashMap<>();

    private static Holder<BiomeBase> BIOME_PLAINS;
    private static int BIOME_COUNT;
    private static CustomRegistry<Holder<BiomeBase>> BIOME_REGISTRY;

    private static IRange TEST_SECTION_RANGE = new OnceRange(15);

    private static void init() {
        BIOME_PLAINS = MinecraftServer.getServer().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        BIOME_COUNT = BiomeModify.getRawVanillaBiomes().mapToInt(v -> v.getInt("id")).max().orElse(-1) + 1;
        BIOME_REGISTRY = CustomRegistry.createBiomeRegistry(customBiomeMap);
        PacketManager.adapter()
                .add(ClientboundLevelChunkWithLightPacket.class, WeatherBiomes::onPacket)
                .listen();

        AnyEvent.addEvent("chunk.test", AnyEvent.type.owner, v -> v.createParam(IRange::parse, "[range]"), (v, range) -> {
            TEST_SECTION_RANGE = range;
            lime.logOP("SET SR: " + TEST_SECTION_RANGE.getAllInts(0).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
        });
    }

    private static void config(JsonObject _json) {
        List<String> whitelist = JsonElementOptional.of(_json.remove("whitelist"))
                .getAsJsonArray()
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsString)
                .flatMap(Optional::stream)
                .toList();

        json.builder.object sourceConfig = json.object();
        _json.entrySet().forEach(group -> group.getValue().getAsJsonObject().entrySet().forEach(dat -> {
            boolean isWhitelist = whitelist.contains(dat.getKey());

            JsonElement value = dat.getValue();
            //if (isWhitelist) lime.logOP("See raw " + dat.getKey() + ": " + value);

            if (value.isJsonPrimitive()) dat.setValue(value = json.object().add("parent", value.getAsString()).build());

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
                //if (isWhitelist) lime.logOP("Contains seasons " + dat.getKey());
                return;
            }
            //if (isWhitelist) lime.logOP("Add seasons " + dat.getKey());
            for (SeasonKey key : SeasonKey.values()) {
                String name = key.key;
                JsonObject raw = element.deepCopy();
                JsonElement parent = raw.remove("parent");
                if (parent != null) raw.addProperty("parent", parent.getAsString() + "#" + name);
                sourceConfig.add(dat.getKey() + "#" + name, raw);
            }
        }));
        HashMap<Toast2<String, SeasonKey>, BiomeData> biomeColorMap = new HashMap<>();
        lime.combineParent(sourceConfig.build(), false, false)
                .entrySet()
                .forEach(kv -> {
                    String[] args = kv.getKey().split("#");
                    if (!whitelist.contains(args[0])) return;
                    //lime.logOP("Setup biome settings: " + Toast.of(args[0], SeasonKey.byKey(args[1])));
                    biomeColorMap.put(Toast.of(args[0], SeasonKey.byKey(args[1])), BiomeData.parseJson(JsonObjectOptional.of(kv.getValue().getAsJsonObject())));
                });

        Toast1<Integer> iterator = Toast.of(BIOME_COUNT*2);
        HashMap<Integer, BiomeHolder> customBiomeList = new HashMap<>();
        HashMap<Toast2<String, SeasonKey>, Integer> seasonToBiomeID = new HashMap<>();
        biomeColorMap.entrySet().stream().sorted(Comparator.comparing(v -> v.getKey().toString())).forEach(kkv -> kkv.getKey().invoke((biomeName, seasonKey) -> {
            int index = iterator.val0;
            customBiomeList.put(index, new BiomeHolder(index, biomeName, seasonKey, kkv.getValue()));
            seasonToBiomeID.put(Toast.of(biomeName, seasonKey), index);
            //lime.logOP("Settings of " + biomeName + "#"+seasonKey.key+": " + kkv.getValue());
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

    private static final reflection.field<Object> value_SingleValuePalette = reflection.field.ofMojang(SingleValuePalette.class, "value");
    private static final reflection.field<Object[]> values_SingleValuePalette = reflection.field.ofMojang(DataPaletteLinear.class, "values");
    private static final reflection.field<RegistryID<?>> values_DataPaletteHash = reflection.field.ofMojang(DataPaletteHash.class, "values");
    private static final reflection.field<Object[]> keys_RegistryID = reflection.field.ofMojang(RegistryID.class, "keys");
    private static final reflection.field<Object[]> byId_RegistryID = reflection.field.ofMojang(RegistryID.class, "byId");

    private static final reflection.field<DataPaletteBlock<Holder<BiomeBase>>> biomes_ChunkSection = reflection.field.ofMojang(ChunkSection.class, "biomes");
    private static final reflection.field<Object> data_DataPaletteBlock = reflection.field.ofMojang(DataPaletteBlock.class, "data");
    private static final reflection.field<DataPalette<Holder<BiomeBase>>> palette_DataPaletteBlock_c = reflection.field.ofMojang(data_DataPaletteBlock.field.getType(), "palette");
    private static final reflection.field<DataBits> storage_DataPaletteBlock_c = reflection.field.ofMojang(data_DataPaletteBlock.field.getType(), "storage");
    private static final reflection.field<Object> configuration_DataPaletteBlock_c = reflection.field.ofMojang(data_DataPaletteBlock.field.getType(), "configuration");
    private static final reflection.constructor<?> new_DataPaletteBlock_c = reflection.constructor.of(data_DataPaletteBlock.field.getType(), configuration_DataPaletteBlock_c.field.getType(), DataBits.class, DataPalette.class);

    private static <T>boolean replaceValues(T[] values, Func1<T, T> replace, T def) {
        int length = values.length;
        boolean changed = false;
        for (int i = 0; i < length; i++) {
            if (values[i] == null) {
                values[i] = def;
                changed = true;
            }

            T result = replace.invoke(values[i]);
            if (result == null) continue;
            changed = true;
            values[i] = result;
        }
        return changed;
    }
    private static <T>DataPalette<T> replaceValues(DataBits storage, DataPalette<T> palette, Func1<T, T> replace, T def) {
        if (palette instanceof SingleValuePalette<T> single) {
            T value = single.valueFor(0);

            T result = replace.invoke(value);
            if (result == null) return palette;
            SingleValuePalette<T> out = (SingleValuePalette<T>)single.copy();
            value_SingleValuePalette.set(out, result);

            return out;
        } else if (palette instanceof DataPaletteLinear<T> linear) {
            DataPaletteLinear<T> out = (DataPaletteLinear<T>)linear.copy();

            T[] values = (T[])values_SingleValuePalette.get(out);

            return replaceValues(values, replace, def) ? out : linear;
        } else if (palette instanceof DataPaletteHash<T> hash) {
            DataPaletteHash<T> out = (DataPaletteHash<T>)hash.copy();

            RegistryID<T> values = (RegistryID<T>)values_DataPaletteHash.get(out);
            T[] keys = (T[])keys_RegistryID.get(values);
            T[] byId = (T[])byId_RegistryID.get(values);

            boolean change = replaceValues(keys, replace, def);
            change = replaceValues(byId, replace, def) || change;

            return change ? out : hash;
        } else if (palette instanceof DataPaletteGlobal<T> global) {
            storage.forEach((index, id) -> {
                T value = global.valueFor(id);
                T result = replace.invoke(value);
                if (result == null) return;
                storage.set(index, global.idFor(value));
            });
            return global;
        } else throw new IllegalArgumentException("Palette '"+palette+"' not supported");
    }
    private static void replaceValues(ChunkSection section, Func1<Holder<BiomeBase>, Holder<BiomeBase>> replace) {
        DataPaletteBlock<Holder<BiomeBase>> biomes = biomes_ChunkSection.get(section);
        Object data = data_DataPaletteBlock.get(biomes);
        DataPalette<Holder<BiomeBase>> palette = palette_DataPaletteBlock_c.get(data);
        DataBits storage = storage_DataPaletteBlock_c.get(data);

        DataPalette<Holder<BiomeBase>> newPalette = replaceValues(storage, palette, replace, BIOME_PLAINS);
        if (newPalette == palette) return;
        Object configuration = configuration_DataPaletteBlock_c.get(data);
        data_DataPaletteBlock.set(biomes, new_DataPaletteBlock_c.newInstance(configuration, storage, newPalette));
    }
    // tp -1476 58 -123
    public static void onPacket(ClientboundLevelChunkWithLightPacket packet, PacketEvent event) {
        if (!Weather.isSeasons()) return;
        //boolean isDebug = packet.getX() == -98 && packet.getZ() == -13;
        //boolean isDebug = packet.getX() == 43 && packet.getZ() == 18;
        //if (isDebug) lime.logOP("Begin update chunk");

        //DataPalette<T> palette
        ClientboundLevelChunkPacketData change = packet.getChunkData();
        PacketDataSerializer buffer = change.getReadBuffer();
        WorldServer world = ((CraftWorld)event.getPlayer().getWorld()).getHandle();

        //HashMap<String, Integer> executors = new HashMap<>();

        List<ChunkSection> sections = CustomRegistry.readSections(buffer, world, BIOME_REGISTRY, BIOME_PLAINS)
                .peek(kv -> kv.invoke((section, index) -> {
                    /*boolean isTestSection;
                    if (isDebug) {
                        isTestSection = TEST_SECTION_RANGE.hasInt(0, index);
                        lime.logOP("Test section " + index + ": " + isTestSection);
                    } else {
                        isTestSection = false;
                    }*/
                    List<String> lines = new ArrayList<>();
                    replaceValues(section, biome -> {
                        if (biome == null) biome = BIOME_PLAINS;
                        return biome.unwrapKey()
                                //.filter(isTestSection ? ExtMethods.filterLogExecute(lines::add, "Full key: {0}") : v -> true)
                                .map(v -> v.location().toString())
                                //.filter(isTestSection ? ExtMethods.filterLogExecute(lines::add, "Location: {0} / Season: " + Weather.getCurrentSeason().key) : v -> true)
                                .map(biomeName -> seasonToBiomeID.get(Toast.of(biomeName, Weather.getCurrentSeason())))
                                //.filter(isTestSection ? ExtMethods.filterLogExecute(lines::add, "Biome index: {0}") : v -> true)
                                .map(customBiomeMap::get)
                                //.filter(isTestSection ? ExtMethods.filterLogExecute(lines::add, "Biome: {0}") : v -> true)
                                .orElse(null);
                    });
                    /*if (isTestSection)
                        executors.compute(String.join("\n", lines), (k,count) -> (count == null ? 0 : count) + 1);*/
                    /*DataPalette<Holder<BiomeBase>> palette = reflection.dynamic.ofValue(section)
                            .getMojang("biomes")
                            .getMojang("data")
                            .<DataPalette<Holder<BiomeBase>>>getMojang("palette")
                            .value;
                    for (int x = 0; x < 4; x++)
                        for (int y = 0; y < 4; y++)
                            for (int z = 0; z < 4; z++) {
                                List<String> lines = new ArrayList<>();
                                BiomeHolder holder = section.getNoiseBiome(x,y,z).unwrapKey()
                                        .filter(isTestSection ? ExtMethods.filterLogExecute(lines::add, "Full key: {0}") : v -> true)
                                        .map(v -> v.location().toString())
                                        .filter(isTestSection ? ExtMethods.filterLogExecute(lines::add, "Location: {0} / Season: " + Weather.getCurrentSeason().key) : v -> true)
                                        .map(biomeName -> seasonToBiomeID.get(Toast.of(biomeName, Weather.getCurrentSeason())))
                                        .filter(isTestSection ? ExtMethods.filterLogExecute(lines::add, "Biome index: {0}") : v -> true)
                                        .map(customBiomeMap::get)
                                        .filter(isTestSection ? ExtMethods.filterLogExecute(lines::add, "Biome: {0}") : v -> true)
                                        .orElse(null);
                                if (holder != null) {
                                    if (isTestSection) lines.add("Holder: " + holder.vanillaKey + "#" + holder.seasonKey.key);
                                    section.setBiome(x, y, z, holder);
                                    if (isTestSection) {
                                        DataPalette<Holder<BiomeBase>> palette = reflection.dynamic.ofValue(section)
                                                .getMojang("biomes")
                                                .getMojang("data")
                                                .<DataPalette<Holder<BiomeBase>>>getMojang("palette")
                                                .value;
                                        lines.add("Biome RAW index: " + palette.idFor(holder));
                                    }
                                }
                                if (isTestSection)
                                    executors.compute(String.join("\n", lines), (k,count) -> (count == null ? 0 : count) + 1);
                            }*/
                }))
                .map(v -> v.val0)
                .toList();
        //if (isDebug) executors.forEach((key, count) -> lime.logOP("EXECUTE '"+count+"':\n" + key));
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
                .map(seasonKey -> seasonToBiomeID.get(Toast.of(key, seasonKey)))
                .filter(Objects::nonNull)
                .map(customBiomeMap::get);
    }
    public static Optional<BiomeHolder> selectBiome(SeasonKey season, String key) {
        return Optional.ofNullable(seasonToBiomeID.get(Toast.of(key, season)))
                .map(customBiomeMap::get);
    }
}
















