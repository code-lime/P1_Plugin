package org.lime.gp.block.component.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import org.lime.system.execute.Func2;
import org.lime.system.json;
import org.lime.system.utils.MathUtils;

import java.util.*;

public final class MultiBlockInstance extends BlockInstance implements CustomTileMetadata.Tickable, CustomTileMetadata.Destroyable {
    public interface OwnerVariableModifiable extends CustomTileMetadata.Element {
        void onOwnerVariableModify(CustomTileMetadata metadata, Map<String, String> variables);
    }
    public final HashMap<UUID, BlockPosition> offsets = new HashMap<>();

    @Override public MultiBlockComponent component() { return (MultiBlockComponent)super.component(); }

    public MultiBlockInstance(MultiBlockComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    @Override public void read(JsonObjectOptional json) {
        offsets.clear();

        json.getAsJsonObject("offsets")
                .ifPresentOrElse(v -> v.forEach((key, offset) -> offset.getAsString().ifPresent(value -> offsets.put(UUID.fromString(key), parse(value)))),
                        () -> {
                            BlockPosition position = metadata().skull.getBlockPos();
                            json.getAsJsonObject("positions")
                                    .ifPresent(v -> v.forEach((key, pos) -> pos.getAsString().ifPresent(value -> offsets.put(UUID.fromString(key), parse(value).subtract(position)))));
                        });
    }
    @Override public json.builder.object write() {
        return json.object()
                .addObject("offsets", _v -> _v.add(offsets, UUID::toString, v -> v.getX() + " " + v.getY() + " " + v.getZ()));
    }
    public static JsonObject mapBlockUuids(JsonObject json, BlockPosition position, Func2<BlockPosition, UUID, UUID> mapper) {
        if (json.has("offsets")) {
            json = json.deepCopy();
            Map<String, JsonElement> offsets = json.getAsJsonObject("offsets").asMap();
            Map<String, JsonElement> elements = new HashMap<>();
            offsets.forEach((key, value) -> {
                BlockPosition offset = parse(value.getAsString());
                elements.put(mapper.invoke(position.offset(offset), UUID.fromString(key)).toString(), value);
            });
            offsets.clear();
            offsets.putAll(elements);
        }
        return json;
    }

    private static BlockPosition parse(String text) {
        var pos = MathUtils.getPosToast(text);
        return new BlockPosition(pos.val0, pos.val1, pos.val2);
    }
    public void child(UUID uuid, BlockPosition position) {
        offsets.put(uuid, position.subtract(metadata().skull.getBlockPos()));
        saveData();
    }
    private int ticker = 0;
    private long lastVariableIndex = -1;
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        BlockPosition position = metadata.skull.getBlockPos();
        World world = metadata.skull.getLevel();
        metadata.list(DisplayInstance.class)
                .findAny()
                .ifPresent(display -> {
                    long variableIndex = display.variableIndex();
                    if (variableIndex == lastVariableIndex) return;
                    lastVariableIndex = variableIndex;
                    Map<String, String> variable = display.getAll();
                    offsets.forEach((uuid, offset) -> world.getBlockEntity(position.offset(offset), TileEntityTypes.SKULL)
                            .flatMap(Blocks::customOf)
                            .ifPresent(v -> v.list(OwnerVariableModifiable.class)
                                    .forEach(_v -> _v.onOwnerVariableModify(v, variable))
                            )
                    );
                });
        if ((ticker++ % 40) != 0) return;
        offsets.forEach((uuid, offset) -> world.getBlockEntity(position.offset(offset), TileEntityTypes.SKULL)
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
        BlockPosition position = metadata.skull.getBlockPos();

        offsets.forEach((uuid, offset) -> world.getBlockEntity(position.offset(offset), TileEntityTypes.SKULL)
                .flatMap(Blocks::customOf)
                .map(v -> v.skull)
                .filter(other -> other.customUUID().filter(_uuid -> _uuid.equals(uuid)).isPresent())
                .ifPresent(skull -> ofplayer.ifPresentOrElse(player -> {
                    List<EntityItem> captureDrops = world.captureDrops;
                    player.gameMode.destroyBlock(position.offset(offset));
                    world.captureDrops = captureDrops;
                }, () -> world.destroyBlock(position.offset(offset), false))));
        inDestroyMethod = false;
    }
}
































