package org.lime.gp.module.mobs.spawn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lime.gp.module.mobs.IMobCreator;
import org.lime.gp.module.mobs.IPopulateSpawn;

import java.util.Optional;

public interface ISpawn {
    Optional<IMobCreator> generateMob(IPopulateSpawn populate);

    static ISpawn parse(JsonElement json) {
        if (json.isJsonPrimitive()) return new SingleSpawn(json.getAsJsonPrimitive());
        else if (json.isJsonArray()) return new FirstSpawn(json.getAsJsonArray());
        else if (json.isJsonNull()) return EmptySpawn.Instance;
        else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            return obj.has("type") ? switch (obj.get("type").getAsString()) {
                case "random" -> new RandomSpawn(obj);
                default -> new EntitySpawn(obj);
            } : new FilterSpawn(obj);
        }
        throw new IllegalArgumentException("[LOOT] Error parse SpawnTable");
    }
}
