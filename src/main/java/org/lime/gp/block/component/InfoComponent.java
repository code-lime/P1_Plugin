package org.lime.gp.block.component;

import com.google.gson.*;
import org.bukkit.util.Vector;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

public class InfoComponent {
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Component.Any.class)
    public @interface Component {
        @Retention(RetentionPolicy.RUNTIME) @interface Any {
            Component[] value();
        }

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
            public static Value ofAngle(String angle_name) {
                return valueOf("ANGLE_" + angle_name);
            }

            public Toast3<Integer, Integer, Integer> rotate(Toast3<Integer, Integer, Integer> pos) {
                return switch (this) {
                    case ANGLE_0, ANGLE_45 -> Toast.of(pos.val0, pos.val1, pos.val2);
                    case ANGLE_90, ANGLE_135 -> Toast.of(-pos.val2, pos.val1, pos.val0);
                    case ANGLE_180, ANGLE_225 -> Toast.of(-pos.val0, pos.val1, -pos.val2);
                    case ANGLE_270, ANGLE_315 -> Toast.of(pos.val2, pos.val1, -pos.val0);
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
        private final Func2<ComponentDynamic<?, ?>, CustomTileMetadata, T> createInstance;
        private final String name;
        public GenericDynamicComponent(String name, BlockInfo info, Func2<ComponentDynamic<?, ?>, CustomTileMetadata, T> createInstance) {
            super(info);
            this.createInstance = createInstance;
            this.name = name;
        }
        @Override public String name() { return getName(name); }

        @Override public T createInstance(CustomTileMetadata metadata) { return createInstance.invoke(this, metadata); }
        @Override public Class<T> classInstance() { throw new ToDoException("CLASS COMPONENT"); }
        @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }

        public static <T extends BlockInstance>GenericDynamicComponent<T> of(String name, BlockInfo info, Func2<ComponentDynamic<?, ?>, CustomTileMetadata, T> createInstance) { return new GenericDynamicComponent<>(name, info, createInstance); }
        public static String getName(String name) { return name + ".generic"; }
    }
}


















