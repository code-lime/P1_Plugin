package org.lime.gp.item.data;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.lime.system.Regex;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.gp.item.Items;
import org.lime.system.utils.RandomUtils;

public interface Checker {
    default boolean check(ItemStack item) { return Items.getGlobalKeyByItem(item).map(this::check).orElse(false); }
    default boolean check(net.minecraft.world.item.ItemStack item) { return Items.getGlobalKeyByItem(item).map(this::check).orElse(false); }
    boolean check(String key);
    Stream<String> getWhitelistKeys();
    Stream<IItemCreator> getWhitelistCreators();
    Stream<Material> getWhitelist();

    Optional<IItemCreator> getRandomCreator();

    static Checker createCheck(Func1<String, Boolean> filter) {
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
                    .map(Map.Entry::getValue)
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
                return creators.isEmpty() ? Optional.empty() : Optional.of(RandomUtils.rand(creators));
            }
        };
    }

    static Checker createCheck(Collection<String> regexList) {
        return createCheck(value -> regexList.stream().anyMatch(regex -> Regex.compareRegex(value, regex)));
    }
    static Checker createCheck(String regex) {
        return createCheck(value -> Regex.compareRegex(value, regex));
    }
    static Checker createRawCheck(Collection<net.minecraft.world.item.ItemStack> items) {
        return createCheck(items.stream().map(Items::getGlobalKeyByItem).flatMap(Optional::stream).collect(Collectors.toSet())::contains);
    }
    static Checker createRawCheck(net.minecraft.world.item.ItemStack item) {
        return Items.getGlobalKeyByItem(item).map(key -> createCheck(key::equalsIgnoreCase)).orElseGet(Checker::empty);
    }
    static Checker createRawCheck(IItemCreator creator) {
        return createCheck(creator.getKey()::equalsIgnoreCase);
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