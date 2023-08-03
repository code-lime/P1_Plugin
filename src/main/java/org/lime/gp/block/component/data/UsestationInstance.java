package org.lime.gp.block.component.data;

import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.CustomTileMetadata.Tickable;
import org.lime.gp.block.component.list.UsestationComponent;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.settings.list.NextSetting;
import org.lime.gp.module.JavaScript;
import org.lime.gp.module.TimeoutData;
import org.lime.json.JsonObjectOptional;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;

import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;
import org.lime.Position;
import org.lime.system;

public class UsestationInstance extends BlockComponentInstance<UsestationComponent> implements Tickable {
    public final int distance;
    public static org.lime.core.element create() {
        return org.lime.core.element.create(UsestationInstance.class)
                .withInit(UsestationInstance::init);
    }

    public static class UsestationJS extends org.lime.JavaScript.InstanceJS {
        public boolean inZone(String uuid, String block) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if (player == null) return false;
            return TimeoutData.allValues(StationTimeout.class)
                .filter(v -> v.blockKey.equals(block))
                .anyMatch(v -> v.position.getLocation().distance(player.getLocation()) < v.distance);
        }
        public boolean hasUse(String uuid, String check) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if (player == null) return false;
            Checker checker = Checker.createCheck(check);
            PlayerInventory inventory = player.getInventory();
            return Stream.of(inventory.getItem(EquipmentSlot.HAND), inventory.getItem(EquipmentSlot.OFF_HAND))
                .filter(checker::check)
                .findFirst()
                .map(item -> true)
                .orElse(false);
        }
        public boolean use(String uuid, String check) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if (player == null) return false;
            Checker checker = Checker.createCheck(check);
            PlayerInventory inventory = player.getInventory();
            return Stream.of(inventory.getItem(EquipmentSlot.HAND), inventory.getItem(EquipmentSlot.OFF_HAND))
                .filter(checker::check)
                .findFirst()
                .map(item -> {
                    Items.getOptional(NextSetting.class, item)
                        .flatMap(v -> Items.createItem(v.next()))
                        .ifPresent(v -> Items.dropGiveItem(player, v, false));
                    item.subtract(1);
                    return true;
                })
                .orElse(false);
        }
    }
    
    public static void init() {
        JavaScript.js.instances.put("usestation", new UsestationJS());
        JavaScript.js.reinstance(false);
    }

    public UsestationInstance(UsestationComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        distance = component.distance;
    }

    private static class StationTimeout extends TimeoutData.ITimeout {
        public String blockKey;
        public Position position;
        public int distance;

        public StationTimeout(String blockKey, int x, int y, int z, World world, int distance) {
            this.blockKey = blockKey;
            this.position = new Position(world, x, y, z);
            this.distance = distance;
        }
    }
    

    @Override public void read(JsonObjectOptional json) { }
    @Override public system.json.builder.object write() { return system.json.object(); }

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        BlockPosition position = event.getPos();
        TimeoutData.put(
            metadata.key.uuid(),
            StationTimeout.class,
            new StationTimeout(
                metadata.key.type(),
                position.getX(),
                position.getY(),
                position.getZ(),
                event.getWorld().getWorld(),
                this.distance
            )
        );
    }
}
