package org.lime.gp.module.mobs;

import net.minecraft.core.BlockPosition;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.IBlockDataHolder;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.gp.filter.data.IFilterParameterInfo;
import org.lime.gp.module.biome.time.SeasonKey;
import org.lime.gp.module.biome.weather.BiomeChecker;
import org.lime.gp.module.biome.weather.WeatherType;
import org.lime.system.json;
import org.lime.system.range.IRange;
import org.lime.system.toast.Toast;
import org.lime.system.utils.MathUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parameters {
    public static final SpawnParameter<Entity> ThisEntity = SpawnParameter.of("this");
    public static final SpawnParameter<Vector> Origin = SpawnParameter.of("origin");
    public static final SpawnParameter<IBlockData> FloorBlock = SpawnParameter.of("floor_block");
    public static final SpawnParameter<SeasonKey> SeasonKey = SpawnParameter.of("season_key");
    public static final SpawnParameter<WeatherType> Weather = SpawnParameter.of("weather_type");
    public static final SpawnParameter<CreatureSpawnEvent.SpawnReason> SpawnReason = SpawnParameter.of("spawn_reason");


    public static IFilterInfo<IPopulateSpawn> filterInfo() {
        return new IFilterInfo<IPopulateSpawn>() {
            @Override public Optional<IFilterParameterInfo<IPopulateSpawn, ?>> getParamInfo(String key) { return Parameters.paramInfo(key); }
            @Override public Collection<IFilterParameterInfo<IPopulateSpawn, ?>> getAllParams() { return Parameters.allInfo(); }
        };
    }

    private static final Map<String, IFilterParameterInfo<IPopulateSpawn, ?>> allInfo = Stream.<IFilterParameterInfo<IPopulateSpawn, ?>>builder()
            .add(ThisEntity.createInfoEqualsIgnoreCase("this", v -> v.getBukkitEntity().getType().name()))
            .add(FloorBlock.createInfoEqualsIgnoreCase("block", IBlockDataHolder::toString))
            .add(Origin.createInfoWorldFilter("biome",
                    (v, world) -> world.getBiome(new BlockPosition((int)v.getX(), (int)v.getY(), (int)v.getZ()))
                            .unwrapKey()
                            .map(ResourceKey::location)
                            .map(MinecraftKey::toString)
                            .orElse("NULL"),
                    BiomeChecker::createCheck,
                    (checker, v, world) -> checker.check(world.getBiome(new BlockPosition((int)v.getX(), (int)v.getY(), (int)v.getZ())).value())
            ))
            .add(Origin.createInfoFilter("position", v -> MathUtils.getDouble(v.getX()) + " " + MathUtils.getDouble(v.getY()) + " " + MathUtils.getDouble(v.getZ()), s -> {
                String[] args = s.split(" ");
                return Toast.of(IRange.parse(args[0]), IRange.parse(args[1]), IRange.parse(args[2]));
            }, (range, position) -> range.invokeGet((x, y, z) -> x.inRange(position.getX(), 16) && y.inRange(position.getY(), 16) && z.inRange(position.getZ(), 16))))
            .add(ThisEntity.createInfoFilter("tags",
                    v -> json.by(v.getTags()).build().toString(),
                    s -> List.of(s.split(",")),
                    (tags, entity) -> entity.getTags().containsAll(tags)))
            .add(SeasonKey.createInfoEqualsIgnoreCase("season", v -> v.key))
            .add(Weather.createInfoEqualsIgnoreCase("weather", Enum::name))
            .add(SpawnReason.createInfoEqualsIgnoreCase("reason", Enum::name))
            .build()
            .collect(Collectors.toMap(IFilterParameterInfo::name, v -> v));

    public static Optional<IFilterParameterInfo<IPopulateSpawn, ?>> paramInfo(String key) { return Optional.ofNullable(allInfo.get(key)); }
    public static Collection<IFilterParameterInfo<IPopulateSpawn, ?>> allInfo() { return allInfo.values(); }
}














