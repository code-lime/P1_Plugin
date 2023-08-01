package org.lime.gp.sound;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.lime;
import org.lime.system;

import java.util.Collection;
import java.util.List;

public class TagSound extends ISound {
    public final ISound none;
    public final List<system.Toast2<String, ISound>> values;
    public TagSound(ISound none, List<system.Toast2<String, ISound>> values) {
        this.none = none;
        this.values = values;
    }

    private ISound getBy(Collection<String> tags) {
        for (system.Toast2<String, ISound> item : values) {
            if (tags.contains(item.val0))
                return item.val1;
        }
        return none;
    }

    @Override public void playSound(Player player, Collection<String> tags) { getBy(tags).playSound(player, tags); }
    @Override public void playSound(Player player, Vector position, Collection<String> tags) { getBy(tags).playSound(player, position, tags); }
    @Override public void playSound(Player player, Entity target, Collection<String> tags) { getBy(tags).playSound(player, target, tags); }
    @Override public void playSound(Player player, int targetId, Collection<String> tags) { getBy(tags).playSound(player, targetId, tags); }
    @Override public void playSound(Location location, Collection<String> tags) { getBy(tags).playSound(location, tags); }
}














