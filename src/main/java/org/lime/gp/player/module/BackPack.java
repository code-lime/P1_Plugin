package org.lime.gp.player.module;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.lime.display.Passenger;
import org.lime.display.models.display.ChildEntityDisplay;
import org.lime.display.models.display.IAnimationData;
import org.lime.display.models.shadow.Builder;
import org.lime.display.models.shadow.EntityBuilder;
import org.lime.plugin.CoreElement;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.display.DisplayManager;
import org.lime.display.Displays;
import org.lime.display.ObjectDisplay;
import org.lime.gp.lime;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.BackPackSetting;

import net.minecraft.core.Vector3f;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.item.ItemStack;

public class BackPack {
    public static CoreElement create() {
        return CoreElement.create(BackPack.class)
                .withInit(BackPack::init);
    }
    private static final BackPackManager manager = new BackPackManager();
    private static final EntityBuilder model = lime.models.builder().entity()
            .entity(EntityTypes.ARMOR_STAND)
            .nbt(() -> {
                EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, lime.MainWorld.getHandle());
                stand.setNoBasePlate(true);
                stand.setSmall(true);
                stand.setInvisible(true);
                stand.setInvulnerable(true);
                stand.setMarker(true);
                stand.setHeadPose(new Vector3f(0, 0, 0));
                return stand;
            })
            .addEquipment(EnumItemSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
    public static void init() {
        Displays.initDisplay(manager);
    }

    public record ItemInfo(ItemStack item) implements IAnimationData {
        @Override public Map<String, Object> data() { return Collections.emptyMap(); }
    }

    private static class BackPackDisplay extends ObjectDisplay<ItemInfo, Marker> {
        @Override public double getDistance() { return 15; }
        @Override public Location location() { return player.getLocation(); }

        private final Player player;

        public ItemInfo data;
        public ChildEntityDisplay<ItemInfo> model;
    
        protected BackPackDisplay(Player player, ItemInfo data) {
            super(player.getLocation());
            this.player = player;
            this.data = data;
            this.model = preInitDisplay(BackPack.model.display(this));
            this.model.setEquipment(EnumItemSlot.HEAD, data.item());
            postInit();
            this.model.addCustomPassengerID(player.getEntityId());
        }
        @Override public void update(ItemInfo data, double delta) {
            if (this.data != data) this.model.setEquipment(EnumItemSlot.HEAD, (this.data = data).item());
            super.update(data, delta);
            this.invokeAll(this::sendData);
        }
    
        @Override protected Marker createEntity(Location location) {
            return new Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
        }
    }
    private static class BackPackManager extends DisplayManager<Player, ItemInfo, BackPackDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public Map<Player, ItemInfo> getData() {
            return Bukkit.getOnlinePlayers()
                .stream()
                .flatMap(player -> {
                    PlayerInventory inventory = player.getInventory();
                    return Items.getOptional(BackPackSetting.class, inventory.getChestplate())
                        .map(v -> v.data.get(BackPackSetting.PoseType.getPose(player)))
                        .map(v -> Toast.of(player, v))
                        .stream();
                })
                .collect(Collectors.toMap(kv -> kv.val0, kv -> new ItemInfo(kv.val1)));
        }
        @Override public BackPackDisplay create(Player player, ItemInfo data) { return new BackPackDisplay(player, data); }
    }
}
