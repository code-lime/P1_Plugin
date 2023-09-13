package org.lime.gp.player.ui;

import org.lime.*;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.lime;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.module.EntityPosition;
import org.lime.plugin.CoreElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Compass {
    public static CoreElement create() {
        return CoreElement.create(Compass.class)
                .withInit(Compass::init);
    }

    public static void init() {
        CustomUI.addListener(new CompassUI());
        lime.timer()
                .setAsync()
                .withLoopTicks(1)
                .withCallback(Compass::update)
                .run();
    }

    public static class CompassUI extends CustomUI.GUI {
        private CompassUI() { super(CustomUI.IType.BOSSBAR); }
        @Override public Collection<ImageBuilder> getUI(Player player) { return contains.getOrDefault(player.getUniqueId(), ICompassObject.EMPTY).getUI(); }
    }

    public static abstract class ICompassObject {
        public abstract Collection<ImageBuilder> getUI();
        public abstract boolean isDestroy();
        public abstract void destroy();
        public abstract void add(Location location, TextColor color);

        public static final ICompassObject EMPTY = new ICompassObject() {
            @Override public Collection<ImageBuilder> getUI() { return Collections.emptyList(); }
            @Override public boolean isDestroy() { return false; }
            @Override public void destroy() {}
            @Override public void add(Location location, TextColor color) {}
        };
    }
    public static class CompassObject extends ICompassObject {
        public final UUID uuid;
        public final angle rotation;
        public final Location location;
        public final List<ImageBuilder> components = new LinkedList<>();
        private static final ImageBuilder background = ImageBuilder.of(0xEFe3, 195);
        private CompassObject(UUID uuid, Location location) {
            this.uuid = uuid;
            this.location = location;
            this.rotation = angle.of(location);
            components.add(background);
        }

        @Override public Collection<ImageBuilder> getUI() {
            return components;
        }

        private static class angle {
            private final double value;
            private angle(double value) {
                value = value - (int)value;
                if (value < 0) value++;
                this.value = value;
            }

            /*public double getAngle() {
                return getAngle(360);
            }
            public double getZeroAngle() {
                return getZeroAngle(360);
            }
            public double getAngle(double delta) {
                return getDelta(value * 360, delta);
            }*/
            public double getZeroAngle(double delta) {
                return getDelta(value * 360 - 180, delta);
            }
            private static double getDelta(double angle, double delta) {
                double _delta = delta / 2;
                if (angle > _delta) return _delta;
                if (angle < -_delta) return -_delta;
                return angle;
            }

            public static angle of(double angle) {
                return new angle(angle / 360);
            }
            public static angle of(Location location) {
                return of(0 - location.getYaw() % 360);
            }
            /*public static angle of(Player player) {
                return of(player.getLocation());
            }*/
            public static angle of(Vector center, Vector offset) {
                Vector normal = new Vector().add(offset).subtract(center).setY(0).normalize();
                double x = normal.getX();
                double y = normal.getZ();
                return of(Math.toDegrees(Math.atan2(y,x)));
            }

            public angle add(double angle) {
                return new angle(value + angle / 360);
            }
            /*public angle del(double angle) {
                return new angle(value - angle / 360);
            }*/

            public angle add(angle angle) {
                return new angle(value + angle.value);
            }
            /*public angle del(angle angle) {
                return new angle(value - angle.value);
            }*/
        }

        private static List<ImageBuilder> writeNum(int offset, TextColor color, int num) {
            num = Math.abs(num);
            List<ImageBuilder> components = new LinkedList<>();
            String num_text = String.valueOf(Math.abs(num));
            int length = num_text.length();
            char[] chars = num_text.toCharArray();
            int size = (length - 1) * 2;
            for (int i = 0; i < length; i++) {
                char ch = chars[i];
                components.add(ImageBuilder.of(0xE600 + (ch - '0'), ch == '1' ? 1 : 3).withColor(color).withOffset((offset - size) + i * 4));
            }
            return components;
        }

        private boolean _destroy = false;
        @Override public void add(Location location, TextColor color) {
            if (location == null || location.getWorld() != this.location.getWorld()) return;

            Vector position = this.location.toVector();
            Vector _target = location.toVector();
            int distance = (int)Math.floor(_target.distance(position));
            angle target = angle.of(position, _target).add(90).add(rotation);
            int offset = (int)target.getZeroAngle(180);
            components.add(ImageBuilder.of(0xEFE4, 3).withColor(color).withOffset(offset));
            components.addAll(writeNum(offset, color, distance));
        }
        @Override public void destroy() {
            if (_destroy) return;
            _destroy = true;
        }
        @Override public boolean isDestroy() {
            return _destroy;
        }
    }

    private static final ConcurrentHashMap<UUID, ICompassObject> contains = new ConcurrentHashMap<>();

    public static void update() {
        HashMap<UUID, ICompassObject> _contains = new HashMap<>();

        Tables.COMPASS_TARGET_TABLE.forEach(row -> Optional.ofNullable(EntityPosition.onlinePlayers.get(row.uuid))
                .map(EntityPosition.playerLocations::get)
                .ifPresent(position -> {
                    Location location = row.getTargetLocation();
                    if (location == null) return;
                    _contains.compute(row.uuid, (k, v) -> {
                        if (v == null) v = new CompassObject(k, position);
                        v.add(location, TextColor.fromHexString("#" + row.color));
                        return v;
                    });
                }));

        contains.putAll(_contains);
        contains.entrySet().removeIf(kv -> {
            if (_contains.containsKey(kv.getKey())) return false;
            kv.getValue().destroy();
            return true;
        });
    }
}
















































