package org.lime.gp.player.module;

import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.lime.system;
import org.lime.display.DisplayManager;
import org.lime.display.Displays;
import org.lime.display.Models;
import org.lime.display.ObjectDisplay;
import org.lime.display.Models.Model;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.BackPackSetting;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;

public class BackPack {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(BackPack.class)
                .withInit(BackPack::init);
    }
    
    public static void init() {
        Displays.initDisplay(new BackPackManager());
    }

    private static class BackPackDisplay extends ObjectDisplay<Model, Marker> {
        @Override public double getDistance() { return 15; }
        @Override public Location location() { return player.getLocation(); }

        private final Player player;

        public Model data;
        public Models.Model.ChildDisplay<Model> model;
    
        protected BackPackDisplay(Player player, Model data) {
            super(player.getLocation());
            this.player = player;
            this.data = data;
            this.model = preInitDisplay(data.display(this));
            postInit();
            Displays.addPassengerID(entityID, this.model.entityID);
        }
        @Override public void update(Model data, double delta) {
            if (this.data != data) {
                Displays.addPassengerID(entityID, this.model.entityID);
                Displays.removePassengerID(this.model.entityID);
                this.data = data;
            }
            super.update(data, delta);
            this.invokeAll(this::sendData);
        }
    
        @Override protected Marker createEntity(Location location) {
            return new Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
        }
    }
    private static class BackPackManager extends DisplayManager<Player, Model, BackPackDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public Map<Player, Model> getData() {
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
        @Override public BackPackDisplay create(Player player, Model data) { return new BackPackDisplay(player, data); }
    }
}
