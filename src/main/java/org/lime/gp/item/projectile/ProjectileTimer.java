package org.lime.gp.item.projectile;

import org.bukkit.entity.Player;
import org.lime.gp.item.weapon.WeaponData;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;

import java.util.Collections;
import java.util.List;

public class ProjectileTimer implements CustomUI.IUI {
    @Override public CustomUI.IType getType() { return CustomUI.IType.ACTIONBAR; }
     @Override public List<ImageBuilder> getUI(Player player) {
         int ticks = ProjectileItem.getTimerTicks(player.getUniqueId());
         if (ticks <= 0) return Collections.emptyList();
         return Collections.singletonList(ImageBuilder.of(player, WeaponData.timerMs(ticks * 50L)));
    }
}
