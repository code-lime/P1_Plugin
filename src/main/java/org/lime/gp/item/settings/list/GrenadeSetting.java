package org.lime.gp.item.settings.list;

import com.destroystokyo.paper.ParticleBuilder;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntitySnowball;
import net.minecraft.world.level.World;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftSnowball;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftLocation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.extension.HitUtils;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.item.settings.use.ITimeUse;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.item.settings.use.target.SelfTarget;
import org.lime.gp.item.weapon.Bullets;
import org.lime.gp.lime;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.player.module.TargetMove;
import org.lime.gp.player.module.needs.INeedEffect;
import org.lime.gp.player.module.needs.NeedSystem;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.gp.sound.Sounds;
import org.lime.json.JsonObjectOptional;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Func1;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;
import org.lime.unsafe;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

@Setting(name = "grenade")
public class GrenadeSetting extends ItemSetting<JsonObject> implements ITimeUse<SelfTarget> {
    public static CoreElement create() {
        return CoreElement.create(GrenadeSetting.class)
                .withInstance(new Listener() {
                    @EventHandler private static void on(ProjectileHitEvent e) {
                        if (!(e.getEntity() instanceof CraftSnowball snowball)) return;
                        EntitySnowball handle = snowball.getHandle();
                        Items.getOptional(GrenadeSetting.class, handle.getItem())
                                .ifPresent(grenade -> {
                                    Set<String> tags = snowball.getScoreboardTags();
                                    if (!tags.contains("grenade"))
                                        return;
                                    e.setCancelled(true);

                                    Block block = e.getHitBlock();
                                    BlockFace face = e.getHitBlockFace();
                                    Entity entity = e.getHitEntity();

                                    Location location;

                                    if (entity != null) location = entity.getLocation();
                                    else if (block != null) {
                                        location = block.getLocation().toCenterLocation();
                                        if (face != null)
                                            location.add(face.getDirection().multiply(0.6));
                                    } else location = snowball.getLocation();

                                    if (grenade.sticky || tags.contains("falling") || !(handle.getOwner() instanceof EntityHuman human)) grenade.hit(location);
                                    else grenade.throwExecute(human, handle.getItem().asBukkitCopy(), true, location.toVector());

                                    snowball.remove();
                                });
                    }
                });
    }

    public enum GrenadeType {
        fire(FireGrenade.class, FireGrenade::new),
        flash(FlashGrenade.class, FlashGrenade::new),
        fragment(FragmentGrenade.class, FragmentGrenade::new),
        smoke(SmokeGrenade.class, SmokeGrenade::new),
        gas(GasGrenade.class, GasGrenade::new);

        private final Class<? extends IGrenade> tClass;
        private final Func1<JsonObject, ? extends IGrenade> creator;

        <T extends IGrenade>GrenadeType(Class<T> tClass, Func1<JsonObject, T> creator) {
            this.creator = creator;
            this.tClass = tClass;
        }

        public IGrenade create(JsonObject json) {
            return creator.invoke(json);
        }
        public JObject docs(IDocsLink docs) {
            return unsafe.createInstance(tClass)
                    .docs(docs)
                    .addFirst(JProperty.require(IName.raw("type"), IJElement.raw(this.name()), IComment.text("Тип гранаты")));
        }
    }

    private interface IGrenade {
        void explosion(Location location);
        JObject docs(IDocsLink docs);
    }
    private static class FireGrenade implements IGrenade {
        private final double radius;
        private final int time;
        private final int fire;

        private final ParticleBuilder particle;

        public FireGrenade(JsonObject json) {
            radius = json.get("radius").getAsDouble();
            time = json.get("time").getAsInt();
            fire = json.get("fire").getAsInt();
            particle = Particle.FLAME.builder()
                    .offset(0.35, 0.2, 0.35)
                    .count(json.get("count").getAsInt())
                    .force(false)
                    .extra(0);
        }
        @Override public JObject docs(IDocsLink docs) {
            return JObject.of(
                    JProperty.require(IName.raw("radius"), IJElement.raw(1.0), IComment.text("Радиус распространения огня")),
                    JProperty.require(IName.raw("time"), IJElement.raw(1), IComment.text("Время активности огня в тиках")),
                    JProperty.require(IName.raw("fire"), IJElement.raw(1), IComment.text("Время горения существа наступившего в огонь в тиках")),
                    JProperty.require(IName.raw("count"), IJElement.raw(1), IComment.text("Интенсивность (количество на 1 блоке в тик) отображения частиц огня"))
            );
        }

        @Override public void explosion(Location location) {
            org.bukkit.World world = location.getWorld();
            int radius = (int)Math.ceil(this.radius);
            int _x = location.getBlockX();
            int _y = location.getBlockY();
            int _z = location.getBlockZ();
            Vector center = location.toVector();
            Map<Toast2<Integer, Integer>, Double> blocks = new HashMap<>();
            double sqrRadius = radius * radius;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Vector point = new Vector(x + _x, _y + 0.25, z + _z);
                    if (point.distanceSquared(center) > sqrRadius)
                        continue;
                    if (!HitUtils.isHitPoint(world, center, point, 0.75))
                        continue;
                    Toast2<Integer, Integer> key = Toast.of(x, z);
                    TargetMove.getOptionalHeight(point.toLocation(world).getBlock())
                            .ifPresent(height -> blocks.put(key, height));
                }
            }
            tick(world, _x, _z, blocks, time);
        }
        private void tick(org.bukkit.World world, int x, int z, Map<Toast2<Integer, Integer>, Double> blocks, int iterator) {
            blocks.forEach((point, y) -> {
                Location location = new Location(world, x + point.val0 + 0.5, y, z + point.val1 + 0.5);
                location.getNearbyLivingEntities(0.6).forEach(entity -> {
                    if (entity.getFireTicks() >= fire) return;
                    entity.setFireTicks(fire);
                });
                particle.location(location).spawn();
            });
            if (iterator <= 1) return;
            lime.onceTicks(() -> tick(world, x, z, blocks, iterator - 1), 1);
        }
    }
    private static class FlashGrenade implements IGrenade {
        private final double radius;
        private final int blur;

        private static final ParticleBuilder particle = Particle.FLASH.builder()
                .offset(0.2, 0.2, 0.2)
                .count(5)
                .force(false)
                .extra(0);

        public FlashGrenade(JsonObject json) {
            radius = json.get("radius").getAsDouble();
            blur = json.get("blur").getAsInt();
        }
        @Override public JObject docs(IDocsLink docs) {
            return JObject.of(
                    JProperty.require(IName.raw("radius"), IJElement.raw(1.0), IComment.text("Радиус срабатывания флешки")),
                    JProperty.require(IName.raw("blur"), IJElement.raw(1), IComment.text("Время потери зрения в тиках"))
            );
        }

        public static CoreElement create() {
            return CoreElement.create(FlashGrenade.class)
                    .withInit(FlashGrenade::init);
        }


        private static double MUTATE_FLASH = 0.3;
        private static int BLUR_FLASH = 18;
        private static void init() {
            AnyEvent.addEvent("tmp.flash", AnyEvent.type.owner_console, v -> v.createParam(Double::parseDouble, "[0.0;1.0]"), (p, v) -> MUTATE_FLASH = v);
            AnyEvent.addEvent("tmp.flash.max_blur", AnyEvent.type.owner_console, v -> v.createParam(Integer::parseInt, "10", "20"), (p, v) -> BLUR_FLASH = v);
        }

        private static double getAngle(Location location, Location eye) {
            Vector direction = eye.getDirection();
            Vector delta = location.toVector().subtract(eye.toVector());
            return Math.toDegrees(direction.angle(delta));
        }
        private static double getLevel(Player player, Location location, Location eye, double sqrRadius) {
            double b = Math.max(0, Math.min(1, getAngle(location, eye) / 180));
            b = 1 - b;

            //double mutate = b < 0.7 ? MUTATE_FLASH : 1;

            if (!HitUtils.isHitPoint(player.getWorld(), eye.toVector(), location.toVector(), 0.75)) return 0;
            double a = Math.max(0, Math.min(1, eye.distanceSquared(location) / sqrRadius));

            if (b < 0.7) b = (1 - MUTATE_FLASH) * b + MUTATE_FLASH;

            return (1 - a * a) * b;
        }
        @Override public void explosion(Location location) {
            particle.location(location).spawn();
            double sqrRadius = radius * radius;
            location.getNearbyPlayers(radius).forEach(player -> {
                Location eye = player.getEyeLocation();
                double level = getLevel(player, location, eye, sqrRadius);
                if (level <= 0) return;
                tick(player, (int)Math.round(blur * level));
            });
        }
        private void tick(Player player, int blurForce) {
            if (blurForce <= 0) return;
            List<ImageBuilder> images = new ArrayList<>();
            ImageBuilder blur = Bullets.BLUR.withColor(TextColor.color(1f, 1f, 1f));
            for (int i = 0; i < Math.min(BLUR_FLASH, blurForce / 2); i++)
                images.add(blur);
            CustomUI.TitleUI.show(player, images);
            lime.onceTicks(() -> tick(player, blurForce - 1), 1);
        }
    }
    private static class FragmentGrenade implements IGrenade {
        private final double radius;
        private final double damage;

        private static final ParticleBuilder particle = Particle.FLASH.builder()
                .offset(0.5, 0.5, 0.5)
                .count(5)
                .force(false)
                .extra(0);

        public FragmentGrenade(JsonObject json) {
            radius = json.get("radius").getAsDouble();
            damage = json.get("damage").getAsDouble();
        }
        @Override public JObject docs(IDocsLink docs) {
            return JObject.of(
                    JProperty.require(IName.raw("radius"), IJElement.raw(1.0), IComment.text("Радиус взрыва")),
                    JProperty.require(IName.raw("damage"), IJElement.raw(1.0), IComment.text("Урон от взрыва"))
            );
        }

        private static Vector[] getPoints(LivingEntity entity) {
            Vector top = entity.getEyeLocation().toVector();
            Vector bottom = entity.getLocation().toVector();
            return new Vector[] { top, top.getMidpoint(bottom), bottom };
        }
        @Override public void explosion(Location location) {
            particle.location(location).spawn();
            Vector center = location.toVector();

            org.bukkit.World world = location.getWorld();
            double sqrRadius = radius * radius;
            location.getNearbyLivingEntities(radius).forEach(p -> {
                if (!(p instanceof CraftLivingEntity entity)) return;
                double minDistanceSqr = Double.POSITIVE_INFINITY;
                for (Vector point : getPoints(entity))
                    if (HitUtils.isHitPoint(world, center, point, 0.75))
                        minDistanceSqr = Math.min(minDistanceSqr, center.distanceSquared(point));
                if (minDistanceSqr == Double.POSITIVE_INFINITY)
                    return;
                double level = 1 - Math.max(0, Math.min(1, minDistanceSqr / sqrRadius));
                EntityLiving handle = entity.getHandle();

                handle.hurt(handle.damageSources().cactus(), (float)(damage * level));
            });
        }
    }
    private static class SmokeGrenade implements IGrenade {
        private final int time;

        private static final ParticleBuilder particle = Particle.CAMPFIRE_SIGNAL_SMOKE.builder()
                .offset(1, 1, 1)
                .count(50)
                .force(false)
                .extra(0.01);

        public SmokeGrenade(JsonObject json) {
            time = json.get("time").getAsInt();
        }
        @Override public JObject docs(IDocsLink docs) {
            return JObject.of(
                    JProperty.require(IName.raw("time"), IJElement.raw(1), IComment.text("Время задымления в тиках"))
            );
        }

        @Override public void explosion(Location location) {
            tick(location, time);
        }
        private void tick(Location location, int iterator) {
            particle.location(location).spawn();
            if (iterator <= 1) return;
            lime.onceTicks(() -> tick(location, iterator - 1), 1);
        }
    }
    private static class GasGrenade implements IGrenade {
        public static CoreElement create() {
            return CoreElement.create(GasGrenade.class)
                    .withInit(GasGrenade::init);
        }

        private static class GasTimeout extends TimeoutData.ITimeout {
            private final org.bukkit.World world;
            private final int offsetX;
            private final int offsetZ;
            private final Map<Toast2<Integer, Integer>, Double> positions;
            private final GasGrenade grenade;

            public GasTimeout(org.bukkit.World world, int offsetX, int offsetZ, Map<Toast2<Integer, Integer>, Double> positions, GasGrenade grenade) {
                this.world = world;
                this.offsetX = offsetX;
                this.offsetZ = offsetZ;
                this.positions = positions;
                this.grenade = grenade;
            }

            public boolean inZone(Location location, Set<String> tags) {
                if (world != location.getWorld()) return false;
                Double y = this.positions.get(Toast.of(location.getBlockX() - offsetX, location.getBlockZ() - offsetZ));
                if (y == null) return false;
                double min = y - 1;
                double max = y + 2;
                if (min > location.getY() || location.getY() > max) return false;
                for (Set<String> checkTags : this.grenade.tagsNoNeeds) {
                    checkTags = new HashSet<>(checkTags);
                    checkTags.removeAll(tags);
                    if (checkTags.isEmpty()) return false;
                }
                return true;
            }
        }

        private static void init() {
            NeedSystem.register(GasGrenade::getGasNeeds);
        }
        private static Stream<INeedEffect<?>> getGasNeeds(Player player) {
            Location location = player.getLocation();
            Set<String> tags = player.getScoreboardTags();
            return TimeoutData.values(GasTimeout.class)
                    .filter(v -> v.inZone(location, tags))
                    .flatMap(v -> v.grenade.needs.stream());
        }

        private final List<INeedEffect<?>> needs = new ArrayList<>();
        private final int time;
        private final double radius;
        private final List<Set<String>> tagsNoNeeds = new ArrayList<>();

        private final ParticleBuilder particle;

        public GasGrenade(JsonObject json) {
            time = json.get("time").getAsInt();
            radius = json.get("radius").getAsDouble();
            json.getAsJsonArray("needs").forEach(item -> needs.add(INeedEffect.parse(item.getAsJsonObject())));
            if (json.has("tags_no_needs"))
                json.getAsJsonArray("tags_no_needs")
                        .forEach(tag -> tagsNoNeeds.add(Set.of(tag.getAsString().split(","))));
            TextColor color = TextColor.fromHexString(json.get("color").getAsString());
            if (color == null) color = NamedTextColor.WHITE;
            particle = Particle.SPELL_MOB.builder()
                    .offset(color.red() / 255., color.green() / 255., color.blue() / 255.)
                    .force(false)
                    .count(0)
                    .extra(1);
        }
        @Override public JObject docs(IDocsLink docs) {
            return JObject.of(
                    JProperty.require(IName.raw("time"), IJElement.raw(1), IComment.text("Время загозирования в тиках")),
                    JProperty.require(IName.raw("radius"), IJElement.raw(1.5), IComment.text("Радиус загозирования")),
                    JProperty.require(IName.raw("color"), IJElement.raw("#FFFFFF"), IComment.text("Hex цвет")),
                    JProperty.require(IName.raw("needs"), IJElement.anyList(IJElement.link(docs.need())), IComment.text("Добавляемые потребности при нахождении в зоне")),
                    JProperty.optional(IName.raw("tags_no_needs"), IJElement.anyList(IJElement.raw("TAG,TAG,..,TAG")), IComment.text("Список наборов тэгов игрока. При полном совпадении одного из наборов тэгов события из ").append(IComment.field("needs")).append(IComment.text(" не применяются")))
            );
        }
        
        @Override public void explosion(Location location) {
            org.bukkit.World world = location.getWorld();
            int radius = (int)Math.ceil(this.radius);
            int _x = location.getBlockX();
            int _y = location.getBlockY();
            int _z = location.getBlockZ();
            Vector center = location.toVector();
            Map<Toast2<Integer, Integer>, Double> blocks = new HashMap<>();
            double sqrRadius = radius * radius;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Vector point = new Vector(x + _x, _y + 0.25, z + _z);
                    if (point.distanceSquared(center) > sqrRadius)
                        continue;
                    if (!HitUtils.isHitPoint(world, center, point, 0.75))
                        continue;
                    Toast2<Integer, Integer> key = Toast.of(x, z);
                    TargetMove.getOptionalHeight(point.toLocation(world).getBlock())
                            .ifPresent(height -> blocks.put(key, height));
                }
            }
            tick(UUID.randomUUID(), world, _x, _z, blocks, time);
        }
        private void tick(UUID uuid, org.bukkit.World world, int x, int z, Map<Toast2<Integer, Integer>, Double> blocks, int iterator) {
            TimeoutData.put(uuid, GasTimeout.class, new GasTimeout(world, x, z, blocks, this));
            blocks.forEach((point, y) -> {
                Location location = new Location(world, x + point.val0, y, z + point.val1);
                location.add(Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).setY(0));
                particle.location(location).spawn();
            });
            if (iterator <= 1) return;
            lime.onceTicks(() -> tick(uuid, world, x, z, blocks, iterator - 1), 1);
        }
    }

    private final boolean sticky;
    private final int timeThrow;
    private final int timeExplosion;
    private final int timeCooldown;
    private final String soundThrow;
    private final String soundExplosion;
    private final String prefixThrow;
    private final String prefixCooldown;
    private final Boolean shift;
    private final EquipmentSlot arm;
    private final boolean silent;

    private final IGrenade grenade;

    @Override public int getTime() { return timeThrow; }
    @Override public int getCooldown() { return timeCooldown; }
    @Override public String prefix(boolean self) { return prefixThrow; }
    @Override public String cooldownPrefix() { return prefixCooldown; }
    @Override public boolean timeUse(Player player, SelfTarget target, ItemStack item) {
        return player instanceof CraftPlayer handle && throwExecute(handle.getHandle(), item, false, null);
    }
    @Override public boolean silent() { return silent; }
    @Override public EquipmentSlot arm() { return arm; }
    @Override public Optional<SelfTarget> tryCast(Player player, ITarget target, EquipmentSlot arm, boolean shift) {
        return this.shift != null && this.shift != shift
                ? Optional.empty()
                : Optional.of(SelfTarget.Instance);
    }

    private boolean throwExecute(EntityHuman user, ItemStack item, boolean falling, @Nullable Vector offset) {
        World world = user.level();
        EntitySnowball entitysnowball = new EntitySnowball(world, user);
        entitysnowball.addTag("grenade");
        if (falling)
            entitysnowball.addTag("falling");
        entitysnowball.setItem(CraftItemStack.asNMSCopy(item));
        if (falling) {
            if (offset != null)
                entitysnowball.moveTo(offset.getX(), offset.getY(), offset.getZ());
            entitysnowball.shootFromRotation(user, 0, 0, 0.0F, 0, 0);
        } else {
            entitysnowball.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, 1.5F, 1.0F);
        }
        PlayerLaunchProjectileEvent event = new PlayerLaunchProjectileEvent((Player)user.getBukkitEntity(), item, (Projectile)entitysnowball.getBukkitEntity());
        if (!event.callEvent() || !world.addFreshEntity(entitysnowball))
            return false;
        if (!falling)
            Sounds.playSound(soundThrow, CraftLocation.toBukkit(user.getEyePosition(), world.getWorld(), user.getYRot(), user.getXRot()));
        return true;
    }
    private void hit(Location location) {
        lime.onceTicks(() -> {
            Sounds.playSound(soundExplosion, location);
            grenade.explosion(location);
        }, timeExplosion);
    }

    public GrenadeSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);

        JsonObjectOptional optJson = JsonObjectOptional.of(json);

        sticky = json.has("sticky") && json.get("sticky").getAsBoolean();

        JsonObject time = json.getAsJsonObject("time");
        timeThrow = time.get("throw").getAsInt();
        timeCooldown = time.get("cooldown").getAsInt();
        timeExplosion = time.get("explosion").getAsInt();

        Optional<JsonObjectOptional> prefix = optJson.getAsJsonObject("prefix");
        prefixThrow = prefix.flatMap(v -> v.getAsString("throw")).orElse("");
        prefixCooldown = prefix.flatMap(v -> v.getAsString("cooldown")).orElse("");

        Optional<JsonObjectOptional> sound = optJson.getAsJsonObject("sound");
        soundThrow = sound.flatMap(v -> v.getAsString("throw")).orElse(null);
        soundExplosion = sound.flatMap(v -> v.getAsString("explosion")).orElse(null);

        shift = optJson.getAsBoolean("shift").orElse(null);
        arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
        silent = optJson.getAsBoolean("silent").orElse(false);

        JsonObject grenade = json.getAsJsonObject("grenade");
        GrenadeType type = GrenadeType.valueOf(grenade.get("type").getAsString());
        this.grenade = type.create(grenade);
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        List<JObject> grenadeTypes = Arrays.stream(GrenadeType.values()).map(v -> v.docs(docs)).toList();
        IIndexGroup grenadeData = JsonGroup.of("GrenadeData", index + ".grenadeData", IJElement.or(grenadeTypes));
        return JsonGroup.of(index, JObject.of(
                        JProperty.optional(IName.raw("sticky"), IJElement.bool(), IComment.text("Устанавливает возможность прилипать к поверхности")),
                        JProperty.require(IName.raw("time"), JObject.of(
                                JProperty.require(IName.raw("throw"), IJElement.raw(10), IComment.text("Время активации бросания (использования) в тиках")),
                                JProperty.require(IName.raw("cooldown"), IJElement.raw(10), IComment.text("Время отката использования в тиках")),
                                JProperty.require(IName.raw("explosion"), IJElement.raw(10), IComment.text("Время после попадания перед срабатыванием в тиках"))
                        )),
                        JProperty.optional(IName.raw("sound"), JObject.of(
                                JProperty.optional(IName.raw("throw"), IJElement.link(docs.sound()), IComment.text("Звук бросания")),
                                JProperty.optional(IName.raw("explosion"), IJElement.link(docs.sound()), IComment.text("Звук срабатывания"))
                        )),
                        JProperty.optional(IName.raw("prefix"), JObject.of(
                                JProperty.optional(IName.raw("throw"), IJElement.raw("PREFIX TEXT"), IComment.text("Отображаемый префикс таймера использования")),
                                JProperty.optional(IName.raw("cooldown"), IJElement.raw("PREFIX TEXT"), IComment.text("Отображаемый префикс таймера отката"))
                        )),
                        JProperty.optional(IName.raw("shift"), IJElement.bool(), IComment.empty()
                                .append(IComment.text("Требуется ли нажимать "))
                                .append(IComment.raw("SHIFT"))
                                .append(IComment.text(" для использования. Если не указано - проверка на нажатие отсуствует"))),
                        JProperty.require(IName.raw("arm"), IJElement.link(docs.handType()), IComment.text("Тип руки, в которой происходит взаимодействие")),
                        JProperty.optional(IName.raw("silent"), IJElement.bool(), IComment.text("Будет ли проигран звук ломания предмета")),

                        JProperty.optional(IName.raw("grenade"), IJElement.link(grenadeData), IComment.text("Данные гранаты, которые произойдут после срабатывания"))
                ), IComment.text("Граната. После использования возможен вызов ").append(IComment.link(docs.settingsLink(NextSetting.class))))
                .withChild(grenadeData);
    }
}

















