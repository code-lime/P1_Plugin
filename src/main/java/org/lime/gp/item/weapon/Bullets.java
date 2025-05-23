package org.lime.gp.item.weapon;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.EntitySpectralArrow;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftSpectralArrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.list.BulletSetting;
import org.lime.gp.item.settings.use.target.BlockTarget;
import org.lime.gp.item.settings.use.target.EntityTarget;
import org.lime.gp.lime;
import org.lime.gp.module.ArrowBow;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.plugin.CoreElement;
import org.lime.system.utils.RandomUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Bullets implements Listener {

    public static CoreElement create() {
        return CoreElement.create(Bullets.class)
                .withInstance()
                .withInit(Bullets::init);
    }
    private static final SoundEffect SOUND_EFFECT_NONE = SoundEffect.createFixedRangeEvent(new MinecraftKey("lime", "empty"), 0);
    
    private static BukkitTask updateTask = null;
    private static void reRunUpdate(int deltaTicks, double modify) {
        if (updateTask != null) updateTask.cancel();
        updateTask = lime.repeatTicks(() -> Bullets.update(deltaTicks, modify), deltaTicks);
    }
    public static void init() {
        reRunUpdate(4, 1);
        /*AnyEvent.addEvent("bullet.delta", type.owner_console, v -> v.createParam(Integer::parseInt, "[delta]").createParam(Double::parseDouble, "1", "[modify]"), (player, delta, modify) -> {
            reRunUpdate(delta, modify);
            lime.logOP("Change dalta bullet tick to " + delta + " with modify value " + modify);
        });*/
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(lime._plugin, PacketType.Play.Server.SPAWN_ENTITY, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            @Override public void onPacketSending(PacketEvent event) {
                Object _packet = event.getPacket().getHandle();
                if (_packet instanceof PacketPlayOutSpawnEntity packet) {
                    if (packet.getType() != EntityTypes.SPECTRAL_ARROW) return;
                    Optional.ofNullable(Bukkit.getEntity(packet.getUUID()))
                            .filter(v -> !v.getScoreboardTags().contains("bullet"))
                            .map(v -> new PacketPlayOutSpawnEntity(
                                            packet.getId(),
                                            packet.getUUID(),
                                            packet.getX(),
                                            packet.getY(),
                                            packet.getZ(),
                                            packet.getXRot() * 360.0f / 256.0f,
                                            packet.getYRot() * 360.0f / 256.0f,
                                            EntityTypes.ARROW,
                                            packet.getData(),
                                            new Vec3D(packet.getXa(), packet.getYa(), packet.getZa()),
                                            packet.getYHeadRot()
                                    )
                            )
                            .ifPresent(v -> event.setPacket(new PacketContainer(event.getPacketType(), v)));
                } else if (_packet instanceof PacketPlayOutNamedSoundEffect packet) {
                    if (!packet.getSound().value().getLocation().equals(SOUND_EFFECT_NONE.getLocation())) return;
                    event.setCancelled(true);
                }
            }
        });
    }
    public static final NamespacedKey TASER_TICKS = new NamespacedKey(lime._plugin, "taser_ticks");
    public static final ImageBuilder BLUR = ImageBuilder.of(0xEff8, 2000);

    private static final NamespacedKey TICK_ROTATION_KEY = new NamespacedKey(lime._plugin, "tick_rotation");
    private static final NamespacedKey ARROW_DAMAGE_KEY = new NamespacedKey(lime._plugin, "arrow_damage");
    private static final NamespacedKey ARROW_ITEM_ID_KEY = new NamespacedKey(lime._plugin, "arrow_item_id");

    private static final PotionEffect TASER_FREEZE = new PotionEffect(PotionEffectType.SLOW, 5, 5, false, false, false);
    public static final PotionEffect TRAUMATIC_FREEZE = new PotionEffect(PotionEffectType.SLOW, 4*20, 5, false, false, false);

    public static void update(int deltaTicks, double modify) {
        List<GameMode> gmList = List.of(GameMode.CREATIVE, GameMode.SPECTATOR);
        Bukkit.getWorlds().forEach(world -> {
            world.getLivingEntities().forEach(entity -> {
                PersistentDataContainer container = entity.getPersistentDataContainer();
                Optional.ofNullable(container.get(TASER_TICKS, PersistentDataType.INTEGER))
                        .filter(ticks -> {
                            ticks -= deltaTicks;
                            if (ticks <= 0) {
                                container.remove(TASER_TICKS);
                                return false;
                            }
                            container.set(TASER_TICKS, PersistentDataType.INTEGER, ticks);
                            return true;
                        })
                        .filter(ticks -> !(entity instanceof Player player && gmList.contains(player.getGameMode())))
                        .ifPresent(ticks -> {
                            float rnd = (float) RandomUtils.rand(0.0, 0.5);
                            if (entity instanceof Player player)
                                CustomUI.TitleUI.show(player, BLUR.withColor(TextColor.color(rnd, rnd, 0.5f)));
                            entity.addPotionEffect(TASER_FREEZE);
                            if (ticks % 10 == 0 && entity instanceof CraftLivingEntity centity) {
                                EntityLiving eentity = centity.getHandle();
                                eentity.level().broadcastEntityEvent(eentity, (byte)2);
                                if (eentity instanceof EntityPlayer eplayer)
                                    eplayer.connection.send(new ClientboundHurtAnimationPacket(entity.getEntityId(), RandomUtils.rand(0, 360)));
                                ReflectionAccess.playHurtSound_EntityLiving.call(eentity, new Object[] { eentity.damageSources().generic() });
                            }
                        });
            });
            world.getEntitiesByClass(SpectralArrow.class).forEach(arrow -> {
                Set<String> tags = arrow.getScoreboardTags();
                if (arrow.isOnGround() || !tags.contains("bullet")) return;
                if (tags.remove("bullet:no_ground")) arrow.setGravity(true);
                PersistentDataContainer container = arrow.getPersistentDataContainer();
                Optional.ofNullable(container.get(TICK_ROTATION_KEY, PersistentDataType.DOUBLE))
                        .map(_v -> _v * modify)
                        .filter(RandomUtils::rand_is)
                        .ifPresent(v -> {
                            tags.add("bullet:no_ground");
                            arrow.setGravity(false);
                        });
            });
        });
    }
    public static void spawnBullet(Player owner, Location start, double speed, double down, double damage, Integer id, ItemStack weaponItem) {
        start.getWorld().spawn(start, SpectralArrow.class, projectile -> {
            projectile.setVelocity(start.getDirection().multiply(speed));
            EntitySpectralArrow handle = ((CraftSpectralArrow)projectile).getHandle();
            projectile.addScoreboardTag("bullet");
            PersistentDataContainer container = projectile.getPersistentDataContainer();
            container.set(TICK_ROTATION_KEY, PersistentDataType.DOUBLE, down);
            container.set(ARROW_DAMAGE_KEY, PersistentDataType.DOUBLE, damage);
            if (id != null) container.set(ARROW_ITEM_ID_KEY, PersistentDataType.INTEGER, id);
            projectile.setShooter(owner);
            projectile.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
            handle.life = handle.level().spigotConfig.arrowDespawnRate - 20 * 20;

            ArrowBow.setBowItem(projectile, weaponItem);
        });
    }

    @EventHandler(ignoreCancelled = true) public static void on(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof CraftSpectralArrow _arrow)) return;
        EntitySpectralArrow arrow = _arrow.getHandle();
        if (e.getHitEntity() instanceof CraftLivingEntity _entity) {
            EntityLiving entity = _entity.getHandle();

            Optional.ofNullable(_arrow.getPersistentDataContainer().get(ARROW_DAMAGE_KEY, PersistentDataType.DOUBLE))
                    .ifPresent(damage -> {
                        e.setCancelled(true);
                        DamageSource damagesource;
                        Entity owner = arrow.getOwner();
                        if (owner == null) {
                            damagesource = entity.damageSources().arrow(arrow, arrow);
                        } else {
                            damagesource = entity.damageSources().arrow(arrow, owner);
                            if (owner instanceof EntityLiving living) living.setLastHurtMob(entity);
                        }
                        if (arrow.isCritArrow()) damagesource = damagesource.critical();
                        entity.invulnerableTime = 0;

                        double timeSec = arrow.tickCount / 20.0;

                        Optional<BulletSetting> bulletSetting = Optional.ofNullable(_arrow.getPersistentDataContainer().get(ARROW_ITEM_ID_KEY, PersistentDataType.INTEGER))
                                .map(Items.creators::get)
                                .map(v -> v instanceof ItemCreator c ? c : null)
                                .flatMap(v -> v.getOptional(BulletSetting.class));
                        float hurt_amount = (float)(double)bulletSetting.filter(v -> !Double.isInfinite(v.time_sec))
                                .map(v -> v.time_sec < timeSec ? v.time_damage_scale : (damage - (1 - v.time_damage_scale) * damage * (timeSec / v.time_sec)))
                                .orElse(damage);

                        if (entity.hurt(damagesource, hurt_amount))
                            bulletSetting.ifPresent(v -> v.bullet_action.execute(v.bullet_data, new EntityTarget(entity.getBukkitEntity())));

                        arrow.discard();
                    });
        }
        if (e.getHitBlock() instanceof CraftBlock _block && _block.getHandle() instanceof WorldServer world) {
            Optional.ofNullable(_arrow.getPersistentDataContainer().get(ARROW_DAMAGE_KEY, PersistentDataType.DOUBLE))
                    .ifPresent(damage -> {
                        Optional.ofNullable(_arrow.getPersistentDataContainer().get(ARROW_ITEM_ID_KEY, PersistentDataType.INTEGER))
                                .map(Items.creators::get)
                                .map(v -> v instanceof ItemCreator c ? c : null)
                                .flatMap(v -> v.getOptional(BulletSetting.class))
                                .ifPresent(bullet -> bullet.playSound(_block.getNMS(), _arrow.getLocation()));
                        arrow.setSoundEvent(SOUND_EFFECT_NONE);
                        Optional.ofNullable(_arrow.getPersistentDataContainer().get(ARROW_ITEM_ID_KEY, PersistentDataType.INTEGER))
                                .map(Items.creators::get)
                                .map(v -> v instanceof ItemCreator c ? c : null)
                                .flatMap(v -> v.getOptional(BulletSetting.class))
                                .ifPresent(v -> v.bullet_action.execute(v.bullet_data, new BlockTarget(_block)));
                        world.getBlockEntity(_block.getPosition(), TileEntityTypes.SKULL)
                                .map(v -> v instanceof TileEntityLimeSkull skull ? skull : null)
                                .flatMap(TileEntityLimeSkull::customKey)
                                .ifPresent(key -> {
                                    e.setCancelled(true);
                                    arrow.discard();
                                });
                    });
        }
    }

    @EventHandler public static void on(EntityPushedByEntityAttackEvent e) {
        if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent
                && damageEvent.getDamager() instanceof CraftSpectralArrow spectralArrow
                && spectralArrow.getPersistentDataContainer().has(ARROW_DAMAGE_KEY))
            e.setCancelled(true);
    }
}















