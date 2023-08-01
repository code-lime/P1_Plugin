package org.lime.gp.module.mobs.spawn;

import com.google.gson.JsonArray;
import org.lime.gp.module.mobs.IMobCreator;
import org.lime.gp.module.mobs.IPopulateSpawn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class FirstSpawn implements ISpawn {
    public List<ISpawn> spawns = new ArrayList<>();

    public FirstSpawn(JsonArray json) { json.getAsJsonArray().forEach(kv -> spawns.add(ISpawn.parse(kv))); }
    public FirstSpawn(Collection<ISpawn> spawns) { this.spawns.addAll(spawns); }

    @Override public Optional<IMobCreator> generateMob(IPopulateSpawn populate) {
        for (ISpawn spawn : spawns) {
            Optional<IMobCreator> creator = spawn.generateMob(populate);
            if (creator.isPresent()) return creator;
        }
        return Optional.empty();
    }
}
