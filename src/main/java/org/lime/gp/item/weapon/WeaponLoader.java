package org.lime.gp.item.weapon;

import net.minecraft.world.inventory.ContainerChest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.module.Death;
import org.lime.gp.player.module.HandCuffs;
import org.lime.gp.player.module.Knock;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.sound.Sounds;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WeaponLoader implements Listener {
    public static CoreElement create() {
        return CoreElement.create(WeaponLoader.class)
                .withInstance()
                .withInit(WeaponLoader::init);
    }
    public static void init() {
        CustomUI.addListener(new WeaponUI());
        List<UUID> last_click = new ArrayList<>();
        final PotionEffect HIP = new PotionEffect(PotionEffectType.SLOW, 5, 2, false, false, false);
        final PotionEffect ELBOW = new PotionEffect(PotionEffectType.SLOW, 5, 4, false, false, false);
        lime.repeatTicks(() -> {
            List<UUID> _last_click = new ArrayList<>();
            long now = System.currentTimeMillis();
            click.entrySet().removeIf(kv -> kv.getValue() < now);
            data.entrySet().removeIf(kv -> {
                Player player = EntityPosition.onlinePlayers.getOrDefault(kv.getKey(), null);
                if (player == null) return true;
                WeaponData data = kv.getValue();
                if (!player.getInventory().getItemInMainHand().isSimilar(data.item)) return true;
                data.sync(data.weapon, data.item);
                return false;
            });
            Bukkit.getOnlinePlayers().forEach(player -> {
                UUID uuid = player.getUniqueId();
                PlayerInventory inventory = player.getInventory();
                CraftItemStack offhand = (CraftItemStack)inventory.getItemInOffHand();
                CraftItemStack mainhand = (CraftItemStack)inventory.getItemInMainHand();
                WeaponData _data = data.getOrDefault(uuid, null);
                if (_data != null) {
                    Long last_click_time = click.getOrDefault(uuid, null);
                    if (last_click_time != null) {
                        _last_click.add(uuid);
                        switch (_data.state) {
                            case X1:
                                if (!last_click.contains(uuid) && !Cooldown.hasCooldown(uuid, _data.cooldownKey)) {
                                    if (_data.shoot(player, mainhand))
                                        Cooldown.setCooldown(uuid, _data.cooldownKey, _data.weapon.tick_speed / 20.0);
                                }
                                break;
                            case X3:
                                if (!last_click.contains(uuid) && !Cooldown.hasCooldown(uuid, _data.cooldownKey)) {
                                    boolean apply_cooldown = _data.shoot(player, mainhand, 0);
                                    for (int i = 1; i < 3; i++) _data.shoot(player, mainhand, i * _data.weapon.tick_speed);
                                    if (apply_cooldown) Cooldown.setCooldown(uuid, _data.cooldownKey, 3 * _data.weapon.tick_speed / 20.0);
                                }
                                break;
                            case AUTO:
                                if (!Cooldown.hasCooldown(uuid, _data.cooldownKey)) {
                                    if (_data.shoot(player, mainhand))
                                        Cooldown.setCooldown(uuid, _data.cooldownKey, _data.weapon.tick_speed / 20.0);
                                }
                                break;
                        }
                    } else {
                        _data.recoil = 0;
                    }
                    switch (_data.pose) {
                        case Hip:
                            player.addPotionEffect(HIP);
                            break;
                        case Elbow:
                            player.addPotionEffect(ELBOW);
                            break;
                        default:
                            break;
                    }
                    if (offhand.getType().isAir()) return;
                    inventory.setItemInOffHand(new ItemStack(Material.AIR));
                    Items.dropGiveItem(player, offhand, false);
                    return;
                }
                if (Items.has(WeaponSetting.class, offhand)) {
                    inventory.setItemInOffHand(new ItemStack(Material.AIR));
                    Items.dropGiveItem(player, offhand, false);
                }
                Items.getOptional(WeaponSetting.class, mainhand)
                        .ifPresent(weapon -> data.put(uuid, new WeaponData(weapon, mainhand)));
            });
            last_click.clear();
            last_click.addAll(_last_click);
        }, 1);
    }

    public static final ConcurrentHashMap<UUID, WeaponData> data = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> click = new ConcurrentHashMap<>();

    @EventHandler public static void on(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (e.useItemInHand() == Event.Result.DENY || !(e.getItem() instanceof CraftItemStack item) || Death.isDamageLay(uuid) || HandCuffs.isMove(uuid) || Knock.isKnock(uuid)) return;
        Items.getOptional(WeaponSetting.class, item).ifPresent(weapon -> {
            WeaponData data = WeaponLoader.data.getOrDefault(uuid, null);
            if (data == null) return;
            if (!data.item.isSimilar(item)) return;
            if (e.getAction().isRightClick()) {
                if (lime.isLay(player)) return;
                click.put(player.getUniqueId(), System.currentTimeMillis() + 260);
            } else if (e.getAction().isLeftClick() && player.isSneaking()) {
                WeaponData.State _state = data.state.next(weapon);
                if (data.state.equals(_state)) return;
                data.state = _state;
                data.update(weapon, item);
                Sounds.playSound(weapon.sound_state, player.getLocation());
            }
            e.setCancelled(true);
        });
    }
    @EventHandler public static void on(PlayerSwapHandItemsEvent e) {
        if (!(e.getOffHandItem() instanceof CraftItemStack offhand)) return;
        Items.getOptional(WeaponSetting.class, offhand).ifPresent(weapon -> {
            Player player = e.getPlayer();
            UUID uuid = player.getUniqueId();
            WeaponData data = WeaponLoader.data.getOrDefault(uuid, null);
            if (data == null) return;
            if (!data.item.isSimilar(offhand)) return;
            ItemStack mainhand = e.getMainHandItem();
            e.setMainHandItem(offhand);
            e.setOffHandItem(mainhand);
            data.pose = data.pose.next(weapon);
            data.update(weapon, offhand);
            Sounds.playSound(weapon.sound_pose, player);
        });
    }
    @EventHandler public static void on(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (e.getView() instanceof CraftInventoryView view
                && view.getHandle() instanceof ContainerChest containerChest
                && containerChest.getContainer() instanceof ReadonlyInventory) return;
        switch (e.getClick()) {
            case DROP -> {
                ItemStack item = e.getCurrentItem();
                if (item == null) return;
                Items.getOptional(WeaponSetting.class, item)
                        .ifPresent(weapon -> WeaponSetting.getMagazine(item)
                                .map(_v -> Toast.of(weapon, _v))
                                .ifPresent(vv -> {
                                    Items.dropGiveItem(player, vv.val1, false);
                                    WeaponSetting.setMagazine(item, null);
                                    WeaponLoader.data.remove(player.getUniqueId());
                                    Sounds.playSound(vv.val0.sound_unload, player);
                                    WeaponData.updateSync(player, weapon, item);
                                    e.setCancelled(true);
                                })
                        );
                Items.getOptional(MagazineSetting.class, item)
                        .flatMap(v -> MagazineSetting.getBullets(item).map(_v -> Toast.of(v, _v)))
                        .ifPresent(vv -> {
                            List<ItemStack> bullets = vv.val1;
                            int length = bullets.size();
                            if (length == 0) return;
                            ItemStack bullet = bullets.remove(length - 1);
                            Items.dropGiveItem(player, bullet, false);
                            MagazineSetting.setBullets(item, bullets);
                            WeaponLoader.data.remove(player.getUniqueId());
                            Sounds.playSound(vv.val0.sound_unload, player);
                            e.setCancelled(true);
                        });
            }
            case RIGHT, LEFT -> {
                ItemStack cursor = e.getCursor();
                ItemStack item = e.getCurrentItem();
                if (cursor == null || item == null) return;
                Items.getOptional(MagazineSetting.class, item)
                        .filter(v -> Items.getOptional(BulletSetting.class, cursor).filter(_v -> _v.bullet_type.equals(v.bullet_type)).isPresent())
                        .ifPresent(magazine_setting -> {
                            List<ItemStack> items = MagazineSetting.getBullets(item).orElseGet(ArrayList::new);
                            if (items.size() >= magazine_setting.size) return;
                            e.setCancelled(true);
                            if (Cooldown.hasCooldown(player.getUniqueId(), "weapon.load")) return;
                            Cooldown.setCooldown(player.getUniqueId(), "weapon.load", 0.5);
                            ItemStack bullet = cursor.asOne();
                            cursor.subtract(1);
                            items.add(bullet);
                            MagazineSetting.setBullets(item, items);
                            WeaponLoader.data.remove(player.getUniqueId());
                            Sounds.playSound(magazine_setting.sound_load, (Player) e.getWhoClicked());
                            e.setCancelled(true);
                        });
                Items.getOptional(WeaponSetting.class, item)
                        .filter(v -> Items.getOptional(MagazineSetting.class, cursor).filter(_v -> _v.magazine_type != null).filter(_v -> _v.magazine_type.equals(v.magazine_type)).isPresent())
                        .ifPresent(weapon -> {
                            if (WeaponSetting.getMagazine(item).isPresent()) return;
                            ItemStack magazine = cursor.asOne();
                            cursor.subtract(1);
                            WeaponSetting.setMagazine(item, magazine);
                            WeaponLoader.data.remove(player.getUniqueId());
                            Sounds.playSound(weapon.sound_load, (Player) e.getWhoClicked());
                            WeaponData.updateSync(player, weapon, item);
                            e.setCancelled(true);
                        });
            }
        }
    }
}
