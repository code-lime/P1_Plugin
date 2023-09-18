package org.lime.gp.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EntityLiving;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftLivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.data.Checker;
import org.lime.gp.lime;
import org.lime.gp.player.ui.ImageBuilder;

import java.util.*;

public class MobSaturation implements Listener {
    public static CoreElement create() {
        return CoreElement.create(MobSaturation.class)
                .disable()
                .withInstance()
                .withInit(MobSaturation::init)
                .<JsonObject>addConfig("config", v -> v
                        .withParent("mob_saturation")
                        .withDefault(new JsonObject())
                        .withInvoke(j -> {
                            HashMap<EntityType, SaturationData> saturations = new HashMap<>();
                            j.entrySet().forEach(kv -> saturations.put(EntityType.valueOf(kv.getKey()), new SaturationData(kv.getValue().getAsJsonObject())));
                            SaturationData.saturations.clear();
                            SaturationData.saturations.putAll(saturations);
                        })
                );
    }
    public static void init() {
        lime.repeat(MobSaturation::updateMin, 60);
        lime.repeat(MobSaturation::update2Sec, 2);
    }
    public static void updateMin() {
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(LivingEntity.class).forEach(e -> {
            SaturationData data = SaturationData.saturations.get(e.getType());
            if (data == null) return;
            data.tick(e);
        }));
    }
    public static void update2Sec() {
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(LivingEntity.class).forEach(SaturationData::updateShow));
    }

    public static class SaturationData {
        private static final HashMap<EntityType, SaturationData> saturations = new HashMap<>();
        public static class FoodData {
            public final Checker func;
            public final double saturation;

            public FoodData(String key, JsonElement value) {
                func = Checker.createCheck(key);
                saturation = value.getAsDouble();
            }
        }

        public final LinkedList<SaturationData.FoodData> food = new LinkedList<>();
        public final double time_min;
        public final double step_min;

        public SaturationData(JsonObject json) {
            json.getAsJsonObject("food").entrySet().forEach(kv -> food.add(new SaturationData.FoodData(kv.getKey(), kv.getValue())));
            time_min = json.get("time_min").getAsDouble();
            step_min = 1.0 / time_min;
        }
        private static final NamespacedKey SATURATION_KEY = new NamespacedKey(lime._plugin, "saturation");
        public void tick(LivingEntity entity) {
            PersistentDataContainer container = entity.getPersistentDataContainer();
            double saturation = container.getOrDefault(SATURATION_KEY, PersistentDataType.DOUBLE, 1.0);
            saturation = Math.max(0, saturation - step_min);
            if (saturation <= 0) {
                EntityLiving living = ((CraftLivingEntity)entity).getHandle();
                living.hurt(living.damageSources().starve(), 999999999);
                return;
            }
            container.set(SATURATION_KEY, PersistentDataType.DOUBLE, saturation);
            updateShow(entity);
        }
        public static boolean tryAddFood(LivingEntity entity, double food) {
            PersistentDataContainer container = entity.getPersistentDataContainer();
            double saturation = container.getOrDefault(SATURATION_KEY, PersistentDataType.DOUBLE, 1.0);
            if (saturation > 0.9) return false;
            saturation = Math.min(1, saturation + food);
            container.set(SATURATION_KEY, PersistentDataType.DOUBLE, saturation);
            updateShow(entity);
            return true;
        }

        public Double tryGetFood(ItemStack item) {
            for (SaturationData.FoodData data : food) {
                if (data.func.check(item))
                    return data.saturation;
            }
            return null;
        }

        public static void updateShow(LivingEntity entity) {
            if (!saturations.containsKey(entity.getType())) return;
            Location location = entity.getLocation();
            Component text = getDisplayText(entity);
            int modelID = entity.getEntityId();
            DrawText.show(new DrawText.IShowTimed(2.5) {
                @Override public Optional<Integer> parent() { return Optional.empty();/*of(modelID);*/ }
                @Override public String getID() { return "Mob["+modelID+"].NickName"; }
                @Override public boolean filter(Player player) { return true; }
                @Override public Component text(Player player) { return text; }
                @Override public Location location() { return location; }
                @Override public double distance() { return 5; }
            });
        }
        public static Component getDisplayText(LivingEntity entity) {
            float value = (float)(entity.getPersistentDataContainer().getOrDefault(SATURATION_KEY, PersistentDataType.DOUBLE, 1.0) * 20);

            List<ImageBuilder> images = new ArrayList<>();

            int saturation = 20 - Math.round(value);

            int offset = 18;
            int size = 3;
            int space = 3;

            ImageBuilder _sat = ImageBuilder.of(0xEFF0, size);
            ImageBuilder _part = ImageBuilder.of(0xEFF1, size);
            ImageBuilder _back = ImageBuilder.of(0xEFE9, size);

            boolean isPart = saturation % 2 == 1;
            if (isPart) saturation--;
            for (int i = 0; i < 10; i++) {
                int type = saturation - (9-i) * 2;
                if (type > 0) images.add(_back.withOffset(offset - i * space));
                else if (isPart && type == 0) images.add(_part.withOffset(offset - i * space));
                else images.add(_sat.withOffset(offset - i * space));
            }

            return Component.text(ChatHelper.getSpaceSize(-10)).append(ImageBuilder.join(images, 1));
        }
    }
    @EventHandler public static void on(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof LivingEntity entity)) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        SaturationData data = SaturationData.saturations.get(entity.getType());
        if (data == null) return;
        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        Double saturation = data.tryGetFood(item);
        if (saturation == null) return;
        if (!SaturationData.tryAddFood(entity, saturation)) return;
        item.subtract();
        e.setCancelled(true);
    }
}



























