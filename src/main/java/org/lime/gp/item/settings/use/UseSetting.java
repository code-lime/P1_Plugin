package org.lime.gp.item.settings.use;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EnumItemSlot;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
import org.lime.gp.item.Items;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.item.settings.list.NextSetting;
import org.lime.gp.item.settings.use.target.BlockTarget;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.item.settings.use.target.PlayerTarget;
import org.lime.gp.item.settings.use.target.SelfTarget;
import org.lime.gp.lime;
import org.lime.gp.player.ui.CustomUI;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action2;
import org.lime.system.execute.Execute;
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

    public static String timerMs(int ticks) {
        int totalMs = ticks * 50;
        int ms = totalMs % 1000;
        int sec = totalMs / 1000;
        return StringUtils.leftPad(String.valueOf(sec), 2, '0') + "." + StringUtils.leftPad(String.valueOf(ms), 3, '0');
    }
    public static boolean isDistance(Location first, Location second, double distance) {
        return first.getWorld() == second.getWorld() && first.distance(second) < distance;
    }

    private static boolean onUse(Player player, ITarget target) {
        boolean shift = player.isSneaking();
        for (EquipmentSlot slot : HANDS) {
            if (onUse(player, slot, target, shift))
                return true;
        }
        return false;
    }
    private static boolean onUse(Player player, EquipmentSlot arm, ITarget target, boolean shift) {
        ItemStack item = player.getInventory().getItem(arm);
        return Items.getAll(IUse.class, item)
                .stream()
                .filter(use -> use.arm().equals(arm))
                .anyMatch(use -> use.tryUse(player, target, arm, shift));
    }
    @EventHandler public static void on(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Player target)) return;
        if (onUse(e.getPlayer(), new PlayerTarget(target))) e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        switch (e.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK: break;
            default: return;
        }
        Block block = e.getClickedBlock();
        ITarget target = block == null ? SelfTarget.Instance : new BlockTarget(block);
        if (onUse(e.getPlayer(), target)) e.setCancelled(true);
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

    public static void modifyUseItem(CraftPlayer player, CraftItemStack item, boolean silent) {
        if (item.handle == null) return;
        modifyUseItem(player.getHandle(), item.handle, silent);
    }
    public static void modifyUseItem(EntityPlayer player, net.minecraft.world.item.ItemStack item, boolean silent) {
        if (player.level().isClientSide || player.getAbilities().instabuild) return;
        Optional<NextSetting> nextOption = Items.getOptional(NextSetting.class, item);
        if (item.isDamageableItem()) {
            item.hurtAndBreak(1, player, e2 -> nextOption
                .flatMap(NextSetting::next)
                .map(IItemCreator::createItem)
                .ifPresentOrElse(
                        v -> Items.dropGiveItem(e2.getBukkitEntity(), v, false),
                        silent ? Execute.actionEmpty() : () -> e2.broadcastBreakEvent(EnumItemSlot.MAINHAND)
                ));
            player.inventoryMenu.broadcastFullState();

        } else {
            item.shrink(1);
            nextOption
                .flatMap(NextSetting::next)
                .map(IItemCreator::createItem)
                .ifPresentOrElse(
                        v -> Items.dropGiveItem(player.getBukkitEntity(), v, false),
                        silent ? Execute.actionEmpty() : () -> player.broadcastBreakEvent(EnumItemSlot.MAINHAND)
                );
            player.inventoryMenu.broadcastFullState();
        }
    }
}





