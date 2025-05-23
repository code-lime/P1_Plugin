package org.lime.gp.module;

import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.api.event.PlayerGetUpPoseEvent;
import io.papermc.paper.event.block.BellRevealRaiderEvent;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.Administrator;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.rows.HouseRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.gp.module.damage.EntityDamageByPlayerEvent;
import org.lime.gp.player.voice.Voice;
import org.lime.system.json;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.MathUtils;
import org.lime.system.utils.RandomUtils;

import java.util.*;

public class SingleModules implements Listener {
    public static CoreElement create() {
        return CoreElement.create(SingleModules.class)
                .withInit(SingleModules::init)
                .withInstance()
                .addEmpty("execute_file", () -> {
                    if (!lime.existConfig("execute")) return;
                    json.parse(lime.readAllConfig("execute")).getAsJsonArray().forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.getAsString()));
                });
    }
    private static class MoveData {
        public final Position position;
        public int total = 0;
        public long removeTime = 0;

        private static final int TOTAL_COUNT = 20;
        private static final int WAIT_TIME = 30 * 60 * 1000;

        public MoveData(Position position) {
            this.position = position;
        }

        public void onMove() {
            total++;
            removeTime = System.currentTimeMillis() + WAIT_TIME;
        }
        public boolean trySet() {
            return total > TOTAL_COUNT;
        }
    }


    public static void init() {
        final HashMap<Material, Material> mapReplace = map.<Material, Material>of()
                .add(Material.GRASS_BLOCK, Material.DIRT_PATH)
                //.add(Material.ICE, Material.AIR)
                .build();

        //final HashMap<Position, MoveData> cacheMoveLocation = new HashMap<>();
        lime.repeat(() -> {
            //HashMap<Position, Material> nowLocation = new HashMap<>();
            Bukkit.getOnlinePlayers().forEach(player -> {
                Location location = player.getLocation();
                /*
                Block path = location.clone().add(0, -1, 0).getBlock();
                Material block_type = path.getType();
                if (mapReplace.containsKey(block_type))
                    nowLocation.put(Position.of(path.getLocation()), block_type);
                */

                if (!lime.isLay(player)) return;
                if (!beds.containsValue(player.getUniqueId())) return;
                if (location.getWorld() != lime.MainWorld) return;
                int maxHealth = 6;
                for (HouseRow house : Tables.HOUSE_TABLE.getRows()) {
                    if (house.inZone(location) && house.type != null) {
                        switch (house.type) {
                            case HOSPITAL:
                                maxHealth = Math.max(16, maxHealth);
                                break;
                            default:
                                break;
                        }
                    }
                }
                double health = player.getHealth();
                if (health < maxHealth) player.setHealth(Math.min(maxHealth, health + 0.5));
            });
            /*nowLocation.forEach((pos, material) -> {
                if (RandomUtils.rand_is(0.1))
                    cacheMoveLocation.computeIfAbsent(pos, MoveData::new).onMove();
            });*/
            /*cacheMoveLocation.entrySet().removeIf(kv -> {
                Block block = kv.getKey().getBlock();
                Material material = mapReplace.getOrDefault(block.getType(), null);
                if (material == null) return true;
                if (kv.getValue().trySet()) {
                    block.setType(material);
                    return true;
                }
                return false;
            });*/
        }, 5);
        lime.repeat(() -> Bukkit.getOnlinePlayers().forEach(player -> {
            player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        }), 0.5);
    }

    public static final HashMap<String, UUID> beds = new HashMap<>();

    public static boolean isInBed(UUID uuid) {
        return beds.containsValue(uuid);
    }

    @EventHandler public static void on(ProjectileHitEvent e) {
        Projectile projectile = e.getEntity();
        if (e.getEntity().getType() == EntityType.ARROW) {
            Block block = e.getHitBlock();
            if (block != null && block.getType() == Material.IRON_BARS) {
                Vector velocity = projectile.getVelocity();
                Location location = projectile.getLocation();
                lime.once(() -> {
                    projectile.teleport(location.add(velocity.clone().multiply(1.5)));
                    projectile.setVelocity(velocity.clone().multiply(2));
                }, 0.02);
            }
        }
    }
    @EventHandler public static void on(PlayerBedEnterEvent e) {
        e.setCancelled(true);
        Player player = e.getPlayer();
        if (lime.isLay(player)) return;
        Block bed = e.getBed();
        String bedLoc = MathUtils.getString(bed.getLocation());
        if (beds.containsKey(bedLoc)) return;
        float yaw = 0;
        switch (((Bed)bed.getBlockData()).getFacing()) {
            case WEST: yaw = -90; break;
            case NORTH: yaw = 0; break;
            case EAST: yaw = 90; break;
            case SOUTH: yaw = 180; break;
            default: break;
        }
        beds.put(bedLoc, player.getUniqueId());

        GSitAPI.createPose(bed, player, Pose.SLEEPING, 0, 0, 0, yaw, true);
    }
    @EventHandler public static void on(PlayerGetUpPoseEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        beds.entrySet().removeIf(kv -> kv.getValue().equals(uuid));
    }

    @EventHandler public static void on(PlayerJoinEvent e) {
        e.joinMessage(null);
        Player player = e.getPlayer();
        player.sendMessage(Component.text(StringUtils.repeat(" \n", 100)));
        LangMessages.Message.Chat_Join.sendMessage(player, Apply.of()
                .add("uuid", player.getUniqueId().toString())
                .add("user_name", player.getName()));
    }
    @EventHandler public static void on(PlayerQuitEvent e) {
        e.quitMessage(null);
    }
    @EventHandler public static void on(PlayerResourcePackStatusEvent e) {
        switch (e.getStatus()) {
            case FAILED_DOWNLOAD:
            case ACCEPTED: return;
            default: break;
        }
        Player player = e.getPlayer();
        lime.once(() -> {
            if (Voice.isConnected(player)) return;
            LangMessages.Message.Chat_NoVoice.sendMessage(player);
        }, 3);
    }
    @EventHandler public static void on(EntitySpawnEvent e) {
        switch (e.getEntityType()) {
            case PHANTOM:
            case MUSHROOM_COW:
            case TRADER_LLAMA:
            case WANDERING_TRADER:
            case VILLAGER:
            case ZOMBIE_VILLAGER:
            case CREEPER:
            case ENDERMAN: break;
            default:
                if (e.getEntity() instanceof LivingEntity entity) entity.setCanPickupItems(false);
                return;
        }
        e.setCancelled(true);
    }
    @EventHandler public static void on(CreatureSpawnEvent e) {
        if (e.getEntityType() == EntityType.CHICKEN && e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.EGG) {
            e.setCancelled(true);
        }
    }
    @EventHandler public static void on(SpawnerSpawnEvent e) {
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (e.getAction() == Action.PHYSICAL) {
            if (block == null) return;
            if (block.getType() == Material.FARMLAND) e.setCancelled(true);
            return;
        }
        if (block != null) {
            if (block.getType() == Material.BREWING_STAND) e.setCancelled(true);
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                ItemStack item = e.getItem();
                if (item == null || item.getType() != Material.BONE_MEAL) return;
                switch (block.getType()) {
                    case GRASS:
                    case GRASS_BLOCK: return;
                    default: break;
                }
                e.setUseItemInHand(Event.Result.DENY);
            }
        }
    }
    @EventHandler public static void on(InventoryOpenEvent e) {
        switch (e.getInventory().getType()) {
            case SHULKER_BOX:
            case ENCHANTING:
            case BEACON:
            case SMITHING:
            case BREWING: break;
            case STONECUTTER:
                if (StonecutterModule.isEnable()) return;
                break;
            case GRINDSTONE:
                if (GrindstoneModule.isEnable()) return;
                break;
            default: return;
        }
        e.setCancelled(true);
    }
    @EventHandler public static void on(BlockFertilizeEvent e) {
        switch (e.getBlock().getType()) {
            case GRASS:
            case GRASS_BLOCK: return;
            default: break;
        }
        e.setCancelled(true);
    }
    @EventHandler public static void on(EntityPotionEffectEvent e) {
        if (e.getModifiedType() != PotionEffectType.DOLPHINS_GRACE || e.getAction() != EntityPotionEffectEvent.Action.ADDED) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player target)) return;
        if (!(e.getEntity() instanceof Wolf wolf)) return;
        UUID uuid = wolf.getOwnerUniqueId();
        if (uuid == null) return;
        e.setCancelled(true);
    }

    @EventHandler public static void on(PortalCreateEvent e) {
        if (Nether.isEnable) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerPortalEvent e) {
        if (Nether.isEnable) return;
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        e.setCanCreatePortal(false);
        e.setCancelled(true);
    }

    @EventHandler public static void on(EntityDamageByPlayerEvent e) {
        Player owner = e.getDamageOwner();
        e.getEntityPlayer().ifPresent(target -> {
            if (owner == null) return;
            if (!owner.isOp()) return;
            if (target.isOp()) return;
            ItemStack item = owner.getInventory().getItemInMainHand();
            if (item == null || item.getType() != Material.RED_SHULKER_BOX) return;
            Administrator.aban(target.getUniqueId(), "До разбирательств", null, owner.getUniqueId());
        });
    }
    @EventHandler public static void on(BellRevealRaiderEvent e) {
        e.setCancelled(true);
    }
}
