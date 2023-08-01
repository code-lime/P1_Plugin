package org.lime.gp.player.module.drugs;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.gp.player.inventory.MainPlayerInventory;
import org.lime.gp.player.module.needs.food.ProxyFoodMetaData;
import org.lime.gp.player.module.needs.thirst.Thirst;
import org.lime.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum EffectType {
    NONE((player, tick) -> {
    }),

    SATURATION_FULL(ofPotion(PotionEffectType.SATURATION.createEffect(80, 1).withIcon(false), 40)),
    SPEED_EFFECT(ofPotion(PotionEffectType.SPEED.createEffect(80, 1), 30)),
    REGENERATION_EFFECT(ofPotion(PotionEffectType.REGENERATION.createEffect(80, 0), 40)),
    STRENGTH_EFFECT(ofPotion(PotionEffectType.INCREASE_DAMAGE.createEffect(80, 0), 40)),
    NIGHT_VISION(ofPotion(PotionEffectType.NIGHT_VISION.createEffect(250, 0), 220)),
    BLINDNESS(ofPotion(PotionEffectType.BLINDNESS.createEffect(80, 0), 40)),
    JUMP(ofPotion(PotionEffectType.JUMP.createEffect(80, 0), 40)),

    THIRST_FULL((player, tick) -> {
        if (tick % 40 == 0) Thirst.thirstReset(player);
    }),
    SPEED_RANDOMIZE_EFFECT((player, tick) -> {
        switch (tick % 100) {
            case 0 -> PotionEffectType.SLOW.createEffect(80, 2).apply(player);
            case 70 -> PotionEffectType.SPEED.createEffect(80, 0).apply(player);
        }
    }),
    SATURATION_ZERO((player, tick) -> {
        if (tick % 20 != 0) return;
        ProxyFoodMetaData.ofPlayer(player).ifPresent(food -> food.setSaturation(0));
    }),
    HOTBAR_SORTER((player, tick) -> {
        if (tick % 300 != 0) return;
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> items = new ArrayList<>();
        items.add(inventory.getItemInOffHand());
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (MainPlayerInventory.checkBarrier(item)) break;
            items.add(item);
        }
        Collections.shuffle(items);
        inventory.setItemInOffHand(items.remove(0));
        int length = items.size();
        for (int i = 0; i < length; i++) inventory.setItem(i, items.get(i));
    }),
    ARMS_LOCK((player, tick) -> {
        PlayerInventory inventory = player.getInventory();
        Drugs.lockArmsTick(player, inventory);
    }),
    TREMBLING((player, tick) -> {
        Drugs.freezeList.compute(player.getEntityId(), (_id, _value) -> {
            if (_value == null) player.setFreezeTicks(player.getFreezeTicks() + 1);
            return 10;
        });
    });

    private final system.Action2<Player, Integer> tick;

    EffectType(system.Action2<Player, Integer> tick) {
        this.tick = tick;
    }

    public void tick(Player player, int time) {
        tick.invoke(player, time);
    }

    //private static system.Action2<Player, Integer> ofPotion(PotionEffect potionEffect) { return ofPotion(potionEffect, 0); }
    private static system.Action2<Player, Integer> ofPotion(PotionEffect potionEffect, int skip) {
        PotionEffectType effectType = potionEffect.getType();
        return (player, time) -> {
            PotionEffect current = player.getPotionEffect(effectType);
            if (current == null || current.getAmplifier() < potionEffect.getAmplifier() || current.getDuration() < skip)
                player.addPotionEffect(potionEffect);
        };
    }
}
