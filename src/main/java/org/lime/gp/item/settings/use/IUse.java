package org.lime.gp.item.settings.use;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.gp.item.settings.IItemSetting;
import org.lime.gp.item.settings.use.target.ITarget;

import java.util.Optional;

public interface IUse<T extends ITarget> extends IItemSetting {
    boolean use(Player player, T target, EquipmentSlot arm, boolean shift);
    EquipmentSlot arm();

    Optional<T> tryCast(Player player, ITarget target, EquipmentSlot arm, boolean shift);
    default boolean tryUse(Player player, ITarget target, EquipmentSlot arm, boolean shift) {
        return tryCast(player, target, arm, shift).map(v -> use(player, v, arm, shift)).orElse(false);
    }
}
