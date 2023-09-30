package org.lime.gp.player.module;

import com.google.gson.JsonObject;
import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.api.event.PreEntityGetUpSitEvent;
import dev.geco.gsit.api.event.PrePlayerGetUpPoseEvent;
import dev.geco.gsit.objects.GetUpReason;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.lime.display.Displays;
import org.lime.gp.admin.Administrator;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.gp.database.rows.CityRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.entity.component.data.BackPackInstance;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.item.Items;
import org.lime.gp.item.UseSetting;
import org.lime.gp.item.Vest;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.inventory.MainPlayerInventory;
import org.lime.gp.player.inventory.WalletInventory;
import org.lime.gp.player.level.LevelModule;
import org.lime.gp.player.menu.LangEnum;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.player.module.drugs.Drugs;
import org.lime.gp.player.module.needs.thirst.Thirst;
import org.lime.gp.player.module.needs.food.ProxyFoodMetaData;
import org.lime.gp.player.ui.Infection;
import com.destroystokyo.paper.ParticleBuilder;
import com.google.gson.JsonPrimitive;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.util.Vector;
import org.lime.plugin.CoreElement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.utils.MathUtils;
import org.lime.system.utils.RandomUtils;

import javax.annotation.Nullable;
import java.util.*;

public class Death implements Listener {
    private static Location DEFAULT_SPAWN_LOCATION;
    private static boolean DEATH_VOICE_ENABLE = true;

    public static Location getSpawnLocation(UUID uuid) {
        //UserRow.getBy(uuid).flatMap(UserRow::getCityID).ifPresentOrElse(city -> lime.logOP("CITY: " + city), () -> lime.logOP("CITY: EMPTY"));
        return UserRow.getBy(uuid)
                .flatMap(UserRow::getCityID)
                .flatMap(CityRow::getBy)
                .flatMap(v -> v.posMain)
                .map(v -> v.toLocation(lime.MainWorld))
                .orElse(DEFAULT_SPAWN_LOCATION);
    }
    public static Location getSpawnLocation(Player player) {
        return getSpawnLocation(player.getUniqueId());
    }

    public static CoreElement create() {
        return CoreElement.create(Death.class)
                .withInit(Death::init)
                .withUninit(Death::uninit)
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("spawn")
                        .withDefault(new JsonPrimitive(MathUtils.getString(new Vector(0, 70, 0))))
                        .withInvoke(json -> DEFAULT_SPAWN_LOCATION = MathUtils.getLocation(lime.MainWorld, json.getAsString()))
                )
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("death_voice")
                        .withDefault(new JsonPrimitive(DEATH_VOICE_ENABLE))
                        .withInvoke(json -> DEATH_VOICE_ENABLE = json.getAsBoolean())
                )
                .withInstance();
    }

    public static class DieInfo {
        public final Player player;
        public final Location location;
        public final long dieTime;
        public final Entity killer;
        public long showTimes;
        private static final long showTimesTotal = 60 * 1000;
        public boolean canKill = false;

        public DieInfo(Player player, @Nullable Entity killer) {
            this.player = player;
            this.location = player.getLocation().clone();
            this.dieTime = System.currentTimeMillis();
            this.killer = killer;

            showTimes = dieTime;
        }

        public void tryShow(Player player) {
            long now = System.currentTimeMillis();
            if (now <= showTimes) return;
            long time = (showTimes - dieTime) / 1000;
            MenuCreator.showLang(player, LangEnum.DIE_TIMER, Apply.of().add("time", String.valueOf(time)));
            showTimes = showTimesTotal + now;
        }
        public boolean canKill() { return canKill; }
        public void setCanKill() { canKill = true; }
        public void up() { lime.unLay(player); }
    }
    private static final HashMap<UUID, DieInfo> dieCooldown = new HashMap<>();
    public enum State {
        NONE(0),
        CANT_DIE(1),
        CAN_DIE(2);

        public final int index;

        State(int index) {
            this.index = index;
        }
    }
    public static State getDamageState(UUID uuid) {
        DieInfo info = dieCooldown.get(uuid);
        return info == null ? State.NONE : info.canKill ? State.CAN_DIE : State.CANT_DIE;
    }
    public static boolean isDamageLay(UUID uuid) {
        return dieCooldown.containsKey(uuid);
    }
    public static boolean isDeathMute(UUID uuid) {
        return !DEATH_VOICE_ENABLE && dieCooldown.containsKey(uuid);
    }

    private static final HashSet<UUID> nextSets = new HashSet<>();

    public enum Reason {
        CUSTOM_KILL("custom.kill"),
        SERVER_STOP("server.stop"),
        DISCONNECT("disconnect"),
        KILL("kill"),
        CAUSE("cause"),
        INFECTION("infection");

        private final String key;

        Reason(String key) {
            this.key = key;
        }
    }

    public static void init() {
        AnyEvent.addEvent("custom.kill", AnyEvent.type.none, player -> {
            DieInfo dieInfo = dieCooldown.getOrDefault(player.getUniqueId(), null);
            if (dieInfo == null) return;
            if (!dieInfo.canKill()) return;
            kill(player, Reason.CUSTOM_KILL);
        });
        AnyEvent.addEvent("custom.kill.state", AnyEvent.type.other, player -> {
            DieInfo dieInfo = dieCooldown.getOrDefault(player.getUniqueId(), null);
            if (dieInfo == null) return;
            dieInfo.setCanKill();
        });
        AnyEvent.addEvent("medic.up", AnyEvent.type.other, builder -> builder.createParam(UUID::fromString, "[uuid]"), (player, other_uuid) -> {
            Player other = Bukkit.getPlayer(other_uuid);
            if (other == null) return;
            UseSetting.timeUse(player, other, 10 * 20, (_player, _other) -> dieCooldown.containsKey(other_uuid), (_player, _other) -> {
                if (up(other) != null)
                    _other.setHealth(4);
            }, (_player, _other) -> {});
        });
        AnyEvent.addEvent("opg.kill", AnyEvent.type.other, builder -> builder.createParam(UUID::fromString, "[uuid]"), (player, other) -> {
            up(other);
            Player _other = Bukkit.getPlayer(other);
            if (_other == null) return;
            _other.setHealth(4);
        });
        AnyEvent.addEvent("medic.heal", AnyEvent.type.other, builder -> builder.createParam(UUID::fromString, "[uuid]").createParam(Double::parseDouble, "[HP]"), (player, other, hp) -> {
            Player _other = Bukkit.getPlayer(other);
            if (_other == null) return;
            _other.setHealth(Math.max(0, Math.min(_other.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), _other.getHealth() + hp)));
            _other.removeScoreboardTag("leg.broken");
        });
        AnyEvent.addEvent("reset.head.all", AnyEvent.type.owner_console, _0 -> Bukkit.getOnlinePlayers().forEach(player -> {
            up(player);
            player.getInventory().clear();
            player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            player.setExp(0);
            player.setLevel(0);
            player.setFireTicks(0);
            ProxyFoodMetaData.ofPlayer(player)
                    .ifPresent(food -> {
                        food.setFoodLevel(20);
                        food.setSaturation(10);
                    });
            player.setRemainingAir(player.getMaximumAir());
            player.getActivePotionEffects().forEach(v -> player.removePotionEffect(v.getType()));
            Thirst.thirstValue(player, 6*2);
            Thirst.thirstStateReset(player);
            Infection.clear_kill(player);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            dieCooldown.remove(player.getUniqueId());
            player.removeScoreboardTag("leg.broken");
        }));
        /*
        lime.repeat(() -> {
            if (!SPAWN_LOCATIONS.isEmpty()) return;
            nextSets.removeIf(uuid -> !EntityPosition.onlinePlayers.containsKey(uuid));
            List<UUID> isHeal = new ArrayList<>();
            Toast1<Boolean> isSpawnWork = Toast.of(false);
            Tables.HOUSE_TABLE.getRows().forEach(v -> {
                if (!v.inZone(DEFAULT_SPAWN_LOCATION)) return;
                isSpawnWork.val0 = true;
                EntityPosition.onlinePlayers.forEach((uuid, player) -> {
                    if (player.getWorld() != lime.MainWorld) return;
                    if (!v.inZone(player.getLocation())) return;
                    if (player.getHealth() >= 10) {
                        nextSets.remove(uuid);
                        return;
                    }
                    isHeal.add(uuid);
                    nextSets.add(uuid);
                });
            });
            if (!isSpawnWork.val0) return;
            nextSets.forEach(uuid -> {
                if (isHeal.contains(uuid) || Administrator.inABan(uuid)) return;
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) return;
                player.teleport(DEFAULT_SPAWN_LOCATION);
                lime.once(() -> LangMessages.Message.Medic_Teleport_HP.sendMessage(player), 2);
            });
        }, 2);
        */

        lime.repeat(Death::update, 0.1);
        lime.repeatTicks(Death::updateLock, 1);
    }
    private static StringBuilder toLog(ItemStack item) {
        StringBuilder builder = new StringBuilder();
        if (item == null) return builder.append("NONE");
        builder.append(item.getType().name()).append("*").append(item.getAmount());
        if (!item.hasItemMeta()) return builder;
        return builder.append(json.builder.byObject(item.getItemMeta().serialize()).build().toString());
    }
    public static List<ItemStack> extractInventoryDrop(Player player, boolean extractAll) {
        return extractInventoryDrop(player, extractAll, new StringBuilder());
    }
    public static List<ItemStack> extractInventoryDrop(Player player, boolean extractAll, StringBuilder log) {
        player.closeInventory();
        List<ItemStack> dropped = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (MainPlayerInventory.checkBarrier(item)) continue;
            dropped.add(item);
        }
        for (int i = 27; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (MainPlayerInventory.checkBarrier(item)) continue;
            dropped.add(item);
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand.getType() != Material.AIR) {
            dropped.add(offHand);
        }

        for (ItemStack item : inventory.getArmorContents()) {
            if (item != null && item.getType() != Material.AIR)
                dropped.add(item);
        }

        Location location = player.getLocation().clone();
        log.append("inventory ");
        if (!dropped.isEmpty()) dropped.forEach(item -> {
            log.append(toLog(item)).append(' ');
            CoreProtectHandle.logDrop(location, player, item);
        });
        else log.append("EMPTY ");
        log.append("wallet ");
        if (!WalletInventory.extractItems(player, extractAll, item -> {
            log.append(toLog(item)).append(' ');
            CoreProtectHandle.logDrop(location, player, item);
            dropped.add(item);
        })) log.append("EMPTY ");
        log.append("vest ");
        if (!Vest.extractItems(player, item -> {
            log.append(toLog(item)).append(' ');
            CoreProtectHandle.logDrop(location, player, item);
            dropped.add(item);
        })) log.append("EMPTY ");
        log.append("from ").append(MathUtils.getString(location.toVector())).append(" in ").append(location.getWorld().getKey());
        lime.logToFile("kills", log.toString());

        inventory.clear();
        return dropped;
    }
    public static void kill(Player player, Reason reason) {
        DieInfo dieInfo = up(player);
        Knock.unKnock(player);
        HandCuffs.unLockAny(player);
        TargetMove.unTarget(player.getUniqueId());

        StringBuilder log = new StringBuilder("[{time}] ").append(player.getName()).append("[").append(player.getUniqueId()).append("] killed '").append(reason.key).append("' ");
        for (PotionEffectType type : PotionEffectType.values()) player.removePotionEffect(type);

        List<ItemStack> dropped = extractInventoryDrop(player, false, log);

        if (!dropped.isEmpty()) {
            List<ItemStack> out = new ArrayList<>();
            dropped.removeIf(item -> {
                if (Items.has(BackPackDropSetting.class, item)) {
                    out.add(item);
                    return true;
                }
                return false;
            });
            Location location = player.getLocation().clone();
            if (!out.isEmpty()) Items.dropItem(location, out);
            if (!dropped.isEmpty()) BackPackInstance.dropItems(player, location, dropped);
        }

        player.setHealth(6);
        player.setExp(0);
        player.setLevel(0);
        player.setFireTicks(0);
        ProxyFoodMetaData.ofPlayer(player)
                .ifPresent(food -> {
                    food.setFoodLevel(20);
                    food.setSaturation(10);
                });
        player.setRemainingAir(player.getMaximumAir());
        player.getActivePotionEffects().forEach(v -> player.removePotionEffect(v.getType()));
        Thirst.thirstValue(player, 6*2);
        Thirst.thirstStateReset(player);
        LevelModule.dieAction(player);
        JavaScript.invoke("player_die('"+player.getUniqueId()+"',"+Optional.ofNullable(dieInfo).map(v -> v.killer).map(Entity::getUniqueId).map(v -> "'" + v + "'").orElse("null")+")");
        Infection.clear_kill(player);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        dieCooldown.remove(player.getUniqueId());
        player.removeScoreboardTag("leg.broken");
        Location spawnLocation = getSpawnLocation(player);
        player.teleport(spawnLocation);
        for (int i = 0; i < 5; i++) lime.onceTicks(() -> player.teleport(spawnLocation), i * 2);
        lime.once(() -> LangMessages.Message.Medic_Teleport_Die.sendMessage(player), 2);
    }
    public static @Nullable DieInfo up(UUID player) {
        DieInfo dieInfo = dieCooldown.remove(player);
        if (dieInfo == null) return null;
        dieInfo.up();
        return dieInfo;
    }
    public static @Nullable DieInfo up(Player player) {
        return up(player.getUniqueId());
    }
    private static final ParticleBuilder BLOOD = Particle.BLOCK_CRACK
            .builder()
            .data(Material.REDSTONE_BLOCK.createBlockData())
            .count(10)
            .offset(0.1, 0.1, 0.1);
    private static final PotionEffect BLINDNESS = PotionEffectType.BLINDNESS.createEffect(60, 1).withParticles(false).withAmbient(false).withIcon(false);
    private static final PotionEffect DARKNESS = PotionEffectType.DARKNESS.createEffect(60, 1).withParticles(false).withAmbient(false).withIcon(false);
    //private static final PotionEffect INVISIBILITY = PotionEffectType.INVISIBILITY.createEffect(20, 1).withParticles(false).withAmbient(false).withIcon(false);

    public static void updateLock() {
        dieCooldown.keySet().forEach(uuid -> Drugs.lockArmsTick(Bukkit.getPlayer(uuid)));
    }
    private static final HashMap<UUID, Integer> blindnessCache = new HashMap<>();
    public static void update() {
        dieCooldown.entrySet().removeIf(kv -> {
            if (!(Bukkit.getPlayer(kv.getKey()) instanceof CraftPlayer player)) {
                blindnessCache.remove(kv.getKey());
                return true;
            }
            player.setHealth(1);
            blindnessCache.put(kv.getKey(), 3);
            player.getHandle().connection.send(new PacketPlayOutEntityEffect(player.getEntityId(), new MobEffect(MobEffects.BLINDNESS, 40, 1, false, false, false)));
            DieInfo info = kv.getValue();
            info.tryShow(player);
            if (lime.isLay(player)) {
                return false;
            }
            if (lime.isSit(player) && TargetMove.isTarget(player.getUniqueId())) return false;
            Location location = info.location.clone();
            location.add(0, 1, 0);
            location.setY(TargetMove.getHeight(location.getBlock()));
            lime.unSit(player);
            GSitAPI.createPose(location.getBlock(), player, Pose.SLEEPING, location.getX() % 1, location.getY() % 1 + 0.5, location.getZ() % 1, location.getYaw(), false);
            return false;
        });
        blindnessCache.entrySet().removeIf(kv -> {
            if (!(Bukkit.getPlayer(kv.getKey()) instanceof CraftPlayer player)) return true;
            if (kv.setValue(kv.getValue() - 1) > 0) return false;
            player.addPotionEffect(BLINDNESS);
            player.addPotionEffect(DARKNESS);
            return true;
        });
        Bukkit.getOnlinePlayers().forEach(player -> {
            switch (player.getGameMode()) {
                case CREATIVE:
                case SPECTATOR: return;
                default:
                    break;
            }
            if (player.getScoreboardTags().contains("leg.broken")) player.addPotionEffect(PotionEffectType.SLOW
                    .createEffect(5, 2)
                    .withIcon(false)
                    .withParticles(false)
                    .withAmbient(false)
            );
            if (player.getHealth() > 4) return;
            if (RandomUtils.rand() || RandomUtils.rand()) return;
            BLOOD.location(player.getLocation().clone().add(0, 0.5, 0)).spawn();
        });
    }
    public static void uninit() {
        dieCooldown.forEach((k,v) -> {
            Player player = Bukkit.getPlayer(k);
            if (player == null) return;
            kill(player, Reason.SERVER_STOP);
        });
    }

    @EventHandler public static void on(PrePlayerGetUpPoseEvent e) {
        switch (e.getPoseSeat().getPose()) {
            case SITTING, SLEEPING: break;
            default: return;
        }
        if (e.getReason() != GetUpReason.GET_UP) return;
        if (!isDamageLay(e.getPlayer().getUniqueId())) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PreEntityGetUpSitEvent e) {
        if (e.getReason() != GetUpReason.GET_UP) return;
        if (!isDamageLay(e.getEntity().getUniqueId())) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (!dieCooldown.containsKey(player.getUniqueId())) return;
        kill(player, Reason.DISCONNECT);
    }
    @EventHandler public static void on(PlayerDeathEvent e) {
        Player player = e.getEntity();
        UUID uuid = player.getUniqueId();
        e.setCancelled(true);
        if (!dieCooldown.containsKey(uuid)) {
            EntityDamageEvent damage = player.getLastDamageCause();
            Entity killer = null;
            if (damage != null) {
                if (damage instanceof EntityDamageByEntityEvent ee && (killer = ee.getDamager()) instanceof Mob mob)
                    mob.setTarget(null);
            }
            //lime.logOP("Killer: " + killer);
            //lime.logOP("PlayerKiller: " + ExtMethods.damagerPlayer(killer).orElse(null));
            dieCooldown.put(uuid, new DieInfo(player, ExtMethods.damagerPlayer(killer).orElse(null)));
            return;
        }
        kill(player, Reason.KILL);
    }
    @EventHandler public static void on(EntityDamageByEntityEvent e) {
        List<UUID> uuids = new ArrayList<>();
        if (e.getEntity() instanceof Player) uuids.add(e.getEntity().getUniqueId());
        if (e.getDamager() instanceof Player) uuids.add(e.getDamager().getUniqueId());
        for (UUID uuid : uuids) {
            if (dieCooldown.containsKey(uuid)) {
                e.setCancelled(true);
                return;
            }
        }
    }
    @EventHandler(ignoreCancelled = true) public static void on(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        switch (e.getCause()) {
            case FALL:
                if (player.isInsideVehicle() || Displays.hasVehicle(player.getEntityId())) break;
                final int min = 4;
                final int max = 14;
                double damage = (e.getDamage() - min) / (max - min);
                if (damage > 1) damage = 1;
                if (damage > 0 && RandomUtils.rand_is((damage + 0.2) / 1.2) && player.getWorld() == lime.MainWorld) {
                    player.addScoreboardTag("leg.broken");
                    MenuCreator.showLang(player, LangEnum.ME, Apply.of().add("key", "LEG_BROKEN"));
                }
                break;
            default:
                break;
        }
        if (!dieCooldown.containsKey(uuid)) return;
        switch (e.getCause()) {
            case FIRE:
            case LAVA:
            case DROWNING:
            case FIRE_TICK: kill(player, Reason.CAUSE); break;
            case SUFFOCATION:
                if (!((CraftPlayer)player).getHandle().isInWall()) kill(player, Reason.CAUSE);
                break;
            case KILL:
            case VOID:
                return;
            default:
                break;
        }
        e.setCancelled(true);
    }
    @EventHandler public static void on(BlockPlaceEvent e) {
        if (!dieCooldown.containsKey(e.getPlayer().getUniqueId())) return;
        e.setCancelled(true);
    }
    @EventHandler public static void On(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() == null) return;
        if (!dieCooldown.containsKey(e.getTarget().getUniqueId())) return;
        e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST) public static void OnInv(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!dieCooldown.containsKey(player.getUniqueId())) return;
        e.setCancelled(true);
    }
}
















