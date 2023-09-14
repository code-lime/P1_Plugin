package org.lime.gp.module;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.level.World;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.access.ReflectionAccess;
import org.lime.system.json;

import java.util.function.DoubleSupplier;

public class HorseAttribute {
    public static CoreElement create() {
        return CoreElement.create(HorseAttribute.class)
                .<JsonObject>addConfig("config", v -> v
                        .withParent("horse_attribute")
                        .withDefault(json.object()
                                .add("max_speed_multiply", 0.75)
                                .build())
                        .withInvoke(json -> {
                            setMaxSpeedMultiply(json.get("max_speed_multiply").getAsDouble());
                        })
                );
    }
    private static class ClassLink extends EntityHorseAbstract {
        protected ClassLink(EntityTypes<? extends EntityHorseAbstract> type, World world) { super(type, world); }

        public static double generateSpeed(DoubleSupplier randomDoubleGetter) {
            return EntityHorseAbstract.generateSpeed(randomDoubleGetter);
        }
    }
    private static void setMaxSpeedMultiply(double value) {
        ReflectionAccess.MAX_MOVEMENT_SPEED_EntityHorseAbstract.set(null, (float)ClassLink.generateSpeed(() -> value));
    }
}
