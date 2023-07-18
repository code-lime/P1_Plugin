package org.lime.gp.player.module;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.EntityBat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.horse.EntityHorse;
import net.minecraft.world.entity.animal.horse.EntityLlama;
import net.minecraft.world.entity.animal.horse.HorseColor;
import net.minecraft.world.entity.animal.horse.HorseStyle;
import net.minecraft.world.entity.monster.EntitySlime;
import net.minecraft.world.entity.monster.EntityVex;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.level.ChunkCache;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPanda;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftTropicalFish;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.display.DisplayManager;
import org.lime.display.Displays;
import org.lime.display.ObjectDisplay;
import org.lime.display.models.ChildDisplay;
import org.lime.display.models.Model;
import org.lime.gp.database.rows.PetsRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.extension.PathFinder;
import org.lime.gp.lime;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.module.JavaScript;
import org.lime.system;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class Pets {
    public static core.element create() {
        return core.element.create(Pets.class)
                //.disable()
                .withInit(Pets::init)
                //.withUninit(PetManager::uninit)
                .<JsonObject>addConfig("pets", v -> v
                        .withDefault(new JsonObject())
                        .withInvoke(Pets::config)
                        .orText("pets.js", _v -> _v
                                .withInvoke(t -> Pets.config(system.json.parse(JavaScript.getJsString(t).orElseThrow()).getAsJsonObject()))
                                .withDefault("{}")
                        )
                );
    }
    public static final ConcurrentHashMap<String, AbstractPet> pets = new ConcurrentHashMap<>();
    public static final PetDisplayManager MANAGER = new PetDisplayManager();

    private static class PetDisplay extends ObjectDisplay<PetsRow, Marker> {
        @Override public double getDistance() { return 30; }

        private PetsRow row;

        private final AbstractPet pet;
        private final ChildDisplay<PetsRow> model;
        private final Map<String, Object> data = new HashMap<>();
        private int step;
        private final double speed;
        private final int max_step;
        private Vector last_to = null;

        @Override public Location location() {
            EntityPosition.PositionInfo data = EntityPosition.playerInfo.get(row.uuid);
            if (data == null) return new Location(lime.LoginWorld,0,-100,0);
            Location location = data.location;
            if (location == null) return new Location(lime.LoginWorld,0,-100,0);
            Location loc = location.clone();
            loc.setPitch(0);

            Vector forward = loc.getDirection();
            Vector right = forward.getCrossProduct(new Vector(0, 1, 0));
            Vector pos = location.toVector();
            if (pet.fly) {
                double value = Math.sin(((step * 2.0 / max_step) - 1) * Math.PI);
                Vector to = new Vector().add(pos).add(forward.multiply(-1.5)).add(new Vector(0, 1.5, 0)).add(right.multiply(value * 2));

                if (last_to == null) last_to = to;

                Vector delta = to.clone().subtract(last_to);

                double curr_speed = to.distance(last_to);

                if (curr_speed > speed && curr_speed < 100) {
                    double _speed = speed;
                    if (curr_speed > 80) _speed = 10;
                    else if (curr_speed > 40) _speed = 5;
                    else if (curr_speed > 20) _speed = 3;
                    else if (curr_speed > 10) _speed = 1.5;
                    if (delta.lengthSquared() == 0) delta = new Vector(0,0,0);
                    else delta.normalize().multiply(_speed);
                    to = delta.clone().add(last_to);
                }
                last_to = to;
                return loc.set(to.getX(), to.getY(), to.getZ()).setDirection(delta.lengthSquared() == 0 ? new Vector(0,0,0) : delta.normalize());
            } else {
                if (last_to == null) last_to = loc.toVector();

                double distance = last_to.distance(loc.toVector());

                if (distance > 8) {
                    last_to = loc.toVector();
                    return loc;
                }

                net.minecraft.world.level.World world = ((CraftWorld)loc.getWorld()).getHandle();

                Location start = new Location(loc.getWorld(), last_to.getX(), last_to.getY(), last_to.getZ());
                if (distance > 2.5) {
                    Location end = loc.clone();

                    PathFinder.Path path = PathFinder.calculate(start, end);
                    double size = path.getSize();
                    /*drawPoint(start.toVector(), true);
                    drawPoint(end.toVector(), false);*/
                    if (size >= 2) {
                        Location to = path.getNode(1);
                        Vector adder = to.toVector().subtract(start.toVector());
                        double step = 0.1;
                        if (adder.length() > step) adder.normalize().multiply(step);
                        start.add(adder);
                    }
                    //path.getRawNodesMap().forEach((k,v) -> drawPoint(new Vector(v[0],v[1],v[2]),k/size));
                }

                BlockPosition position = new BlockPosition(start.getBlockX(), start.getBlockY(), start.getBlockZ());
                IBlockData block = world.getBlockState(position);
                VoxelShape shape = block.getCollisionShape(world, position);
                start.setY(start.getBlockY() + (shape.isEmpty() ? -0.01 : shape.bounds().maxY));

                Vector new_to = start.toVector();

                if (new_to.distance(last_to) < 0.011) start.setDirection(data.eye.toVector().subtract(new_to));
                else start.setDirection(start.toVector().subtract(new_to));

                last_to = new_to;

                return start;
            }
        }

        protected PetDisplay(AbstractPet pet, PetsRow row) {
            this.row = row;
            this.pet = pet;
            this.speed = pet.speed + system.rand(0, pet.speed * 0.05) * (system.rand() ? 1 : -1);
            this.max_step = pet.steps;
            this.step = system.rand(0, max_step);
            this.model = preInitDisplay(pet.model().display(this));
            postInit();
        }

        @Override public void update(PetsRow row, double delta) {
            step += 1;
            step %= max_step;
            this.row = row;
            super.update(row, delta);
            if (last_location == null) return;

            int id = row.id;

            if (row.name != null) {
                Location location = last_location;
                Component text = Component.text(row.name).color(row.color == null ? NamedTextColor.WHITE : TextColor.fromHexString("#" + row.color));
                int modelID = this.model.entityID;
                DrawText.show(new DrawText.IShowTimed(0.5) {
                    @Override public Optional<Integer> parent() { return Optional.of(modelID); }
                    @Override public String getID() { return "Pet["+id+"].NickName"; }
                    @Override public boolean filter(Player player) { return true; }
                    @Override public Component text(Player player) { return text; }
                    @Override public Location location() { return location; }
                    @Override public double distance() { return 5; }
                });
            }

            pet.tick(model, data);
            invokeAll(this::sendData);
        }

        @Override protected net.minecraft.world.entity.Marker createEntity(Location location) {
            return new net.minecraft.world.entity.Marker(EntityTypes.MARKER, ((CraftWorld)location.getWorld()).getHandle());
        }
    }
    private static class PetDisplayManager extends DisplayManager<Integer, PetsRow, PetDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }

        @Override public Map<Integer, PetsRow> getData() {
            return Tables.PETS_TABLE.getMapBy(v -> {
                if (v == null) return false;
                Player player = EntityPosition.onlinePlayers.getOrDefault(v.uuid, null);
                if (player == null) return false;
                if (v.isHide) return false;
                AbstractPet pet = pets.getOrDefault(v.pet, null);
                if (pet == null) return false;
                if (player.getGameMode() == GameMode.SPECTATOR) return false;
                if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return false;
                //return pet.fly;
                return true;
            }, v -> v.id);
        }
        @Override public PetDisplay create(Integer integer, PetsRow row) { return new PetDisplay(pets.get(row.pet), row); }
    }

    public static abstract class AbstractPet {
        public final String key;
        public final double speed;
        public final int steps;
        public final boolean fly;

        public abstract Model model();
        public abstract void tick(ChildDisplay<?> model, Map<String, Object> data);

        protected AbstractPet(String key, JsonObject json) {
            this.key = key;
            this.speed = json.has("speed") ? json.get("speed").getAsDouble() : 0.05;
            this.steps = json.has("steps") ? json.get("steps").getAsInt() : 500;
            this.fly = json.has("fly") && json.get("fly").getAsBoolean();
        }

        public static AbstractPet parse(String key, JsonObject json) {
            if (json.has("model")) return new ModelPet(key, json);
            return new VariablePet(key, json);
        }
    }
    public static class VariablePet extends AbstractPet {
        public final EntityTypes<? extends EntityLiving> type;
        public final system.Action1<Entity> variable;
        public final boolean baby;
        public final Model model;

        private static final ConcurrentHashMap<EntityTypes<? extends Entity>, Class<? extends Entity>> entityTypes = new ConcurrentHashMap<>();

        @Override public Model model() { return model; }
        @Override public void tick(ChildDisplay<?> model, Map<String, Object> data) {}

        @SuppressWarnings("all")
        protected VariablePet(String key, JsonObject json) {
            super(key, json);
            this.type = (EntityTypes<? extends EntityLiving>)EntityTypes.byString(json.get("type").getAsString()).get();
            this.baby = json.has("baby") && json.get("baby").getAsBoolean();

            Class<? extends Entity> tClass = entityTypes.computeIfAbsent(this.type, _type -> _type.create(lime.MainWorld.getHandle()).getClass());
            this.variable = variableApply(tClass, json.has("variable") && !json.get("variable").isJsonNull() ? json.get("variable").getAsString() : null);

            this.model = lime.models.builder(this.type)
                    .nbt(createEntity())
                    .collision(false)
                    .build();
        }

        private static MinecraftKey of(NamespacedKey key) {
            return new MinecraftKey(key.getNamespace(), key.getKey());
        }

        @SuppressWarnings("deprecation")
        private static system.Action1<Entity> variableApply(Class<? extends Entity> type, String variable) {
            system.Action1<Entity> apply = v -> {};
            if (variable == null) return apply;
            if (Axolotl.class.isAssignableFrom(type)) apply = apply.andThen(variableApply(Axolotl::setVariant, Axolotl.Variant.byId(org.bukkit.entity.Axolotl.Variant.valueOf(variable).ordinal())));
            if (EntityCat.class.isAssignableFrom(type)) apply = apply.andThen(variableApply(EntityCat::setVariant, BuiltInRegistries.CAT_VARIANT.get(of(Cat.Type.valueOf(variable).getKey()))));
            if (EntityFox.class.isAssignableFrom(type)) apply = apply.andThen(variableApply(EntityFox::setVariant, EntityFox.Type.values()[Fox.Type.valueOf(variable).ordinal()]));
            if (EntityHorse.class.isAssignableFrom(type)) {
                String[] vars = variable.split("&");
                apply = apply.andThen(variableApply(EntityHorse::setVariantAndMarkings, HorseColor.byId(Horse.Color.valueOf(vars[1]).ordinal()), HorseStyle.byId(Horse.Style.valueOf(vars[0]).ordinal())));
            }
            if (EntityLlama.class.isAssignableFrom(type)) apply = apply.andThen(variableApply(EntityLlama::setVariant, EntityLlama.Variant.byId(Llama.Color.valueOf(variable).ordinal())));
            if (EntityPanda.class.isAssignableFrom(type)) apply = apply.andThen(variableApply(EntityPanda::setMainGene, CraftPanda.toNms(Panda.Gene.valueOf(variable))));
            if (EntityParrot.class.isAssignableFrom(type)) apply = apply.andThen(variableApply(EntityParrot::setVariant, EntityParrot.Variant.byId(Parrot.Variant.valueOf(variable).ordinal())));
            if (EntityRabbit.class.isAssignableFrom(type)) apply = apply.andThen(variableApply(EntityRabbit::setVariant, EntityRabbit.Variant.byId(Rabbit.Type.valueOf(variable).ordinal())));
            if (EntitySheep.class.isAssignableFrom(type)) {
                if ("RANDOM".equals(variable)) variable = DyeColor.WHITE.name();
                apply = apply.andThen(variableApply(EntitySheep::setColor, EnumColor.byId(DyeColor.valueOf(variable).getWoolData())));
            }
            if (EntitySlime.class.isAssignableFrom(type)) apply = apply.andThen(variableApply(EntitySlime::setSize, Integer.valueOf(variable), false));
            if (EntityTropicalFish.class.isAssignableFrom(type)) {
                String[] vars = variable.split("&");
                apply = apply.andThen(variableApply(EntityTropicalFish::setPackedVariant, DyeColor.valueOf(vars[0]).getWoolData() << 24 | DyeColor.valueOf(vars[1]).getWoolData() << 16 | CraftTropicalFish.CraftPattern.values()[TropicalFish.Pattern.valueOf(vars[2]).ordinal()].getDataValue()));
            }
            if (EntityBee.class.isAssignableFrom(type)) {
                String[] vars = variable.split("&");

                boolean nectar = switch (vars[0]) { case "NECTAR" -> true; case "NONE" -> false; default -> throw new IllegalArgumentException("Type '"+vars[0]+"' not supported"); };
                boolean stung = switch (vars[1]) { case "STUNG" -> true; case "NONE" -> false; default -> throw new IllegalArgumentException("Type '"+vars[1]+"' not supported"); };
                boolean anger = switch (vars[2]) { case "ANGER" -> true; case "NONE" -> false; default -> throw new IllegalArgumentException("Type '"+vars[2]+"' not supported"); };

                apply = apply.andThen(variableApply(EntityBee::setHasNectar, nectar));
                apply = apply.andThen(variableApply(EntityBee::setHasStung, stung));
                apply = apply.andThen(variableApply(EntityBee::setRemainingPersistentAngerTime, anger ? 1000 : -1));
            }
            if (EntityVex.class.isAssignableFrom(type)) {
                boolean charging = switch (variable) { case "CHARGING" -> true; case "NONE" -> false; default -> throw new IllegalArgumentException("Type '"+variable+"' not supported"); };
                apply = apply.andThen(variableApply(EntityVex::setIsCharging, charging));
            }
            return apply;
        }
        
        @SuppressWarnings("unchecked")
        private static <T extends Entity, V>system.Action1<Entity> variableApply(system.Action2<T, V> apply, V value) {
            return e -> apply.invoke((T)e, value);
        }
        
        @SuppressWarnings("unchecked")
        private static <T extends Entity, V1, V2>system.Action1<Entity> variableApply(system.Action3<T, V1, V2> apply, V1 value1, V2 value2) {
            return e -> apply.invoke((T)e, value1, value2);
        }

        private EntityLiving createEntity() {
            EntityLiving entity = type.create(lime.MainWorld.getHandle());
            if (entity instanceof EntityBat bat) bat.setResting(false);
            if (baby && entity instanceof EntityAgeable age) age.setAge(-1);
            variable.invoke(entity);
            return entity;
        }
    }
    public static class ModelPet extends AbstractPet {
        public final Model model;
        public final String animation_tick;
        public final HashMap<String, Object> animation_args = new HashMap<>();

        @Override public Model model() { return model; }
        @Override public void tick(ChildDisplay<?> model, Map<String, Object> data) {
            if (animation_tick == null) return;
            JavaScript.invoke(animation_tick,
                    system.map.<String, Object>of()
                            .add(animation_args, k -> k, v -> v)
                            .add("data", data)
                            .build()
            );
            this.model.animation.apply(model.js, data);
        }

        private static Object toObj(JsonPrimitive json) {
            return json.isNumber() ? json.getAsNumber() : json.isBoolean() ? json.getAsBoolean() : json.getAsString();
        }

        protected ModelPet(String key, JsonObject json) {
            super(key, json);
            String modelKey = json.get("model").getAsString();
            if (json.has("animation")) {
                JsonObject animation = json.getAsJsonObject("animation");
                animation_tick = animation.has("tick") ? animation.get("tick").getAsString() : null;
                if (animation.has("args")) animation.getAsJsonObject("args").entrySet().forEach(kv -> animation_args.put(kv.getKey(), toObj(kv.getValue().getAsJsonPrimitive())));
            } else {
                animation_tick = null;
            }
            this.model = lime.models.get(modelKey).orElseGet(() -> {
                lime.logOP("Model '"+modelKey+"' in pet '"+key+"' not founded!");
                return lime.models.empty();
            });
        }
    }

    private static BlockPosition toBlock(Vector vector) {
        return new BlockPosition(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }
    private static ChunkCache getChunkCache(net.minecraft.world.level.World world, Vector start, Vector target) {
        return new ChunkCache(world,
                toBlock(start),
                toBlock(target)
        );
    }
    private static IBlockData getBlockOrNull(ChunkCache cache, Vector pos) { return getBlockOrNull(cache, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()); }
    private static IBlockData getBlockOrNull(ChunkCache cache, int x, int y, int z) { return getBlockOrNull(cache, new BlockPosition(x, y, z)); }
    private static IBlockData getBlockOrNull(ChunkCache cache, BlockPosition pos) { return cache.getBlockStateIfLoaded(pos); }
    private static boolean isEmpty(ChunkCache cache, int x, int y, int z) {
        return isEmpty(cache, new Vector(x,y,z));
    }
    private static boolean isEmpty(ChunkCache cache, Vector pos) {
        IBlockData data = cache.getBlockStateIfLoaded(toBlock(pos));
        return data == null || data.isAir();
    }
    private static boolean isEmpty(ChunkCache cache, Location loc) {
        return isEmpty(cache, loc.toVector());
    }
    private static boolean isState(ChunkCache cache, Vector pos) {
        IBlockData data = cache.getBlockStateIfLoaded(toBlock(pos));
        if (data == null) return false;
        Material material = data.getBukkitMaterial();
        return  !material.isAir() && material.isSolid() && material.isBlock();
    }

    /*private static void drawMoved(Location location) {
        Vector delta = new Vector(10, 10, 10);
        Vector pos = location.toVector();
        pos.setX(pos.getBlockX());
        pos.setY(pos.getBlockY());
        pos.setZ(pos.getBlockZ());
        ChunkCache cache = getChunkCache(location.getWorld(), pos.clone().subtract(delta), pos.clone().add(delta));
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    Vector center = new Vector(x,y,z).add(pos);
                    Vector up = new Vector(0, 1, 0).add(center);
                    Vector down = new Vector(0, -1, 0).add(center);
                    if (!isEmpty(cache, up) || !isEmpty(cache, center) || isEmpty(cache, down)) continue;
                    drawPoint(new Vector(0.5, 0.5, 0.5).add(center));
                }
            }
        }
    }
    private static void drawPath(World world, Vector from, Vector to) {
        /*Vector delta = new Vector(10, 10, 10);
        ChunkCache cache = getChunkCache(world, from.clone().subtract(delta), to.clone().add(delta));
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    Vector center = new Vector(x,y,z).add(pos);
                    Vector up = new Vector(0, 1, 0).add(center);
                    Vector down = new Vector(0, -1, 0).add(center);
                    if (!isEmpty(cache, up) || !isEmpty(cache, center) || isEmpty(cache, down)) continue;
                    drawPoint(new Vector(0.5, 0.5, 0.5).add(center));
                }
            }
        }*/
        /*double[] from = null;
        for (double[] to : path.getRawNodesMap().values())
        {
            if (from == null) {
                from = to;
                continue;
            }

            Vector _from = new Vector(from[0], from[1], from[2]);
            Vector _to = new Vector(to[0], to[1], to[2]);

            int size = 5;

            Vector _delta = _to.clone().subtract(_from).multiply(1.0/size);
            Vector pos = _from.clone();
            for (int i = 0; i < size; i++) {
                drawPoint(pos);
                pos.add(_delta);
            }

            from = to;
        }*/
    //}*/
    public static void init() {
        /*lime.RepeatTicks(() -> {
            Bukkit.getOnlinePlayers().forEach(v -> drawMoved(v.getLocation()));
            drawPath(lime.MainWorld, new Vector(-102.5, 4.5, 92.5), Bukkit.getOnlinePlayers().stream().findFirst().get().getLocation().toVector());
        }, 1);*/
    }
    /*public static class PetEntity {
        public DataReader.PetsRow row;
        public final
    }
    public static final HashMap<Integer, PetEntity> entities = new HashMap<>();
    public static void init() {

    }
    public static void uninit() {

    }*/
    public static void config(JsonObject json) {
        /*json = system.json.object()
                .add("temp001", system.json.object()
                        .add("type", "bee")
                        .add("speed", 0.02)
                        .add("steps", 500)
                        .add("baby", true)
                        .add("fly", true)
                )
                .add("temp???", system.json.object()
                        .add("type", "axolotl")
                        .add("speed", 0.06)
                        .add("steps", 500)
                        .add("baby", true)
                        .add("fly", true)
                )
                .add("temp002", system.json.object()
                        .add("type", "bat")
                        .add("speed", 0.06)
                        .add("steps", 500)
                        .add("fly", true)
                )
                .add("temp003", system.json.object()
                        .add("type", "phantom")
                        .add("speed", 0.1)
                        .add("steps", 250)
                        .add("fly", true)
                )
                .add("temp004", system.json.object()
                        .add("type", "vex")
                        .add("speed", 0.06)
                        .add("steps", 500)
                        .add("fly", true)
                )
                .add("temp005", system.json.object()
                        .add("type", "parrot")
                        .add("speed", 0.04)
                        .add("steps", 400)
                        .add("fly", true)
                )
                .add("temp006", system.json.object()
                        .add("type", "pig")
                        .add("speed", 0.04)
                        .add("steps", 500)
                        .add("fly", false)
                        .add("baby", true)
                )
                .build();*/
        HashMap<String, AbstractPet> pets = new HashMap<>();
        json.entrySet().forEach(kv -> pets.put(kv.getKey(), AbstractPet.parse(kv.getKey(), kv.getValue().getAsJsonObject())));
        Displays.uninitDisplay(MANAGER);
        Pets.pets.clear();
        Pets.pets.putAll(pets);
        Displays.initDisplay(MANAGER);
    }

    /*public static class PathFinder {
        private static class Tile {
            public int X;
            public int Y;
            public int Cost;
            public int Distance;
            public Tile Parent;

            public Tile(int x, int y, Tile parent, int cost) {
                this.X = x;
                this.Y = y;
                this.Cost = cost;
                this.Parent = parent;
            }

            public int getCostDistance() { return Cost + Distance; }
            public void SetDistance(int targetX, int targetY) { this.Distance = Math.abs(targetX - X) + Math.abs(targetY - Y); }
        }
        private static List<Tile> GetWalkableTiles(boolean[][] map, Tile currentTile, Tile targetTile)
        {
            var maxX = map[0].length - 1;
            var maxY = map.length - 1;

            Arrays.asList(
                    new Tile(currentTile.X, currentTile.Y - 1, currentTile, currentTile.Cost + 1),
                    new Tile(currentTile.X, currentTile.Y + 1, currentTile, currentTile.Cost + 1),
                    new Tile(currentTile.X - 1, currentTile.Y, currentTile, currentTile.Cost + 1),
                    new Tile(currentTile.X + 1, currentTile.Y, currentTile, currentTile.Cost +)
            ).stream().
            var possibleTiles = new List<Tile>()
            {
                new Tile { X = currentTile.X, Y = currentTile.Y - 1, Parent = currentTile, Cost = currentTile.Cost + 1 },
                    new Tile { X = currentTile.X, Y = currentTile.Y + 1, Parent = currentTile, Cost = currentTile.Cost + 1},
                    new Tile { X = currentTile.X - 1, Y = currentTile.Y, Parent = currentTile, Cost = currentTile.Cost + 1 },
                    new Tile { X = currentTile.X + 1, Y = currentTile.Y, Parent = currentTile, Cost = currentTile.Cost + 1 },
            };

            possibleTiles.ForEach(tile => tile.SetDistance(targetTile.X, targetTile.Y));

            return possibleTiles
                    .Where(tile => tile.X >= 0 && tile.X <= maxX)
                    .Where(tile => tile.Y >= 0 && tile.Y <= maxY)
                    .Where(tile => map[tile.Y][tile.X] == 0)
                    .ToList();
        }
    }*/
}



































