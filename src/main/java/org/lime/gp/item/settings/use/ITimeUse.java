package org.lime.gp.item.settings.use;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.lime;
import org.lime.gp.player.module.Death;
import org.lime.gp.player.module.HandCuffs;
import org.lime.gp.player.perm.Perms;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;

import java.util.Optional;

public interface ITimeUse<T extends ITarget> extends IUse<T> {
    int getTime();

    int getCooldown();

    String prefix(boolean self);

    String cooldownPrefix();

    boolean timeUse(Player player, T target, ItemStack item);

    boolean silent();

    default String useCooldownKey() {
        return creator().getKey() + "#use_item";
    }

    default boolean cooldownTick(Player player, EquipmentSlot arm) {
        return !Death.isDamageLay(player.getUniqueId())
                && !HandCuffs.isMove(player.getUniqueId())
                && Optional.of(player.getInventory().getItem(arm))
                .map(v -> (CraftItemStack) v)
                .flatMap(item -> Items.getOptional(getClass(), item)
                        .filter(v -> v == this)
                        .map(setting -> {
                            double sec = Cooldown.getCooldown(player.getUniqueId(), useCooldownKey());
                            if (sec <= 0) return false;
                            CustomUI.TextUI.show(player, ImageBuilder.of(player, cooldownPrefix() + UseSetting.timerMs((int) Math.floor(sec * 20))));
                            return true;
                        })
                )
                .orElse(false);
    }

    private boolean useTick(CraftPlayer player, T target, Location playerLocation, EquipmentSlot arm, Integer ticks, Perms.ICanData canData) {
        return !Death.isDamageLay(player.getUniqueId())
                && !HandCuffs.isMove(player.getUniqueId())
                && Optional.of(player.getInventory().getItem(arm))
                .map(v -> (CraftItemStack) v)
                .flatMap(item -> Items.getOptional(getClass(), item)
                        .filter(v -> v == this)
                        .filter(v -> canData.isCanUse(v.creator().getKey()))
                        .map(setting -> {
                            int _ticks = ticks == null ? setting.getTime() : ticks;
                            if (_ticks <= 0) {
                                if (!timeUse(player, target, item))
                                    return false;
                                UseSetting.modifyUseItem(player, item, silent());
                                int cooldown = getCooldown();
                                if (cooldown > 0)
                                    Cooldown.setCooldown(player.getUniqueId(), useCooldownKey(), cooldown / 20.0);

                                CustomUI.TextUI.hide(player);
                                return true;
                            }
                            CustomUI.TextUI.show(player, ImageBuilder.of(player, prefix(target.isSelf()) + UseSetting.timerMs(_ticks)));
                            Cooldown.setCooldown(player.getUniqueId(), "use_item", 1);
                            lime.onceTicks(() -> {
                                if (!item.isSimilar(player.getInventory().getItemInMainHand())) return;
                                if (!UseSetting.isDistance(playerLocation, player.getLocation(), 1) || !target.isActive())
                                    return;
                                useTick(player, target, playerLocation, arm, _ticks - 1, canData);
                            }, 1);
                            return true;
                        })
                )
                .orElse(false);
    }

    @Override default boolean use(Player player, T target, EquipmentSlot arm, boolean shift) {
        //if (!shift) return false;
        if (!(player instanceof CraftPlayer cplayer)) return false;
        if (Cooldown.hasCooldown(player.getUniqueId(), "use_item")) return false;
        if (Cooldown.hasCooldown(player.getUniqueId(), useCooldownKey())) return false;
        return useTick(cplayer, target, player.getLocation(), arm, null, Perms.getCanData(player.getUniqueId()));
    }
}
