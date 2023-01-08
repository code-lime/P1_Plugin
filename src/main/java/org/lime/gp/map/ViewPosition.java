package org.lime.gp.map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.system;

import java.util.Optional;

public class ViewPosition {
    private final int x;
    private final int y;
    private final double dx;
    private final double dy;
    private final Position block;
    private final Location location;
    private final MapMonitor.ClickType click;

    private ViewPosition(int x, int y, double dx, double dy, Position block, MapMonitor.ClickType click) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.block = block;
        this.location = block.getLocation(dx, UP_OFFSET, dy);
        this.click = click;
    }
    private ViewPosition(system.Toast2<Integer, Integer> point, system.Toast2<Double, Double> pointD, Position block, MapMonitor.ClickType click) {
        this(point.val0, point.val1, pointD.val0, pointD.val1, block, click);
    }
    private ViewPosition(system.Toast2<system.Toast2<Integer, Integer>, system.Toast2<Double, Double>> point, Position block, MapMonitor.ClickType click) {
        this(point.val0, point.val1, block, click);
    }

    public int getPixelX() { return x; }
    public int getPixelY() { return y; }
    public double getDoubleX() { return dx; }
    public double getDoubleY() { return dy; }
    public Position getBlock() { return block; }
    public Location getLocation() { return location; }
    public MapMonitor.ClickType getClick() { return click; }

    public static Optional<ViewPosition> of(MonitorInstance meta, Player player, MapMonitor.ClickType click) {
        return point(meta, meta.rotation(), player).map(v -> new ViewPosition(v, meta.metadata().position(), click));
    }

    private static system.Toast2<Double, Double> rotate(Double x, Double y, MapMonitor.MapRotation rotation) {
        return switch (rotation) {
            default -> system.toast(x, y);
            case CLOCKWISE -> system.toast(y, 1-x);
            case FLIPPED -> system.toast(1-x, 1-y);
            case COUNTER_CLOCKWISE -> system.toast(1-y, x);
        };
    }
    private static Optional<system.Toast2<system.Toast2<Integer, Integer>, system.Toast2<Double, Double>>> point(MonitorInstance meta, MapMonitor.MapRotation rotation, Player player) {
        return pointD(meta, rotation, player)
                .map(v -> system.toast(system.toast((int)Math.round(v.val0 * 127), (int)Math.round(v.val1 * 127)), v));
    }
    private static final double UP_OFFSET = ((1+(8/60.0))/16.0);
    private static Optional<system.Toast2<Double, Double>> pointD(MonitorInstance meta, MapMonitor.MapRotation rotation, Player player) {
        Location location = player.getEyeLocation();
        Location position = meta.metadata().location().add(meta.offset());
        if (location.getWorld() != position.getWorld()) return Optional.empty();

        double h = position.getY() + UP_OFFSET;// 1/16 + 8/60

        Vector dir = location.getDirection();
        Vector x = location.toVector().add(dir);
        Vector y = location.toVector().subtract(dir);

        boolean isSeeDown = x.getY() < y.getY();
        boolean isInDown = h < location.getY();

        if (isSeeDown != isInDown) return Optional.empty();

        Vector a = new Vector(0, h, 0);
        Vector b = new Vector(1, h, 0);
        Vector c = new Vector(0, h, 1);

        Vector n = new Vector().add(b).subtract(a).crossProduct(new Vector().add(c).subtract(a));
        Vector v = new Vector().add(a).subtract(x);
        double d = new Vector().add(n).dot(v);
        Vector w = new Vector().add(y).subtract(x);
        double e = new Vector().add(n).dot(w);

        if (e != 0) {
            Vector point = new Vector().add(x).add(new Vector().add(w).multiply(d/e));

            double _x = point.getX() - position.getX();
            double _z = point.getZ() - position.getZ();

            if (_x >= 0 && _x <= 1 && _z >= 0 && _z <= 1)
                return Optional.of(rotate(_x, _z, rotation));
        }
        return Optional.empty();
    }
}
