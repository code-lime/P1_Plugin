package org.lime.gp.item.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.lime.system;
import org.lime.gp.item.Items;

public interface Checker {
    default boolean check(ItemStack item) { return Items.getGlobalKeyByItem(item).map(this::check).orElse(false); }
    default boolean check(net.minecraft.world.item.ItemStack item) { return Items.getGlobalKeyByItem(item).map(this::check).orElse(false); }
    boolean check(String key);
    Stream<String> getWhitelistKeys();
    Stream<IItemCreator> getWhitelistCreators();
    Stream<Material> getWhitelist();

    Optional<IItemCreator> getRandomCreator();

    static Checker createCheck(system.Func1<String, Boolean> filter) {
        return new Checker() {
            private Set<String> keys = Collections.emptySet();
            private Set<Material> whitelist = Collections.emptySet();
            private Set<IItemCreator> creators = Collections.emptySet();
    
            private int loaded_index = -1;
            private void tryReload() {
                if (loaded_index == Items.getLoadedIndex()) return;
                loaded_index = Items.getLoadedIndex();

                this.creators = Items.creatorIDs.entrySet()
                    .stream()
                    .filter(v -> filter.invoke(v.getKey()))
                    .map(v -> v.getValue())
                    .collect(Collectors.toSet());
                this.keys = Items.creatorIDs.keySet()
                    .stream()
                    .filter(filter::invoke)
                    .collect(Collectors.toSet());
                this.whitelist = Items.creatorIDs.entrySet()
                    .stream()
                    .filter(v -> filter.invoke(v.getKey()))
                    .flatMap(v -> v.getValue().getWhitelist())
                    .collect(Collectors.toSet());
            }
    
            @Override public boolean check(String key) {
                tryReload();
                return this.keys.contains(key);
            }
            @Override public Stream<String> getWhitelistKeys() {
                tryReload();
                return this.keys.stream();
            }
            @Override public Stream<Material> getWhitelist() {
                tryReload();
                return this.whitelist.stream();
            }
            @Override public Stream<IItemCreator> getWhitelistCreators() {
                tryReload();
                return this.creators.stream();
            }
            @Override public Optional<IItemCreator> getRandomCreator() {
                tryReload();
                return creators.isEmpty() ? Optional.empty() : Optional.of(system.rand(creators));
            }
        };
    }

    static Checker createCheck(Collection<String> regexList) {
        return createCheck(value -> {
            return regexList.stream().anyMatch(regex -> system.compareRegex(value, regex));
        });
    }
    static Checker createCheck(String regex) {
        return createCheck(value -> {
            return system.compareRegex(value, regex);
        });
    }
    static Checker empty() {
        return new Checker() {
            @Override public boolean check(String key) { return false; }
            @Override public Stream<String> getWhitelistKeys() { return Stream.empty(); }
            @Override public Stream<IItemCreator> getWhitelistCreators() { return Stream.empty(); }
            @Override public Stream<Material> getWhitelist() { return Stream.empty(); }
            @Override public Optional<IItemCreator> getRandomCreator() { return Optional.empty(); }
        };
    }
}