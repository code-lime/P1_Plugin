package org.lime.gp.block.component.data;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerCartography;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.BlockSkullShapeInfo;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.map.CraftMapView;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.admin.AnyEvent.type;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.CacheBlockDisplay;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.LootComponent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.CartographyBrush;
import org.lime.gp.item.CartographyBucket;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.gp.map.MapMonitor;
import org.lime.gp.map.MonitorInstance;
import org.lime.gp.map.ViewPosition;
import org.lime.gp.module.DrawMap;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class CartographyInstance extends MonitorInstance implements CustomTileMetadata.Interactable, CustomTileMetadata.Shapeable, CustomTileMetadata.Lootable {
    public static core.element create() {
        Blocks.addDefaultBlocks(new BlockInfo("cartography_table")
                .add(v -> InfoComponent.GenericDynamicComponent.of("cartography", v, CartographyInstance::new))
                .add(v -> new LootComponent(v, List.of(Material.CARTOGRAPHY_TABLE)))
                .addReplace(Material.CARTOGRAPHY_TABLE)
        );
        return core.element.create(CartographyInstance.class)
            .withInit(CartographyInstance::init);
    }

    public static void init() {
        AnyEvent.addEvent("template.map", type.owner, player -> {
            DrawMap map = DrawMap.of(new byte[128*128]);

            int i = 0;
            int offset = Byte.MAX_VALUE - Byte.MIN_VALUE;
            for (int x = 0; x < 32; x++) {
                if (i > offset) break;
                for (int y = 0; y < 32; y++) {
                    if (i > offset) break;
                    map.rectangle(x * 4, y * 4, 4, 4, (byte)(i + Byte.MIN_VALUE));
                    i++;
                }
            }

            Items.dropGiveItem(player, createMap(map.save()), false);
        });
    }

    private byte[] map;
    public CartographyInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(component, metadata);
        CacheBlockDisplay.replaceCacheBlock(metadata.skull, CacheBlockDisplay.ICacheInfo.of(net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE.defaultBlockState()));
        DisplayInstance.markDirtyBlock(metadata.position());
    }

    @Override public MapMonitor.MapRotation rotation() { return MapMonitor.MapRotation.NONE; }
    @Override public boolean isShow() { return map != null; }
    @Override public Vector offset() { return new Vector(0, 1, 0); }

    private static final IChatBaseComponent CONTAINER_TITLE = IChatBaseComponent.translatable("container.cartography_table");
    private static ContainerAccess at(final World world, final BlockPosition pos) {
        return new ContainerAccess(){
            @Override public World getWorld() { return world; }
            @Override public BlockPosition getPosition() { return pos; }
            @Override public <T> Optional<T> evaluate(BiFunction<World, BlockPosition, T> getter) { return Optional.empty(); }
            @Override public void execute(BiConsumer<World, BlockPosition> function) { function.accept(world, pos); }
        };
    }
    private static ITileInventory getInventory(World world, BlockPosition pos) {
        return new TileInventory((syncId, inventory, player) -> new ContainerCartography(syncId, inventory, at(world, pos)), CONTAINER_TITLE);
    }

    private static ItemStack createMap(byte[] data) {
        boolean empty = true;
        for (byte pixel : data) {
            if (pixel == 0) continue;
            empty = false;
            break;
        }
        if (empty) return new ItemStack(Material.MAP);
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta)item.getItemMeta();
        MapView mapView = Bukkit.createMap(lime.LoginWorld);
        mapView.setLocked(false);
        WorldMap map = ReflectionAccess.worldMap_CraftMapView.get(mapView);
        System.arraycopy(data, 0, map.colors, 0, data.length);
        map.setDirty();
        meta.setMapView(mapView);
        item.setItemMeta(meta);
        return item;
    }
    @SuppressWarnings("deprecation")
    private static byte[] readMap(int index) {
        CraftMapView view = (CraftMapView)Bukkit.getMap(index);
        byte[] data = new byte[128*128];
        if (view == null) return data;
        WorldMap map = ReflectionAccess.worldMap_CraftMapView.get(view);
        System.arraycopy(map.colors, 0, data, 0, data.length);
        return data;
    }

    @Override public void read(JsonObjectOptional json) {
        map = json.getAsString("map").map(Base64.getDecoder()::decode).orElse(null);
    }
    @Override public system.json.builder.object write() {
        return system.json.object().add("map", map == null ? null : Base64.getEncoder().encodeToString(map));
    }

    @Override public byte[] preMap() { return map; }

    @Override public void onPostTick(Collection<Player> tickable, Collection<Player> viewers) {
        if (this.map == null) {
            Location location = metadata().location(0.5, 1, 0.5);
            for (Item drop : location.getNearbyEntitiesByType(Item.class, 0.5)) {
                if (drop == null) continue;
                ItemStack item = drop.getItemStack();
                byte[] map;
                switch (item.getType()) {
                    case MAP:
                        map = new byte[128*128];
                        break;
                    case FILLED_MAP:
                        if (item.getItemMeta() instanceof MapMeta meta) {
                            if (!meta.hasMapView()) continue;
                            MapView view = meta.getMapView();
                            if (view == null) continue;
                            if (view.isLocked()) continue;
                            map = readMap(view.getId());
                            break;
                        }
                        continue;
                    default: continue;
                }
                item.subtract(1);
                if (item.getAmount() <= 0) PacketManager.getEntityHandle(drop).kill();
                this.map = map;
                saveData();
                viewers.forEach(player -> DrawMap.sendMap(player, MapID, map));
                return;
            }
        }
        viewers.forEach(viewer -> {
            if (tickable.contains(viewer)) return;
            DrawMap.sendMap(viewer, MapID, map);
        });
    }
    @Override public void onMouseTick(Player player, ViewPosition position) {
        if (map == null) return;
        CartographyBrush.getData(player.getUniqueId()).ifPresentOrElse(brush -> {
            DrawMap draw = DrawMap.of(this.map);
            int part = (int)(Math.pow(2, brush.size));
            int x = part <= 0 ? 0 : (part * (int)Math.round((position.getDoubleX() * SIZE / part) - 0.5));
            int y = part <= 0 ? 0 : (part * (int)Math.round((position.getDoubleY() * SIZE / part) - 0.5));
            draw.rectangle(x, y, part, part, null);
            if (position.getClick().isClick) {
                DrawMap _draw = DrawMap.of(this.map);
                Color brush_color = DrawMap.to(brush.color);
                system.Toast3<Integer, Integer, Integer> delta = system.toast(0,0,0);
                _draw.rectangleFunc(x, y, part, part, color -> {
                    Color _color = DrawMap.to(color);
                    delta.val0 += Math.abs(_color.getRed() - brush_color.getRed());
                    delta.val1 += Math.abs(_color.getGreen() - brush_color.getGreen());
                    delta.val2 += Math.abs(_color.getBlue() - brush_color.getBlue());
                });
                delta.val0 /= 2;
                delta.val1 /= 2;
                delta.val2 /= 2;
                system.Toast3<Integer, Integer, Integer> bucket = system.toast(0,0,0);
                CartographyBucket.modifyData(player.getInventory().getItemInOffHand(), data -> {
                    bucket.val0 = delta.val0;
                    bucket.val1 = delta.val1;
                    bucket.val2 = delta.val2;
                    if (delta.val0 > data.r || delta.val1 > data.g || delta.val2 > data.b) return false;
                    data.r -= delta.val0;
                    data.g -= delta.val1;
                    data.b -= delta.val2;
                    return true;
                }).ifPresentOrElse(state -> {
                    if (state) {
                        _draw.rectangle(x, y, part, part, brush.color);
                        this.map = _draw.save();
                        saveData();
                    } else {
                        LangMessages.Message.Brush_Bucket_Empty.sendMessage(player, Apply.of()
                                .add("total_r", String.valueOf(delta.val0))
                                .add("total_g", String.valueOf(delta.val1))
                                .add("total_b", String.valueOf(delta.val2))

                                .add("bucket_r", String.valueOf(bucket.val0))
                                .add("bucket_g", String.valueOf(bucket.val1))
                                .add("bucket_b", String.valueOf(bucket.val2))
                        );
                    }
                }, () -> LangMessages.Message.Brush_Bucket_NotFound.sendMessage(player));
            }
            DrawMap.sendMap(player, MapID, draw.save());
        }, () -> {
            if (position.getClick().isShift) {
                if (this.map == null) return;
                Items.dropGiveItem(player, createMap(this.map), true);
                this.map = null;
                saveData();
            } else {
                DrawMap.sendMap(player, MapID, map);
            }
        });
    }
    @Override public void onMouseStart(Player player, ViewPosition position) {}
    @Override public void onMouseEnd(Player player) {
        if (map == null) return;
        DrawMap.sendMap(player, MapID, map);
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        event.player().openMenu(getInventory(metadata.skull.getLevel(), metadata.skull.getBlockPos()));
        event.player().awardStat(StatisticList.INTERACT_WITH_CARTOGRAPHY_TABLE);
        return EnumInteractionResult.CONSUME;
    }
    @Override public VoxelShape onShape(CustomTileMetadata metadata, BlockSkullShapeInfo event) {
        return VoxelShapes.block();
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        if (map != null) event.addItem(createMap(map));
    }
}





























