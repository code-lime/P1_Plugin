package org.lime.gp.module.mobs.spawn;

import com.google.gson.JsonPrimitive;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.lime.gp.module.mobs.IMobCreator;
import org.lime.gp.module.mobs.IPopulateSpawn;

import javax.annotation.Nullable;
import java.util.Optional;

public class SingleSpawn implements ISpawn {
    public final @Nullable EntityType type;

    public SingleSpawn(@Nullable EntityType type) { this.type = type; }
    private SingleSpawn(String type) { this(type.equals("EMPTY") ? null : EntityType.valueOf(type)); }
    public SingleSpawn(JsonPrimitive json) { this(json.getAsString()); }
    @Override public Optional<IMobCreator> generateMob(IPopulateSpawn populate) {
        return Optional.of((world, pos) -> {
            if (type == null) return null;
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
