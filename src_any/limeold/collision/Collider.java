package org.lime.gp.entity.collision;

import io.papermc.paper.util.CachedLists;
import io.papermc.paper.util.CollisionUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.util.UnsafeList;
import org.bukkit.util.Vector;
import org.lime.gp.display.Displays;
import org.lime.gp.display.transform.LocalLocation;
import org.lime.gp.display.transform.Transform;
import org.lime.gp.lime;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Collider {
    private final double width;
    private final double height;
    private final double length;
    private final boolean display;

    private final List<Vector> lengthLocals;
    private final List<Vector> heightLocals;
    private final List<Vector> widthLocals;
    private final List<Vector> allLocals;

    private static final double RADIUS = 0.4;
    public Collider(double width, double height, double length) { this(width, height, length, false); }
    public Collider(double width, double height, double length, boolean display) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.display = display;

        this.lengthLocals = getLocals(0, 0, length, (int)(length / RADIUS)).toList();
        this.heightLocals = getLocals(0, height, 0, (int)(height / RADIUS)).toList();
        this.widthLocals = getLocals(width, 0, 0, (int)(width / RADIUS)).toList();
        this.allLocals = getWidthLocals()
                .flatMap(_width -> getHeightLocals()
                        .flatMap(_height -> getLengthLocals()
                                .map(_length -> new Vector().add(_width).add(_height).add(_length))
                        )
                ).toList();
    }

    public LocalLocation local() { return new LocalLocation(width, height, length); }

    private record Point(Vector local, Vector global) {
        public <T>PointData<T> data(T data) { return new PointData<>(local, global, data); }
    }
    private record PointData<T>(Vector local, Vector global, T data) { }

    private static Stream<Vector> getLocals(double lx, double ly, double lz, int steps) {
        return getLocals(new Vector(lx / -2 , ly / -2, lz / -2), new Vector(lx / 2, ly / 2, lz / 2), steps);
    }
    private static Stream<Vector> getLocals(Vector start, Vector end, int steps) {
        Vector delta = new Vector().add(end).subtract(start).divide(new Vector(steps, steps, steps));
        Stream<Vector> stepStream = IntStream.range(1, steps)
                .mapToObj(i -> new Vector().add(delta).multiply(i).add(start));
        return Stream.concat(Stream.of(start), Stream.concat(stepStream, Stream.of(end)));
    }

    public Stream<Vector> getLengthLocals() { return lengthLocals.stream(); }
    public Stream<Vector> getHeightLocals() { return heightLocals.stream(); }
    public Stream<Vector> getWidthLocals() { return widthLocals.stream(); }
    public Stream<Vector> getAllLocals() { return allLocals.stream(); }

    public Stream<Point> getAllSteps(Location location) {
        //return Stream.of(new Point(new Vector(), location.toVector()));
        return Transform.toWorld(location, getAllLocals().map(LocalLocation::new)).map(v -> new Point(v.val1.position(), v.val0.toVector()));
    }
    public Stream<BlockPosition> getAllBlocks(Location location) {
        return getAllSteps(location)
                .map(Point::global)
                .map(v -> new BlockPosition(v.getBlockX(), v.getBlockY(), v.getBlockZ()))
                .distinct();
    }

    private double angle(PointData<Double> point) {
        double z = point.local.getZ();
        return point.data * -90 * (z / length);
    }
    private Double height(Location location, List<Vector> velocity, PointData<Double> point) {
        double height_entity = location.getY();
        double height = point.data + Math.floor(point.global.getY()) - point.local.getY();
        if (point.data == 0) return Double.NaN;
        if (height_entity >= height) return height_entity;
        velocity.add(new Vector(-point.local.getX()*3, -point.local.getY(), -point.local.getZ()*3));
        return height;
    }
    private static float lerp(float a, float b, float f) { return a + f * (b - a); }
    /*public Location collide(HeightReader reader, Location location) {
        List<Vector> velocityList = new ArrayList<>();
        List<PointData<Double>> heights = getAllSteps(location).map(point -> {
            Vector pos = point.global;
            return reader.height(new BlockPosition(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()))
                    .map(point::data);
        }).filter(Optional::isPresent).map(Optional::get).toList();
        float angle = location.getPitch();
        angle = lerp(angle, (float)heights.stream().mapToDouble(this::angle).average().orElse(angle), 0.1f);
        double height_entity = location.getY();
        //system.Toast1<Integer> air_count = system.toast(0);
        double height = heights.stream()
                .mapToDouble(v -> height(location, velocityList, v))
                .map(v -> Double.isNaN(v) ? height_entity : v)
                .average()
                .orElse(height_entity);
        //if (air_count.val0 == 0)
        if (height - location.getY() >= gravity) height += 1;
        Vector _velocity = new Vector();
        velocityList.forEach(_velocity::add);
        //int velocitySize = velocityList.size();
        //if (velocitySize > 0) velocity.divide(new Vector(velocitySize, velocitySize, velocitySize));
        return system.toast(_velocity, new Location(location.getWorld(), location.getX(), height, location.getZ(), location.getYaw(), angle));
    }*/
    public static double midpoint(double value1, double value2) {
        return (value1 + value2) / 2;
    }
    public static system.Toast2<Double, Double> compareAdd(double value1, double value2) {
        if (value1 == 0) return system.toast(value2, 0.0);
        if (value2 == 0) return system.toast(value1, 0.0);
        if (value1 > 0 && value2 > 0) return system.toast(Math.max(value1, value2), 0.0);
        if (value1 < 0 && value2 < 0) return system.toast(Math.min(value1, value2), 0.0);
        return system.toast(value1 + value2, 0.0);
        //double _value1 = Math.abs(value1);
        //double _value2 = Math.abs(value2);
        //return system.toast(value1 + value2, (_value1 + _value2) * 0.1);
    }
    public static Vector compareAdd(Vector pos1, Vector pos2) {
        system.Toast2<Double, Double> x = compareAdd(pos1.getX(), pos2.getX());
        system.Toast2<Double, Double> z = compareAdd(pos1.getZ(), pos2.getZ());

        system.Toast2<Double, Double> y = compareAdd(pos1.getY(), pos2.getY());
        return new Vector(
                x.val0,
                y.val0, //+ x.val1 + y.val1 + z.val1,
                z.val0);
    }
    public static Vector round(Vector pos) {
        return new Vector(system.round(pos.getX(), 3), system.round(pos.getY(), 3), system.round(pos.getZ(), 3));
    }
    /*public Location collide(HeightReader reader, Location location) {
        List<Vector> velocityList = new ArrayList<>();
        float angle = location.getPitch();
        angle = lerp(angle, (float)heights.stream().mapToDouble(this::angle).average().orElse(angle), 0.1f);
        double height_entity = location.getY();
        //system.Toast1<Integer> air_count = system.toast(0);
        double height = heights.stream()
                .mapToDouble(v -> height(location, velocityList, v))
                .map(v -> Double.isNaN(v) ? height_entity : v)
                .average()
                .orElse(height_entity);
        //if (air_count.val0 == 0)
        if (height - location.getY() >= gravity) height += 1;
        Vector _velocity = new Vector();
        velocityList.forEach(_velocity::add);
        //int velocitySize = velocityList.size();
        //if (velocitySize > 0) velocity.divide(new Vector(velocitySize, velocitySize, velocitySize));
        return system.toast(_velocity, new Location(location.getWorld(), location.getX(), height, location.getZ(), location.getYaw(), angle));
    }*/

    private static Location locked(Location location, AxisAlignedBB aabb) {
        Vector position = location.toVector();
        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();

        if (x > aabb.minX) x = aabb.minX;
        if (x < aabb.maxX) x = aabb.maxX;

        if (y > aabb.minY) y = aabb.minY;
        if (y < aabb.maxY) y = aabb.maxY;

        if (z > aabb.minZ) z = aabb.minZ;
        if (z < aabb.maxZ) z = aabb.maxZ;

        return location.set(x,y,z);
    }

    private static Optional<Double> offsetOf(double value, double delta) {
        if (value > 0) return value > delta ? Optional.empty() : Optional.of(value - delta);
        else if (value < 0) return value < -delta ? Optional.empty() : Optional.of(value + delta);
        else return Optional.of(-delta);
    }
    private static double offsetOfY(double value, double delta) {
        return value < -delta && value > delta ? 0 : (value + delta);
    }
    public AxisAlignedBB offsetOf(AxisAlignedBB aabb, Location location, Vector local) {
        double dx = width / 2;
        double dy = height / 2;
        double dz = length / 2;

        double x = local.getX();
        double y = local.getY();
        double z = local.getZ();

        double ax = Math.abs(x);
        double ay = Math.abs(y);
        double az = Math.abs(z);

        Vector border = Transform.toWorld(location, new LocalLocation(local)).toVector();

        if (ax > ay && ax > az) return offsetOf(x,dx).map(_x -> Transform.toWorld(location, new LocalLocation(new Vector(_x, 0, 0))));
        if (ay > ax && ay > az) return new Vector(Double.NaN, offsetOfY(y,dy), Double.NaN);
        if (az > ax && az > ay) return new Vector(Double.NaN, Double.NaN, offsetOf(z,dz));

        return new Vector(offsetOf(x,dx), offsetOfY(y,dy), offsetOf(z,dz));
    }
    private int tick = 0;
    public Location collide(HeightReader reader, Location location) {
        tick = (tick + 1) % 20;
        int offset = (int)(Math.max(width, Math.max(height, length)) / 2 + 2);
        int _x = location.getBlockX();
        int _y = location.getBlockY();
        int _z = location.getBlockZ();
        List<BoxSphereCollider.Box> boxes = new ArrayList<>();
        for (int x = -offset; x <= offset; x++)
            for (int y = -offset; y <= offset; y++)
                for (int z = -offset; z <= offset; z++)
                    reader.boxes(new BlockPosition(_x+x,_y+y,_z+z)).ifPresent(boxes::addAll);

        List<BoxSphereCollider.Point> points = getAllSteps(location)
                .flatMap(point -> {
                    BoxSphereCollider.Sphere sphere = BoxSphereCollider.Sphere.of(point.global, RADIUS);
                    return boxes.stream()
                            .map(sphere::pointInside)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(v -> {
                                if (tick == 0) Displays.drawPoint(v.toVector(), 0);
                                return true;
                            });
                    //.map(v -> {
                                /*Vector local = Transform.toLocal(location, v.toVector());
                                double lx = local.getX();
                                double ly = local.getY();
                                double lz = local.getZ();

                                double dx = width / 2;
                                double dy = height / 2;
                                double dz = length / 2;

                                double ox = lx > 0 ? (dx - lx);
                                double oy = 0;
                                double oz = 0;*/

                                /*offsetOf(, Transform.toLocal(location, v.toVector()))));
                                double bx = border.getX();
                                double by = border.getY();
                                double bz = border.getZ();

                                return new AxisAlignedBB(
                                        Double.isNaN(bx) ? Double.NEGATIVE_INFINITY : bx > , Double.POSITIVE_INFINITY,
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                        false
                                );*/
                    //return Transform.toWorld(location, new LocalLocation(offsetOf(Transform.toLocal(location, v.toVector())))).toVector().subtract(location.toVector());
                    //});
                })
                .toList();
        AxisAlignedBB aabb = new AxisAlignedBB(
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                false
        );
        for (BoxSphereCollider.Point point : points) {
            aabb = offsetOf(aabb, location, Transform.toLocal(location, point.toVector()));
        }
        /*
                //.map(Collider::round)
                .<AxisAlignedBB>collect(() -> new AxisAlignedBB(
                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                        false
                ), (a,b) -> {

                }, (a,b) -> {
                    a.intersect(b)
                });*/
        //moveTo.setX(0).setZ(0);
        location.add(moveTo.getX(), moveTo.getY(), moveTo.getZ());
        return location;
    }
    public void draw(Location location) {
        if (display) getAllSteps(location)
                .forEach(v -> {
                    if (tick == 0) Displays.drawPoint(v.global);

                    /*Displays.drawPoint(new Vector(0, RADIUS, 0).add(v.global), false);
                    Displays.drawPoint(new Vector(0, -RADIUS, 0).add(v.global), false);
                    Displays.drawPoint(new Vector(RADIUS, 0, 0).add(v.global), false);
                    Displays.drawPoint(new Vector(-RADIUS, 0, 0).add(v.global), false);
                    Displays.drawPoint(new Vector(0, 0, RADIUS).add(v.global), false);
                    Displays.drawPoint(new Vector(0, 0, -RADIUS).add(v.global), false);*/
                });
    }
}





















