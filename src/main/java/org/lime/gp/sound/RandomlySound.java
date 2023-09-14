package org.lime.gp.sound;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.system.utils.RandomUtils;

import java.util.Collection;
import java.util.List;

public class RandomlySound extends ISound {
    public final List<ISound> list;

    public RandomlySound(List<ISound> list) { this.list = list; }

    @Override public void playSound(Player player, Collection<String> tags) { RandomUtils.rand(list).playSound(player, tags); }
    @Override public void playSound(Player player, Vector position, Collection<String> tags) { RandomUtils.rand(list).playSound(player, position, tags); }
    @Override public void playSound(Player player, Entity target, Collection<String> tags) { RandomUtils.rand(list).playSound(player, target, tags); }
    @Override public void playSound(Player player, int targetId, Collection<String> tags) { RandomUtils.rand(list).playSound(player, targetId, tags); }
    @Override public void playSound(Location location, Collection<String> tags) { RandomUtils.rand(list).playSound(location, tags); }
}
