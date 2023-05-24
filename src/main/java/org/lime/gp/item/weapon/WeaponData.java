package org.lime.gp.item.weapon;

import com.google.gson.JsonPrimitive;
import net.minecraft.world.entity.EnumItemSlot;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.display.Displays;
import org.lime.display.transform.LocalLocation;
import org.lime.display.transform.Transform;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.BulletSetting;
import org.lime.gp.item.settings.list.MagazineSetting;
import org.lime.gp.item.settings.list.WeaponSetting;
import org.lime.gp.lime;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.gp.sound.Sounds;
import org.lime.system;

import java.util.*;

public class WeaponData {
    private static boolean WEAPON_SIT_LOCK = true;

    public static core.element create() {
        return core.element.create(WeaponData.class)
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("weapon_sit")
                        .withDefault(new JsonPrimitive(WEAPON_SIT_LOCK))
                        .withInvoke(_v -> WEAPON_SIT_LOCK = _v.getAsBoolean())
                );
    }

    public enum State {
        AUTO,
        X1,
        X3;

        public State next() {
            State[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public State next(WeaponSetting weapon) {
            State state = this;
            do state = state.next();
            while (!weapon.states.contains(state) && this != state);
            return state;
        }

        public static State def(WeaponSetting weapon) {
            for (State state : values()) {
                if (weapon.states.contains(state))
                    return state;
            }
            return State.AUTO;
        }
    }
    public enum Pose {
        None, //Нет прицела
        Hip, //От бедра
        Elbow; //От локтя

        public Pose next() {
            Pose[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public Pose next(WeaponSetting weapon) {
            Pose pose = this;
            do pose = pose.next();
            while (!weapon.poses.containsKey(pose) && this != pose);
            return pose;
        }

        public static Pose def(WeaponSetting weapon) {
            for (Pose pose : values()) {
                if (weapon.poses.containsKey(pose))
                    return pose;
            }
            return Pose.None;
        }
    }

    public static final NamespacedKey STATE_KEY = new NamespacedKey(lime._plugin, "state");

    public State state;
    public Pose pose;
    public WeaponSetting weapon;
    public CraftItemStack item;
    public double recoil = 0;
    public int ammo;
    public long waitInitTime;

    public final String cooldownKey;

    public WeaponData(WeaponSetting weapon, CraftItemStack item) {
        this.waitInitTime = System.currentTimeMillis() + (int)(weapon.init_sec * 1000);
        this.weapon = weapon;
        this.state = Optional.of(item)
                .map(ItemStack::getItemMeta)
                .map(PersistentDataHolder::getPersistentDataContainer)
                .map(v -> v.get(STATE_KEY, PersistentDataType.STRING))
                .flatMap(v -> system.tryParse(State.class, v))
                .filter(weapon.states::contains)
                .orElseGet(() -> State.def(weapon));
        this.pose = Pose.def(weapon);
        this.item = item;
        this.cooldownKey = "weapon.shoot." + weapon.creator().getKey();

        this.ammo = WeaponSetting.getMagazine(item)
                .or(() -> Items.has(MagazineSetting.class, item) ? Optional.of(item) : Optional.empty())
                .flatMap(MagazineSetting::getBullets)
                .map(List::size)
                .orElse(0);
    }

    public static String timerMs(long totalMs) {
        long ms = (totalMs % 1000) / 100;
        long sec = totalMs / 1000;
        return StringUtils.leftPad(String.valueOf(sec), 2, '0') + "." + StringUtils.leftPad(String.valueOf(ms), 1, '0');
    }
    public void update(WeaponSetting weapon, CraftItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(weapon.weaponDisplay(this.pose, WeaponSetting.getMagazine(item).map(ItemStack::getItemMeta).filter(ItemMeta::hasCustomModelData).map(ItemMeta::getCustomModelData).orElse(null), ammo));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(STATE_KEY, PersistentDataType.STRING, state.name());
        item.setItemMeta(meta);
        this.item = item;
    }
    public void sync(WeaponSetting weapon, CraftItemStack item) {
        WeaponSetting.Pose pose = weapon.poses.getOrDefault(this.pose, null);
        if (pose == null) return;
        ItemMeta meta = item.getItemMeta();
        int custom_model_data = weapon.weaponDisplay(this.pose, WeaponSetting.getMagazine(item).map(ItemStack::getItemMeta).filter(ItemMeta::hasCustomModelData).map(ItemMeta::getCustomModelData).orElse(null), ammo);
        if (meta.hasCustomModelData() && custom_model_data == meta.getCustomModelData()) return;
        meta.setCustomModelData(custom_model_data);
        item.setItemMeta(meta);
        this.item = item;
    }
    public String toData() {
        return state + " / " + pose;
    }
    public List<ImageBuilder> getUI(Player player) {
        List<ImageBuilder> images = new ArrayList<>();

        Optional.ofNullable(weapon.ui.get(state))
                .map(v -> v.of(ammo, Cooldown.getCooldown(player.getUniqueId(), cooldownKey)))
                .ifPresent(images::add);

        if (WEAPON_SIT_LOCK && (player.isInsideVehicle() || Displays.hasVehicle(player.getEntityId()))) {
            waitInitTime = System.currentTimeMillis() + (int)(weapon.init_sec * 1000);
            images.add(ImageBuilder.of(player, "Невозможно стрелять сидя"));
        } else {
            long delta = waitInitTime - System.currentTimeMillis();
            if (delta > 0) images.add(ImageBuilder.of(player, timerMs(delta)));
        }

        return images;
    }
    public boolean shoot(Player shooter, CraftItemStack main_hand) {
        return shoot(shooter, main_hand, 0);
    }
    private boolean spawnShoot(Player shooter, BulletSetting bullet, WeaponSetting.Pose pose, Vector offset, int wait_ticks) {
        if (wait_ticks == 0) return spawnShoot(shooter, bullet, pose, offset);
        else lime.onceTicks(() -> spawnShoot(shooter, bullet, pose, offset), wait_ticks);
        return true;
    }
    
    @SuppressWarnings("deprecation")
    private boolean spawnShoot(Player shooter, BulletSetting bullet, WeaponSetting.Pose pose, Vector offset) {
        Location location = shooter.getEyeLocation();
        location = Transform.toWorld(location, new LocalLocation(offset.multiply(new Vector(switch (shooter.getMainHand()) {
            case RIGHT -> -1;
            case LEFT -> 1;
        }, 1, 1))));

        recoil = shooter.isFlying() || shooter.isSwimming() || !shooter.isOnGround() ? weapon.recoil : Math.min(weapon.recoil, recoil + weapon.recoil_speed);
        location.setPitch((float) (location.getPitch() - recoil));

        for (int i = 0; i < bullet.count; i++) {
            Location bullet_location = location.clone();

            double split = pose == null ? 2.5 : pose.range(recoil, weapon.recoil);
            bullet_location.setYaw(bullet_location.getYaw() + (float) system.rand(-split, split));
            bullet_location.setPitch(bullet_location.getPitch() + (float) system.rand(-split, split));

            bullet_location.add(bullet_location.getDirection().multiply(0.5)).add(new Vector(0, -0.5, 0));

            Bullets.spawnBullet(shooter, bullet_location, weapon.bullet_speed, weapon.bullet_down, weapon.damage_scale * bullet.damage, bullet.creator().getID());
        }
        Sounds.playSound(weapon.sound_shoot, location);
        return true;
    }
    public boolean shoot(Player shooter, CraftItemStack main_hand, int wait_ticks) {
        item = main_hand;
        if (waitInitTime - System.currentTimeMillis() > 0) return false;
        return WeaponSetting.getMagazine(item)
                .or(() -> Items.has(MagazineSetting.class, item) ? Optional.of(item) : Optional.empty())
                .map(magazine -> {
                    List<ItemStack> bullets = MagazineSetting.getBullets(magazine).orElseGet(ArrayList::new);
                    int length = bullets.size();
                    if (length == 0) return null;
                    ItemStack bullet = bullets.remove(length - 1);
                    MagazineSetting.setBullets(magazine, bullets);
                    ammo = length - 1;
                    WeaponSetting.setMagazine(item, magazine == item ? null : magazine);
                    item.handle.hurtAndBreak(1, ((CraftPlayer)shooter).getHandle(), e2 -> e2.broadcastBreakEvent(EnumItemSlot.MAINHAND));
                    return bullet;
                })
                .flatMap(v -> Items.getOptional(BulletSetting.class, v))
                .map(bullet -> spawnShoot(shooter, bullet, weapon.poses.getOrDefault(this.pose, null), weapon.poses.get(pose).offset.clone(), wait_ticks))
                .orElse(false);
    }

    public static void updateSync(Player player, WeaponSetting weapon, ItemStack item) {
        if (WeaponLoader.data.containsKey(player.getUniqueId())) return;
        ItemMeta meta = item.getItemMeta();
        Optional<ItemStack> magazine = WeaponSetting.getMagazine(item);
        int custom_model_data = weapon.weaponDisplay(Pose.def(weapon), magazine.map(ItemStack::getItemMeta).filter(ItemMeta::hasCustomModelData).map(ItemMeta::getCustomModelData).orElse(null), magazine.flatMap(MagazineSetting::getBullets).map(List::size).orElse(0));
        if (meta.hasCustomModelData() && custom_model_data == meta.getCustomModelData()) return;
        meta.setCustomModelData(custom_model_data);
        item.setItemMeta(meta);
    }
}













