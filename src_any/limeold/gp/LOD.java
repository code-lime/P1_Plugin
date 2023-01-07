package org.lime.gp.block.display;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.block.BlockSkullEventShape;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.block.BlocksOld;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.extension.JsonNBT;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;
import java.util.stream.Collectors;

public class LOD {
    public enum TypeLOD {
        None,
        Block,
        ItemFrame
    }
    public static abstract class ILOD implements InfoComponent.IReplace, InfoComponent.IShape {
        public final UUID uuid;
        public final double distance;
        public final List<Variable> variables = new LinkedList<>();

        public static final class Variable {
            public final List<system.Toast2<String, List<String>>> values = new ArrayList<>();
            public final ILOD ILOD;

            public Variable(double distance, JsonObject owner, JsonObject child) {
                child.entrySet().forEach(kv -> {
                    if (kv.getKey().equals("result")) return;
                    values.add(system.toast(kv.getKey(), Collections.singletonList(kv.getValue().getAsString())));
                });
                ILOD = LOD.ILOD.parse(distance, lime.combineJson(owner, child.get("result"), false).getAsJsonObject());
            }
            public Variable(ILOD lod, List<system.Toast2<String, List<String>>> variable) {
                this.ILOD = lod;
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

        public ILOD(double distance, JsonObject json) {
            this.uuid = UUID.randomUUID();
            this.distance = distance;

            if (json.has("variable")) {
                json.getAsJsonArray("variable").forEach(variable -> {
                    JsonObject owner = json.deepCopy();
                    owner.remove("variable");
                    variables.add(new Variable(distance, owner, variable.getAsJsonObject()));
                });
            }
        }

        public static ILOD parse(double distance, JsonObject json) {
            if (json.has("item")) return new ItemFrameLOD(distance, json);
            else if (json.has("material")) return new BlockLOD(distance, json);
            else return new NoneLOD(distance, json);
        }
        public abstract TypeLOD type();

        public List<ILOD> lods() {
            if (variables.size() == 0) return Collections.singletonList(this);
            List<ILOD> lods = new LinkedList<>();
            lods.add(this);
            variables.forEach(variable -> lods.addAll(variable.ILOD.lods()));
            return lods;
        }

        public ILOD lod(Map<String, String> values) {
            for (Variable variable : variables) {
                if (variable.is(values))
                    return variable.ILOD.lod(values);
            }
            return this;
        }

        @Override public String toString() {
            return "[distance=" + system.getDouble(distance) + (variables.size() == 0 ? "" : ":") + variables.stream().map(Object::toString).collect(Collectors.joining(",")) + "]";
        }
    }
    public static class BlockLOD extends ILOD {
        public final Material material;
        public final IBlockData blockData;
        public final NBTTagCompound nbt;

        public final boolean hasCollision;

        public BlockLOD(double distance, JsonObject json) {
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
                    blockData = BlocksOld.setValue(blockData, state, kv.getValue().getAsString());
                }
            }
            this.blockData = blockData;
            this.nbt = json.has("nbt") ? JsonNBT.toNBT(json.getAsJsonObject("nbt")) : null;
            this.hasCollision = !json.has("hasCollision") || json.get("hasCollision").getAsBoolean();
        }

        @Override public Result replace(Input input) {
            return new Result(nbt, blockData);
        }
        @Override public boolean asyncShape(TileEntitySkull state, BlockSkullEventShape e) {
            e.setResult(this.hasCollision ? blockData.getShape(e.getWorld(), e.getPos(), e.getContext()) : VoxelShapes.empty());
            return true;
        }
        @Override public TypeLOD type() { return TypeLOD.Block; }

        @Override public String toString() { return blockData + "" + (nbt == null ? "" : GameProfileSerializer.structureToSnbt(nbt))+super.toString(); }

    }
    public static class ItemFrameLOD extends BlockLOD {
        public final ItemStack item;
        public final net.minecraft.world.item.ItemStack nms_item;
        public final InfoComponent.Rotation.Value rotation;

        public ItemFrameLOD(double distance, JsonObject json) {
            super(distance, json);
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
        }
        @Override public TypeLOD type() { return TypeLOD.ItemFrame; }

        @Override public String toString() {
            return super.toString()+ "^" + item + "R" + rotation.angle;
        }
    }
    public static class NoneLOD extends ILOD {
        public NoneLOD(double distance, JsonObject json) { super(distance, json); }
        public NoneLOD(double distance) { this(distance, new JsonObject()); }
        @Override public boolean asyncShape(TileEntitySkull state, BlockSkullEventShape e) { return false; }
        @Override public Result replace(Input input) { return input.toResult(); }
        @Override public TypeLOD type() { return TypeLOD.None; }
    }

    private static class Builder {
        public final double distance;

        public ILOD base = null;
        public final List<system.Toast2<ILOD, List<system.Toast2<String, List<String>>>>> childs = new LinkedList<>();

        public Builder(double distance) {
            this.distance = distance;
        }

        public void append(ILOD lod, List<system.Toast2<String, List<String>>> variable) {
            if (variable.size() == 0) base = lod;
            else childs.add(system.toast(lod, variable));
        }

        public ILOD build() {
            ILOD base = this.base == null ? new NoneLOD(distance) : this.base;
            childs.forEach(child -> base.variables.add(new ILOD.Variable(child.val0, child.val1)));
            return base;
        }
    }

    public static double load(BlocksOld.InfoCreator creator, JsonObject json, List<LOD.ILOD> lodList, Map<UUID, LOD.ILOD> lodMapUUID) {
        HashMap<Double, Builder> lodDistanceMap = new HashMap<>();
        json.entrySet().forEach(kv -> {
            String[] _arr = kv.getKey().split("\\?");
            double distance = Double.parseDouble(_arr[0]);
            String[] _args = Arrays.stream(_arr).skip(1).collect(Collectors.joining("?")).replace('?', '&').split("&");
            List<system.Toast2<String, List<String>>> map = new ArrayList<>();
            for (String _arg : _args) {
                String[] _kv = _arg.split("=");
                if (_kv.length == 1) {
                    if (_kv[0].length() > 0) lime.logOP("[Warning] Key '"+_kv[0]+"' of LOD '"+kv.getKey()+"' in block '"+creator.getKey()+"' is empty. Skipped");
                    continue;
                }
                map.add(system.toast(_kv[0], Arrays.asList(Arrays.stream(_kv).skip(1).collect(Collectors.joining("=")).split(","))));
            }
            lodDistanceMap.compute(distance, (k,v) -> {
                if (v == null) v = new Builder(k);
                v.append(ILOD.parse(distance, kv.getValue().getAsJsonObject()), map);
                return v;
            });
        });
        lodDistanceMap.values()
                .stream()
                .map(Builder::build)
                .sorted(Comparator.<ILOD>comparingDouble(v -> v.distance).reversed())
                .forEach(lodList::add);
        lodList.stream()
                .map(ILOD::lods)
                .flatMap(Collection::stream)
                .forEach(lod -> lodMapUUID.put(lod.uuid, lod));
        return lodList.size() == 0 ? -1 : lodList.get(0).distance;
    }
}
