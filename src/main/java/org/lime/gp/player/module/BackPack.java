package org.lime.gp.player.module;

import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.lime.display.models.ChildDisplay;
import org.lime.display.models.Model;
import org.lime.system;
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
    public static org.lime.core.element create() {
        return org.lime.core.element.create(BackPack.class)
                .withInit(BackPack::init);
    }
    private static final BackPackManager manager = new BackPackManager();
    private static final Model model = lime.models.builder(EntityTypes.ARMOR_STAND)
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
        .addEquipment(EnumItemSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY)
        .build();
    public static void init() {
        Displays.initDisplay(manager);
    }

    private static class BackPackDisplay extends ObjectDisplay<ItemStack, Marker> {
        @Override public double getDistance() { return 15; }
        @Override public Location location() { return player.getLocation(); }

        private final Player player;

        public ItemStack data;
        public ChildDisplay<ItemStack> model;
    
        protected BackPackDisplay(Player player, ItemStack data) {
            super(player.getLocation());
            this.player = player;
            this.data = data;
            this.model = preInitDisplay(BackPack.model.display(this));
            this.model.setEquipment(EnumItemSlot.HEAD, data);
            postInit();
            Displays.addPassengerID(player.getEntityId(), this.model.entityID);
        }
        @Override public void update(ItemStack data, double delta) {
            if (this.data != data) this.model.setEquipment(EnumItemSlot.HEAD, this.data = data);
            super.update(data, delta);
            this.invokeAll(this::sendData);
        }
    
        @Override protected Marker createEntity(Location location) {
            return new Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
        }
    }
    private static class BackPackManager extends DisplayManager<Player, ItemStack, BackPackDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public Map<Player, ItemStack> getData() {
            return Bukkit.getOnlinePlayers()
                .stream()
                .flatMap(player -> {
                    PlayerInventory inventory = player.getInventory();
                    return Items.getOptional(BackPackSetting.class, inventory.getChestplate())
                        .map(v -> v.data.get(BackPackSetting.PoseType.getPose(player)))
                        .map(v -> system.toast(player, v))
                        .stream();
                })
                .collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1));
        }
        @Override public BackPackDisplay create(Player player, ItemStack data) { return new BackPackDisplay(player, data); }
    }
}
