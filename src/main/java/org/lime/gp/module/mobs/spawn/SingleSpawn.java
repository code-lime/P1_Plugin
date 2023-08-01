package org.lime.gp.module.mobs.spawn;

import com.google.gson.JsonPrimitive;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.lime.gp.module.mobs.IMobCreator;
import org.lime.gp.module.mobs.IPopulateSpawn;

import java.util.Optional;

public class SingleSpawn implements ISpawn {
    public final EntityType type;

    public SingleSpawn(EntityType type) { this.type = type; }
    public SingleSpawn(JsonPrimitive json) { this(EntityType.valueOf(json.getAsString())); }
    @Override public Optional<IMobCreator> generateMob(IPopulateSpawn populate) {
        return Optional.of((world, pos) -> {
            CraftWorld craftWorld = world.getWorld();
            return ((CraftEntity)craftWorld.spawnEntity(
                    new Location(craftWorld, pos.x, pos.y, pos.z),
                    type,
                    CreatureSpawnEvent.SpawnReason.CUSTOM,
                    entity -> entity.addScoreboardTag("spawn#generic")
            )).getHandle();
        });
    }
}
