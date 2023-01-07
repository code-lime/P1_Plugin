package org.lime.gp.block.component.display;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.network.chat.ChatHexColor;
import net.minecraft.world.level.block.BlockSkullShapeInfo;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.lime.display.Models;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.extension.JsonNBT;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DisplayPartial {
    public enum TypePartial {
        None,
        Block,
        Frame,
        Model
    }

    public static final class Variable {
        public final List<system.Toast2<String, List<String>>> values = new ArrayList<>();
        public final Partial partial;

        public Variable(double distance, JsonObject owner, JsonObject child) {
            child.entrySet().forEach(kv -> {
                if (kv.getKey().equals("result")) return;
                values.add(system.toast(kv.getKey(), Collections.singletonList(kv.getValue().getAsString())));
            });
            partial = Partial.parse(distance, lime.combineJson(owner, child.get("result"), false).getAsJsonObject());
        }
        public Variable(Partial partial, List<system.Toast2<String, List<String>>> variable) {
            this.partial = partial;
            this.values.addAll(variable);
        }
        public boolean is(Map<String, String> values) {
            for (system.Toast2<String, List<String>> kv : this.values) {
                String str = values.getOrDefault(kv.val0, null);
                if (!kv.val1.contains(str)) return false;
            }
            return true;
        }
        @Override public String toString() {
            return values.stream().map(kv -> kv.val0+"="+String.join(",",kv.val1)).collect(Collectors.joining(","));
        }
    }
    public static abstract class Partial {
        public final UUID uuid;
        public final double distanceSquared;
        public final List<Variable> variables = new LinkedList<>();

        public Partial(double distance, JsonObject json) {
            this.uuid = UUID.randomUUID();
            this.distanceSquared = distance > 0 ? (distance * distance) : 0;

            if (json.has("variable")) json.getAsJsonArray("variable").forEach(variable -> {
                JsonObject owner = json.deepCopy();
                owner.remove("variable");
                variables.add(new Variable(distance, owner, variable.getAsJsonObject()));
            });
        }

        public UUID unique() { return uuid; }

        public abstract TypePartial type();
        public List<Partial> partials() {
            if (variables.size() == 0) return Collections.singletonList(this);
            List<Partial> partials = new LinkedList<>();
            partials.add(this);
            variables.forEach(variable -> partials.addAll(variable.partial.partials()));
            return partials;
        }
        public Partial partial(Map<String, String> values) {
            for (Variable variable : variables) {
                if (variable.is(values))
                    return variable.partial.partial(values);
            }
            return this;
        }
        @Override public String toString() {
            return "[distance=" + system.getDouble(Math.sqrt(distanceSquared)) + (variables.size() == 0 ? "" : ":") + variables.stream().map(Object::toString).collect(Collectors.joining(",")) + "]";
        }

        public static Partial parse(double distance, JsonObject json) {
            if (json.has("model")) return new ModelPartial(distance, json);
            else if (json.has("item")) return new FramePartial(distance, json);
            else if (json.has("material")) return new BlockPartial(distance, json);
            else return new NonePartial(distance, json);
        }
    }
    public static class BlockPartial extends Partial implements CustomTileMetadata.Shapeable {
        public final Material material;
        public final IBlockData blockData;
        public final TileEntityTypes<?> type;
        public final JsonNBT.DynamicNBT<NBTTagCompound> nbt;

        public final boolean hasCollision;

        public BlockPartial(double distance, IBlockData blockData) {
            super(distance, new JsonObject());
            this.material = blockData.getBukkitMaterial();
            this.blockData = blockData;
            this.type = null;
            this.nbt = null;
            this.hasCollision = blockData.getBlock().hasCollision;
        }
        public BlockPartial(double distance, JsonObject json) {
            super(distance, json);
            this.material = Material.valueOf(json.get("material").getAsString());
            IBlockData blockData = CraftMagicNumbers
                    .getBlock(material)
                    .defaultBlockState();
            if (json.has("states")) {
                HashMap<String, IBlockState<?>> states = system.map.<String, IBlockState<?>>of()
                        .add(blockData.getProperties(), IBlockState::getName, v -> v)
                        .build();
                for (Map.Entry<String, JsonElement> kv : json.get("states").getAsJsonObject().entrySet()) {
                    IBlockState<?> state = states.getOrDefault(kv.getKey(), null);
                    if (state == null) continue;
                    blockData = BlockInfo.setValue(blockData, state, kv.getValue().getAsString());
                }
            }
            this.blockData = blockData;
            this.nbt = json.has("nbt") ? JsonNBT.toDynamicNBT(json.getAsJsonObject("nbt"), List.of("{color}")) : null;
            this.type = this.nbt == null ? null : IRegistry.BLOCK_ENTITY_TYPE.stream().filter(v -> v.isValid(this.blockData)).findAny().orElse(null);
            this.hasCollision = !json.has("has_collision") || json.get("has_collision").getAsBoolean();
        }

        @Override public TypePartial type() { return TypePartial.Block; }
        @Override public String toString() { return blockData + "" + (nbt == null ? "" : GameProfileSerializer.structureToSnbt(nbt.build(Collections.emptyMap())))+super.toString(); }

        @Override public @Nullable VoxelShape onShape(CustomTileMetadata metadata, BlockSkullShapeInfo event) {
            return this.hasCollision ? blockData.getShape(event.world(), event.pos(), event.context()) : VoxelShapes.empty();
        }

        private final ConcurrentHashMap<String, NBTTagCompound> variableConvert = new ConcurrentHashMap<>();
        private NBTTagCompound ofVariable(Map<String, String> variable) {
            String color = variable.getOrDefault("display.color", "FFFFFF");
            String key = "display.color=" + color;
            return variableConvert.computeIfAbsent(key, (_key) -> {
                HashMap<String, NBTBase> map = new HashMap<>();
                map.put("{color}", NBTTagInt.valueOf(ChatHexColor.parseColor("#" + color).getValue()));
                return nbt.build(map);
            });
        }
        public BlockDisplay.IBlock getDynamicDisplay(BlockPosition position, Map<String, String> variable) {
            return type == null
                    ? BlockDisplay.IBlock.of(blockData)
                    : BlockDisplay.ITileBlock.of(blockData, ReflectionAccess.init_PacketPlayOutTileEntityData.newInstance(position, type, ofVariable(variable)));
        }
    }
    public static class FramePartial extends BlockPartial {
        private final ItemStack item;
        private final net.minecraft.world.item.ItemStack nms_item;
        public final InfoComponent.Rotation.Value rotation;
        public final boolean show;

        public FramePartial(double distance, JsonObject json) {
            super(distance, json);
            if (json.has("item")) {
                JsonElement item = json.get("item");
                if (item.isJsonPrimitive()) {
                    String[] args = item.getAsString().split("\\^");
                    this.item = new ItemStack(Material.valueOf(args[0]));
                    if (args.length >= 2) {
                        ItemMeta meta = this.item.getItemMeta();
                        meta.setCustomModelData(Integer.parseInt(args[1]));
                        this.item.setItemMeta(meta);
                    }
                } else {
                    JsonObject _item = item.getAsJsonObject();
                    this.item = new ItemStack(Material.valueOf(_item.get("material").getAsString()));
                    ItemMeta meta = this.item.getItemMeta();
                    if (_item.has("id")) meta.setCustomModelData(_item.get("id").getAsInt());
                    this.item.setItemMeta(meta);
                }
                rotation = json.has("rotation") ? InfoComponent.Rotation.Value.ofAngle(json.get("rotation").getAsInt()) : InfoComponent.Rotation.Value.ANGLE_0;
                nms_item = CraftItemStack.asNMSCopy(this.item);
                show = true;
            } else {
                item = new ItemStack(Material.AIR);
                nms_item = net.minecraft.world.item.ItemStack.EMPTY;
                rotation = InfoComponent.Rotation.Value.ANGLE_0;
                show = false;
            }
        }

        private final ConcurrentHashMap<String, net.minecraft.world.item.ItemStack> variableConvert = new ConcurrentHashMap<>();
        public net.minecraft.world.item.ItemStack nms(Map<String, String> variable) {
            String color = variable.getOrDefault("display.color", "default");
            String key = "display.color=" + color;
            return variableConvert.computeIfAbsent(key, (_key) -> {
                net.minecraft.world.item.ItemStack nms_item = this.nms_item.copy();
                if (color.equals("default")) return nms_item;
                ItemNMS.setColor(nms_item, TextColor.fromHexString("#" + color));
                return nms_item;
            });
        }

        @Override public TypePartial type() { return TypePartial.Frame; }
        @Override public String toString() {
            return super.toString()+ "^" + item + "R" + rotation.angle;
        }
    }
    public static class ModelPartial extends FramePartial {
        private final String model;
        private Models.Model generic = null;

        public ModelPartial(double distance, JsonObject json) {
            super(distance, json);
            this.model = parseModel(json.get("model"));
        }

        private String parseModel(JsonElement json) {
            if (json.isJsonPrimitive()) return json.getAsString();
            generic = lime.models.parse(json.getAsJsonObject());
            return "#generic";
        }

        public Optional<Models.Model> model() {
            return Optional.ofNullable(generic).or(() -> lime.models.get(model));
        }

        @Override public TypePartial type() { return TypePartial.Model; }
        @Override public String toString() { return super.toString()+ "^" + model; }
    }
    public static class NonePartial extends Partial {
        public NonePartial(double distance, JsonObject json) { super(distance, json); }
        public NonePartial(double distance) { this(distance, new JsonObject()); }
        @Override public TypePartial type() { return TypePartial.None; }
    }

    private static class Builder {
        public final double distance;
        public Partial base = null;
        public final List<system.Toast2<Partial, List<system.Toast2<String, List<String>>>>> childs = new LinkedList<>();

        public Builder(double distance) {
            this.distance = distance;
        }
        public void append(Partial partial, List<system.Toast2<String, List<String>>> variable) {
            if (variable.size() == 0) base = partial;
            else childs.add(system.toast(partial, variable));
        }
        public Partial build() {
            Partial base = this.base == null ? new NonePartial(distance) : this.base;
            childs.forEach(child -> base.variables.add(new Variable(child.val0, child.val1)));
            return base;
        }
    }

    public static double load(BlockInfo creator, JsonObject json, List<Partial> partials, Map<UUID, Partial> partialMap) {
        HashMap<Double, Builder> distanceBuilder = new HashMap<>();
        json.entrySet().forEach(kv -> {
            if (kv.getKey().equals("animation")) return;
            String[] _arr = kv.getKey().split("\\?");
            double distance = Double.parseDouble(_arr[0]);
            String[] _args = Arrays.stream(_arr).skip(1).collect(Collectors.joining("?")).replace('?', '&').split("&");
            List<system.Toast2<String, List<String>>> map = new ArrayList<>();
            for (String _arg : _args) {
                String[] _kv = _arg.split("=");
                if (_kv.length == 1) {
                    if (_kv[0].length() > 0) lime.logOP("[Warning] Key '"+_kv[0]+"' of Partial '"+kv.getKey()+"' in block '"+creator.getKey()+"' is empty. Skipped");
                    continue;
                }
                map.add(system.toast(_kv[0], Arrays.asList(Arrays.stream(_kv).skip(1).collect(Collectors.joining("=")).split(","))));
            }
            distanceBuilder.compute(distance, (k,v) -> {
                if (v == null) v = new Builder(k);
                v.append(Partial.parse(distance, kv.getValue().getAsJsonObject()), map);
                return v;
            });
        });
        distanceBuilder.values()
                .stream()
                .map(Builder::build)
                .sorted(Comparator.<Partial>comparingDouble(v -> v.distanceSquared).reversed())
                .forEach(partials::add);
        partials.stream()
                .map(Partial::partials)
                .flatMap(Collection::stream)
                .forEach(partial -> partialMap.put(partial.uuid, partial));
        return partials.size() == 0 ? -1 : partials.get(0).distanceSquared;
    }
    public static double loadStatic(BlockInfo creator, List<Partial> load, List<Partial> partials, Map<UUID, Partial> partialMap) {
        load.stream()
                .sorted(Comparator.<Partial>comparingDouble(v -> v.distanceSquared).reversed())
                .forEach(partials::add);
        partials.stream()
                .map(Partial::partials)
                .flatMap(Collection::stream)
                .forEach(partial -> partialMap.put(partial.uuid, partial));
        return partials.size() == 0 ? -1 : partials.get(0).distanceSquared;
    }
}












