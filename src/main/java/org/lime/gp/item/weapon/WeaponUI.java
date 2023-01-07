package org.lime.gp.item.weapon;

import org.bukkit.entity.Player;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;

import java.util.Collection;
import java.util.Collections;

public class WeaponUI implements CustomUI.IUI {
    @Override public Collection<ImageBuilder> getUI(Player player) {
        WeaponData data = WeaponLoader.data.get(player.getUniqueId());
        return data == null ? Collections.emptyList() : data.getUI(player);
    }
    @Override public CustomUI.IType getType() {
        return CustomUI.IType.ACTIONBAR;
    }
}
