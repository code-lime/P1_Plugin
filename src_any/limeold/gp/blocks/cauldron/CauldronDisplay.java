package org.lime.gp.block.component.data.cauldron;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Vector3f;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.lime.gp.display.DisplayManager;
import org.lime.gp.display.ObjectDisplay;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.module.TimeoutData;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CauldronDisplay extends ObjectDisplay<CauldronDisplay.CauldronData, EntityArmorStand> {
    public static final class CauldronData extends TimeoutData.ITimeout {
        private final Location location;
        private final ItemStack head;

        private CauldronData(Location location, ItemStack head) {
            super(5);
            this.location = location;
            this.head = head;
        }
        public static Optional<CauldronData> of(Location location, int level, Color color) {
            if (color == null) return Optional.empty();
            int cmd;
            switch (level) {
                default: return Optional.empty();
                case 1: cmd = 8080001; break;
                case 2: cmd = 8080002; break;
                case 3: cmd = 8080003; break;
            }
            ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
            LeatherArmorMeta meta = (LeatherArmorMeta)item.getItemMeta();
            meta.setColor(color);
            meta.setCustomModelData(cmd);
            item.setItemMeta(meta);
            return Optional.of(new CauldronData(location, item));
        }

        public ItemStack head() { return head; }
        public Location location() { return location; }
    }
    @Override public double getDistance() { return 10; }

    private CauldronData data;
    private static final ItemStack AIR = new ItemStack(Material.AIR);
    private ItemStack head = AIR;

    protected CauldronDisplay(CauldronData data) {
        super(data.location());
        this.data = data;
        postInit();
    }

    @Override public boolean isFilter(Player player) { return !head.getType().isAir(); }
    @Override protected void sendData(Player player, boolean child) {
        PacketPlayOutEntityTeleport ppoet = new PacketPlayOutEntityTeleport(entity);
        PacketPlayOutEntityEquipment ppoee = new PacketPlayOutEntityEquipment(entityID, Collections.singletonList(new Pair<>(EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(head))));
        PacketManager.sendPackets(player, ppoet, ppoee);//, ppoehr, ppoel);
        super.sendData(player, child);
    }
    @Override protected EntityArmorStand createEntity(Location location) {
        EntityArmorStand stand = new EntityArmorStand(
                ((CraftWorld)location.getWorld()).getHandle(),
                location.getX(), location.getY(), location.getZ());
        stand.setNoBasePlate(true);
        stand.setSmall(true);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.setHeadPose(new Vector3f(0, 0, 0));
        stand.setYRot(0);
        stand.setXRot(0);
        return stand;
    }
    public boolean setHead(ItemStack head) {
        head = head == null ? AIR : head;
        if (this.head.isSimilar(head)) return false;
        this.head = head;
        if (!this.head.getType().isAir()) return true;
        this.hideAll();
        return false;
    }
    public boolean updateHead() { return setHead(data.head()); }

    @Override public void update(CauldronData data, double delta) {
        this.data = data;
        super.update(data, delta);
        if (updateHead()) invokeAll(this::sendData);
    }

    private static class CauldronManager extends DisplayManager<UUID, CauldronData, CauldronDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }

        @Override public Map<UUID, CauldronData> getData() { return TimeoutData.map(CauldronData.class); }
        @Override public CauldronDisplay create(UUID key, CauldronData data) { return new CauldronDisplay(data); }
    }
    public static DisplayManager<?, ?, ?> manager() { return new CauldronManager(); }
}















