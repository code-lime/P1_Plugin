package org.lime.gp.module.biome.weather;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.BiomeBase;
import org.lime.gp.module.biome.time.SeasonKey;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BiomeHolder implements Holder<BiomeBase> {
    public final int index;
    public final String vanillaKey;
    public final SeasonKey seasonKey;
    public final BiomeData biomeData;

    public BiomeHolder(int index, String vanillaKey, SeasonKey seasonKey, BiomeData biomeData) {
        this.index = index;
        this.vanillaKey = vanillaKey;
        this.seasonKey = seasonKey;
        this.biomeData = biomeData;
    }

    @Override public BiomeBase value() { return null; }

    @Override public boolean isBound() { return true; }

    @Override public boolean is(MinecraftKey id) { return false; }
    @Override public boolean is(ResourceKey<BiomeBase> key) { return false; }
    @Override public boolean is(Predicate<ResourceKey<BiomeBase>> predicate) { return false; }
    @Override public boolean is(TagKey<BiomeBase> tag) { return false; }

    @Override public Stream<TagKey<BiomeBase>> tags() { return Stream.empty(); }

    @Override public Either<ResourceKey<BiomeBase>, BiomeBase> unwrap() { return Either.right(value()); }
    @Override public Optional<ResourceKey<BiomeBase>> unwrapKey() { return Optional.empty(); }

    @Override public b kind() { return b.DIRECT; }
    @Override public boolean canSerializeIn(HolderOwner<BiomeBase> owner) { return true; }

    @Override public String toString() { return index + ":" + vanillaKey + "#" + seasonKey.key; }
}
