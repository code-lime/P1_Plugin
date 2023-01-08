package org.lime.gp.item;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.lime;
import org.lime.gp.player.module.Death;
import org.lime.gp.player.module.HandCuffs;
import org.lime.gp.player.ui.CustomUI;
import org.lime.system;

import java.util.Optional;

public class UseSetting implements Listener {
    public static core.element create() {
        return core.element.create(UseSetting.class)
                .withInstance();
    }
    public interface IUse extends Settings.IItemSetting {
        boolean use(Player player, Player target, EquipmentSlot arm, boolean shift);
        EquipmentSlot arm();
    }
    public static String timerMs(int ticks) {
        int totalMs = ticks * 50;
        int ms = totalMs % 1000;
        int sec = totalMs / 1000;
        return StringUtils.leftPad(String.valueOf(sec), 2, '0') + "." + StringUtils.leftPad(String.valueOf(ms), 3, '0');
    }
    private static boolean isDistance(Location first, Location second, double distance) {
        return first.getWorld() == second.getWorld() && first.distance(second) < distance;
    }
    public interface ITimeUse extends IUse {
        int getTime();
        void timeUse(Player player, Player target, ItemStack item);

        private boolean useTick(Player player, Player target, EquipmentSlot arm, Integer ticks) {
            return !Death.isDamageLay(player.getUniqueId())
                    && !HandCuffs.isMove(player.getUniqueId())
                    && Optional.of(player.getInventory().getItem(arm))
                            .flatMap(item -> Items.getOptional(getClass(), item)
                                    .filter(v -> v == this)
                                    .map(setting -> {
                                        int _ticks = ticks == null ? setting.getTime() : ticks;
                                        if (_ticks <= 0) {
                                            timeUse(player, target, item);
                                            Items.getOptional(Settings.NextSetting.class, item)
                                                    .flatMap(v -> Items.createItem(v.next))
                                                    .ifPresent(v -> Items.dropGiveItem(player, v, false));
                                            item.subtract(1);
                                            CustomUI.TextUI.hide(player);
                                            return true;
                                        }
                                        Location l1 = player.getLocation().clone();
                                        Location l2 = target.getLocation().clone();
                                        CustomUI.TextUI.show(player, timerMs(_ticks));
                                        Cooldown.setCooldown(player.getUniqueId(), "use_item", 1);
                                        lime.onceTicks(() -> {
                                            if (!item.isSimilar(player.getInventory().getItemInMainHand())) return;
                                            if (!isDistance(l1, player.getLocation(), 0.1) || !isDistance(l2, target.getLocation(), 0.1)) return;
                                            useTick(player, target, arm, _ticks - 1);
                                        }, 1);
                                        return true;
                                    })
                            )
                            .orElse(false);
        }
        @Override default boolean use(Player player, Player target, EquipmentSlot arm, boolean shift) {
            if (!shift) return false;
            if (Cooldown.hasCooldown(player.getUniqueId(), "use_item")) return false;
            return useTick(player, target, arm, null);
        }
    }

    private static final EquipmentSlot[] HANDS = new EquipmentSlot[] { EquipmentSlot.HAND, EquipmentSlot.OFF_HAND };
    private static boolean onUse(Player player, Player target) {
        boolean shift = player.isSneaking();
        for (EquipmentSlot slot : HANDS) {
            if (onUse(player, slot, target, shift))
                return true;
        }
        return false;
    }
    private static boolean onUse(Player player, EquipmentSlot arm, Player target, boolean shift) {
        ItemStack item = player.getInventory().getItem(arm);
        return Items.getAll(IUse.class, item)
                .stream()
                .filter(use -> use.arm().equals(arm))
                .map(use -> use.use(player, target, arm, shift))
                .findAny()
                .orElse(false);
    }
    @EventHandler public static void on(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Player target)) return;
        if (onUse(e.getPlayer(), target)) e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        switch (e.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK: break;
            default: return;
        }
        if (onUse(e.getPlayer(), e.getPlayer())) e.setCancelled(true);
    }

    public static void timeUse(Player player, Player target, int ticks, system.Func2<Player, Player, Boolean> check, system.Action2<Player, Player> execute, system.Action2<Player, Player> cancel) {
        timeUse(player, target, 0.1, ticks, check, execute, cancel);
    }
    public static void timeUse(Player player, Player target, double distance, int ticks, system.Func2<Player, Player, Boolean> check, system.Action2<Player, Player> execute, system.Action2<Player, Player> cancel) {
        if (Cooldown.hasCooldown(player.getUniqueId(), "use_item")) return;
        tickUse(player, target, distance, ticks, check, execute, cancel);
    }
    private static void tickUse(Player player, Player target, double distance, int ticks, system.Func2<Player, Player, Boolean> check, system.Action2<Player, Player> execute, system.Action2<Player, Player> cancel) {
        if (!player.isOnline() || !target.isOnline() || !check.invoke(player, target)) {
            cancel.invoke(player, target);
            return;
        }
        if (ticks <= 0) {
            execute.invoke(player, target);
            CustomUI.TextUI.hide(player);
            return;
        }
        Location l1 = player.getLocation().clone();
        Location l2 = target.getLocation().clone();
        CustomUI.TextUI.show(player, timerMs(ticks));
        Cooldown.setCooldown(player.getUniqueId(), "use_item", 1);
        lime.onceTicks(() -> {
            if (!isDistance(l1, player.getLocation(), distance) || !isDistance(l2, target.getLocation(), distance)) return;
            tickUse(player, target, distance, ticks - 1, check, execute, cancel);
        }, 1);
    }
}





