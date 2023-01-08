package org.lime.gp.player.ui;

import org.lime.*;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.lime.display.Displays;

import java.util.*;

public class Saturation implements CustomUI.IUI {
    private static final Saturation Instance = new Saturation();
    private Saturation() {}

    public static core.element create() {
        return core.element.create(Saturation.class)
                .withInit(Saturation::Init)
                .withInstance(Instance);
    }

    public static void Init() {
        CustomUI.addListener(Instance);
    }

    @Override public Collection<ImageBuilder> getUI(Player player) {
        switch (player.getGameMode()) {
            case CREATIVE:
            case SPECTATOR: return Collections.emptyList();
            default:
                break;
        }
        Entity entity = player.getVehicle();
        if (!(entity == null || entity instanceof Boat || entity instanceof Minecart) || Displays.hasVehicle(player.getEntityId())) return Collections.emptyList();
        List<ImageBuilder> images = new ArrayList<>();

        int saturation = Math.round(player.getSaturation());
        if (saturation > 0)
        {
            if (saturation % 2 == 1)
            {
                int i = (saturation + 1) / 2 - 1;
                images.add(ImageBuilder.of(0xEFE1, 9).withOffset(86 - i * 8));
            }
            saturation = (saturation / 2) - 1;
            ImageBuilder _sat = ImageBuilder.of(0xEFE2, 9);
            for (int i = 0; i <= saturation; i++)
                images.add(_sat.withOffset(86 - i * 8));
        }

        return images;
    }

    @Override
    public CustomUI.IType getType() {
        return CustomUI.IType.ACTIONBAR;
    }
}















