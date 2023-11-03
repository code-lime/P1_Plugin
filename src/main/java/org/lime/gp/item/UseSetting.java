package org.lime.gp.item;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EnumItemSlot;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.item.settings.IItemSetting;
import org.lime.gp.item.settings.list.NextSetting;
import org.lime.gp.lime;
import org.lime.gp.player.module.Death;
import org.lime.gp.player.module.HandCuffs;
import org.lime.gp.player.ui.CustomUI;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action2;
import org.lime.system.execute.Func2;

import java.util.HashSet;
import java.util.Optional;

public class UseSetting implements Listener {
    public static CoreElement create() {
        return CoreElement.create(UseSetting.class)
                .withInstance()
                .withInit(UseSetting::init);
    }
    private static void init() {
        lime.repeat(UseSetting::updateSec, 1);
        lime.repeatTicks(UseSetting::updateTick, 1);
    }

    private static final EquipmentSlot[] HANDS = new EquipmentSlot[] { EquipmentSlot.HAND, EquipmentSlot.OFF_HAND };
    private static void updateSec() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (cachePlayers.contains(player)) return;
            for (EquipmentSlot slot : HANDS) {
                ItemStack item = player.getInventory().getItem(slot);
                if (Items.getAll(ITimeUse.class, item).stream().anyMatch(use -> use.arm().equals(slot)))
                    cachePlayers.add(player);
            }
        });
    }
    private static final HashSet<Player> cachePlayers = new HashSet<>();
    private static void updateTick() {
        cachePlayers.removeIf(v -> !onCooldownTick(v));
    }
    private static boolean onCooldownTick(Player player) {
        for (EquipmentSlot slot : HANDS) {
            if (onCooldownTick(player, slot))
                return true;
        }
        return false;
    }
    private static boolean onCooldownTick(Player player, EquipmentSlot arm) {
        ItemStack item = player.getInventory().getItem(arm);
        return Items.getAll(ITimeUse.class, item)
                .stream()
                .filter(use -> use.arm().equals(arm))
                .map(use -> use.cooldownTick(player, arm))
                .findAny()
                .orElse(false);
    }

    public interface IUse extends IItemSetting {
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
        int getCooldown();
        String prefix(boolean self);
        String cooldownPrefix();
        void timeUse(Player player, Player target, ItemStack item);

        default String useCooldownKey() {
            return creator().getKey() + "#use_item";
        }

        private boolean cooldownTick(Player player, EquipmentSlot arm) {
            return !Death.isDamageLay(player.getUniqueId())
                    && !HandCuffs.isMove(player.getUniqueId())
                    && Optional.of(player.getInventory().getItem(arm))
                    .map(v -> (CraftItemStack)v)
                    .flatMap(item -> Items.getOptional(getClass(), item)
                            .filter(v -> v == this)
                            .map(setting -> {
                                double sec = Cooldown.getCooldown(player.getUniqueId(), useCooldownKey());
                                if (sec <= 0) return false;
                                CustomUI.TextUI.show(player, cooldownPrefix() + timerMs((int)Math.floor(sec * 20)));
                                return true;
                            })
                    )
                    .orElse(false);
        }
        private boolean useTick(CraftPlayer player, CraftPlayer target, Location playerLocation, Location targetLocation, EquipmentSlot arm, Integer ticks) {
            return !Death.isDamageLay(player.getUniqueId())
                    && !HandCuffs.isMove(player.getUniqueId())
                    && Optional.of(player.getInventory().getItem(arm))
                            .map(v -> (CraftItemStack)v)
                            .flatMap(item -> Items.getOptional(getClass(), item)
                                    .filter(v -> v == this)
                                    .map(setting -> {
                                        int _ticks = ticks == null ? setting.getTime() : ticks;
                                        if (_ticks <= 0) {
                                            timeUse(player, target, item);
                                            modifyUseItem(player, item);
                                            int cooldown = getCooldown();
                                            if (cooldown > 0)
                                                Cooldown.setCooldown(player.getUniqueId(), useCooldownKey(), cooldown / 20.0);

                                            CustomUI.TextUI.hide(player);
                                            return true;
                                        }
                                        CustomUI.TextUI.show(player, prefix(player == target) + timerMs(_ticks));
                                        Cooldown.setCooldown(player.getUniqueId(), "use_item", 1);
                                        lime.onceTicks(() -> {
                                            if (!item.isSimilar(player.getInventory().getItemInMainHand())) return;
                                            if (!isDistance(playerLocation, player.getLocation(), 1) || !isDistance(targetLocation, target.getLocation(), 1)) return;
                                            useTick(player, target, playerLocation, targetLocation, arm, _ticks - 1);
                                        }, 1);
                                        return true;
                                    })
                            )
                            .orElse(false);
        }
        @Override default boolean use(Player player, Player target, EquipmentSlot arm, boolean shift) {
            //if (!shift) return false;
            if (!(player instanceof CraftPlayer cplayer) || !(target instanceof CraftPlayer ctarget)) return false;
            if (Cooldown.hasCooldown(player.getUniqueId(), "use_item")) return false;
            if (Cooldown.hasCooldown(player.getUniqueId(), useCooldownKey())) return false;
            return useTick(cplayer, ctarget, player.getLocation(), target.getLocation(), arm, null);
        }
    }

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

    public static void timeUse(Player player, Player target, int ticks, Func2<Player, Player, Boolean> check, Action2<Player, Player> execute, Action2<Player, Player> cancel) {
        timeUse(player, target, 0.1, ticks, check, execute, cancel);
    }
    public static void timeUse(Player player, Player target, double distance, int ticks, Func2<Player, Player, Boolean> check, Action2<Player, Player> execute, Action2<Player, Player> cancel) {
        if (Cooldown.hasCooldown(player.getUniqueId(), "use_item")) return;
        tickUse(player, target, distance, ticks, check, execute, cancel);
    }
    private static void tickUse(Player player, Player target, double distance, int ticks, Func2<Player, Player, Boolean> check, Action2<Player, Player> execute, Action2<Player, Player> cancel) {
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

    public static void modifyUseItem(CraftPlayer player, CraftItemStack item) {
        if (item.handle == null) return;
        modifyUseItem(player.getHandle(), item.handle);
    }
    public static void modifyUseItem(EntityPlayer player, net.minecraft.world.item.ItemStack item) {
        if (player.level().isClientSide || player.getAbilities().instabuild) return;
        Optional<NextSetting> nextOption = Items.getOptional(NextSetting.class, item);
        if (item.isDamageableItem()) {
            item.hurtAndBreak(1, player, e2 -> {
                nextOption
                    .flatMap(NextSetting::next)
                    .map(IItemCreator::createItem)
                    .ifPresentOrElse(
                        v -> Items.dropGiveItem(e2.getBukkitEntity(), v, false),
                        () -> e2.broadcastBreakEvent(EnumItemSlot.MAINHAND)
                    );
            });
        } else {
            item.shrink(1);
            nextOption
                .flatMap(NextSetting::next)
                .map(IItemCreator::createItem)
                .ifPresentOrElse(
                    v -> Items.dropGiveItem(player.getBukkitEntity(), v, false),
                    () -> player.broadcastBreakEvent(EnumItemSlot.MAINHAND)
                );
        }
    }
}





