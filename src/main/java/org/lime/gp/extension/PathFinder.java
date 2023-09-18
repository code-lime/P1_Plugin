package org.lime.gp.extension;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.util.Vector;
import org.lime.system.toast.*;

import java.util.HashMap;

public class PathFinder {
    private static BlockPosition toBlock(Vector vector) {
        return new BlockPosition(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    @SuppressWarnings("unused")
    private static boolean isEmpty(WorldServer reader, int x, int y, int z) {
        return isEmpty(reader, new Vector(x,y,z));
    }
    private static boolean isEmpty(WorldServer reader, Vector pos) {
        IBlockData data = reader.getBlockStateIfLoaded(toBlock(pos));
        return data == null || data.isAir();
    }
    private static boolean isEmpty(WorldServer reader, Location loc) {
        return isEmpty(reader, loc.toVector());
    }
    private static boolean locationMatch(Location loc1, Location loc2) {
        return locationMatch(loc1, loc2, 1.3);
    }
    private static boolean locationMatch(Location loc1, Location loc2, double radiusDistance) {
        if (loc1.getWorld() != loc2.getWorld()) return false;
        return loc1.distance(loc2) < radiusDistance;
    }

    /**
     * Calculates and returns the path to the target from the starting point.
     * This also accounts for pitch and yaw toward the target.
     *
     * @param start The location to start the path at
     * @param target The location to find the path towards, starting at the start
     * @return The path from the start to the target
     */
    public static Path calculate(Location start, Location target) {
        HashMap<Integer, double[]> locations = new HashMap<Integer, double[]>();
        World world = start.getWorld();
        WorldServer reader = ((CraftWorld)world).getHandle();
        Location current = start;//start.subtract(0, 1, 0);
        locations.put(0, getCoordinates(setLocationDirection(current, target)));
        for (int n = 1; n <= 1000; n++) {
            double H = Double.MAX_VALUE;
            Location correct = null;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location check = current.clone().add(x, 0, z);
                    double newH = check.distanceSquared(target);
                    if (!isEmpty(reader, check)) {
                        if (isEmpty(reader, check.clone().add(0, 1, 0)) && isEmpty(reader, check.clone().add(0, 2, 0))) {
                            check = check.clone().add(0, 1, 0);
                            newH = check.distanceSquared(target);
                        } else {
                            newH += 400;// 20 squared
                        }
                    }
                    if (newH < H) {
                        H = newH;
                        correct = check;
                    }
                }
            }
            boolean newNode = correct != null && H < Double.MAX_VALUE && !locationMatch(correct, current);
            Location found = setLocationDirection(newNode ? correct : current, target);
            locations.put(n, getCoordinates(found));
            if (!newNode) {
                break;
            }
            current = correct;
            if (locationMatch(target, current, 2)) {
                break;// target reached
            }
        }
        return new Path(world, locations);
    }

    /**
     * Gets a serializable array of coordinates for this location.
     *
     * [0] = x<br>
     * [1] = y<br>
     * [2] = z<br>
     * [3] = yaw<br>
     * [4] = pitch
     *
     * @param found The location to get the coordinates array for
     */
    public static double[] getCoordinates(Location found) {
        double[] coordinates = new double[5];
        coordinates[0] = found.getX();
        coordinates[1] = found.getY();
        coordinates[2] = found.getZ();
        coordinates[3] = found.getYaw();
        coordinates[4] = found.getPitch();
        return coordinates;
    }

    public static boolean pathReaches(Path path, Location loc) {
        for (int i = 0; i <= path.getRawNodesMap().keySet().size(); i++) {
            Location node = path.getNode(i);
            if (node == null || !locationMatch(loc, node)) {
                continue;
            }
            return true;
        }
        return true;
    }

    public static boolean pathReaches(Path path, Location loc, int radiusDistance) {
        for (int i = 0; i <= path.getRawNodesMap().keySet().size(); i++) {
            Location node = path.getNode(i);
            if (node == null) {
                continue;
            }
            if (!locationMatch(loc, node, radiusDistance)) {
                return false;
            }
        }
        return true;
    }

    public static Location setLocationDirection(Location loc, Location lookat) {
        loc = loc.clone();
        // double b = lookat.getX() - loc.getX();
        // double d = lookat.getY() - loc.getY();
        // double a = lookat.getZ() - loc.getZ();
        // double c = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
        // double e = Math.sqrt(Math.pow(c, 2) + Math.pow(d, 2));
        // loc.setYaw((float) Math.toDegrees(Math.asin(a / c)));
        // loc.setPitch((float) Math.toDegrees(Math.asin(d / e)));
        // or... -----------------------------------------------------
        double dx = lookat.getX() - loc.getX();
        double dy = lookat.getY() - loc.getY();
        double dz = lookat.getZ() - loc.getZ();
        if (dx != 0) {
            if (dx < 0) {
                loc.setYaw((float) (1.5 * Math.PI));
            } else {
                loc.setYaw((float) (0.5 * Math.PI));
            }
            loc.setYaw(loc.getYaw() - (float) Math.atan(dz / dx));
        } else if (dz < 0) {
            loc.setYaw((float) Math.PI);
        }
        double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));
        loc.setPitch((float) -Math.atan(dy / dxz));
        loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI);
        loc.setPitch(loc.getPitch() * 180f / (float) Math.PI);
        return loc;
    }

    public static HashMap<Toast3<Integer, Integer, Integer>, Double> getHeight(net.minecraft.world.level.World world, Vector center, int radius) {
        return getHeight(world, center.getBlockX(), center.getBlockY(), center.getBlockZ(), radius);
    }
    public static HashMap<Toast3<Integer, Integer, Integer>, Double> getHeight(net.minecraft.world.level.World world, int center_x, int center_y, int center_z, int radius) {
        return getHeight(world, center_x - radius, center_y - radius, center_z - radius, (radius * 2) + 1, (radius * 2) + 1, (radius * 2) + 1);
    }
    public static HashMap<Toast3<Integer, Integer, Integer>, Double> getHeight(net.minecraft.world.level.World world, Vector center, Vector size) {
        Vector min = new Vector()
                .add(center)
                .subtract(size);
        Vector _size = new Vector()
                .add(size)
                .multiply(2)
                .add(new Vector(1, 1, 1));

        return getHeight(world,
                min.getBlockX(),
                min.getBlockY(),
                min.getBlockZ(),

                _size.getBlockX(),
                _size.getBlockY(),
                _size.getBlockZ());
    }
    public static HashMap<Toast3<Integer, Integer, Integer>, Double> getHeight(net.minecraft.world.level.World world, int min_x, int min_y, int min_z, int size_x, int size_y, int size_z) {
        HashMap<Toast3<Integer, Integer, Integer>, Double> map = new HashMap<>();
        for (int _x = 0; _x < size_x; _x++) {
            int x = min_x + _x;
            for (int _y = 0; _y < size_y; _y++) {
                int y = min_y + _y;
                for (int _z = 0; _z < size_z; _z++) {
                    int z = min_z + _z;
                    BlockPosition position = new BlockPosition(x, y, z);
                    IBlockData block = world.getBlockState(position);
                    VoxelShape shape = block.getCollisionShape(world, position);
                    double h = shape.isEmpty() ? 0 : shape.bounds().maxY;

                    int offset_y = 0;
                    while (h > 1) {
                        map.merge(Toast.of(x,y + offset_y,z), 1.0, Math::max);
                        offset_y++;
                        h--;
                    }
                    map.merge(Toast.of(x,y + offset_y,z), h, Math::max);
                }
            }
        }
        return map;
    }

    public static class Path {
        private final HashMap<Integer, double[]> locations;
        private final World world;

        public int getSize() {
            return locations.size();
        }


        public Path(World world, HashMap<Integer, double[]> locations) {
            this.world = world;
            this.locations = locations;
        }

        public Location getEndNode() {
            for (int i = locations.size(); i > 0; i--) {
                Location node = getNode(i);
                if (node != null) {
                    return node;
                }
            }
            return null;
        }

        /**
         * Gets the location related to the number given.
         * The numbers are in sequence towards the target.
         *
         * @param nodeNumber The number of the point to get
         * @return The location related to the number given
         */
        public Location getNode(int nodeNumber) {
            if (locations.get(nodeNumber) != null) {
                double[] coords = locations.get(nodeNumber);
                return new Location(world, coords[0], coords[1], coords[2], (float) coords[3], (float) coords[4]);
            }
            return null;
        }

        public double getPitch(int nodeNumber) {
            return locations.get(nodeNumber)[4];
        }

        public HashMap<Integer, double[]> getRawNodesMap() {
            return locations;
        }

        public double getX(int nodeNumber) {
            return locations.get(nodeNumber)[0];
        }

        public double getY(int nodeNumber) {
            return locations.get(nodeNumber)[1];
        }

        public double getYaw(int nodeNumber) {
            return locations.get(nodeNumber)[3];
        }

        public double getZ(int nodeNumber) {
            return locations.get(nodeNumber)[2];
        }
    }
}

















