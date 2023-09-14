package org.lime.gp.sound;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface IReplaceInfo {
    void playSound(ISound sound, Player player, Collection<String> tags);
    default Collection<String> tags() { return Collections.emptyList(); }

    static IReplaceInfo ofID(int id) { return (sound, player, tags) -> sound.playSound(player, id, tags); }
    static IReplaceInfo ofPos(Vector pos) { return (sound, player, tags) -> sound.playSound(player, pos, tags); }

    default IReplaceInfo setTags(Collection<String> tags) {
        return tags.isEmpty() ? this : new IReplaceInfo() {
            @Override public void playSound(ISound sound, Player player, Collection<String> tags) { IReplaceInfo.this.playSound(sound, player, tags); }
            @Override public Collection<String> tags() { return tags; }
        };
    }
    default IReplaceInfo setTags(String... tags) { return setTags(List.of(tags)); }
}










