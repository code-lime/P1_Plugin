package org.lime.gp.module.npc.display;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.lime.gp.module.DrawText;

import java.util.Optional;

public class ShowNone implements DrawText.IShow {
    @Override public String getID() { return display.npc().key() + ".NPC.None"; }
    @Override public boolean filter(Player player) { return display.isFilter(player); }
    @Override public Optional<Integer> parent() { return Optional.of(display.entityID); }
    @Override public Location location() { return location; }
    @Override public double distance() { return display.getDistance(); }
    @Override public boolean tryRemove() { return false; }
    @Override public Component text(Player player) { return Component.empty(); }

    public final EPlayerDisplay display;
    public final Location location;

    public ShowNone(EPlayerDisplay display) {
        this.display = display;
        this.location = display.lastLocation();
    }
}
