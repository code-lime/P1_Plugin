package org.lime.gp.entity.component.data.boat;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.lime.display.ObjectDisplay;

import java.util.UUID;

public class BoatDisplay extends ObjectDisplay<BoatData, Marker> {
    private final UUID uuid;
    private BoatData data;

    @Override public Location location() { return data.partPosition.toLocation(data.world); }

    public BoatDisplay(UUID uuid, BoatData data) {
        this.uuid = uuid;
        this.data = data;
    }

    @Override public void update(BoatData data, double delta) {
        this.data = data;
        super.update(data, delta);
    }

    @Override protected Marker createEntity(Location location) {
        return new Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
    }
}
