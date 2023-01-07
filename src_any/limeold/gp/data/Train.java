package org.limeold.gp.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.EnumItemSlot;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Train implements Listener {
    public static core.element create() {
        return core.element.create(Train.class)
                .withInstance()
                .disable()
                .<JsonObject>addConfig("train", v -> v.withDefault(new JsonObject()).withInvoke(Train::config))
                .withInit(Train::init);
    }
    public static final ConcurrentHashMap<String, TrainPath> path_list = new ConcurrentHashMap<>();
    public static class TrainPath {
        public static class Part {
            public final Vector position;
            public final double distance;
            private final boolean first;
            public Part next;
            public Part back;

            public Part(Vector position, double distance, boolean first) {
                this.position = position;
                this.distance = distance;
                this.first = first;
            }

            public Part add(Vector position) {
                Part part = new Part(position, this.position.distance(position), false);
                next = part;
                part.back = this;
                return part;
            }

            public Vector get(double point) {
                if (point <= 0) return back.position;
                if (point > distance) return next.get(point - distance);
                double delta = point / distance;

                Vector from = position;
                Vector to = back.position;

                double x = (1 - delta) * to.getX() + delta * from.getX();
                double y = (1 - delta) * to.getY() + delta * from.getY();
                double z = (1 - delta) * to.getZ() + delta * from.getZ();

                return new Vector(x+0.5, y+0.5, z+0.5);
            }
            public double total() { return total(true); }
            private double total(boolean first) { return (this.first ? (first ? next.total(false) : 0) : next.total(false)) + distance; }

            public static Part parse(JsonArray json) {
                Part part = null;
                Part first = null;
                for (JsonElement item : json) {
                    Vector pos = system.getVector(item.getAsString());
                    if (part == null) first = part = new Part(pos, 0, true);
                    else part = part.add(pos);
                }
                if (first == null) throw new IllegalArgumentException("PATH LENGTH ERROR");
                part.next = first;
                first.back = part;
                return first;
            }
        }
        public final World world;
        public final Part first;
        public final double total;
        public TrainPath(World world, Part first) {
            this.world = world;
            this.first = first;
            this.total = first.total();
        }
        public Location getLocation(double point) {
            Vector pos = first.get(point);
            Vector forward = first.get(point + 0.1);
            return new Location(world, pos.getX(), pos.getY(), pos.getZ())
                    .setDirection(new Vector().add(forward).subtract(pos));
        }
        public Location getLocation(double point, double size) {
            Vector pos1 = first.get(point + size / 2);
            Vector pos2 = first.get(point - size / 2);
            Vector pos = pos1.midpoint(pos2);
            return new Location(world, pos.getX(), pos.getY(), pos.getZ())
                    .setDirection(new Vector().add(pos1).subtract(pos2));
        }

        public static TrainPath parse(JsonObject json) {
            return new TrainPath(Bukkit.getWorlds().get(json.get("world").getAsInt()), Part.parse(json.get("path").getAsJsonArray()));
        }
    }
    public static final ConcurrentHashMap<String, TrainObject> train_list = new ConcurrentHashMap<>();
    public static class TrainObject {
        public static class TrainModel {
            public final double size;
            public final List<Vector> ddraw = new ArrayList<>();
            public TrainModel(JsonObject json) {
                size = json.get("size").getAsDouble();
                json.get("ddraw").getAsJsonArray().forEach(i -> ddraw.add(system.getVector(i.getAsString())));
            }
        }
        public final String path;
        public final system.LockToast1<Double> point = system.toast(0.0).lock();
        public final system.LockToast1<Double> speed = system.toast(0.0).lock();
        public final List<TrainModel> parts = new ArrayList<>();
        public Location position(int index) {
            TrainPath path = path_list.getOrDefault(this.path, null);
            if (path == null) return new Location(lime.MainWorld, 0, 100, 0);
            double prefix = 0;
            for (int i = 0; i < index; i++) prefix += parts.get(i).size;
            return path.getLocation(point.edit0(v -> v % path.total) + parts.get(index).size, prefix);
        }

        public TrainObject(JsonObject json) {
            this.path = json.get("path").getAsString();
            HashMap<String, TrainModel> models = new HashMap<>();
            json.get("models").getAsJsonObject().entrySet().forEach(kv -> models.put(kv.getKey(), new TrainModel(kv.getValue().getAsJsonObject())));
            json.get("parts").getAsJsonArray().forEach(e -> parts.add(models.get(e.getAsString())));
            speed.edit0(v -> v + 0.75);
        }

        public void move(double mult) {
            point.edit0(v -> v + speed.get0() * mult);
        }
        public List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipment(int index) {
            List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> equipment = new ArrayList<>();
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.GLASS);
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(99002);
            item.setItemMeta(meta);
            equipment.add(new Pair<>(EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(item)));
            return equipment;
        }

        public static TrainObject parse(JsonObject json) {
            return new TrainObject(json);
        }
    }
    public static void init() {
        lime.timer()
                .withCallback(Train::updateAsync)
                .withWaitTicks(1)
                .withLoopTicks(1)
                .setAsync()
                .run();
    }
    public static double step = 0;
    public static void updateAsync(double delta) {
        train_list.forEach((k,v) -> v.move(1.0/delta));
    }
    public static void config(JsonObject json) {
        HashMap<String, TrainPath> path_list = new HashMap<>();
        json.get("path").getAsJsonObject().entrySet().forEach(kv -> path_list.put(kv.getKey(), TrainPath.parse(kv.getValue().getAsJsonObject())));

        HashMap<String, TrainObject> train_list = new HashMap<>();
        json.get("train").getAsJsonObject().entrySet().forEach(kv -> train_list.put(kv.getKey(), TrainObject.parse(kv.getValue().getAsJsonObject())));

        Train.train_list.clear();
        Train.train_list.putAll(train_list);
        Train.path_list.clear();
        Train.path_list.putAll(path_list);
    }
}





































