package org.lime.gp.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.extension.JManager;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.HorseArmorSetting;
import org.lime.gp.lime;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.*;

public class HorseRiders implements Listener {
    public static CoreElement create() {
        return CoreElement.create(HorseRiders.class)
                .withInstance()
                .withInit(HorseRiders::init);

    }

    public enum HorseState {
        Driver,
        Passenger
    }
    public enum TargetType {
        Horse("horse-4to-to/horse"),
        Stand("horse-4to-to/stand");
        public final String key;
        TargetType(String key) {
            this.key = key;
        }
    }

    public static void setEntityTargetData(Entity entity, TargetType type, Entity target) {
        JManager.set(entity.getPersistentDataContainer(), type.key, new JsonPrimitive(target.getUniqueId().toString()));
    }
    public static Entity getEntityTargetData(Entity entity, TargetType type) {
        JsonPrimitive json = JManager.get(JsonPrimitive.class, entity.getPersistentDataContainer(), type.key, null);
        return json == null ? null : Bukkit.getEntity(UUID.fromString(json.getAsString()));
    }
    public static boolean hasEntityTargetData(Entity entity, TargetType type) {
        return JManager.has(entity.getPersistentDataContainer(), type.key);
    }
    public static void removeEntityTargetData(Entity entity, TargetType type) {
        JManager.del(entity.getPersistentDataContainer(), type.key);
    }
    public static boolean checkCanUse(Player player, Entity vehicle, HorseState state) {
        switch (state) {
            case Driver:
                return isCanInteractEntity(player.getUniqueId(), vehicle);
            case Passenger:
                if (vehicle instanceof Horse horse) {
                    ItemStack armor = horse.getInventory().getArmor();
                    if (armor == null || armor.getType().isAir()) return false;
                }
                return true;
        }
        return false;
    }
    public static void init() {
        AnyEvent.addEvent("animal.owner.set", AnyEvent.type.other, builder -> builder.createParam(UUID::fromString, "[animal_uuid]").createParam(Integer::parseInt, "[owner_id]"), (player, animal_uuid, owner_id) -> {
            Entity entity = Bukkit.getEntity(animal_uuid);
            if (!(entity instanceof Tameable tameable)) return;
            if (!player.getUniqueId().equals(tameable.getOwnerUniqueId())) return;
            UserRow.getBy(owner_id)
                    .ifPresent(user -> {
                        UUID uuid = user.uuid;
                        tameable.setOwner(new AnimalTamer() {
                            @Override public String getName() { return null; }
                            @Override public UUID getUniqueId() { return uuid; }
                        });
                        JManager.del(tameable.getPersistentDataContainer(), "sub_owners");
                    });
        });
        AnyEvent.addEvent("animal.sub", AnyEvent.type.other, builder -> builder.createParam("add","del").createParam(UUID::fromString, "[animal_uuid]").createParam(Integer::parseInt, "[sub_id]"), (player, state, animal_uuid, sub_id) -> {
            Entity entity = Bukkit.getEntity(animal_uuid);
            if (!(entity instanceof Tameable tameable)) return;
            if (!player.getUniqueId().equals(tameable.getOwnerUniqueId())) return;
            UserRow.getBy(sub_id).ifPresent(sub -> {
                UUID sub_uuid = sub.uuid;
                Set<UUID> subs = new HashSet<>();
                JManager.get(JsonArray.class, tameable.getPersistentDataContainer(), "sub_owners", new JsonArray())
                        .forEach(v -> subs.add(UUID.fromString(v.getAsString())));
                switch (state) {
                    case "add": subs.add(sub_uuid); break;
                    case "del": subs.remove(sub_uuid); break;
                    default: return;
                }
                JManager.set(tameable.getPersistentDataContainer(), "sub_owners", json.array().add(subs, UUID::toString).build());
            });
        });
        lime.repeat(() -> Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(Horse.class).forEach(horse -> {
            ItemStack armor = horse.getInventory().getArmor();
            if (armor == null) return;
            if (!armor.getItemMeta().hasCustomModelData()) return;
            if (Items.getOptional(HorseArmorSetting.class, armor).map(v -> v.isArmor).orElse(false)) return;
            horse.getInventory().setArmor(null);
            Items.dropItem(horse.getLocation(), armor);
        })), 0.1);
        lime.repeat(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Entity vehicle = player.getVehicle();
                if (vehicle == null) continue;
                if (vehicle.getType() != EntityType.ARMOR_STAND) continue;
                Entity horse = getHorseByStand(vehicle);
                if (horse == null) continue;
                if (!checkCanUse(player, horse, HorseState.Passenger)) {
                    removeEntityTargetData(vehicle, TargetType.Stand);
                    vehicle.remove();
                }
            }
        }, 0.5);
        Bukkit.getScheduler().runTaskTimer(lime._plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Entity vehicle = player.getVehicle();
                if (vehicle == null) continue;
                if (vehicle.getType() != EntityType.ARMOR_STAND) continue;
                Entity horse = getHorseByStand(vehicle);
                if (horse == null) continue;
                Location standLocation = getStandLocation(horse);
                vehicle.teleport(standLocation);
                Location horseLocation = horse.getLocation();
                vehicle.setRotation(horseLocation.getYaw(), horseLocation.getPitch());
            }
        }, 0L, 1L);
    }

    public static boolean isCanInteractEntity(UUID uuid, Entity entity) {
        return UserRow.getBy(uuid)
                .map(row -> {
                    if (row.role == 9) return true;
                    if (!(entity instanceof Tameable tameable) || !tameable.isTamed()) return true;
                    if (uuid.equals(tameable.getOwnerUniqueId())) return true;
                    JsonArray sub_owners = JManager.get(JsonArray.class, tameable.getPersistentDataContainer(), "sub_owners", new JsonArray());
                    return sub_owners.contains(new JsonPrimitive(uuid.toString()));
                })
                .orElse(false);
    }
    private static boolean tryPassanger(PlayerInteractEntityEvent event, Player player, Entity target) {
        if (!isRidable(target.getType())) return false;
        if (target.getPassengers().isEmpty()) {
            if (!checkCanUse(player, target, HorseState.Driver)) event.setCancelled(true);
            return false;
        }
        if (hasEntityTargetData(target, TargetType.Stand)) return false;
        if (player.getInventory().getItemInMainHand().getType() == Material.LEAD) return false;
        if (player.getInventory().getItemInOffHand().getType() == Material.LEAD) return false;
        if (player.getGameMode() == GameMode.SPECTATOR) return false;
        if (player.isSneaking()) return false;
        if (player.getVehicle() != null) return false;
        if (!checkCanUse(player, target, HorseState.Passenger)) return false;
        ArmorStand armorStand = createArmorStand(getStandLocation(target));
        Location targetLocation = target.getLocation();
        armorStand.setRotation(targetLocation.getYaw(), targetLocation.getPitch());
        setEntityTargetData(target, TargetType.Stand, armorStand);
        setEntityTargetData(armorStand, TargetType.Horse, target);
        armorStand.addPassenger(player);
        return true;
    }
    @EventHandler(priority = EventPriority.LOWEST) private static void on(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) return;
        Entity target = event.getRightClicked();
        Player player = event.getPlayer();
        if (tryPassanger(event, player, target)) return;
        if (!(target instanceof Tameable tameable)) return;
        if (checkCanUse(player, tameable, HorseState.Driver)) return;
        event.setCancelled(true);
    }
    private static void setPosition(Entity entity, Location location) {
        ((CraftEntity)entity).getHandle().moveTo(location.getX(), location.getY(), location.getZ());
    }
    @EventHandler private static void on(PlayerMoveEvent event) {
        Entity vehicle = event.getPlayer().getVehicle();
        if (vehicle == null) return;
        if (!isRidable(vehicle.getType())) return;
        Entity stand = getStandByHorse(vehicle);
        if (stand == null) return;
        Location standLocation = getStandLocation(vehicle);
        setPosition(stand, standLocation);
        Location vehicleLocation = vehicle.getLocation();
        stand.setRotation(vehicleLocation.getYaw(), vehicleLocation.getPitch());
    }

    @EventHandler private static void on(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        /*if (ExtMethods.damagerPlayer(event).map(v -> !isCanInteractEntity(v.getUniqueId(), victim)).orElse(false)) {
            event.setCancelled(true);
            return;
        }*/
        if (victim.getType() == EntityType.PLAYER) {
            Entity damagerVehicle = event.getDamager().getVehicle();
            if (damagerVehicle == null) return;
            if (!isRidable(damagerVehicle.getType())) return;
            Entity standPassenger = getStandPassenger(damagerVehicle);
            if (standPassenger == null) return;
            if (standPassenger.equals(victim)) event.setCancelled(true);
        }
        if (isRidable(victim.getType())) {
            Entity standPassenger = getStandPassenger(victim);
            if (standPassenger == null) return;
            if (standPassenger.equals(event.getDamager())) event.setCancelled(true);
        }
    }

    @EventHandler private static void on(PlayerQuitEvent event) {
        Entity vehicle = event.getPlayer().getVehicle();
        if (vehicle == null) return;
        if (isRidable(vehicle.getType())) {
            Entity armorStand = getStandByHorse(vehicle);
            if (armorStand == null) return;
            Entity standPassenger = getStandPassenger(vehicle);
            if (standPassenger == null) return;
            if (standPassenger instanceof Player player) {
                if (checkCanUse(player, vehicle, HorseState.Driver)) vehicle.addPassenger(player);
            } else vehicle.addPassenger(standPassenger);
            removeEntityTargetData(vehicle, TargetType.Stand);
            armorStand.remove();
        }
        if (vehicle.getType() == EntityType.ARMOR_STAND) {
            Entity horse = getHorseByStand(vehicle);
            if (horse == null) return;
            removeEntityTargetData(horse, TargetType.Stand);
            vehicle.remove();
        }
    }
    @EventHandler private static void on(EntityDismountEvent e) {
        Entity from = e.getDismounted();
        if (isRidable(from.getType())) {
            Entity armorStand = getStandByHorse(from);
            if (armorStand == null) return;
            Entity standPassenger = getStandPassenger(from);
            if (standPassenger == null) return;

            if (standPassenger instanceof Player) {
                if (checkCanUse((Player)standPassenger, from, HorseState.Driver))
                    from.addPassenger(standPassenger);
            } else {
                from.addPassenger(standPassenger);
            }

            removeEntityTargetData(from, TargetType.Stand);
            armorStand.remove();
        }
        if (from.getType() == EntityType.ARMOR_STAND) {
            Entity horse = getHorseByStand(from);
            if (horse == null) return;
            removeEntityTargetData(horse, TargetType.Stand);
            from.remove();
        }
    }
    private static Entity getStandByHorse(Entity vehicle) {
        return getEntityTargetData(vehicle, TargetType.Stand);
    }
    private static Entity getHorseByStand(Entity vehicle) {
        return getEntityTargetData(vehicle, TargetType.Horse);
    }
    private static Location getStandLocation(Entity vehicle) {
        Location vehicleLocation = vehicle.getLocation().clone();
        EntityType vehicleType = vehicle.getType();
        return switch (vehicleType) {
            case HORSE, SKELETON_HORSE -> vehicleLocation
                    .add(vehicleLocation.getDirection().normalize().setY(0).multiply(-0.55D))
                    .add(0.0D, 1.25D, 0.0D);
            case LLAMA, TRADER_LLAMA -> vehicleLocation
                    .add(vehicleLocation.getDirection().normalize().setY(0).multiply(-0.65D))
                    .add(0.0D, 1.25D, 0.0D);
            case MULE -> vehicleLocation
                    .add(vehicleLocation.getDirection().normalize().setY(0).multiply(-0.4D))
                    .add(0.0D, 1.0D, 0.0D);
            case DONKEY -> vehicleLocation
                    .add(vehicleLocation.getDirection().normalize().setY(0).multiply(-0.35D))
                    .add(0.0D, 0.8D, 0.0D);
            case PIG -> vehicleLocation
                    .add(vehicleLocation.getDirection().normalize().setY(0).multiply(-0.35D))
                    .add(0.0D, 0.65D, 0.0D);
            default -> vehicleLocation;
        };
    }
    private static ArmorStand createArmorStand(Location location) {
        World world = location.getWorld();
        if (world == null) throw new NullPointerException();
        return world.spawn(location, ArmorStand.class, CreatureSpawnEvent.SpawnReason.CUSTOM, stand -> {
            stand.setMarker(true);
            stand.setVisible(false);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setInvulnerable(true);
        });
    }
    private static Entity getStandPassenger(Entity vehicle) {
        //if (standMetadata.isEmpty()) return null;
        Entity armorStand = getStandByHorse(vehicle);
        if (armorStand == null) return null;
        List<Entity> standPassengers = armorStand.getPassengers();
        if (standPassengers.isEmpty()) return null;
        return standPassengers.get(0);
    }
    private static boolean isRidable(EntityType entityType) {
        return switch (entityType) {
            case HORSE -> true;
            default -> false;
        };
    }
}

































