package org.lime.gp.module.biome.weather;

import net.minecraft.core.IRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.BiomeBase;
import org.bukkit.block.Biome;
import org.lime.gp.lime;
import org.lime.system.Regex;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

public interface BiomeChecker {
    default boolean check(Biome item) { return check(item.getKey().toString()); }
    default boolean check(BiomeBase item) {
        if (item == null) return false;
        MinecraftKey key = BIOMES.getKey(item);
        return key != null && check(key.toString());
    }
    boolean check(String key);
    Stream<String> getBiomeKeys();
    Stream<BiomeBase> getBiomes();
    boolean isEmpty();

    IRegistry<BiomeBase> BIOMES = MinecraftServer.getServer().registryAccess().registryOrThrow(Registries.BIOME);

    static BiomeChecker createCheck(Func1<String, Boolean> filter) {
        HashSet<String> keys = new HashSet<>();
        HashSet<BiomeBase> list = new HashSet<>();
        BIOMES.entrySet()
                .forEach(kv -> {
                    String key = kv.getKey().location().toString();
                    if (!filter.invoke(key)) return;
                    keys.add(key);
                    list.add(kv.getValue());
                });
        return new BiomeChecker() {
            @Override public boolean check(String key) { return keys.contains(key); }
            @Override public Stream<String> getBiomeKeys() { return keys.stream(); }
            @Override public Stream<BiomeBase> getBiomes() { return list.stream(); }
            @Override public boolean isEmpty() { return keys.isEmpty(); }
        };
    }

    static BiomeChecker createCheck(Collection<String> regexList) {
        BiomeChecker checker = createCheck(value -> regexList.stream().anyMatch(regex -> Regex.compareRegex(value, regex)));
        if (checker.isEmpty()) lime.logOP("Biome in list "+(regexList.isEmpty() ? "[]" : ("p'"+String.join("', '", regexList)+"']"))+" is EMPTY. Maybe error...");
        return checker;
    }
    static BiomeChecker createCheck(String regex) {
        BiomeChecker checker = createCheck(value -> Regex.compareRegex(value, regex));
        if (checker.isEmpty()) lime.logOP("Biome in '"+regex+"' is EMPTY. Maybe error...");
        return checker;
    }
    static BiomeChecker empty() {
        return new BiomeChecker() {
            @Override public boolean check(String key) { return false; }
            @Override public Stream<String> getBiomeKeys() { return Stream.empty(); }
            @Override public Stream<BiomeBase> getBiomes() { return Stream.empty(); }
            @Override public boolean isEmpty() { return true; }
        };
    }
}












