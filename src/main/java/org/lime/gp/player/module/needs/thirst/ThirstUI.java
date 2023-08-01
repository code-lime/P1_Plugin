package org.lime.gp.player.module.needs.thirst;

import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.lime.display.Displays;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ThirstUI implements CustomUI.IUI {
    @Override public Collection<ImageBuilder> getUI(Player player) {
        switch (player.getGameMode()) {
            case CREATIVE, SPECTATOR -> { return Collections.emptyList(); }
        }
        Entity entity = player.getVehicle();
        if (!(entity == null || entity instanceof Boat || entity instanceof Minecart) || Displays.hasVehicle(player.getEntityId())) return Collections.emptyList();
        ThirstData data = Thirst.getThirst(player);
        List<ImageBuilder> images = new ArrayList<>();
        int level = 0;
        if (player.getRemainingAir() < player.getMaximumAir()) level++;
        StateColor color = data.getStateColor();
        int value = (int)Math.round(data.value);

        boolean half = value % 2 == 1;
        if (half)
        {
            int i = (value + 1) / 2 - 1;
            images.add(color.get(StateColor.Type.Half, level).addOffset(86 - i * 8));
        }
        value = (value / 2) - 1;
        for (int i = 0; i <= value; i++) images.add(color.get(StateColor.Type.Whole, level).addOffset(86 - i * 8));
        for (int i = value + (half ? 2 : 1); i < 10; i++) images.add(color.get(StateColor.Type.Empty, level).addOffset(86 - i * 8));

        return images;
    }
    @Override public CustomUI.IType getType() {
        return CustomUI.IType.ACTIONBAR;
    }
}















