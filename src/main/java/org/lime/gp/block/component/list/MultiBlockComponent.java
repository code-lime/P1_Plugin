package org.lime.gp.block.component.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.lime.Position;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.MultiBlockInstance;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.system;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@InfoComponent.Component(name = "multiblock")
public final class MultiBlockComponent extends ComponentDynamic<JsonObject, MultiBlockInstance> {
    public final Map<system.Toast3<Integer, Integer, Integer>, IBlock> blocks = new LinkedHashMap<>();

    public interface IBlock {
        Optional<TileEntityLimeSkull> set(Player player, UUID ownerBlock, String ownerBlockType, Position position, InfoComponent.Rotation.Value rotation, system.Toast3<Integer, Integer, Integer> local);

        static IBlock create(JsonElement json) {
            String key = json.getAsString();
            return (player, ownerBlock, ownerBlockType, position, rotation, local) -> Blocks.creator(key).map(block -> block.setBlock(position.offset(rotation.rotate(local)), system.map.<String, JsonObject>of()
                            .add("other.generic", system.json.object()
                                    .add("position", position.toSave())
                                    .add("owner", ownerBlock.toString())
                                    .add("owner_type", ownerBlockType)
                                    .build()
                            )
                            .add("display", system.json.object().add("rotation", rotation.angle + "").build())
                            .build()))
                    .filter(skull -> {
                        CoreProtectHandle.logSetBlock(skull, player);
                        return true;
                    });
        }
    }

    public MultiBlockComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        json.get("blocks").getAsJsonObject().entrySet().forEach(kv -> blocks.put(system.getPosToast(kv.getKey()), IBlock.create(kv.getValue())));
        blocks.remove(system.toast(0, 0, 0));
    }

    public boolean isCan(Block block, InfoComponent.Rotation.Value rotation) {
        for (system.Toast3<Integer, Integer, Integer> _p : blocks.keySet()) {
            system.Toast3<Integer, Integer, Integer> p = rotation.rotate(_p);
            if (!block.getRelative(p.val0, p.val1, p.val2).getType().isAir())
                return false;
        }
        return true;
    }

    @Override
    public MultiBlockInstance createInstance(CustomTileMetadata metadata) {
        return new MultiBlockInstance(this, metadata);
    }
}
