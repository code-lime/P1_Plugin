package org.lime.gp.player.module.needs.food;

import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.lime.display.Displays;
import org.lime.display.Passenger;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.plugin.CoreElement;

import java.util.*;

public class FoodUI implements CustomUI.IUI {
    private static final FoodUI Instance = new FoodUI();
    private FoodUI() {}

    public static CoreElement create() {
        return CoreElement.create(FoodUI.class)
                .withInit(FoodUI::init)
                .withInstance(Instance);
    }

    public static void init() {
        CustomUI.addListener(Instance);
    }

    @Override public Collection<ImageBuilder> getUI(Player player) {
        switch (player.getGameMode()) {
            case CREATIVE, SPECTATOR -> { return Collections.emptyList(); }
        }
        Entity entity = player.getVehicle();
        if (!(entity == null || entity instanceof Boat || entity instanceof Minecart) || Passenger.hasVehicle(player.getEntityId())) return Collections.emptyList();
        List<ImageBuilder> images = new ArrayList<>();

        if (FoodType.IsVanilla) {
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
        } else {
            int saturation = Math.round(player.getSaturation() / 20f * 81f);
            images.add(ImageBuilder.of(0xE74E + saturation, 82).withOffset(50));
            ProxyFoodMetaData.ofPlayer(player)
                    .map(ProxyFoodMetaData::food)
                    .map(v -> v instanceof TypedFoodLevel typed ? typed : null)
                    .ifPresent(typed -> {
                        int offsetX = 13;
                        int lastImageOffset = 0;
                        for (FoodType type : new FoodType[] { FoodType.Dessert, FoodType.Fruit, FoodType.Meat, FoodType.Cereals }) {
                            float value = typed.getValue(type) / 2;
                            int iconCount = type.maxCount / 2;
                            for (int i = 0; i < iconCount; i++) {
                                images.add(
                                        ImageBuilder.of(type.imageByValue(Math.max(Math.min(value - (iconCount - i - 1), 1), 0)), type.imageWidth)
                                                .withOffset(offsetX + lastImageOffset)
                                );
                                offsetX += type.imageWidth + lastImageOffset;
                                lastImageOffset = type.imageOffset;
                            }
                            offsetX += type.imageEndOffset;
                        }
                    });
        }

        return images;
    }

    @Override public CustomUI.IType getType() {
        return CustomUI.IType.ACTIONBAR;
    }
}















