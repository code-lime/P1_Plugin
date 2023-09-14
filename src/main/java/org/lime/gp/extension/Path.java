package org.lime.gp.extension;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.lime.display.Displays;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.json;
import org.lime.system.utils.MathUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Path {
    private static final ConcurrentHashMap<UUID, PathData> savedData = new ConcurrentHashMap<>();
    private static class PathData {
        public final List<Vector> positions = new ArrayList<>();
        private Path path;
        private final World world;
        public PathData(World world) {
            this.world = world;
        }
        public final void add(Vector position) {
            positions.add(position);
            path = Path.of(world, positions);
        }
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            JsonArray arr = new JsonArray();
            positions.forEach(p -> arr.add(MathUtils.getString(p)));
            json.add("path", arr);
            json.addProperty("world", Bukkit.getWorlds().indexOf(path.world));
            return json;
        }

        public static boolean save(PathData data, String name) {
            if (data == null) return true;
            JsonObject _json = json.parse(lime.readAllConfig("path")).getAsJsonObject();
            _json.add(name, data.toJson());
            lime.writeAllConfig("path", json.format(_json));
            return true;
        }
    }
    private static final ConcurrentHashMap<UUID, Path> showData = new ConcurrentHashMap<>();
    private static boolean show(String key, Player player) {
        if (!has(key)) return true;
        showData.put(player.getUniqueId(), get(key));
        return true;
    }

    private static class Listener implements org.bukkit.event.Listener {
        @EventHandler public static void on(PlayerInteractEvent e) {
            if (e.getHand() != EquipmentSlot.HAND) return;
            Vector pos = new Vector(0,0,0);
            switch (e.getAction()) {
                case LEFT_CLICK_AIR:
                case RIGHT_CLICK_AIR:
                case PHYSICAL: return;
                case RIGHT_CLICK_BLOCK: pos = e.getBlockFace().getDirection();
                case LEFT_CLICK_BLOCK: pos = e.getClickedBlock().getLocation().toVector().add(pos); break;
            }
            PathData pathData = savedData.getOrDefault(e.getPlayer().getUniqueId(), null);
            if (pathData == null) return;
            pathData.add(pos);
        }
    }

    public static CoreElement create() {
        return CoreElement.create(Path.class)
                .disable()
                .withInstance(new Listener())
                .withInit(Path::init)
                .addCommand("path", _v -> _v
                        .withCheck(v -> v.isOp() && v instanceof Player)
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> Arrays.asList("start", "save", "cancel", "show", "hide", "reload", "single");
                            case 2 -> switch (args[0]) {
                                case "save" -> Collections.singletonList("[name]");
                                case "show" -> path_list.keySet();
                                case "single" -> Arrays.asList("on", "off");
                                default -> Collections.emptyList();
                            };
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> switch (args.length) {
                            case 1 -> switch (args[0]) {
                                case "start" -> savedData.put(((Player)sender).getUniqueId(), new PathData(((Player)sender).getWorld())) == null ? true : true;
                                case "cancel" -> savedData.remove(((Player)sender).getUniqueId()) == null ? true : true;
                                case "reload" -> reload();
                                case "hide" -> showData.remove(((Player)sender).getUniqueId()) == null ? true : true;
                                default -> false;
                            };
                            case 2 -> switch (args[0]) {
                                case "save" -> PathData.save(savedData.remove(((Player)sender).getUniqueId()), args[1]) || reload();
                                case "single" -> (show_single = args[1].equals("on")) ? true : true;
                                case "show" -> show(args[1], (Player)sender);
                                default -> false;
                            };
                            default -> false;
                        })
                );
    }
    private static final ConcurrentHashMap<String, Path> path_list = new ConcurrentHashMap<>();
    public static List<String> allPath() {
        return new ArrayList<>(path_list.keySet());
    }

    public static void init() {
        reload();
        lime.repeat(Path::update, 0.1);
    }
    private static int step = 0;
    private static final int max_step = 10;
    public static void update() {
        step = (step + 1) % max_step;
        double delta = step / (double)max_step;
        showData.forEach((k,v) -> show(Bukkit.getPlayer(k), v, delta, Particle.FLAME));
        savedData.forEach((k,v) -> show(Bukkit.getPlayer(k), v.path, delta, Particle.SOUL_FIRE_FLAME));
    }
    private static boolean show_single = true;
    private static void show(Player player, Path path, double delta, Particle particle) {
        if (path == null) return;
        double total = path.getTotal(true);
        double distance = 10;
        int size = path.path.size();
        for (int i = 0; i < size; i++) {
            Displays.drawPoint(path.path.get(i).position, i / (size - 1.0));
        }
        if (show_single) {
            particle.builder()
                    .location(path.getLocation(delta * total, true))
                    .count(1)
                    .force(false)
                    .extra(0)
                    .offset(0,0,0)
                    .receivers(player)
                    .spawn();
        } else {
            for (double i = delta * distance; i < total; i += distance) {
                particle.builder()
                        .location(path.getLocation(i, true))
                        .count(1)
                        .force(false)
                        .extra(0)
                        .offset(0,0,0)
                        .receivers(player)
                        .spawn();
            }
        }
    }
    public static boolean reload() {
        if (!lime.existConfig("path")) lime.writeAllConfig("path", "{}");
        JsonObject _json = json.parse(lime.readAllConfig("path")).getAsJsonObject();
        HashMap<String, Path> path_list = new HashMap<>();
        _json.entrySet().forEach(kv -> path_list.put(kv.getKey(), parse(path_list, kv.getValue().getAsJsonObject())));

        Path.path_list.clear();
        Path.path_list.putAll(path_list);
        return true;
    }
    public static Path get(String key) { return path_list.get(key); }
    public static boolean has(String key) { return path_list.containsKey(key); }

    public static class Part {
        @SuppressWarnings("unused")
        private enum Variable {
            FIRST(true, false),
            OTHER(false, false),
            LAST(false, true);

            public final boolean first;
            public final boolean last;

            Variable(boolean first, boolean last) {
                this.first = first;
                this.last = last;
            }
        }
        public final Vector position;
        public final double distance;
        @SuppressWarnings("unused")
        private final Variable variable;

        private Part(Vector position, double distance, Variable variable) {
            this.position = position;
            this.distance = distance;
            this.variable = variable;
        }

        public static List<Part> of(List<Vector> list) {
            List<Part> path = new LinkedList<>();
            //system.distinct(list);
            int size = list.size();

            Vector last = size == 0 ? null : list.get(size - 1);
            for (int i = 0; i < size; i++) {
                Vector pos = list.get(i);
                double distance = pos.distance(last);
                path.add(new Part(
                        pos,
                        distance,
                        i == 0 ? Part.Variable.FIRST : (i == size - 1 ? Part.Variable.LAST : Part.Variable.OTHER)
                ));
                last = pos;
            }
            return path;
        }
        public static List<Part> of(HashMap<String, Path> path_list, JsonArray json) {
            List<Vector> list = new ArrayList<>();
            String last = null;
            for (JsonElement item : json) {
                String curr = item.getAsString();
                if (curr.equals(last)) continue;
                last = curr;
                if (curr.toLowerCase().startsWith("path:")) list.addAll(path_list.get(curr.substring(5)).points());
                else list.add(MathUtils.getVector(curr));
            }
            return of(list);
        }
    }

    public final World world;
    public final List<Part> path;
    private final double _total;
    private final double _totalCircle;
    public double getTotal(boolean circle) { return circle ? _totalCircle : _total; }
    public Path(World world, List<Part> path) {
        this.world = world;
        this.path = path;
        this._total = path.stream().skip(1).mapToDouble(v -> v.distance).sum();
        this._totalCircle = path.stream().mapToDouble(v -> v.distance).sum();
    }
    public List<Vector> points() {
        return path.stream().map(v -> v.position).collect(Collectors.toList());
    }

    private Vector getOf(Vector from, Vector to, double delta) {
        double x = (1 - delta) * to.getX() + delta * from.getX();
        double y = (1 - delta) * to.getY() + delta * from.getY();
        double z = (1 - delta) * to.getZ() + delta * from.getZ();

        return new Vector(x + 0.5, y + 0.5, z + 0.5);
    }

    private Vector get(double point, boolean circle) {
        double total = getTotal(circle);
        int size = path.size();
        int length = size;
        if (circle) {
            point %= total;
            if (point < 0) point += total;
        } else {
            if (point < 0) return new Vector().add(path.get(0).position);
            if (point >= total) return new Vector().add(path.get(size - 1).position);
            length--;
        }
        for (int i = 0; i < length; i++) {
            Part next = path.get((i + 1) % size);
            Part part = path.get(i);
            double distance = next.distance;
            if (point >= distance) {
                point -= distance;
                continue;
            }
            return getOf(part.position, next.position, 1 - (point / distance));
        }
        return new Vector().add(path.get(0).position);
    }

    public Location getLocation(double point, boolean circle) {
        Vector pos = get(point, circle);
        Vector forward = get(point + 0.1, circle);
        return new Location(world, pos.getX(), pos.getY(), pos.getZ())
                .setDirection(new Vector().add(forward).subtract(pos));
    }
    public Location getLocation(double point, double size, boolean circle) {
        Vector pos1 = get(point + size / 2, circle);
        Vector pos2 = get(point - size / 2, circle);
        Vector pos = pos1.midpoint(pos2);
        return new Location(world, pos.getX(), pos.getY(), pos.getZ())
                .setDirection(new Vector().add(pos1).subtract(pos2));
    }

    public static Path parse(HashMap<String, Path> path_list, JsonObject json) {
        return new Path(Bukkit.getWorlds().get(json.get("world").getAsInt()), Part.of(path_list, json.get("path").getAsJsonArray()));
    }
    public static Path of(World world, List<Vector> list) {
        return new Path(world, Part.of(list));
    }
}


















