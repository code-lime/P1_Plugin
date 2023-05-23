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
import org.lime.core;
import org.lime.gp.admin.Administrator;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.rows.HouseRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.gp.module.damage.PlayerDamageByPlayerEvent;
import org.lime.gp.player.voice.Voice;
import org.lime.system;

import java.util.*;

public class SingleModules implements Listener {
    public static core.element create() {
        return core.element.create(SingleModules.class)
                .withInit(SingleModules::init)
                .withInstance()
                .addEmpty("execute_file", () -> {
                    if (!lime.existConfig("execute")) return;
                    system.json.parse(lime.readAllConfig("execute")).getAsJsonArray().forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.getAsString()));
                });
    }
    public static void init() {
        final HashMap<Material, Material> mapReplace = system.map.<Material, Material>of()
                .add(Material.GRASS_BLOCK, Material.DIRT_PATH)
                //.add(Material.ICE, Material.AIR)
                .build();

        final HashMap<Position, Material> moveLocation = new HashMap<>();
        lime.repeat(() -> {
            HashMap<Position, Material> nowLocation = new HashMap<>();
            Bukkit.getOnlinePlayers().forEach(player -> {
                Location location = player.getLocation();
                Block path = location.clone().add(0, -1, 0).getBlock();
                Material block_type = path.getType();
                if (mapReplace.containsKey(block_type)) nowLocation.put(Position.of(path.getLocation()), block_type);

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
            moveLocation.entrySet().removeIf(kv -> {
                Position location = kv.getKey();
                if (nowLocation.containsKey(location)) return true;
                Block block = location.getBlock();
                Material material = mapReplace.getOrDefault(block.getType(), null);
                if (material == null) return true;
                if (system.rand_is(0.05)) block.setType(material);
                return true;
            });
            moveLocation.putAll(nowLocation);
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
        String bedLoc = system.getString(bed.getLocation());
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
        LangMessages.Message.Chat_Join.sendMessage(player, Apply.of().add("uuid", player.getUniqueId().toString()));
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
    @EventHandler public static void on(StructureGrowEvent e) {
        Location location = e.getLocation();
        int x = location.getBlockX();
        int y = location.getBlockY() - 1;
        int z = location.getBlockZ();
        World world = location.getWorld();
        Block block = world.getBlockAt(x, y, z);
        switch (block.getType()) {
            case DIRT:
            case GRASS_BLOCK:
            case PODZOL: break;
            default: e.setCancelled(true); return;
        }
        List<Block> blocks = new ArrayList<>();
        for (int _x = -1; _x <= 1; _x++) {
            for (int _y = -1; _y <= 0; _y++)
                for (int _z = -1; _z <= 1; _z++)
                    if (_x != 0 || _y != 0 || _z != 0)
                        blocks.add(world.getBlockAt(x + _x, y + _y, z + _z));
        }
        blocks.removeIf(b -> switch (b.getType()) {
            case DIRT, GRASS_BLOCK, PODZOL -> false;
            default -> true;
        });
        Collections.shuffle(blocks);
        int length = blocks.size();
        length = Math.max(length / 3, length);
        for (int i = 0; i < length; i++) blocks.get(i).setType(system.rand(Material.ROOTED_DIRT, Material.COARSE_DIRT));
        lime.nextTick(() -> {
            switch (block.getType()) {
                case DIRT:
                case GRASS_BLOCK:
                case PODZOL: break;
                default: return;
            }
            block.setType(system.rand(Material.ROOTED_DIRT, Material.COARSE_DIRT));
        });
    }
    @EventHandler public static void on(InventoryOpenEvent e) {
        switch (e.getInventory().getType()) {
            case SHULKER_BOX:
            case ENCHANTING:
            case BEACON:
            case SMITHING:
            case BREWING: break;
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

    @EventHandler public static void on(PlayerDamageByPlayerEvent e) {
        Player owner = e.getDamageOwner();
        Player target = e.getEntity();
        if (owner == null) return;
        if (!owner.isOp()) return;
        if (target.isOp()) return;
        ItemStack item = owner.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.RED_SHULKER_BOX) return;
        Administrator.aban(target.getUniqueId(), "До разбирательств", null, owner.getUniqueId());
    }
    @EventHandler public static void on(BellRevealRaiderEvent e) {
        e.setCancelled(true);
    }
}
