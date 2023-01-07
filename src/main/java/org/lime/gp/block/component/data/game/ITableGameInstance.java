package org.lime.gp.block.component.data.game;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockSkullShapeInfo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.display.CacheBlockDisplay;
import org.lime.gp.block.component.display.DisplayInstance;
import org.lime.gp.map.MapMonitor;
import org.lime.gp.map.MonitorInstance;
import org.lime.gp.map.ViewPosition;
import org.lime.gp.module.DrawMap;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public abstract class ITableGameInstance<IElement extends ITableGameInstance.IGameElement> extends MonitorInstance implements CustomTileMetadata.Shapeable {
    public final int count;
    public final int SIZE_BOX;
    private final IElement[] elements;
    private boolean dirty = true;
    public void markDirty() {
        dirty = true;
    }

    private final DrawMap map = DrawMap.of();

    public ITableGameInstance(int count, Class<IElement> elementClass, ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(component, metadata);
        this.count = count;
        this.SIZE_BOX = SIZE / count;
        this.elements = (IElement[]) Array.newInstance(elementClass, count * count);
        setupDefault();

        CacheBlockDisplay.replaceCacheBlock(metadata.skull,
                CacheBlockDisplay.ICacheInfo.of(Blocks.BLACK_CARPET.defaultBlockState()));
        DisplayInstance.markDirtyBlock(metadata.position());
    }

    public Stream<IElement> getAll() { return Arrays.stream(Arrays.copyOf(elements, elements.length)); }

    public IElement getOf(int x, int y) {
        return in(x,y) ? elements[of(x,y)] : null;
    }
    public void setOf(int x, int y, IElement element) {
        if (!in(x, y)) return;
        elements[of(x,y)] = element;
    }
    public boolean in(int x, int y) {
        return x >= 0 && x < count && y >= 0 && y < count;
    }

    public IElement getOf(system.Toast2<Integer, Integer> pos) {
        return getOf(pos.val0, pos.val1);
    }
    public void setOf(system.Toast2<Integer, Integer> pos, IElement element) {
        setOf(pos.val0, pos.val1, element);
    }
    public boolean in(system.Toast2<Integer, Integer> pos) {
        return in(pos.val0, pos.val1);
    }

    public system.Toast2<Integer, Integer> box(int x, int y) {
        return system.toast(Math.max(Math.min(x, count - 1), 0), Math.max(Math.min(y, count - 1), 0));
    }


    public abstract IElement readElement(JsonObjectOptional json);
    public abstract void setupDefault();
    public abstract void drawBackground(DrawMap map);
    public abstract void drawBackground(int x, int y, DrawMap map);

    private int of(int x, int y) { return y * count + x; }
    private system.Toast2<Integer, Integer> of(int index) { return system.toast(index % count, index / count); }

    @Override public void read(JsonObjectOptional json) {
        json.getAsJsonArray("elements")
                .filter(v -> v.size() == count * count)
                .ifPresentOrElse(elements -> {
                    for (int x = 0; x < count; x++)
                        for (int y = 0; y < count; y++) {
                            int index = of(x,y);
                            elements.getAsJsonObject(index)
                                    .map(this::readElement)
                                    .ifPresent(v -> this.elements[index] = v);
                        }
                }, this::setupDefault);
    }
    @Override public system.json.builder.object write() {
        return system.json.object()
                .addArray("elements", v -> v.add(elements, IGameElement::write));
    }

    @Override public MapMonitor.MapRotation rotation() {
        return metadata()
                .list(DisplayInstance.class)
                .findAny()
                .flatMap(DisplayInstance::getRotation)
                .map(MapMonitor.MapRotation::of)
                .orElse(MapMonitor.MapRotation.NONE);
    }

    @Override public byte[] preMap() { return map.save(); }

    @Override public void onPostTick(Collection<Player> tickable, Collection<Player> viewers) {
        if (dirty) {
            dirty = false;
            drawBackground(map);
            for (int x = 0; x < count; x++)
                for (int y = 0; y < count; y++)
                    elements[of(x,y)].draw(x,y, SIZE_BOX, map);
            byte[] _map = map.save();
            viewers.forEach(viewer -> {
                if (tickable.contains(viewer)) return;
                DrawMap.sendMap(viewer, MapID, _map);
            });
        }
    }

    private system.Toast2<Integer, Integer> ofPixel(int pixel_x, int pixel_y) {
        return box((pixel_x * count) / SIZE, (pixel_y * count) / SIZE);
    }

    @Override public void onMouseTick(Player player, ViewPosition position) {
        system.Toast2<Integer, Integer> pos = ofPixel((int)Math.round(position.getDoubleX() * SIZE), (int)Math.round(position.getDoubleY() * SIZE));
        DrawMap map = this.map.copy();
        drawBackground(pos.val0, pos.val1, map);
        elements[of(pos.val0, pos.val1)].select(player, pos.val0, pos.val1, SIZE_BOX, position.getClick(), map);
        DrawMap.sendMap(player, MapID, map.save());
    }
    @Override public void onMouseStart(Player player, ViewPosition position) {

    }
    @Override public void onMouseEnd(Player player) {
        DrawMap.sendMap(player, MapID, map.save());
    }

    @Override public @Nullable VoxelShape onShape(CustomTileMetadata metadata, BlockSkullShapeInfo event) {
        return Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
    }

    public interface IGameElement {
        void draw(int x, int y, int size, DrawMap map);
        void select(Player player, int x, int y, int size, MapMonitor.ClickType click, DrawMap map);
        system.json.builder.object write();
    }
}
















