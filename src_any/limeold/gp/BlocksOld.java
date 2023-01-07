package org.lime.gp.block;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.entity.TileEntitySkullEventDestroy;
import net.minecraft.world.level.block.BlockSkullEventInteract;
import net.minecraft.world.level.block.BlockSkullEventShape;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftSkull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.persistence.PersistentDataContainer;
import org.lime.Position;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.component.Components;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.extension.JManager;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.gp.item.Items;
import org.lime.gp.item.Settings;
import org.lime.gp.lime;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.system;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlocksOld implements Listener {
    public static core.element create() {
        return core.element.create(BlocksOld.class)
                .withInstance()
                .disable()
                .withInit(BlocksOld::init)
                .<JsonObject>addConfig("blocks", v -> v.withDefault(new JsonObject()).withInvoke(BlocksOld::config))
                .addCommand("set.block ", v -> v.withCheck(ServerOperator::isOp)
                        .withUsage("/set.block [x:int,~] [y:int,~] [z:int,~] [block:key] {rotation:null/"+Arrays.stream(InfoComponent.Rotation.Value.values()).map(_v -> _v.angle + "").collect(Collectors.joining("/"))+"} {data:json}")
                        .withTab((sender, args) -> switch(args.length) {
                            case 1,2,3 -> Collections.singletonList(Optional.ofNullable(sender instanceof Player player ? player.getTargetBlock(5) : null)
                                    .map(p -> p.getLocation().toVector())
                                    .map(p -> switch (args.length) {
                                        case 1 -> p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
                                        case 2 -> p.getBlockY() + " " + p.getBlockZ();
                                        case 3 -> "" + p.getBlockZ();
                                        default -> "";
                                    })
                                    .orElse("~"));
                            case 4 -> creators.keySet();
                            case 5 -> Streams.concat(Stream.of("null"), Arrays.stream(InfoComponent.Rotation.Value.values()).map(_v -> _v.angle + "")).collect(Collectors.toList());
                            default -> Collections.singletonList("{data}");
                        })
                        .withExecutor((sender, args) -> {
                            if (args.length < 4) return false;
                            Optional<Location> player_location = Optional.ofNullable(sender instanceof Player p ? p : null).map(Entity::getLocation);
                            Integer x = tryParse(args[0]).or(() -> player_location.map(Location::getBlockX)).orElse(null);
                            if (x == null) {
                                sender.sendMessage("Value '"+args[0]+"' of argument 'x' is not supported!");
                                return true;
                            }
                            Integer y = tryParse(args[1]).or(() -> player_location.map(Location::getBlockY)).orElse(null);
                            if (y == null) {
                                sender.sendMessage("Value '"+args[1]+"' of argument 'y' is not supported!");
                                return true;
                            }
                            Integer z = tryParse(args[2]).or(() -> player_location.map(Location::getBlockZ)).orElse(null);
                            if (z == null) {
                                sender.sendMessage("Value '"+args[2]+"' of argument 'z' is not supported!");
                                return true;
                            }
                            World world = player_location.map(Location::getWorld).orElse(lime.MainWorld);
                            InfoCreator creator = creators.getOrDefault(args[3], null);
                            if (creator == null) {
                                sender.sendMessage("Value '"+args[3]+"' of argument 'block' is not supported!");
                                return true;
                            }
                            InfoComponent.Rotation.Value rotation;
                            if (args.length < 5 || args[4].equals("null")) {
                                rotation = null;
                            } else {
                                InfoComponent.Rotation.Value _rotation = tryParse(args[4])
                                        .map(_angle -> { try { return InfoComponent.Rotation.Value.ofAngle(_angle); } catch (Exception e) { return null; } })
                                        .orElse(null);
                                if (_rotation == null) {
                                    sender.sendMessage("Value '"+args[4]+"' of argument 'rotation' is not supported!");
                                    return true;
                                }
                                rotation = _rotation;
                            }
                            String data = Arrays.stream(args).skip(5).collect(Collectors.joining(" "));
                            if (data.length() == 0) data = "{}";
                            if (rotation == null) creator.setBlock(new Position(world, x, y, z), system.json.parse(data).getAsJsonObject());
                            else creator.setMultiBlock(new Position(world, x, y, z), system.json.parse(data).getAsJsonObject(), rotation);
                            sender.sendMessage("Block '"+args[3]+"' set in "+x+" "+y+" "+z);
                            return true;
                        })
                )
                .addCommand("marker.block ", v -> v.withCheck(ServerOperator::isOp)
                        .withUsage("/marker.block [x:int,~] [y:int,~] [z:int,~] [format:force|native]")
                        .withTab((sender, args) -> switch(args.length) {
                            case 1,2,3 -> Collections.singletonList(Optional.ofNullable(sender instanceof Player player ? player.getTargetBlock(5) : null)
                                    .map(p -> p.getLocation().toVector())
                                    .map(p -> switch (args.length) {
                                        case 1 -> p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
                                        case 2 -> p.getBlockY() + " " + p.getBlockZ();
                                        case 3 -> "" + p.getBlockZ();
                                        default -> "";
                                    })
                                    .orElse("~"));
                            case 4 -> Arrays.asList("force", "native");
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> {
                            if (args.length != 4) return false;
                            Optional<Location> player_location = Optional.ofNullable(sender instanceof Player p ? p : null).map(Entity::getLocation);
                            Integer x = tryParse(args[0]).or(() -> player_location.map(Location::getBlockX)).orElse(null);
                            if (x == null) {
                                sender.sendMessage("Value '"+args[0]+"' of argument 'x' is not supported!");
                                return true;
                            }
                            Integer y = tryParse(args[1]).or(() -> player_location.map(Location::getBlockY)).orElse(null);
                            if (y == null) {
                                sender.sendMessage("Value '"+args[1]+"' of argument 'y' is not supported!");
                                return true;
                            }
                            Integer z = tryParse(args[2]).or(() -> player_location.map(Location::getBlockZ)).orElse(null);
                            if (z == null) {
                                sender.sendMessage("Value '"+args[2]+"' of argument 'z' is not supported!");
                                return true;
                            }
                            boolean force;
                            switch (args[3]) {
                                case "force": force = true; break;
                                case "native": force = false; break;
                                default: {
                                    sender.sendMessage("Value '"+args[3]+"' of argument 'format' is not supported!");
                                    return true;
                                }
                            }
                            tryMarked(player_location.map(Location::getWorld).orElse(lime.MainWorld).getBlockAt(x,y,z), force);
                            sender.sendMessage("Block in "+x+" "+y+" "+z+" marked");
                            return true;
                        })
                )
                .addCommand("modify.block ", v -> v.withCheck(ServerOperator::isOp)
                        .withUsage("/modify.block [x:int,~] [y:int,~] [z:int,~] [format:replace|merge] [data:json]")
                        .withTab((sender, args) -> switch(args.length) {
                            case 1,2,3 -> Collections.singletonList(Optional.ofNullable(sender instanceof Player player ? player.getTargetBlock(5) : null)
                                    .map(p -> p.getLocation().toVector())
                                    .map(p -> switch (args.length) {
                                        case 1 -> p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
                                        case 2 -> p.getBlockY() + " " + p.getBlockZ();
                                        case 3 -> "" + p.getBlockZ();
                                        default -> "";
                                    })
                                    .orElse("~"));
                            case 4 -> Arrays.asList("replace", "merge");
                            default -> Collections.singletonList("{data}");
                        })
                        .withExecutor((sender, args) -> {
                            if (args.length < 5) return false;
                            Optional<Location> player_location = Optional.ofNullable(sender instanceof Player p ? p : null).map(Entity::getLocation);
                            Integer x = tryParse(args[0]).or(() -> player_location.map(Location::getBlockX)).orElse(null);
                            if (x == null) {
                                sender.sendMessage("Value '"+args[0]+"' of argument 'x' is not supported!");
                                return true;
                            }
                            Integer y = tryParse(args[1]).or(() -> player_location.map(Location::getBlockY)).orElse(null);
                            if (y == null) {
                                sender.sendMessage("Value '"+args[1]+"' of argument 'y' is not supported!");
                                return true;
                            }
                            Integer z = tryParse(args[2]).or(() -> player_location.map(Location::getBlockZ)).orElse(null);
                            if (z == null) {
                                sender.sendMessage("Value '"+args[2]+"' of argument 'z' is not supported!");
                                return true;
                            }
                            World world = player_location.map(Location::getWorld).orElse(lime.MainWorld);
                            boolean replace;
                            switch (args[3]) {
                                case "replace": replace = true; break;
                                case "merge": replace = false; break;
                                default: {
                                    sender.sendMessage("Value '"+args[3]+"' of argument 'format' is not supported!");
                                    return true;
                                }
                            }
                            String data = Arrays.stream(args).skip(5).collect(Collectors.joining(" "));
                            if (data.length() == 0) data = "{}";
                            JsonObject json = system.json.parse(data).getAsJsonObject();
                            Optional.ofNullable(world.getBlockAt(x,y,z).getState() instanceof Skull skull ? skull : null)
                                    .flatMap(skull -> formatBlock(skull, json, replace))
                                    .ifPresentOrElse(
                                            result -> sender.sendMessage("Block '"+result.info.key+"' modify in "+x+" "+y+" "+z),
                                            () -> sender.sendMessage("Block in "+x+" "+y+" "+z+" is empty")
                                    );
                            return true;
                        })
                )
                .addCommand("fill.block ", v -> v.withCheck(ServerOperator::isOp)
                        .withUsage("/fill.block [x1:int,~] [y1:int,~] [z1:int,~] [x2:int,~] [y2:int,~] [z2:int,~] [block:key] {data:json}")
                        .withTab((sender, args) -> switch(args.length) {
                            case 1,2,3,4,5,6 -> Collections.singletonList(Optional.ofNullable(sender instanceof Player player ? player.getTargetBlock(5) : null)
                                    .map(p -> p.getLocation().toVector())
                                    .map(p -> switch (args.length) {
                                        case 1,4 -> p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
                                        case 2,5 -> p.getBlockY() + " " + p.getBlockZ();
                                        case 3,6 -> "" + p.getBlockZ();
                                        default -> "";
                                    })
                                    .orElse("~"));
                            case 7 -> creators.keySet();
                            default -> Collections.singletonList("{data}");
                        })
                        .withExecutor((sender, args) -> {
                            if (args.length < 7) return false;
                            Optional<Location> player_location = Optional.ofNullable(sender instanceof Player p ? p : null).map(Entity::getLocation);

                            Integer x1 = tryParse(args[0]).or(() -> player_location.map(Location::getBlockX)).orElse(null);
                            if (x1 == null) { sender.sendMessage("Value '"+args[0]+"' of argument 'x1' is not supported!"); return true; }
                            Integer y1 = tryParse(args[1]).or(() -> player_location.map(Location::getBlockY)).orElse(null);
                            if (y1 == null) { sender.sendMessage("Value '"+args[1]+"' of argument 'y1' is not supported!"); return true; }
                            Integer z1 = tryParse(args[2]).or(() -> player_location.map(Location::getBlockZ)).orElse(null);
                            if (z1 == null) { sender.sendMessage("Value '"+args[2]+"' of argument 'z1' is not supported!"); return true; }

                            Integer x2 = tryParse(args[3]).or(() -> player_location.map(Location::getBlockX)).orElse(null);
                            if (x2 == null) { sender.sendMessage("Value '"+args[3]+"' of argument 'x2' is not supported!"); return true; }
                            Integer y2 = tryParse(args[4]).or(() -> player_location.map(Location::getBlockY)).orElse(null);
                            if (y2 == null) { sender.sendMessage("Value '"+args[4]+"' of argument 'y2' is not supported!"); return true; }
                            Integer z2 = tryParse(args[5]).or(() -> player_location.map(Location::getBlockZ)).orElse(null);
                            if (z2 == null) { sender.sendMessage("Value '"+args[5]+"' of argument 'z2' is not supported!"); return true; }

                            World world = player_location.map(Location::getWorld).orElse(lime.MainWorld);
                            InfoCreator creator = creators.getOrDefault(args[6], null);
                            if (creator == null) {
                                sender.sendMessage("Value '"+args[6]+"' of argument 'block' is not supported!");
                                return true;
                            }
                            system.Action4<Integer, Integer, Boolean, system.Action1<Integer>> forBWs = (from,to,full,callback) -> {
                                int _min = Math.min(from, to);
                                int _max = Math.max(from, to) + (full ? 1 : 0);
                                for (int i = _min; i < _max; i++) callback.invoke(i);
                            };
                            system.Toast1<Integer> count = system.toast(0);
                            String data = Arrays.stream(args).skip(7).collect(Collectors.joining(" "));
                            if (data.length() == 0) data = "{}";
                            JsonObject _data = system.json.parse(data).getAsJsonObject();
                            forBWs.invoke(x1, x2, true, x -> forBWs.invoke(y1, y2, true, y -> forBWs.invoke(z1, z2, true, z -> {
                                creator.setBlock(new Position(world, x, y, z), _data);
                                count.val0++;
                            })));
                            sender.sendMessage("Block '"+args[6]+"' filled count: "+count.val0);
                            return true;
                        })
                );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void on(BlockPlaceEvent e) {
        Items.getOptional(Settings.BlockSetting.class, e.getItemInHand())
                .ifPresent(setting -> {
                    Block block = e.getBlock();
                    InfoComponent.Rotation.Value rotation = InfoComponent.Rotation.of(e.getPlayer().getLocation().getDirection(), setting.rotation.keySet());
                    Position position = new Position(block);
                    String block_id = setting.rotation.getOrDefault(rotation, null);
                    if (block_id == null) {
                        e.setCancelled(true);
                        return;
                    }
                    BlocksOld.InfoCreator creator = BlocksOld.creator(block_id).orElse(null);
                    if (creator == null) {
                        e.setCancelled(true);
                        return;
                    }
                    Components.MultiBlockComponent multiBlockComponent = creator.component(Components.MultiBlockComponent.class).orElse(null);
                    if (multiBlockComponent != null && !multiBlockComponent.isCan(block, rotation)) {
                        e.setCancelled(true);
                        return;
                    }
                    creator.setBlock(position, system.json.object().addObject("LOD", v -> v.add("rotation", rotation.angle + "")).build(), e).ifPresentOrElse(result -> {
                        if (multiBlockComponent == null) return;
                        multiBlockComponent.blocks.forEach((local, data) -> data.set(result.info().uuid, position, rotation, local));
                    }, () -> e.setCancelled(true));
                });
    }

    public static abstract class InfoBaseInstance<I extends InfoComponent.StaticInfoComponent<?>> extends InfoInstance {
        private final I component;
        public I component() {
            return component;
        }

        public InfoBaseInstance(I component, Info info) {
            super(info);
            this.component = component;
        }
    }
    public static abstract class InfoInstance implements InfoComponent.IReplace, Closeable {
        private final system.Lock lock = system.Lock.create();
        private JsonObject json;
        private boolean edited = true;
        private final Info info;
        private final UUID instanceUUID;

        public UUID infoUUID() { return info.uuid; }
        public UUID instanceUUID() { return instanceUUID; }

        public InfoInstance(Info info) {
            this.instanceUUID = UUID.randomUUID();
            this.info = info;
        }

        public abstract JsonObject load(JsonObject json);
        public boolean tick(TileEntitySkull skull) { return true; }
        public void interact(TileEntitySkull state, BlockSkullEventInteract event) { }
        public void save() { }
        public Info info() { return info; }
        public void setSaved(JsonObject json) {
            try (system.Action0 ignored = lock.lock()) {
                if (this.json.equals(json)) return;
                this.json = json;
                this.edited = true;
            }
        }
        @Override public void close() {}

        private system.Toast2<JsonElement, Boolean> serialize() {
            try (system.Action0 ignored = lock.lock()) {
                system.Toast2<JsonElement, Boolean> ret = system.toast(json, edited);
                this.edited = false;
                return ret;
            }
        }
    }

    public static final class Info implements InfoComponent.IReplace, Closeable {
        public final UUID uuid;
        public final String key;
        private final JsonObject saved;

        public boolean isLootGenerated = false;

        public Optional<InfoCreator> getCreator() {
            return Optional.ofNullable(creators.getOrDefault(key, null));
        }
        public final ConcurrentHashMap<String, InfoInstance> instances = new ConcurrentHashMap<>();
        public <T extends InfoInstance>Optional<T> instance(Class<T> tClass) {
            return instances.values().stream().map(v -> tClass.isInstance(v) ? (T)v : null).filter(Objects::nonNull).findAny();
        }

        private Info(UUID uuid, JsonObject json) {
            this.uuid = uuid;
            this.key = json.get("key").getAsString();
            this.saved = json;
        }

        private InfoInstance iLoad(InfoComponent.DynamicInfoComponent<?,?> obj, JsonElement json) {
            InfoInstance instance = obj.createInstance(this);
            instance.json = instance.load(json == null || !json.isJsonObject() ? new JsonObject() : json.getAsJsonObject());
            return instance;
        }

        public boolean tick(TileEntitySkull state) {
            Optional<InfoCreator> creator = getCreator();
            if (creator.isEmpty()) return true;
            InfoCreator info = creator.get();
            boolean ret = true;
            boolean edited = false;
            JsonObject save;
            if (saved.has("components")) save = saved.getAsJsonObject("components");
            else saved.add("components", save = new JsonObject());
            for (Map.Entry<String, InfoComponent.StaticInfoComponent<?>> kv : info.components.entrySet()) {
                InfoComponent.StaticInfoComponent<?> component = kv.getValue();
                ret = component.tick(state) && ret;
                if (!(component instanceof InfoComponent.DynamicInfoComponent<?, ?> dynamicComponent)) continue;
                String key = kv.getKey();
                InfoInstance instance = instances.compute(key, (_key, _instance) -> _instance == null
                        ? iLoad(dynamicComponent, save.has(_key) ? save.get(_key) : null)
                        : _instance);
                ret = instance.tick(state) && ret;
                system.Toast2<JsonElement, Boolean> serialize = instance.serialize();
                save.add(key, serialize.val0);
                edited = serialize.val1 || edited;
            }
            if (edited) {
                state.persistentDataContainer.set(JManager.key("custom_block"), LimePersistentDataType.JSON_OBJECT, saved);
                update(state);
            }
            return ret;
        }
        public void interact(TileEntitySkull state, BlockSkullEventInteract event) {
            Optional<InfoCreator> creator = getCreator();
            if (creator.isEmpty()) return;
            InfoCreator info = creator.get();
            boolean edited = false;
            JsonObject save;
            if (saved.has("components")) save = saved.getAsJsonObject("components");
            else saved.add("components", save = new JsonObject());
            for (Map.Entry<String, InfoComponent.StaticInfoComponent<?>> kv : info.components.entrySet()) {
                InfoComponent.StaticInfoComponent<?> component = kv.getValue();
                component.interact(state, event);
                if (!(component instanceof InfoComponent.DynamicInfoComponent<?, ?> dynamicComponent)) continue;
                String key = kv.getKey();
                InfoInstance instance = instances.compute(key, (_key, _instance) -> _instance == null
                        ? iLoad(dynamicComponent, save.has(_key) ? save.get(_key).getAsJsonObject() : null)
                        : _instance);
                instance.interact(state, event);
                system.Toast2<JsonElement, Boolean> serialize = instance.serialize();
                save.add(key, serialize.val0);
                edited = serialize.val1 || edited;
            }
            if (edited) {
                state.persistentDataContainer.set(JManager.key("custom_block"), LimePersistentDataType.JSON_OBJECT, saved);
                update(state);
            }
        }
        public Optional<org.lime.Position> position() { return BlockMap.positionByUUID(uuid); }

        @Override public Result replace(Input input) {
            Optional<InfoCreator> creator = getCreator();
            if (creator.isEmpty()) return input.toResult();
            List<InfoComponent.IReplace> replaces = new ArrayList<>();
            replaces.addAll(instances.values());
            replaces.addAll(creator.get().components.values());
            return InfoComponent.IReplace.replace(input, replaces);
        }

        public <T> Stream<T> getAll(Class<T> tClass) {
            Optional<InfoCreator> creator = getCreator();
            if (creator.isEmpty()) return Stream.empty();
            return Streams.concat(
                    instances.values().stream().filter(tClass::isInstance).map(v -> (T)v),
                    creator.get().components.values().stream().filter(tClass::isInstance).map(v -> (T)v)
            );
        }

        @Override public void close() {
            instances.values().forEach(InfoInstance::close);
        }
    }
    public static final class InfoCreator {
        private final String _key;
        public String getKey() { return _key; }
        public final HashMap<String, InfoComponent.StaticInfoComponent<?>> components = new HashMap<>();
        public void add(InfoComponent.StaticInfoComponent<?> component) {
            this.components.put(component.name(), component);
        }
        public <T extends InfoComponent.StaticInfoComponent<?>>Optional<T> component(Class<T> tClass) {
            for (InfoComponent.StaticInfoComponent<?> component : components.values()) {
                if (tClass.isInstance(component))
                    return Optional.of((T)component);
            }
            return Optional.empty();
        }

        private InfoCreator(String key, JsonObject json) {
            this._key = key;

            if (json.has("components")) json.get("components").getAsJsonObject().entrySet().forEach(kv -> {
                InfoComponent.StaticInfoComponent<?> setting = InfoComponent.StaticInfoComponent.parse(kv.getKey(), this, kv.getValue());
                components.put(setting.name(), setting);
            });
        }
        public InfoCreator(String key, system.Action1<InfoCreator> init) {
            this._key = key;
            init.invoke(this);
        }

        public Optional<BlocksOld.BlockResult> setBlock(Position position) {
            return setBlock(position, new JsonObject());
        }
        public Optional<BlocksOld.BlockResult> setBlock(Position position, JsonObject json) {
            return BlocksOld.setBlock(position, system.json.object().add("key", getKey()).add("components", json).build());
        }
        public Optional<BlocksOld.BlockResult> setBlock(Position position, BlockPlaceEvent e) {
            return setBlock(position, new JsonObject(), e);
        }
        public Optional<BlocksOld.BlockResult> setBlock(Position position, JsonObject json, BlockPlaceEvent e) {
            return setBlock(position, json).map(v -> { v.info.getAll(InfoComponent.ISetBlock.class).forEach(_v -> _v.onSet(v.skull, e)); return v; });
        }

        public Optional<BlocksOld.BlockResult> setMultiBlock(Position position, JsonObject json, InfoComponent.Rotation.Value rotation) {
            Optional<BlocksOld.BlockResult> _result = setBlock(position, system.json.object()
                    .add(json)
                    .addObject("LOD", v -> v.add("rotation", rotation.angle + ""))
                    .build()
            );
            _result.ifPresent(result -> {
                Components.MultiBlockComponent multiBlockComponent = component(Components.MultiBlockComponent.class).orElse(null);
                if (multiBlockComponent == null) return;
                multiBlockComponent.blocks.forEach((local, data) -> data.set(result.info().uuid, position, rotation, local));
            });
            return _result;
        }

        private class Replacer<T> {
            public final system.Func1<Block, T> read;
            public final system.Action2<T, Info> edit;

            private Replacer(system.Func1<Block, T> read, system.Action2<T, Info> edit) {
                this.read = read;
                this.edit = edit;
            }

            public void replace(Block block) {
                T val = read.invoke(block);
                Position position = new Position(block.getLocation());
                InfoCreator.this.setBlock(position).ifPresent(result -> edit.invoke(val, result.info()));
            }
        }
        private final HashMap<Material, Replacer<?>> replaces = new HashMap<>();
        public <T>InfoCreator addReplace(Material material, system.Func1<Block, T> read, system.Action2<T, Info> edit) {
            replaces.put(material, this.new Replacer<>(read, edit));
            return this;
        }
        public InfoCreator addReplace(Material material, system.Action1<Info> apply) {
            replaces.put(material, new Replacer<>(b -> true, (b,i) -> apply.invoke(i)));
            return this;
        }
        public InfoCreator addReplace(Material material) {
            return addReplace(material, info -> {});
        }
    }

    private static Optional<Integer> tryParse(String num) {
        try { return Optional.of(Integer.parseInt(num)); }
        catch (Exception e) { return Optional.empty();}
    }

    public static <T extends Comparable<T>> IBlockData setValue(IBlockData data, IBlockState<T> state, String value) {
        return state.getValue(value).map(v -> data.setValue(state, v)).orElse(data);
    }
    public static <T extends Comparable<T>>String getValue(IBlockData data, IBlockState<T> state) {
        return state.getName(data.getValue(state));
    }

    public static Position position(net.minecraft.world.level.World world, BlockPosition position) {
        return new Position(world.getWorld(), position.getX(), position.getY(), position.getZ());
    }
    public static Position position(World world, BlockPosition position) {
        return new Position(world, position.getX(), position.getY(), position.getZ());
    }

    private static void update(TileEntity tileEntity) {
        net.minecraft.world.level.World world = tileEntity.getLevel();
        if (world == null) {
            lime.logOP("WORLD NULL");
            return;
        }
        BlockPosition position = tileEntity.getBlockPos();
        IBlockData state = tileEntity.getBlockState();
        world.setBlock(position, state, 3);
        world.sendBlockUpdated(position, state, state, 3);
    }

    private static TileEntityLimeSkull base(TileEntitySkull skull) {
        return (TileEntityLimeSkull)(Object)skull;
    }
    private static TileEntityLimeSkull base(Skull skull) {
        return base(((CraftSkull)skull).getTileEntity());
    }

    private static Optional<UUID> tryGetUUID(Block block) {
        return tryGetUUID(block == null ? null : (block.getState() instanceof Skull skull ? skull : null));
    }
    private static Optional<UUID> tryGetUUID(Skull state) {
        if (state == null || state.getType() != Material.SKELETON_SKULL) return Optional.empty();
        return base(state).customUUID();
    }
    private static Optional<UUID> tryGetUUID(TileEntitySkull state) {
        if (state == null || state.getBlockState().getBukkitMaterial() != Material.SKELETON_SKULL) return Optional.empty();
        return base(state).customUUID();
    }
    public static Optional<BlocksOld.BlockResult> syncBlock(Block block) {
        return syncBlock(block == null ? null : block.getState());
    }
    public static Optional<BlocksOld.BlockResult> syncBlock(BlockState state) {
        if (!(state instanceof Skull skull)) return Optional.empty();
        return tryGetUUID(skull).map(uuid -> {
            system.Toast1<BlocksOld.Info> load = system.toast(null);
            BlocksOld.Info _info = BlockMap.byPosition.compute(Position.of(state.getLocation()), (pos, info) -> {
                if (info != null && info.uuid.equals(uuid)) return info;
                if (info != null) info.close();
                info = new BlocksOld.Info(uuid, skull.getPersistentDataContainer().getOrDefault(JManager.key("custom_block"), LimePersistentDataType.JSON_OBJECT, new JsonObject()));
                state.update(true);
                BlockMap.byUUID.put(info.uuid, pos);
                load.val0 = info;
                return info;
            });
            TileEntitySkull _skull = ((CraftSkull)skull).getTileEntity();
            if (load.val0 != null) load.val0.tick(_skull);
            return new BlocksOld.BlockResult(_skull, _info);
        });
    }
    public static Optional<BlocksOld.Info> syncGetBlock(Block block) {
        return block == null ? Optional.empty() : syncGetBlock(block.getState());
    }
    public static Optional<BlocksOld.Info> syncGetBlock(BlockState state) {
        return state instanceof Skull skull ? tryGetUUID(skull)
                .map(uuid -> {
                    system.Toast1<Boolean> load = system.toast(false);
                    BlocksOld.Info _info = BlockMap.byPosition.compute(Position.of(state.getLocation()), (pos, info) -> {
                        if (info != null && info.uuid.equals(uuid)) return info;
                        if (info != null) info.close();
                        info = new BlocksOld.Info(uuid, skull.getPersistentDataContainer().getOrDefault(JManager.key("custom_block"), LimePersistentDataType.JSON_OBJECT, new JsonObject()));
                        state.update(true);
                        BlockMap.byUUID.put(info.uuid, pos);
                        load.val0 = true;
                        return info;
                    });
                    if (load.val0) _info.tick(((CraftSkull)skull).getTileEntity());
                    return _info;
                }) : Optional.empty();
    }
    public static Optional<BlocksOld.Info> syncGetBlock(TileEntitySkull state) {
        BlockPosition position = state.getBlockPos();
        return tryGetUUID(state)
                .flatMap(uuid -> Optional.of(state)
                        .map(TileEntity::getLevel)
                        .map(net.minecraft.world.level.World::getWorld)
                        .map(world -> {
                            system.Toast1<Boolean> load = system.toast(false);
                            BlocksOld.Info _info = BlockMap.byPosition.compute(Position.of(state.getLevel().getWorld(), position.getX(), position.getY(), position.getZ()), (pos, info) -> {
                                if (info != null && info.uuid.equals(uuid)) return info;
                                if (info != null) info.close();
                                info = new BlocksOld.Info(uuid, state.persistentDataContainer.getOrDefault(JManager.key("custom_block"), LimePersistentDataType.JSON_OBJECT, new JsonObject()));
                                update(state);
                                BlockMap.byUUID.put(info.uuid, pos);
                                load.val0 = true;
                                return info;
                            });
                            if (load.val0) _info.tick(state);
                            return _info;
                        })
                );
    }
    private static void syncChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities())
            syncBlock(state);
    }

    public record BlockResult(TileEntitySkull skull, BlocksOld.Info info) { }

    private static Optional<BlockResult> formatBlock(Skull state, JsonObject json, boolean replace) {
        PersistentDataContainer pdc = state.getPersistentDataContainer();
        JsonObject data = pdc.get(JManager.key("custom_block"), LimePersistentDataType.JSON_OBJECT);
        if (data == null) return Optional.empty();
        pdc.set(JManager.key("custom_block"), LimePersistentDataType.JSON_OBJECT, replace ? json : lime.combineJson(json, data).getAsJsonObject());
        if (!state.update()) return Optional.empty();
        return syncBlock(state);
    }
    private static Optional<BlockResult> setBlock(Skull state, UUID uuid, JsonObject json) {
        PersistentDataContainer pdc = state.getPersistentDataContainer();
        pdc.set(JManager.key("custom_block_uuid"), LimePersistentDataType.UUID, uuid);
        pdc.set(JManager.key("custom_block"), LimePersistentDataType.JSON_OBJECT, json);
        if (!state.update()) return Optional.empty();
        return syncBlock(state);
    }
    private static Optional<BlockResult> setBlock(Position pos, JsonObject json) {
        Block block = pos.getBlock();
        block.setType(Material.SKELETON_SKULL);
        return setBlock((Skull)block.getState(), UUID.randomUUID(), json);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH) public static void on(PopulateLootEvent e) {
        Vec3D pos = e.getOrDefault(PopulateLootEvent.Parameters.Origin, null);
        if (pos == null || !e.has(PopulateLootEvent.Parameters.BlockState)) return;
        if (!(e.getOrDefault(PopulateLootEvent.Parameters.BlockEntity, null) instanceof TileEntitySkull skull)) return;
        BlockMap.byPosition(skull.getLevel(), skull.getBlockPos())
                .filter(v -> !v.isLootGenerated)
                .map(v -> {
                    v.isLootGenerated = true;
                    return v;
                })
                .ifPresent(info -> e.setItems(info.getAll(InfoComponent.ILoot.class).flatMap(v -> v.populate(skull, e).stream()).collect(Collectors.toList())));
    }
    public static void onBlockMove(BlockPistonEvent e, List<Block> blocks) {
        for (Block block : blocks) {
            if (BlockMap.byPosition(new Position(block)).isEmpty()) continue;
            e.setCancelled(true);
            return;
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void onBlockPiston(BlockPistonExtendEvent e) {
        onBlockMove(e, e.getBlocks());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public static void onBlockPiston(BlockPistonRetractEvent e) {
        onBlockMove(e, e.getBlocks());
    }

    public static ConcurrentLinkedQueue<Block> marked = new ConcurrentLinkedQueue<>();
    public static void tryMarked(Block block, boolean force) {
        InfoCreator.Replacer<?> replacer = replaceBlocks.getOrDefault(block.getType(), null);
        if (replacer == null) return;
        if (force) replacer.replace(block);
        else marked.add(block);
    }
    public static void tick() {
        BlockMap.removeIfPosition((pos, info) -> {
            if (!pos.world.isChunkLoaded(pos.x >> 4, pos.z >> 4)) return true;
            WorldServer world = ((CraftWorld)pos.world).getHandle();
            if (!(world.getBlockEntity(new BlockPosition(pos.x, pos.y, pos.z)) instanceof TileEntitySkull skull)) return true;
            if (!tryGetUUID(skull).map(info.uuid::equals).orElse(false)) return true;
            boolean skip = info.tick(skull);
            if (skip) return false;
            info.close();
            return false;
        });
        BlockMap.sendToClient();
        marked.removeIf(block -> {
            Optional.ofNullable(replaceBlocks.getOrDefault(block.getType(), null)).ifPresent(kv -> kv.replace(block));
            return true;
        });
    }

    private static void setBlockData(float speed, boolean uncache) {
        net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.forEach(data -> {
            if (data.getBukkitMaterial() != Material.SKELETON_SKULL) return;
            ReflectionAccess.destroySpeed_BlockData.set(data, speed);
            if (uncache) {
                ReflectionAccess.cache_BlockData.set(data, null);
                ReflectionAccess.shapeExceedsCube_BlockData.set(data, true);
                ReflectionAccess.opacityIfCached_BlockData.set(data, -1);
            }
        });
        ReflectionAccess.destroyTime_BlockData.set(ReflectionAccess.properties_BlockBase.get(net.minecraft.world.level.block.Blocks.SKELETON_SKULL), speed);
    }

    @EventHandler public static void on(BlockSkullEventInteract e) {
        if (e.getWorld().getBlockEntity(e.getPos()) instanceof TileEntitySkull skull)
            syncGetBlock(skull).ifPresent(info -> info.interact(skull, e));
    }
    @EventHandler public static void on(BlockSkullEventShape e) {
        if (!(e.getWorld().getBlockEntity(e.getPos()) instanceof TileEntitySkull skull)) return;
        net.minecraft.world.level.World world = skull.getLevel();
        if (world == null) return;
        BlockMap.byPosition(world, skull.getBlockPos())
                .ifPresent(info -> info.getAll(InfoComponent.IShape.class).filter(v -> v.asyncShape(skull, e)).findFirst());
    }
    @EventHandler public static void on(TileEntitySkullEventDestroy e) {
        TileEntitySkull skull = e.getSkull();
        net.minecraft.world.level.World world = skull.getLevel();
        if (world == null) return;
        if (!Optional.ofNullable(world.getChunkIfLoaded(skull.getBlockPos())).map(v -> v.loaded).orElse(false)) return;
        BlockMap.byPosition(world, skull.getBlockPos())
                .ifPresent(info -> info.getAll(InfoComponent.IDestroy.class).forEach(v -> v.onDestroy(skull, e)));
    }

    private static final InfoCreator OTHER_CREATOR = new InfoCreator("#other", creator -> creator.add(InfoComponent.GenericDynamicComponent.other(creator)));
    public static InfoCreator otherCreator() {
        return OTHER_CREATOR;
    }
    public static void init() {
        AnyEvent.addEvent("blocks.json", AnyEvent.type.owner_console, b -> b.createParam(v -> v, overrides::keySet), (p, key) -> {
            lime.logOP("Block '" + key + "':\n" + overrides.getOrDefault(key, null));
        });
        addDefaultBlocks(OTHER_CREATOR);
        setBlockData(0.0f, true);

        lime.timer().withLoopTicks(1).setSync().withCallback(BlocksOld::tick).run();
    }
    private static final HashMap<String, InfoCreator> creators = new HashMap<>();
    private static final HashMap<String, InfoCreator> defaultBlocks = new HashMap<>();
    private static final HashMap<Material, InfoCreator.Replacer<?>> replaceBlocks = new HashMap<>();

    public static Optional<InfoCreator> creator(String key) {
        return Optional.ofNullable(creators.getOrDefault(key, null));
    }

    private static final HashMap<String, JsonObject> overrides = new HashMap<>();

    private static void modify(JsonElement base, HashMap<String, Map<String, JsonObject>> modify_map) {
        if (base.isJsonArray()) base.getAsJsonArray().forEach(item -> modify(item, modify_map));
        else if (base.isJsonObject()) {
            JsonObject _base = base.getAsJsonObject();
            _base.deepCopy().entrySet().forEach(_kv -> {
                String key = _kv.getKey();
                JsonElement item = _kv.getValue();
                if (item.isJsonObject()) modify(_base, key, item.getAsJsonObject(), modify_map);
                else modify(item, modify_map);
            });
        }
    }
    private static void modify(JsonObject base, String key, JsonObject child, HashMap<String, Map<String, JsonObject>> modify_map) {
        Map<String, JsonObject> addMap = modifyAdd(base, key, child, modify_map);
        JsonObject _base = new JsonObject();
        base.entrySet().forEach(kv -> {
            if (kv.getKey().equals(key)) addMap.forEach(_base::add);
            else _base.add(kv.getKey(), kv.getValue());
        });
        base.entrySet().clear();
        _base.entrySet().forEach(kv -> base.add(kv.getKey(), kv.getValue()));
    }
    private static Map<String, JsonObject> modifyAdd(JsonObject base, String key, JsonObject child, HashMap<String, Map<String, JsonObject>> modify_map) {
        JsonElement modify = child.remove("modify");

        for (Map.Entry<String, JsonElement> kv : child.deepCopy().entrySet()) {
            JsonElement element = kv.getValue();
            if (element.isJsonObject()) modify(child, kv.getKey(), element.getAsJsonObject(), modify_map);
            else if (element.isJsonArray()) modify(element, modify_map);
        }

        if (modify == null) return Collections.singletonMap(key, child);

        String modify_key = modify.getAsString();
        if (!modify_map.containsKey(modify_key)) {
            lime.logOP("[Warning] Modify '"+modify_key+"' in block '"+key+"' nof founded!");
            return Collections.singletonMap(key, child);
        }

        return modify_map.get(modify_key)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(kv -> key + kv.getKey(), kv -> lime.combineJson(kv.getValue().deepCopy(), child.deepCopy(), false).getAsJsonObject()));
    }

    public static void config(JsonObject json) {
        HashMap<String, InfoCreator> creators = new HashMap<>(defaultBlocks);
        JsonElement modify_json = json.remove("MODIFY_LIST");
        json = lime.combineParent(json, true, false);
        if (modify_json != null) {
            HashMap<String, Map<String, JsonObject>> modify_map = system.map.<String, Map<String, JsonObject>>of()
                    .add(modify_json.getAsJsonObject().entrySet(), Map.Entry::getKey, kv ->
                            system.map.<String, JsonObject>of()
                                    .add(lime.combineParent(kv.getValue().getAsJsonObject(), false, false).entrySet(), Map.Entry::getKey, _kv -> _kv.getValue().getAsJsonObject())
                                    .build()
                    )
                    .build();
            modify(json, modify_map);
        }

        overrides.clear();
        json.entrySet().forEach((kv) -> {
            overrides.put(kv.getKey(), kv.getValue().getAsJsonObject());
            creators.put(kv.getKey(), new InfoCreator(kv.getKey(), kv.getValue().getAsJsonObject()));
        });
        BlockMap.clear();
        replaceBlocks.clear();
        BlocksOld.creators.clear();
        BlocksOld.creators.putAll(creators);
        BlocksOld.creators.values().forEach(creator -> replaceBlocks.putAll(creator.replaces));

        Bukkit.getWorlds().forEach(world -> {
            for (Chunk chunk : world.getLoadedChunks()) syncChunk(chunk);
        });
        BlockDisplay.reload();
    }
    public static void addDefaultBlocks(InfoCreator creator) {
        defaultBlocks.put(creator.getKey(), creator);
    }
}

















