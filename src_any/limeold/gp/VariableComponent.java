package org.lime.gp.block.component;

import com.google.gson.JsonObject;
import net.minecraft.world.level.block.BlockSkullEventShape;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import org.lime.gp.block.display.BlockDisplay;
import org.lime.gp.block.BlocksOld;
import org.lime.system;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VariableComponent extends BlocksOld.InfoInstance implements InfoComponent.IShape {
    public final Components.LODComponent component;

    public VariableComponent(Components.LODComponent component, BlocksOld.Info info) {
        super(info);
        this.component = component;
    }

    private final ConcurrentHashMap<String, String> variables = new ConcurrentHashMap<>();

    public double maxDistance() {
        return component.maxDistance;
    }

    public Optional<LOD.ILOD> getLOD(double distance) {
        for (LOD.ILOD lod : component.lodList) {
            if (lod.distance <= distance) {
                return Optional.of(lod.lod(variables));
            }
        }
        return Optional.empty();
    }

    public Map<UUID, LOD.ILOD> getLodMap() {
        return component.lodMapUUID;
    }

    public void set(String key, String value) {
        variables.put(key, value);
        save();
    }

    @Override
    public InfoComponent.IReplace.Result replace(InfoComponent.IReplace.Input input) {
        return input.tryGetInfo()
                .map(v -> v.uuid)
                .map(v -> BlockDisplay.BLOCK_MANAGER.getDisplays().getOrDefault(v, null))
                .flatMap(v -> v.getLodUUID(input.player))
                .map(v -> getLodMap().getOrDefault(v, null))
                .map(v -> v.replace(input))
                .orElseGet(() -> maxDistance() > 0 ? component.lodList.get(0).lod(variables).replace(input) : input.toResult());
    }

    @Override
    public JsonObject load(JsonObject json) {
        json.entrySet().forEach(kv -> variables.put(kv.getKey(), kv.getValue().getAsString()));
        return json;
    }

    @Override
    public void save() {
        setSaved(system.json.object()
                .add(variables, k -> k, v -> v)
                .build());
    }

    @Override public boolean asyncShape(TileEntitySkull state, BlockSkullEventShape e) {
        return getLOD(0).map(lod -> lod.asyncShape(state, e)).orElse(false);
    }
}
