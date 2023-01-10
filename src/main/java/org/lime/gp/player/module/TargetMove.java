package org.lime.gp.player.module;

import dev.geco.gsit.GSitMain;
import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.api.event.PrePlayerGetUpSitEvent;
import dev.geco.gsit.api.event.PrePlayerPoseEvent;
import dev.geco.gsit.api.event.PrePlayerSitEvent;
import dev.geco.gsit.objects.GSeat;
import dev.geco.gsit.objects.GetUpReason;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.display.Displays;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.system;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

public class TargetMove implements Listener {
    public static core.element create() {
        return core.element.create(TargetMove.class)
                .withInstance()
                .withInit(TargetMove::init);
    }
    public static final HashMap<UUID, system.Toast2<UUID, Integer>> targets = new HashMap<>();
    public static void init() {
        AnyEvent.addEvent("target.move", AnyEvent.type.other, v -> v.createParam(UUID::fromString, "[uuid]"), (player, target_uuid) -> {
            Player target = Bukkit.getPlayer(target_uuid);
            if (target == null) return;
            UUID playerUUID = player.getUniqueId();
            UUID targetUUID = target.getUniqueId();
            if (playerUUID.equals(targetUUID)) return;
            if (player.getWorld() != target.getWorld() || player.getLocation().distance(target.getLocation()) > 5) return;
            Optional.ofNullable(GSitAPI.getPose(target))
                    .filter(v -> v.getPose() == Pose.SLEEPING)
                    .ifPresent(pose -> {
                        Location targetLocation = targetOf(player, target, true).val0;
                        if (targetLocation == null) return;
                        GSitAPI.removePose(pose, GetUpReason.PLUGIN);
                        targets.put(playerUUID, system.toast(targetUUID, 10));
                        isSync = true;
                        try {
                            GSitAPI.createSeat(targetLocation.getBlock(), target, false, targetLocation.getX() % 1 - 0.5, 0, targetLocation.getZ() % 1 - 0.5, targetLocation.getYaw(), true, true);
                        } finally {
                            isSync = false;
                        }
                        if (updateSingle(playerUUID, targetUUID, true)) targets.remove(playerUUID);
                    });
        });
        lime.repeatTicks(TargetMove::update, 1);
    }
    public static final PotionEffect SLOW = PotionEffectType.SLOW.createEffect(5, 3).withAmbient(false).withIcon(false).withParticles(false);
    public static final PotionEffect ROTATE_SLOW = PotionEffectType.SLOW.createEffect(5, 4).withAmbient(false).withIcon(false).withParticles(false);

    public static void update() {
        targets.entrySet().removeIf(kv -> {
            system.Toast2<UUID, Integer> target = kv.getValue();
            boolean init = target.val1 > 0;
            if (init) target.val1--;
            return updateSingle(kv.getKey(), target.val0, init);
        });
    }
    public static boolean updateSingle(UUID playerUUID, UUID targetUUID, boolean init) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || player.isInsideVehicle() || Displays.hasVehicle(player.getEntityId())) {
            Optional.ofNullable(Bukkit.getPlayer(targetUUID))
                    .map(GSitAPI::getSeat)
                    .ifPresent(v -> GSitAPI.removeSeat(v, GetUpReason.PLUGIN));
            return true;
        }
        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null) return true;
        Drugs.lockArmsTick(target);
        GSeat seat = GSitAPI.getSeat(target);
        if (seat == null) return true;
        system.Toast1<Boolean> remove = system.toast(false);
        targetOf(player, target, init).invoke((target_move, isSee) -> {
            if (target_move == null) {
                remove.val0 = true;
                return;
            }
            moveSeat(seat, target_move);
            player.addPotionEffect(isSee ? SLOW : ROTATE_SLOW);
        });
        return remove.val0;
    }
    public static system.Toast2<Location, Boolean> targetOf(Player player, Player target, boolean init) {
        Location location = player.getLocation();
        if (location.getWorld() != target.getWorld()) return system.toast(null, false);
        Location target_location = target.getLocation();
        if (location.distanceSquared(target_location) > 9) return system.toast(null, false);
        Vector delta = target_location.toVector().setY(0).subtract(location.toVector().setY(0));
        double length = delta.length();
        if (!init && length > 1.5) return system.toast(null, false);
        delta.normalize();
        location.setPitch(0);
        double dot = delta.dot(location.getDirection());
        location.setDirection(delta);
        return system.toast(location.add(delta).add(0, -0.5, 0), dot > 0.5D);
    }
    public static boolean isTarget(UUID target) {
        return targets.values().stream().map(v -> v.val0).anyMatch(target::equals);
    }
    public static void unTarget(UUID target) {
        targets.values().removeIf(v -> target.equals(v.val0));
    }
    public static double getHeight(Block block) {
        return IntStream.range(-1, 2)
                .mapToObj(y -> block.getRelative(0, y, 0))
                .map(Block::getBoundingBox)
                .filter(v -> v.getHeight() > 0)
                .mapToDouble(BoundingBox::getMaxY)
                .max()
                .orElseGet(block::getY);
    }
    public static void moveSeat(final GSeat seat, final Location location) {
        lime.nextTick(() -> {
            GSitMain GPM = GSitMain.getInstance();

            Block block = location.getBlock();

            //double o = block.getBoundingBox().getMinY() + block.getBoundingBox().getHeight();
            //o = (o == 0.0D ? 1.0D : o - (double)block.getY()) + GPM.getCManager().S_SITMATERIALS.getOrDefault(block.getType(), 0.0D);
            //Location l = new Location(location.getWorld(), location.getX(), block.getY() + o - 0.2D, location.getZ());
            Location l = new Location(location.getWorld(), location.getX(), getHeight(block), location.getZ());

            GPM.getSitUtil().removeSeatBlock(seat.getBlock(), seat);
            seat.setBlock(block);
            seat.setLocation(l);
            seat.setReturn(l);
            GPM.getSitUtil().setSeatBlock(seat.getBlock(), seat);
            seat.getEntity().setRotation(location.getYaw(), location.getPitch());
            GPM.getPlayerUtil().pos(seat.getEntity(), seat.getLocation());
        });
    }

    private static boolean isSync = false;
    @EventHandler public static void on(PrePlayerGetUpSitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (isTarget(uuid) && Death.isDamageLay(uuid)) e.setCancelled(true);
    }
    @EventHandler public static void on(PrePlayerSitEvent e) {
        if (isSync) return;
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (isTarget(uuid) && Death.isDamageLay(uuid)) e.setCancelled(true);
    }
    @EventHandler public static void on(PrePlayerPoseEvent e) {
        if (isSync) return;
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (isTarget(uuid) && Death.isDamageLay(uuid)) e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW) public static void on(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        system.Toast2<UUID, Integer> target = targets.get(player.getUniqueId());
        if (target == null) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!player.isSneaking()) return;
        if (!(e.getRightClicked() instanceof Player other)) return;
        if (!other.getUniqueId().equals(target.val0)) return;
        unTarget(target.val0);
        e.setCancelled(true);
    }
}















