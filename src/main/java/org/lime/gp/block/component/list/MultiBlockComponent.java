package org.lime.gp.block.component.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.lime.Position;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.InfoComponent.Rotation.Value;
import org.lime.gp.block.component.data.MFPInstance;
import org.lime.gp.block.component.data.MultiBlockInstance;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.json;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.MathUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@InfoComponent.Component(name = "multiblock")
public final class MultiBlockComponent extends ComponentDynamic<JsonObject, MultiBlockInstance> {
    public final Map<Toast3<Integer, Integer, Integer>, IBlock> blocks = new LinkedHashMap<>();

    public interface IBlock {
        Optional<List<TileEntityLimeSkull>> set(Player player, UUID ownerBlock, String ownerBlockType, Position position, InfoComponent.Rotation.Value rotation, Toast3<Integer, Integer, Integer> local);
        boolean isCan(Block block, InfoComponent.Rotation.Value rotation);
        static IBlock create(JsonElement _json) {
            String key = _json.getAsString();
            return new IBlock() {
                @Override public Optional<List<TileEntityLimeSkull>> set(Player player, UUID ownerBlock, String ownerBlockType, Position position, Value rotation, Toast3<Integer, Integer, Integer> local) {
                    return Blocks.creator(key).map(block -> {
                                var offset = rotation.rotate(local);
                                return block.setMultiBlock(player, position.offset(offset), map.<String, JsonObject>of()
                                        .add("other.generic", json.object()
                                                .add("offset", -offset.val0 + " " + -offset.val1 + " " + -offset.val2)
                                                .add("owner", ownerBlock.toString())
                                                .add("owner_type", ownerBlockType)
                                                .build()
                                        )
                                        .add("display", json.object().add("rotation", rotation.angle + "").build())
                                        .build(), rotation);
                            })
                        .filter(skulls -> {
                            skulls.forEach(skull -> CoreProtectHandle.logSetBlock(skull, player));
                            return true;
                        });
                }
                @Override public boolean isCan(Block block, Value rotation) {
                    return Blocks.creator(key)
                        .flatMap(v -> v.component(MultiBlockComponent.class))
                        .map(v -> v.isCan(block, rotation))
                        .orElse(true);
                }

            };
        }
    }

    public MultiBlockComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        json.get("blocks").getAsJsonObject().entrySet().forEach(kv -> blocks.put(MathUtils.getPosToast(kv.getKey()), IBlock.create(kv.getValue())));
        blocks.remove(Toast.of(0, 0, 0));
    }

    public boolean isCan(Block block, InfoComponent.Rotation.Value rotation) {
        for (Map.Entry<Toast3<Integer, Integer, Integer>, IBlock> _kv : blocks.entrySet()) {
            Toast3<Integer, Integer, Integer> p = rotation.rotate(_kv.getKey());
            Block target = block.getRelative(p.val0, p.val1, p.val2);
            if (!target.getType().isAir()) return false;
            if (!_kv.getValue().isCan(target, rotation)) return false;
        }
        return true;
    }

    @Override public MultiBlockInstance createInstance(CustomTileMetadata metadata) { return new MultiBlockInstance(this, metadata); }
    @Override public Class<MultiBlockInstance> classInstance() { return MultiBlockInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
