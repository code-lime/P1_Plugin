package org.lime.gp.block.component;

import com.google.gson.*;
import org.bukkit.util.Vector;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.system;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

public class InfoComponent {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Component {
        String name();
    }

    public static class Rotation {
        public enum Value {
            ANGLE_0,
            ANGLE_45,
            ANGLE_90,
            ANGLE_135,
            ANGLE_180,
            ANGLE_225,
            ANGLE_270,
            ANGLE_315;

            public final int angle;
            public final org.bukkit.util.Vector direction;

            Value() {
                this.angle = 45 * ordinal();
                this.direction = new org.bukkit.util.Vector(0, 0, 1).rotateAroundY(Math.toRadians(180 - angle));
            }

            public static Value ofAngle(int angle) {
                return valueOf("ANGLE_" + angle);
            }

            public system.Toast3<Integer, Integer, Integer> rotate(system.Toast3<Integer, Integer, Integer> pos) {
                return switch (this) {
                    case ANGLE_0, ANGLE_45 -> system.toast(pos.val0, pos.val1, pos.val2);
                    case ANGLE_90, ANGLE_135 -> system.toast(-pos.val2, pos.val1, pos.val0);
                    case ANGLE_180, ANGLE_225 -> system.toast(-pos.val0, pos.val1, -pos.val2);
                    case ANGLE_270, ANGLE_315 -> system.toast(pos.val2, pos.val1, -pos.val0);
                };
            }
        }

        private static double getMod(double x, double y, double z) {
            return Math.sqrt(x * x + y * y + z * z);
        }

        private static double getAngle(org.bukkit.util.Vector a, org.bukkit.util.Vector b) {
            double ab = a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
            double _a = getMod(a.getX(), a.getY(), a.getZ());
            double _b = getMod(b.getX(), b.getY(), b.getZ());
            return Math.acos(ab / (_a * _b));
        }

        public static Value of(Vector direction, Collection<Value> rotations) {
            double min_angle = 0;
            Value min_value = null;
            for (Value rotation : rotations) {
                double angle = getAngle(direction, rotation.direction);
                if (min_value == null || min_angle > angle) {
                    min_angle = angle;
                    min_value = rotation;
                }
            }
            return min_value == null ? Value.ANGLE_0 : min_value;
        }
    }
    public static final class GenericDynamicComponent<T extends BlockInstance> extends ComponentDynamic<JsonObject, T> {
        private final system.Func2<ComponentDynamic<?, ?>, CustomTileMetadata, T> createInstance;
        private final String name;
        public GenericDynamicComponent(String name, BlockInfo info, system.Func2<ComponentDynamic<?, ?>, CustomTileMetadata, T> createInstance) {
            super(info);
            this.createInstance = createInstance;
            this.name = name;
        }
        @Override public T createInstance(CustomTileMetadata metadata) { return createInstance.invoke(this, metadata); }
        @Override public String name() { return getName(name); }

        public static <T extends BlockInstance>GenericDynamicComponent<T> of(String name, BlockInfo info, system.Func2<ComponentDynamic<?, ?>, CustomTileMetadata, T> createInstance) {
            return new GenericDynamicComponent<>(name, info, createInstance);
        }
        public static String getName(String name) {
            return name + ".generic";
        }
    }
}


















