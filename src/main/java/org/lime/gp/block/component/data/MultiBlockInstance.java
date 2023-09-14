package org.lime.gp.block.component.data;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullDestroyInfo;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.MultiBlockComponent;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.MathUtils;

import java.util.*;

public final class MultiBlockInstance extends BlockInstance implements CustomTileMetadata.Tickable, CustomTileMetadata.Destroyable {
    public interface OwnerVariableModifiable extends CustomTileMetadata.Element {
        void onOwnerVariableModify(CustomTileMetadata metadata, Map<String, String> variables);
    }
    public final HashMap<UUID, BlockPosition> positions = new HashMap<>();

    @Override public MultiBlockComponent component() { return (MultiBlockComponent)super.component(); }

    public MultiBlockInstance(MultiBlockComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    @Override public void read(JsonObjectOptional json) {
        positions.clear();
        json.getAsJsonObject("positions")
                .map(JsonObjectOptional::entrySet)
                .stream()
                .flatMap(Collection::stream)
                .forEach(kv -> kv.getValue().getAsString().ifPresent(value -> positions.put(UUID.fromString(kv.getKey()), parse(value))));
    }
    @Override public json.builder.object write() {
        return json.object()
                .addObject("positions", _v -> _v.add(positions, UUID::toString, v -> v.getX() + " " + v.getY() + " " + v.getZ()));
    }
    private static BlockPosition parse(String text) {
        var pos = MathUtils.getPosToast(text);
        return new BlockPosition(pos.val0, pos.val1, pos.val2);
    }
    public void child(UUID uuid, BlockPosition position) {
        positions.put(uuid, position);
        saveData();
    }
    private int ticker = 0;
    private long lastVariableIndex = -1;
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        World world = metadata.skull.getLevel();
        metadata.list(DisplayInstance.class)
                .findAny()
                .ifPresent(display -> {
                    long variableIndex = display.variableIndex();
                    if (variableIndex == lastVariableIndex) return;
                    lastVariableIndex = variableIndex;
                    Map<String, String> variable = display.getAll();
                    positions.forEach((uuid, pos) -> world.getBlockEntity(pos, TileEntityTypes.SKULL)
                            .flatMap(Blocks::customOf)
                            .ifPresent(v -> v.list(OwnerVariableModifiable.class)
                                    .forEach(_v -> _v.onOwnerVariableModify(v, variable))
                            )
                    );
                });
        if ((ticker++ % 40) != 0) return;
        positions.forEach((uuid, pos) -> world.getBlockEntity(pos, TileEntityTypes.SKULL)
                .map(v -> v instanceof TileEntityLimeSkull skull ? skull : null)
                .ifPresentOrElse(skull -> {
                    if (skull.customUUID().filter(_uuid -> _uuid.equals(uuid)).isEmpty()) metadata.setAir();
                }, metadata::setAir));
    }
    public boolean inDestroyMethod = false;
    @Override public void onDestroy(CustomTileMetadata metadata, BlockSkullDestroyInfo event) {
        inDestroyMethod = true;
        Optional<EntityPlayer> ofplayer = event.player().map(v -> v instanceof EntityPlayer p ? p : null);
        World world = event.world();
        positions.forEach((uuid, pos) -> world.getBlockEntity(pos, TileEntityTypes.SKULL)
                .flatMap(Blocks::customOf)
                .map(v -> v.skull)
                .filter(other -> other.customUUID().filter(_uuid -> _uuid.equals(uuid)).isPresent())
                .ifPresent(skull -> ofplayer.ifPresentOrElse(player -> {
                    List<EntityItem> captureDrops = world.captureDrops;
                    player.gameMode.destroyBlock(pos);
                    world.captureDrops = captureDrops;
                }, () -> world.destroyBlock(pos, false))));
        inDestroyMethod = false;
    }
}
































