package org.lime.gp.craft.slot.output;

import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import org.bukkit.entity.Player;
import org.lime.gp.player.level.LevelModule;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface IOutputVariable {
    Optional<Integer> getLevel();
    Optional<Integer> getLevel(int work);

    static IOutputVariable empty() {
        return new IOutputVariable() {
            @Override public Optional<Integer> getLevel() { return Optional.empty(); }
            @Override public Optional<Integer> getLevel(int work) { return Optional.empty(); }
        };
    }
    static IOutputVariable of(Player player) { return of(player.getUniqueId()); }
    static IOutputVariable of(EntityHuman player) { return of(player.getUUID()); }
    static IOutputVariable of(UUID uuid) {
        return new IOutputVariable() {
            @Override public Optional<Integer> getLevel() { return LevelModule.getCurrentLevel(uuid); }
            @Override public Optional<Integer> getLevel(int work) { return LevelModule.getCurrentLevel(uuid, work); }
        };
    }
}
