package org.lime.gp.module.npc.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.lime.gp.module.DrawText;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ShowNickName implements DrawText.IShow {
    @Override public String getID() { return display.npc().key() + ".NPC.NickName"; }
    @Override public boolean filter(Player player) { return display.isFilter(player); }
    @Override public Optional<Integer> parent() { return Optional.of(display.entityID); }
    @Override public Location location() { return location; }
    @Override public double distance() { return 20; }
    @Override public boolean tryRemove() { return false; }
    @Override public Component text(Player player) {
        List<Component> components = display.npc().getDisplayName(player);
        if (components.isEmpty()) return null;
        Collections.reverse(components);
        return Component.join(JoinConfiguration.newlines(), components);
    }

    public final EPlayerDisplay display;
    public final Location location;

    public ShowNickName(EPlayerDisplay display) {
        this.display = display;
        this.location = display.lastLocation();
    }
}
