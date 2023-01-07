package org.lime.gp.entity.collision;

import io.papermc.paper.util.CollisionUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.phys.AxisAlignedBB;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Stream;

public class BoxSphereCollider {
    private static double pow(double value) {
        return value * value;
    }
    //https://forum.sources.ru/index.php?showtopic=330868
    public record Point(double x, double y, double z) {
        public static final Point ZERO = new Point(0,0,0);

        public double of(int index) {
            return switch (index) {
                case 0 -> x;
                case 1 -> y;
                case 2 -> z;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
        public Point of(int index, double value) {
            return switch (index) {
                case 0 -> new Point(value, y, z);
                case 1 -> new Point(x, value, z);
                case 2 -> new Point(x, y, value);
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        public Vector toVector() { return new Vector(x,y,z); }

        public double distanceSqr(Point point) {
            return pow(this.x - point.x) + pow(this.y - point.y) + pow(this.z - point.z);
        }
    }
    public record Line(Point p1, Point p2) {
        public double lineDistanceSqr(Point point) {
            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double dz = p2.z - p1.z;
            double t = (dx * (point.x - p1.x) + dy * (point.y - p1.y) + dz * (point.z - p1.z)) / (pow(dx) + pow(dy) + pow(dz));
            t = Math.min(Math.max(t, 0), 1);
            double px = dx * t + p1.x;
            double py = dy * t + p1.y;
            double pz = dz * t + p1.z;
            return pow(point.x - px) + pow(point.y - py) + pow(point.z - pz);
        }
    }
    public record Box(Point min, Point max) {
        public Stream<Line> lines() {
            Point p1 = new Point(min.x, min.y, min.z);
            Point p2 = new Point(max.x, min.y, min.z);
            Point p3 = new Point(min.x, min.y, max.z);
            Point p4 = new Point(max.x, min.y, max.z);

            Point p5 = new Point(min.x, max.y, min.z);
            Point p6 = new Point(max.x, max.y, min.z);
            Point p7 = new Point(min.x, max.y, max.z);
            Point p8 = new Point(max.x, max.y, max.z);

            return Stream.of(
                    new Line(p1, p2),
                    new Line(p2, p3),
                    new Line(p3, p4),
                    new Line(p4, p1),

                    new Line(p5, p6),
                    new Line(p6, p7),
                    new Line(p7, p8),
                    new Line(p8, p5),

                    new Line(p1, p5),
                    new Line(p2, p6),
                    new Line(p3, p7),
                    new Line(p4, p8)
            );
        }
        public double length() {
            return pow(max.x - min.x) + pow(max.y - min.y) + pow(max.z - min.z);
        }
        public boolean isEmpty() {
            return max.x - min.x <= 0 || max.y - min.y <= 0 || max.z - min.z <= 0;
        }

        public static Box of(double width, double height, double length) {
            double dx = width / 2;
            double dy = height / 2;
            double dz = length / 2;
            return new Box(new Point(-dx, -dy, -dz), new Point(dx, dy, dz));
        }
        public static Box of(BlockPosition position, AxisAlignedBB aabb) {
            return new Box(
                    new Point(aabb.minX + position.getX(), aabb.minY + position.getY(), aabb.minZ + position.getZ()),
                    new Point(aabb.maxX + position.getX(), aabb.maxY + position.getY(), aabb.maxZ + position.getZ())
            );
        }
    }
    public record Sphere(Point center, double radius) {
        Optional<Double> distanceInside(Box box) {
            if (box.length() == 0) return Optional.empty();
            double distanceSqr = box.lines()
                    .mapToDouble(v -> v.lineDistanceSqr(center))
                    .min()
                    .orElse(0);
            if (distanceSqr > pow(radius)) return Optional.empty();
            double distance = Math.sqrt(distanceSqr);
            return Optional.of(radius - distance);
        }
        Optional<Double> distanceInside(Collection<Box> boxes) {
            return distanceInside(boxes.stream());
        }
        Optional<Double> distanceInside(Stream<Box> boxes) {
            return Optional.of(boxes.filter(box -> box.length() > 0)
                            .flatMap(Box::lines)
                            .mapToDouble(v -> v.lineDistanceSqr(center))
                            .min()
                    )
                    .filter(OptionalDouble::isPresent)
                    .map(OptionalDouble::getAsDouble)
                    .filter(distanceSqr -> distanceSqr <= pow(radius))
                    .map(Math::sqrt)
                    .map(distance -> radius - distance);
        }

        Optional<Point> pointInside(Box aabb, double modify) {
            double x = center.x;
            if (x < aabb.min.x) x = aabb.min.x;
            if (x > aabb.max.x) x = aabb.max.x;

            double y = center.y;
            if (y < aabb.min.y) y = aabb.min.y;
            if (y > aabb.max.y) y = aabb.max.y;

            double z = center.z;
            if (z < aabb.min.z) z = aabb.min.z;
            if (z > aabb.max.z) z = aabb.max.z;

            return Optional.of(new Point(x, y, z))
                    .filter(v -> v.distanceSqr(center) <= pow(radius * modify));
        }
        Optional<Point> pointInside(Box aabb) {
            double x = center.x;
            if (x < aabb.min.x) x = aabb.min.x;
            if (x > aabb.max.x) x = aabb.max.x;

            double y = center.y;
            if (y < aabb.min.y) y = aabb.min.y;
            if (y > aabb.max.y) y = aabb.max.y;

            double z = center.z;
            if (z < aabb.min.z) z = aabb.min.z;
            if (z > aabb.max.z) z = aabb.max.z;

            return Optional.of(new Point(x, y, z))
                    .filter(v -> v.distanceSqr(center) <= pow(radius));
        }
        Optional<Point> pointInside(Collection<Box> aabbs) {
            return pointInside(aabbs.stream());
        }
        Optional<Point> pointInside(Stream<Box> aabbs) {
            return aabbs.map(aabb -> {
                        double x = center.x;
                        if (x < aabb.min.x) x = aabb.min.x;
                        if (x > aabb.max.x) x = aabb.max.x;

                        double y = center.y;
                        if (y < aabb.min.y) y = aabb.min.y;
                        if (y > aabb.max.y) y = aabb.max.y;

                        double z = center.z;
                        if (z < aabb.min.z) z = aabb.min.z;
                        if (z > aabb.max.z) z = aabb.max.z;
                        return new Point(x, y, z);
                    })
                    .min(Comparator.comparingDouble(v -> v.distanceSqr(center)))
                    .filter(v -> v.distanceSqr(center) <= pow(radius));
        }

        public static Sphere of(Vector center, double radius) {
            return new Sphere(new Point(center.getX(), center.getY(), center.getZ()), radius);
        }
    }
    public record SphereGroup(List<Sphere> spheres) {
        public boolean isEmpty() {
            for (Sphere sphere : spheres)
                if (sphere.radius > 1.0E-7)
                    return true;
            return false;
        }
    }
    public record RotatedBox(AxisAlignedBB box, float yaw, float pitch) {
        public boolean isEmpty() {
            return CollisionUtil.isEmpty(box);
        }

        public static RotatedBox of(double width, double height, double length, float yaw, float pitch) {
            double dx = width / 2;
            double dy = height / 2;
            double dz = length / 2;
            return new RotatedBox(new AxisAlignedBB(-dx, -dy, -dz, dx, dy, dz, false), yaw, pitch);
        }

        public RotatedBox cutUpwards(double dy) {
            return new RotatedBox(CollisionUtil.cutUpwards(box, dy), yaw, pitch);
        }
        public RotatedBox cutDownwards(double dy) {
            return new RotatedBox(CollisionUtil.cutDownwards(box, dy), yaw, pitch);
        }
    }
}
















