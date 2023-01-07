package p1.data;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.gson.JsonObject;
import curve.PolygonClipper;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.World;
import net.minecraft.world.level.material.FluidTypeWater;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.system;
import p1.*;
import InputEvent;
import PathFinder;
import Input;
import Model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class CarManager implements Listener {
    public static core.element create() {
        return core.element.create(CarManager.class)
                .disable()
                .withInit(CarManager::init)
                .addCommand("spawn.car", v -> v
                        .withUsage("/spawn.car [car]")
                        .withCheck(s -> s instanceof Player p && p.isOp())
                        .withTab((system.Func0<Collection<String>>)cars::keySet)
                        .withExecutor((s,a) -> {
                            Player player = (Player)s;
                            if (a.length != 1) return false;
                            String carKey = a[0];
                            CarData carData = cars.getOrDefault(carKey, null);
                            if (carData == null) {
                                s.sendMessage("Car not founded '"+carKey+"'");
                                return true;
                            }
                            s.sendMessage("Spawned car '"+carKey+"'");
                            carData.spawn(player.getLocation());
                            return true;
                        })
                )
                .addCommand("sc", v -> v
                        .withCheck(s -> s instanceof Player p && p.isOp())
                        .withExecutor(s -> {
                            Player player = (Player)s;

                            CarDisplay display = manager.getDisplays().values().stream().findFirst().orElse(null);
                            if (display == null) return true;
                            Model.ChildDisplay<CarMeta> sit = display.model.singleOfKey("driver.sit");
                            if (sit == null) return true;
                            sit.sit(player);

                            return true;
                        })
                )
                .addCommand("sc2", v -> v
                        .withCheck(s -> s instanceof Player p && p.isOp())
                        .withExecutor(s -> {
                            Player player = (Player)s;
                            Integer vehicle = Displays.getVehicle(player.getEntityId());
                            if (vehicle == null) return false;
                            Model.ChildDisplay<?> display = Displays.byID(Model.ChildDisplay.class, vehicle);
                            if (display == null) return false;
                            if (!(display.objectParent() instanceof CarDisplay car)) return false;
                            UUID uuid = car.meta.getLoaded().getUniqueId();
                            CustomMeta.LoadedEntity.ofSync(uuid, CarMeta.class, meta -> {
                                meta.fuel += 1;
                            });
                            return true;
                        })
                )
                .<JsonObject>addConfig("cars", v -> v.withInvoke(CarManager::config).withDefault(new JsonObject()))
                .withInstance();
    }

    public static class CarData {
        public final String type;
        public final String model;
        public final double size;
        public final Vector scale;
        public final Vector offset;

        public final double fuel_sec;

        private final Input original_input;

        public Model model() { return Model.get(model); }
        public Input getInput(system.Func0<Player> driver) { return original_input.deepClone(driver); }

        public CarData(String type, JsonObject json) {
            this.type = type;
            this.size = json.get("size").getAsDouble();
            this.model = json.get("model").getAsString();
            this.scale = system.getVector(json.get("scale").getAsString());
            this.offset = system.getVector(json.get("offset").getAsString());
            this.original_input = Input.of(json.get("input").getAsJsonObject(), null);
            this.fuel_sec = json.has("fuel_sec") ? json.get("fuel_sec").getAsDouble() : 0;
        }
        public static CarData parse(String type, JsonObject json) { return new CarData(type, json); }
        public void spawn(Location location) {
            CustomMeta.spawnEntityMeta(location, entity -> {
                CarMeta meta = entity.getOrAdd(CarMeta.class);
                meta.type = type;
                return meta;
            });
        }
    }
    public static class CarMeta extends CustomMeta.IEntityMeta<JsonObject> {
        private String type;
        public double fuel;
        public CarData getData() { return cars.getOrDefault(type, null); }

        @Override public void create() { }
        @Override public void destroy() { }

        @Override public void read(JsonObject json) {
            type = json.get("type").getAsString();
            fuel = json.has("fuel") ? json.get("fuel").getAsInt() : 0;
        }
        @Override public JsonObject write() {
            return system.json.object()
                    .add("type", type)
                    .add("fuel", fuel)
                    .build();
        }
    }

    public static final ConcurrentHashMap<String, CarData> cars = new ConcurrentHashMap<>();
    public static void config(JsonObject json) {
        HashMap<String, CarData> cars = new HashMap<>();
        json.entrySet().forEach(kv -> cars.put(kv.getKey(), CarData.parse(kv.getKey(), kv.getValue().getAsJsonObject())));
        CarManager.cars.clear();
        CarManager.cars.putAll(cars);
        Displays.uninitDisplay(manager);
        Displays.initDisplay(manager);
    }

    public static final CarDisplayManager manager = new CarDisplayManager();
    private static <T>List<T> randomize(List<T> list) {
        Collections.shuffle(list);
        return list;
    }
    public static void init() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(lime._plugin, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketPlayInUseEntity use = (PacketPlayInUseEntity)event.getPacket().getHandle();
                use.dispatch(new PacketPlayInUseEntity.c() {
                    @Override public void onAttack() { onInteraction(EnumHand.MAIN_HAND); }
                    @Override public void onInteraction(EnumHand enumHand) { onInteraction(enumHand, Vec3D.ZERO); }
                    @Override public void onInteraction(EnumHand enumHand, Vec3D vec3D) {
                        PlayerUseUnknownEntityEvent e = new PlayerUseUnknownEntityEvent(
                                event.getPlayer(),
                                use.getEntityId(),
                                use.getActionType() == PacketPlayInUseEntity.b.ATTACK,
                                enumHand == EnumHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND
                        );
                        lime.invokeSync(() -> on(e));
                    }
                });
            }
        });
        /*lime.NextTick(() -> {
            int count = 0;
            for (int i = 0; i < 1000; i++) {

                List<system.Toast2<Double, Double>> points = PolygonClipper.clip(
                        randomize(Arrays.asList(system.toast(0.0,3.0),system.toast(0.5,0.5),system.toast(3.0,0.0),system.toast(0.5,-0.5),system.toast(0.0,-3.0),system.toast(-0.5,-0.5),system.toast(-3.0,0.0),system.toast(-0.5,0.5))),
                        randomize(Arrays.asList(system.toast(-2.0,-2.0),system.toast(-2.0,2.0),system.toast(2.0,2.0),system.toast(2.0,-2.0))),
                        true
                );
                if (PolygonClipper.sort(points).equals(points)) count++;
                lime.LogOP("I: " + count + " / " + (i + 1));
            }
        });*/
        lime.timer().setAsync().withLoopTicks(1).withCallback(() -> {
            CarDisplay.velocity_list.removeIf(kv -> {
                kv.val2--;
                return kv.val2 <= 0;
            });
            CarDisplay.velocity_check.removeIf(kv -> {
                kv.val1.invoke(kv.val0);
                return true;
            });
        }).run();

        lime.repeat(() -> manager.getDisplays().forEach((uuid, display) -> {
            if (!display.hasDriver()) return;
            CustomMeta.LoadedEntity.ofSync(uuid, CarMeta.class, meta -> {
                double _new = Math.max(0, meta.fuel - display.data.fuel_sec);
                if (meta.fuel == _new) return false;
                meta.fuel = _new;
                return true;
            });
        }), 1);

        //Displays.initDisplay(new TempDisplayManager());
    }
    public static void on(PlayerUseUnknownEntityEvent e) {
        if (e.isAttack()) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Model.ChildDisplay<?> display = Displays.byID(Model.ChildDisplay.class, e.getEntityId());
        if (display == null) return;
        if (!(display.objectParent() instanceof CarDisplay car)) return;
        if (display.keys().contains("gas_tank")) {
            ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
            if (item.getType() != Material.LAVA_BUCKET) return;
            CustomMeta.LoadedEntity.ofSync(car.meta.getLoaded().getUniqueId(), CarMeta.class, meta -> { meta.fuel += 1; });
            item.setType(Material.BUCKET);
            return;
        }
        Integer id = display.keys().stream().filter(v -> v.startsWith("sit.input.")).findFirst().map(v -> Integer.parseInt(v.substring(10))).orElse(null);
        if (id == null) return;
        List<Model.ChildDisplay<CarMeta>> sits = car.model.ofKey("sit." + id);
        for (Model.ChildDisplay<CarMeta> sit : sits) {
            if (sit.hasSitter()) continue;
            sit.sit(e.getPlayer());
            return;
        }
    }
    @EventHandler public static void on(InputEvent e) {
        if (!e.isUnmount()) return;
        Integer vehicle = Displays.getVehicle(e.getPlayer().getEntityId());
        if (vehicle == null) return;
        Model.ChildDisplay<?> display = Displays.byID(Model.ChildDisplay.class, vehicle);
        if (display == null) return;
        if (!(display.objectParent() instanceof CarDisplay car)) return;
        display.unsit();
    }

    /*public static class TempDisplay extends Displays.ObjectDisplay<Displays.LocalLocation, net.minecraft.world.entity.Marker> {
        @Override public double getDistance() { return 200; }
        @Override public Location location() { return Displays.Transform.toWorld(new Location(lime.MainWorld, -336.5, 73.5, -191.5), local); }

        public final Model.ChildDisplay<Displays.LocalLocation> model;
        private final Displays.LocalLocation local;
        protected TempDisplay(Displays.LocalLocation local) {
            this.local = local;
            this.model = preInitDisplay(Model.Variable.TEMP.get().display(this));
            postInit();
        }

        @Override protected net.minecraft.world.entity.Marker createEntity(Location location) {
            return new Marker(EntityTypes.Y, ((CraftWorld)location.getWorld()).getHandle());
        }
    }
    public static class TempDisplayManager extends Displays.DisplayManager<Integer, Displays.LocalLocation, TempDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }

        @Override public Map<Integer, Displays.LocalLocation> getData() {
            Displays.LocalLocation owner = new Displays.LocalLocation(0, 0, 0, 0, 0);
            HashMap<Integer, Displays.LocalLocation> list = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                owner = owner.setYaw(i * 3);
                Displays.LocalLocation local = new Displays.LocalLocation(0, 0, 3, 0, 0);
                list.put(i, owner = owner.combine(local));
            }
            return list;
        }
        @Override public TempDisplay create(Integer id, Displays.LocalLocation data) {
            lime.LogOP("L: " + data);
            return new TempDisplay(data);
        }
    }*/

    public static class CarDisplay extends Displays.ObjectDisplay<CarMeta, net.minecraft.world.entity.Marker> {
        private static class CarVelocity {
            public static class Result {
                public final List<BlockFace> faces;
                public final Vector velocity;
                public final double square;

                public Result(List<BlockFace> faces, Vector velocity, double square) {
                    this.faces = faces;
                    this.velocity = velocity;
                    this.square = square;
                }
            }
            private static int sindex = 0;
            private final int index = sindex++;
            private final Location location;
            private final Vector scale;
            private final Vector offset;
            private final HashSet<system.Toast3<Integer, Integer, Integer>> locals;

            public CarVelocity(Location location, Vector scale, Vector offset, HashSet<system.Toast3<Integer, Integer, Integer>> locals) {
                this.location = location;
                this.locals = locals;
                this.scale = scale;
                this.offset = offset;
            }

            private static List<system.Toast2<Double, Double>> block(system.Toast3<Double, Integer, Double> pos) {
                return PolygonClipper.sort(Arrays.asList(
                        system.toast(pos.val0 - 0.5, pos.val2 - 0.5),
                        system.toast(pos.val0 - 0.5, pos.val2 + 0.5),
                        system.toast(pos.val0 + 0.5, pos.val2 + 0.5),
                        system.toast(pos.val0 + 0.5, pos.val2 - 0.5)
                ));
            }
            private List<system.Toast2<Double, Double>> collider() {
                List<Displays.LocalLocation> list = new ArrayList<>();

                int scale_x = (int)Math.ceil(scale.getX() / 2);
                int scale_z = (int)Math.ceil(scale.getZ() / 2);

                list.add(new Displays.LocalLocation(new Vector(-scale_x, 0, -scale_z).add(offset)));
                list.add(new Displays.LocalLocation(new Vector(scale_x, 0, scale_z).add(offset)));
                list.add(new Displays.LocalLocation(new Vector(-scale_x, 0, scale_z).add(offset)));
                list.add(new Displays.LocalLocation(new Vector(scale_x, 0, -scale_z).add(offset)));

                List<system.Toast2<Double, Double>> collider = new ArrayList<>();
                Displays.Transform.toWorld(location, list).forEach(pos -> collider.add(system.toast(pos.getX(), pos.getZ())));
                return collider;
            }

            public void invoke(String key) {
                List<system.Toast2<Double, Double>> collider = collider();
                List<system.Toast3<Double, Integer, Double>> locals = new ArrayList<>();
                this.locals.forEach(local -> locals.add(system.toast(local.val0 + 0.5, local.val1, local.val2 + 0.5)));
                PolygonClipper.sort(collider);
                locals.forEach(pos -> {
                    Displays.drawPoint(new Vector(pos.val0, pos.val1 + 0.5, pos.val2));
                    List<system.Toast2<Double, Double>> clip = PolygonClipper.clip(block(pos), collider, false);
                    if (clip.size() == 0) return;
                    double square = PolygonClipper.square(clip);
                    system.Toast2<Double, Double> center = PolygonClipper.center(clip);
                    Vector velocity = new Vector(center.val0, pos.val1, center.val1).subtract(location.toVector()).multiply(-1);//.normalize().multiply(-square);

                    List<BlockFace> faces = new ArrayList<>();

                    if (center.val0 < pos.val0) faces.add(BlockFace.EAST);
                    else if (center.val0 > pos.val0) faces.add(BlockFace.WEST);

                    if (center.val1 < pos.val2) faces.add(BlockFace.SOUTH);
                    else if (center.val1 > pos.val2) faces.add(BlockFace.NORTH);

                    velocity.multiply(square);

                    //lime.LogOP("F: " + location.toVector().add(velocity) + " A " + clip.size());

                    Displays.drawPoint(location.toVector().add(velocity));

                    velocity_list.add(system.toast(key, new Result(faces, velocity, square), 10));
                });
            }
        }
        private static final ConcurrentLinkedQueue<system.Toast3<String, CarVelocity.Result, Integer>> velocity_list = new ConcurrentLinkedQueue<>();
        private static final ConcurrentLinkedQueue<system.Toast2<String, CarVelocity>> velocity_check = new ConcurrentLinkedQueue<>();

        @Override public double getDistance() { return 200; }
        @Override public Location location() { return meta.getLocation(); }

        public final Model.ChildDisplay<CarMeta> model;
        public final CarData data;
        public CarMeta meta;

        public Input input;
        public final static double totalWaterTimeout = 20;
        public double waterTimeout;

        public Player getDriver() {
            Model.ChildDisplay<?> sit = model.singleOfKey("driver.sit");
            if (sit == null) return null;
            return sit.sitter();
        }
        public boolean hasDriver() {
            Model.ChildDisplay<?> sit = model.singleOfKey("driver.sit");
            if (sit == null) return false;
            return sit.hasSitter();
        }

        private static boolean isWater(Location location) {
            Block block = location.getBlock();
            return block.getType() == Material.WATER || (block.getBlockData() instanceof CraftBlockData cbd && cbd.getState().getFluidState().getType() instanceof FluidTypeWater);
        }

        protected CarDisplay(CarMeta meta) {
            this.meta = meta;
            this.data = meta.getData();
            this.model = preInitDisplay(this.data.model().display(this));
            this.input = this.data.getInput(this::getDriver);
            postInit();
            Location location = location();
            waterTimeout = isWater(location) ? totalWaterTimeout : 0;
            this.last_height = location.getY();
        }

        private double last_wheel_rotation;
        private double last_height;

        private static Vector middle(List<Vector>... lists) {
            Vector center = new Vector();
            int count = 0;
            for (List<Vector> list : lists) {
                count += list.size();
                list.forEach(center::add);
            }
            return center.divide(new Vector(count, count, count));
        }

        @Override public void update(CarMeta meta, double delta) {
            this.meta = meta;
            Location location = location();
            waterTimeout = isWater(location)
                    ? Math.min(totalWaterTimeout, waterTimeout + delta)
                    : Math.max(0, waterTimeout - delta);
            super.update(meta, delta);
            Integer send = null;
            double isWater = Math.min(1, (Math.min(totalWaterTimeout, Math.max(0, waterTimeout)) / totalWaterTimeout) * 2);
            boolean isFuel = true;//meta.fuel > 0;
            boolean isDisable = isWater >= 1;
            if (!isFuel) isDisable = true;
            if (isDisable) this.input.stop();
            double add = (isDisable ? 0 : 1) * (1 - isWater) * this.input.update(delta);
            double rotation = this.input.getRotation() * 45;
            double wheel_rotation = data.size * Math.tan(Math.toRadians(90 - rotation));
            if (last_wheel_rotation != wheel_rotation) send = 0;
            last_wheel_rotation = wheel_rotation;
            this.model.ofKey("wheel-forward").forEach(wheel -> wheel.offset = wheel.offset.setYaw((float)rotation));

            system.Toast2<List<Model.ChildDisplay<CarMeta>>, List<Model.ChildDisplay<CarMeta>>> point = system.toast(new ArrayList<>(), new ArrayList<>());
            this.model.ofKey("point").forEach(model -> model.keys().forEach(key -> {
                switch (key) {
                    case "forward": point.val0.add(model); break;
                    case "backward": point.val1.add(model); break;
                }
            }));
            if (isFuel) this.model.ofKey("gas_tank.output").forEach(model -> {
                Particle.REDSTONE.builder()
                        .allPlayers()
                        .force(false)
                        .count(1)
                        .extra(0)
                        .offset(0,0,0)
                        .data(new Particle.DustOptions(Color.fromRGB(0x252525), 1.5f))
                        .location(model.lastLocation())
                        .spawn();
            });
            double this_height;
            double last_height = this.last_height;
            int count0 = point.val0.size();
            int count1 = point.val1.size();
            double angle;
            HashMap<system.Toast3<Integer, Integer, Integer>, Double> height;
            if (count0 != 0 && count1 != 0) {
                List<Vector> pos_forward = new ArrayList<>();
                List<Vector> pos_backward = new ArrayList<>();
                Vector pos_center = new Vector();
                point.val0.forEach(v -> {
                    Vector pos = v.lastLocation().toVector();
                    pos_center.add(pos);
                    pos_forward.add(pos);
                });
                point.val1.forEach(v -> {
                    Vector pos = v.lastLocation().toVector();
                    pos_center.add(pos);
                    pos_backward.add(pos);
                });
                int count_total = count0 + count1;
                pos_center.divide(new Vector(count_total, count_total, count_total));

                World world = ((CraftWorld)meta.getWorld()).getHandle();
                height = PathFinder.getHeight(world, location().toVector(), new Vector(5, 1, 5));

                system.Func1<Vector, Vector> editHeight = pos -> {
                    int x = pos.getBlockX();
                    int y = pos.getBlockY();
                    int z = pos.getBlockZ();

                    double _h_up = height.getOrDefault(system.toast(x, y + 1, z), 0.0);
                    double _h_zero = height.getOrDefault(system.toast(x, y, z), 0.0);
                    double _h_down = height.getOrDefault(system.toast(x, y - 1, z), 0.0);

                    if (_h_zero == 1) _h_zero += _h_up;
                    else if (_h_up > 0) _h_zero += 1 + _h_up;
                    else if (_h_zero == 0) _h_zero -= 1 - _h_down;

                    return new Vector().add(pos).setY(y + _h_zero);
                };
                system.Func1<List<Vector>, List<Vector>> editHeightList = pos_list -> pos_list.stream().map(editHeight).collect(Collectors.toList());

                List<Vector> forward = editHeightList.invoke(pos_forward);
                List<Vector> backward = editHeightList.invoke(pos_backward);
                Vector fore = middle(forward);
                Vector back = middle(backward);

                //lime.LogOP("D:" + system.getDouble(fore.distance(back)) + ":" + system.getDouble(middle(pos_forward).distance(middle(pos_backward))));

                Vector center = fore.getMidpoint(back);

                double f1 = center.getY();
                double f2 = pos_center.getY();
                double f_delta = f1 - f2;

                if (f_delta > 0.5) f_delta = 0.5;
                else if (f_delta < -0.5) f_delta = -0.5;
                else if (Math.abs(f_delta) < 0.0001) f_delta = 0;

                double double_height = system.round(last_height - f2, 5);

                double _fore = fore.getY();
                double _back = back.getY();

                double _distance = fore.distance(back);
                double _height = _back - _fore;

                angle = Math.min(20, Math.max(-20, Math.toDegrees(Math.asin(_height / _distance))));

                this.last_height = this_height = f1 + (f_delta / 10) + double_height;

                //lime.LogOP("H:\n   " + system.getDouble(__h) + " / " + system.getDouble(_height) + "\n   " + system.getDouble(f_delta / 10));

                if (Math.abs(last_height - this_height) < 0.0001) this.last_height = this_height = last_height;
            } else return;

            int scale_x = (int)Math.ceil(data.scale.getX());
            int scale_y = (int)Math.ceil(data.scale.getY() / 2);
            int scale_z = (int)Math.ceil(data.scale.getZ());

            HashSet<system.Toast3<Integer, Integer, Integer>> locals = new HashSet<>();

            Location location2D = location.clone();
            location2D.setPitch(0);
            for (double x = -scale_x; x <= scale_x; x+=0.5) {
                for (double y = 1; y <= scale_y * 2; y+=0.5) {
                    for (double z = -scale_z; z <= scale_z; z+=0.5) {
                        Vector local = new Vector(x + 0.5, y + 0.5, z + 0.5);
                        local = Displays.Transform.toWorld(location2D, new Displays.LocalLocation(local.add(data.offset))).toVector();
                        system.Toast3<Integer, Integer, Integer> pos = system.toast(local.getBlockX(), local.getBlockY(), local.getBlockZ());
                        double h = height.getOrDefault(pos, 0.0);
                        if (h == 0) continue;
                        Displays.drawPoint(new Vector(pos.val0 + 0.5, pos.val1 + 0.5, pos.val2 + 0.5), h);
                        locals.add(pos);
                    }
                }
            }
            String key = meta.getKey();
            velocity_check.add(system.toast(key, new CarVelocity(location, data.scale, data.offset, locals)));
            List<BlockFace> faces = new ArrayList<>();
            Vector velocity = new Vector();
            system.Toast1<Integer> count = system.toast(0);
            system.Toast1<Double> square = system.toast(0.0);
            velocity_list.removeIf(kv -> {
                if (!key.equals(kv.val0)) return false;
                count.val0++;
                faces.addAll(kv.val1.faces);
                velocity.add(kv.val1.velocity);
                return true;
            });
            int _count = count.val0;
            if (_count > 0) {
                velocity.divide(new Vector(_count, _count, _count));
            }
            if (Double.isNaN(square.val0)) square.val0 = 0.0;
            if (Double.isNaN(velocity.getX()) || Double.isNaN(velocity.getY()) || Double.isNaN(velocity.getZ())) velocity.copy(new Vector());

            double delta_height = (this_height + last_height) / 2;

            location.setY((location.getY() + delta_height) / 2);
            double rot = location.getYaw();
            double a = wheel_rotation == 0 ? 0 : (add / wheel_rotation);
            rot += Math.toDegrees(a);

            location.setPitch((float)system.round(((location.getPitch() + angle) / 2), 5));
            location.setYaw((float) rot);
            add += 2 * (angle / 360.0);
            Location old = location.clone();
            location.add(location.getDirection().multiply(add));

            location.add(velocity);
            faces.forEach(face -> {
                int mod_x = face.getModX();
                int mod_z = face.getModZ();
                if (mod_x != 0) {
                    double _a = location.getX();
                    double _b = old.getX();
                    if (Double.compare(_a, _b) == mod_x) {
                        this.input.stop();
                        location.setX(_b);
                    }
                }
                if (mod_z != 0) {
                    double _a = location.getZ();
                    double _b = old.getZ();
                    if (Double.compare(_a, _b) == mod_x) {
                        this.input.stop();
                        location.setZ(_b);
                    }
                }
            });

            if (send == null && !Displays.isEqualsLocation(lastLocation(), location, -1))
                send = 1;

            meta.edit(marker -> marker.teleport(location));
            last_location = location;
            if (send == null) return;
            ISE = (ISE + 1) % 3;
            if (ISE > 0) return;
            this.invokeAll(this::sendData);
        }
        private int ISE = 0;
        @Override protected net.minecraft.world.entity.Marker createEntity(Location location) {
            return new Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
        }
    }
    public static class CarDisplayManager extends Displays.DisplayManager<UUID, CarMeta, CarDisplay> {
        @Override public boolean isFast() { return true; }

        @Override public Map<UUID, CarMeta> getData() { return CustomMeta.LoadedEntity.allReadOnly(CarMeta.class).stream().collect(Collectors.toMap(k -> k.getLoaded().getUniqueId(), v -> v)); }
        @Override public CarDisplay create(UUID uuid, CarMeta carMeta) { return new CarDisplay(carMeta); }
    }
}
    /*public static class Car {
        public final CarData car;
        public final String data;
        public final Input input;

        public final system.LockToast1<Double> angle = system.toast(0.0).lock();
        public final system.LockToast1<Double> speed = system.toast(0.0).lock();

        private Car(ArmorStand stand) {
            this.base = stand;
            JsonObject json = JManager.FromContainer(JsonObject.class, stand.getPersistentDataContainer(), "car_data", null);
            data = json.get("data").getAsString();
            car = datas.getOrDefault(data, null);
        }
        public void update() {
            /*speed.
            speed = speed - speed % 0.005;
            if (Math.abs(speed) <= 0.009) speed = 0;
            double angle = Math.toRadians(this.angle);
            Vector forward = new Vector(1, 0, 0).rotateAroundY(angle).multiply(speed * 0.1);
            Location center = base.getLocation().add(forward);
            center.setYaw(0);
            center.setPitch(0);
            base.teleport(center);
            base.setHeadPose(new EulerAngle(0, angle, 0));

            ScoreboardUI.SendFakeScoreboard(getInputPlayer(), "car", system.map.<String, Integer>of()
                    .add("Type: " + data, -1)
                    .add("Pos: " + system.getString(center.toVector()), -2)
                    .add("*" + system.getDouble(speed), -3)
                    .add("^" + system.getDouble(angle), -4)
                    .build()
            );

            sits.forEach((local,sit) -> {
                Location location = center.clone().add(new Vector().add(local).rotateAroundY(angle));
                ((CraftEntity)sit)
                        .getHandle()
                        .setPositionRotation(
                                location.getX(),
                                location.getY(),
                                location.getZ(),
                                location.getYaw(),
                                location.getPitch()
                        );
            });*//*
        }

        public void input(InputEvent.Axis horizontal, InputEvent.Axis vertical) {
            /*switch (vertical) {
                case Down: speed = Math.max(speed - 0.01, -2); break;
                case Up: speed = Math.min(speed + 0.01, 3); break;
            }
            switch (horizontal) {
                case Down: angle = angle - 0.3 * speed; break;
                case Up: angle = angle + 0.3 * speed; break;
            }*//*
        }
    }

    public static void init() {
        AnyEvent.AddEvent("car.spawn", AnyEvent.type.owner, v -> v.CreateParam(datas::get, datas::keySet), (player, car) -> car.spawn(player.getLocation()));
        AnyEvent.AddEvent("car.action", AnyEvent.type.owner, v -> v.CreateParam(_v -> cars.get(UUID.fromString(_v)), () -> cars.keySet().stream().map(UUID::toString).collect(Collectors.toList())).CreateParam("sit"), (player, car, action) -> {
            switch (action) {
                case "sit": {
                    Entity input = car.getInput();
                    input.getPassengers().forEach(input::removePassenger);
                    input.addPassenger(player);
                    return;
                }
            }
        });
        lime.RepeatTicks(CarManager::update, 1);
        lime.Repeat(CarManager::updateInit, 1);
        BlockContainer.loadMeta(CarData.CarMeta.class, v -> v.withUpdate(CarData.CarMeta::update));
        Displays.InitDisplay(new CarData.CarDisplayManager());
    }
    public static void updateInit() {
        /*Bukkit.getWorlds().forEach(world -> world.getEntities().forEach(entity -> {
            Set<String> tags = entity.getScoreboardTags();
            UUID uuid = entity.getUniqueId();
            if (tags.contains("car") && entity instanceof ArmorStand stand) {
                if (cars.containsKey(uuid)) return;
                cars.put(uuid, new Car(stand));
            } else if (tags.contains("car_part")) {
                if (cars.values().stream().noneMatch(car -> car.sits.values().stream().anyMatch(v -> v.getUniqueId().equals(uuid)))) {
                    PacketManager.GetEntityHandle(entity).killEntity();
                    lime.LogOP("KILL: " + uuid);
                }
            }
        }));*//*
    }
    public static void update() {
        cars.values().forEach(Car::update);
    }

    /*private static class CarDisplay extends Displays.ObjectDisplay<NPC.NPCObject, EntityPlayer> {
        private static class NPCName extends Displays.ObjectDisplay<NPC.NPCObject, EntityAreaEffectCloud> {
            @Override public int getDistance() {
                return 15;
            }
            private final Component name;
            private final Displays.ObjectDisplay<?, ?> parent;
            private boolean isInit = false;
            protected NPCName(Displays.ObjectDisplay<?, ?> parent, Component name, Location location) {
                super(location);
                this.name = name;
                this.parent = parent;
                postInit();
            }
            @Override protected EntityAreaEffectCloud createEntity(Location location) {
                EntityAreaEffectCloud stand = new EntityAreaEffectCloud(
                        ((CraftWorld)location.getWorld()).getHandle(),
                        location.getBlockX(), location.getBlockY(), location.getBlockZ());
                stand.setDuration(2000000000);
                stand.R = 2000000000;
                stand.setCustomName(ChatHelper.toNMS(name));
                stand.setRadius(0);
                stand.setCustomNameVisible(true);
                stand.setOnGround(true);
                stand.setNoGravity(true);
                return stand;
            }
            @Override protected void Show(Player player) {
                if (!isInit) {
                    Displays.addPassengerID(this.parent.entityID, entityID);
                    isInit = true;
                }
                super.Show(player);
                lime.Once(() -> {
                    WrapperPlayServerMount mount = new WrapperPlayServerMount();
                    mount.setEntityID(this.parent.entityID);
                    mount.setPassengerIds(Displays.getPassengerIDs(this.parent.entityID));
                    mount.sendPacket(player);
                }, 1);
            }
        }

        @Override public int getDistance() {
            return 40;
        }
        public double getTargetDistance() {
            return 10;
        }

        private final NPC.NPCObject npc;
        public final List<Pair<EnumItemSlot, ItemStack>> equipment;
        public boolean invisible = false;
        public NPC.NPCDisplay.SkinPart skinPart = NPC.NPCDisplay.SkinPart.All;
        public static final DataWatcherObject<Byte> SKIN_PART = new DataWatcherObject<>(13, DataWatcherRegistry.a);

        private Location target = null;

        @Override public boolean IsFilter(Player player) {
            return npc.isShow(player.getUniqueId());
        }

        @Override public Location getLocation() {
            return getByTarget(target, null);
        }

        private Location getByTarget(Location target, Double minDistance) {
            Location location = super.getLocation();
            if (target == null) return location;
            if (minDistance != null && (location.getWorld() != target.getWorld() || location.distance(target) > minDistance)) return location;
            return location.clone().setDirection(target.clone().subtract(location).toVector());
        }

        public enum SkinPart {
            None(0),
            Cape(1 << 0),
            Jacket(1 << 1),
            LeftSleeve(1 << 2),
            RightSleeve(1 << 3),
            LeftPantsLeg(1 << 4),
            RightPantsLeg(1 << 5),
            Hat(1 << 6),
            All(Combine(Cape, Jacket, LeftSleeve, RightSleeve, LeftPantsLeg, RightPantsLeg, Hat));

            byte bit;

            SkinPart(int bit) {
                this.bit = (byte)bit;
            }

            public static byte Combine(NPC.NPCDisplay.SkinPart... parts) {
                byte bit = 0;
                for (NPC.NPCDisplay.SkinPart part : parts) bit |= part.bit;
                return bit;
            }
        }

        protected NPCDisplay(NPC.NPCObject npc) {
            super(npc.location);
            this.npc = npc;
            this.equipment = npc.createEquipment();

            system.Toast1<Displays.ObjectDisplay<?, ?>> parent = system.toast(this);
            npc.name.forEach(name -> {
                NPC.NPCDisplay.NPCName npcName = new NPC.NPCDisplay.NPCName(parent.val0, ChatHelper.FormatComponent(name), npc.location);
                parent.val0 = npcName;
                preInitDisplay(npcName);
            });

            postInit();
        }
        @Override protected void SendData(Player player) {
            PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook relMoveLook = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(entityID, (short)0, (short)0, (short)0, (byte)0, (byte)0, true);
            PacketPlayOutNamedEntitySpawn ppones = new PacketPlayOutNamedEntitySpawn(entity);
            PacketPlayOutPlayerInfo ppopi_add = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, entity);
            PacketPlayOutPlayerInfo ppopi_del = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e, entity);
            PacketPlayOutEntityEquipment ppoee = equipment.size() <= 0 ? null : new PacketPlayOutEntityEquipment(entityID, equipment);

            //DataWatcher w = entity.getDataWatcher(); //DataWatcher for client side NPC
            //if (invisible) w.set(new DataWatcherObject<>(0, DataWatcherRegistry.a), (byte) 0x20); //0x20 makes NPC invisible but still rendered
            //PacketPlayOutEntityMetadata ppoem = new PacketPlayOutEntityMetadata(entityID, w, true);

            PacketManager.SendPackets(player, ppopi_add, ppones, relMoveLook, ppoee);
            lime.Once(() -> PacketManager.SendPacket(player, ppopi_add), 0.5);
            lime.Once(() -> PacketManager.SendPacket(player, ppopi_del), 5);
            super.SendData(player);
        }

        private static UUID createUUID() {
            String uuid = UUID.randomUUID().toString();
            return UUID.fromString(uuid.substring(0,14)+'1'+uuid.substring(15));
        }


        @Override protected EntityPlayer createEntity(Location location) {
            UUID fakePlayerUUID = createUUID();

            WorldServer world = ((CraftWorld)location.getWorld()).getHandle();
            EntityPlayer fakePlayer = new EntityPlayer(
                    ((CraftServer)Bukkit.getServer()).getServer(),
                    world,
                    SkinManager.SetSkinOrDownload(new GameProfile(fakePlayerUUID, ""), npc.skin)
            );

            fakePlayer.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            return fakePlayer;
        }

        @Override
        protected void editDataWatcher(Player player, Displays.EditedDataWatcher dataWatcher) {
            //dataWatcher.setCustom(SKIN_PART, (byte)0xFF);
            //dataWatcher.set(, (byte)0xFF);

            //lime.LogOP("DATA: " + dataWatcher.get(new DataWatcherObject<>(16, DataWatcherRegistry.a)).byteValue());
            dataWatcher.setCustom(EntityHuman.bP, Byte.MAX_VALUE);

            super.editDataWatcher(player, dataWatcher);
        }

        @Override public void Update(NPC.NPCObject npc) {
            if (npc.single) {
                Location location = getLocation();
                entity.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

                super.Update(npc);

                PacketPlayOutEntityTeleport movePacket = new PacketPlayOutEntityTeleport(entity);
                PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(entity, (byte) MathHelper.d((location.getYaw() % 360.0F) * 256.0F / 360.0F));
                this.InvokeAll(player -> PacketManager.SendPackets(player, movePacket, headPacket));
            } else {
                super.Update(npc);

                this.InvokeAll(player -> {
                    Location location = getByTarget(player.getLocation(), getTargetDistance());
                    WrapperPlayServerEntityTeleport wpset = new WrapperPlayServerEntityTeleport();
                    wpset.setEntityID(entityID);
                    wpset.setLocation(location);
                    wpset.setOnGround(entity.isOnGround());
                    wpset.sendPacket(player);

                    PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(entity, (byte)MathHelper.d((location.getYaw() % 360.0F) * 256.0F / 360.0F));
                    PacketManager.SendPacket(player, headPacket);
                });
            }
        }
        public void OnClick(Player player, boolean isShift) {
            String menu = npc.menu;
            if (isShift) menu = npc.shift_menu == null ? menu : npc.shift_menu;
            MenuCreator.Show(player, menu);
        }

        @Override public void Hide(Player player) {
            super.Hide(player);
            PacketPlayOutPlayerInfo ppopi = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e, entity);
            PacketManager.SendPackets(player, ppopi);
        }

        public void Update() {
            if (npc.single) {
                Player near = this.GetNearShow(getTargetDistance(), p -> p.getGameMode() != GameMode.SPECTATOR);
                target = near == null ? null : near.getLocation();
            } else {
                target = null;
            }
            Update(npc);
        }

        public static NPC.NPCDisplay create(String key, NPC.NPCObject npc) {
            return new NPC.NPCDisplay(npc);
        }
    }
    private static class CarDisplayManager extends Displays.DisplayManager<String, NPC.NPCObject, NPC.NPCDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }

        @Override public HashMap<String, Car> getData() { return npc_list; }
        @Override public CarDisplay create(String key, NPC.NPCObject npc) {
            return CarDisplay.create(key, npc);
        }
        public void Update() {
            this.getDisplays().values().forEach(NPC.NPCDisplay::Update);
        }
    }*/






















