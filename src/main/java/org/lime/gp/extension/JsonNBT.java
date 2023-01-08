package org.lime.gp.extension;

import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;
import net.minecraft.nbt.*;
import org.lime.system;

import java.util.*;

public class JsonNBT {
    public static NBTTagList toNBT(JsonArray json) { return (NBTTagList)toNBT((JsonElement) json); }
    public static NBTTagCompound toNBT(JsonObject json) { return (NBTTagCompound)toNBT((JsonElement) json); }
    public static NBTBase toNBT(JsonElement jsonElement) {
        if (jsonElement instanceof JsonPrimitive jsonPrimitive) {
            if (jsonPrimitive.isBoolean()) {
                boolean value = jsonPrimitive.getAsBoolean();
                if (value) return NBTTagByte.valueOf(true);
                else return NBTTagByte.valueOf(false);
            } else if (jsonPrimitive.isNumber()) {
                Number number = jsonPrimitive.getAsNumber();
                if (number instanceof Byte) return NBTTagByte.valueOf(number.byteValue());
                else if (number instanceof Short) return NBTTagShort.valueOf(number.shortValue());
                else if (number instanceof Integer) return NBTTagInt.valueOf(number.intValue());
                else if (number instanceof Long) return NBTTagLong.valueOf(number.longValue());
                else if (number instanceof Float) return NBTTagFloat.valueOf(number.floatValue());
                else if (number instanceof Double) return NBTTagDouble.valueOf(number.doubleValue());
                else if (number instanceof LazilyParsedNumber) return NBTTagDouble.valueOf(number.doubleValue());
                else throw new AssertionError("[NBT] Json number type '"+(number == null ? "<?~Empty>" : number.getClass().getName())+"' not supported!");
            } else if (jsonPrimitive.isString()) {
                return NBTTagString.valueOf(jsonPrimitive.getAsString());
            } else throw new AssertionError("[NBT] Json primitive '"+jsonPrimitive+"' not supported!");
        } else if (jsonElement instanceof JsonArray jsonArray) {
            NBTTagList nbtList = new NBTTagList();
            for (JsonElement element : jsonArray) nbtList.add(toNBT(element));
            return nbtList;
        } else if (jsonElement instanceof JsonObject jsonObject) {
            NBTTagCompound nbtCompound = new NBTTagCompound();
            jsonObject.entrySet().forEach(kv -> nbtCompound.put(kv.getKey(), toNBT(kv.getValue())));
            return nbtCompound;
        } else if (jsonElement instanceof JsonNull) return new NBTTagCompound();
        else throw new AssertionError("[NBT] Json type '"+(jsonElement == null ? "<?~Empty>" : jsonElement.getClass().getName())+"' not supported!");
    }

    public interface DynamicNBT<T extends NBTBase> {
        T build(Map<String, NBTBase> map);
    }

    public static DynamicNBT<NBTTagList> toDynamicNBT(JsonArray json, List<String> list) { return _toDynamicNBT(json, list); }
    public static DynamicNBT<NBTTagCompound> toDynamicNBT(JsonObject json, List<String> list) { return _toDynamicNBT(json, list); }
    public static DynamicNBT<NBTBase> toDynamicNBT(JsonElement json, List<String> list) { return _toDynamicNBT(json, list); }

    private interface IPath {
        NBTBase getChild(NBTBase parent);
        void setChild(NBTBase parent, NBTBase child);
    }
    private static class ArrayPath implements IPath {
        public int index;
        public ArrayPath(int index) {
            this.index = index;
        }
        @Override public NBTBase getChild(NBTBase parent) {
            return ((NBTList<?>)parent).get(index);
        }
        @Override public void setChild(NBTBase parent, NBTBase child) {
            setChild((NBTList<?>)parent, index, child);
        }
        @SuppressWarnings("unchecked")
        private static <T extends NBTBase>void setChild(NBTList<T> parent, int index, NBTBase child) {
            parent.set(index, (T)child);
        }
    }
    private static class ObjectPath implements IPath {
        public String key;
        public ObjectPath(String key) {
            this.key = key;
        }
        @Override public NBTBase getChild(NBTBase parent) {
            return ((NBTTagCompound)parent).get(key);
        }
        @Override public void setChild(NBTBase parent, NBTBase child) {
            ((NBTTagCompound)parent).put(key, child);
        }
    }
    private static class MultiPath implements IPath {
        public final List<IPath> paths;
        public MultiPath(List<IPath> paths) {
            this.paths = paths;
        }
        @Override public NBTBase getChild(NBTBase parent) {
            for (IPath path : paths) parent = path.getChild(parent);
            return parent;
        }
        @Override public void setChild(NBTBase parent, NBTBase child) {
            int length = paths.size();
            for (int i = 0; i < length - 1; i++) parent = paths.get(i).getChild(parent);
            paths.get(length - 1).setChild(parent, child);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends NBTBase>DynamicNBT<T> _toDynamicNBT(JsonElement jsonElement, List<String> list) {
        system.Toast2<NBTBase, Map<String, List<MultiPath>>> data = toDynamicNBT(Collections.emptyList(), jsonElement, list);
        NBTBase nbt = data.val0;
        Map<String, List<MultiPath>> modify = data.val1;
        return map -> {
            NBTBase nbt_copy = nbt.copy();
            map.forEach((key, value) -> {
                List<MultiPath> paths = modify.get(key);
                if (paths == null) return;
                paths.forEach(path -> path.setChild(nbt_copy, value.copy()));
            });
            return (T)nbt_copy;
        };
    }
    private static system.Toast2<NBTBase, Map<String, List<MultiPath>>> toDynamicNBT(List<IPath> path, JsonElement jsonElement, List<String> list) {
        if (jsonElement instanceof JsonPrimitive jsonPrimitive) {
            if (jsonPrimitive.isBoolean()) {
                boolean value = jsonPrimitive.getAsBoolean();
                if (value) return system.toast(NBTTagByte.valueOf(true), Collections.emptyMap());
                else return system.toast(NBTTagByte.valueOf(false), Collections.emptyMap());
            } else if (jsonPrimitive.isNumber()) {
                Number number = jsonPrimitive.getAsNumber();
                if (number instanceof Byte) return system.toast(NBTTagByte.valueOf(number.byteValue()), Collections.emptyMap());
                else if (number instanceof Short) return system.toast(NBTTagShort.valueOf(number.shortValue()), Collections.emptyMap());
                else if (number instanceof Integer) return system.toast(NBTTagInt.valueOf(number.intValue()), Collections.emptyMap());
                else if (number instanceof Long) return system.toast(NBTTagLong.valueOf(number.longValue()), Collections.emptyMap());
                else if (number instanceof Float) return system.toast(NBTTagFloat.valueOf(number.floatValue()), Collections.emptyMap());
                else if (number instanceof Double) return system.toast(NBTTagDouble.valueOf(number.doubleValue()), Collections.emptyMap());
                else if (number instanceof LazilyParsedNumber) return system.toast(NBTTagDouble.valueOf(number.doubleValue()), Collections.emptyMap());
                else throw new AssertionError("[NBT] Json number type '"+(number == null ? "<?~Empty>" : number.getClass().getName())+"' not supported!");
            } else if (jsonPrimitive.isString()) {
                String str = jsonPrimitive.getAsString();
                return system.toast(NBTTagString.valueOf(str), list.contains(str)
                        ? Collections.singletonMap(str, Collections.singletonList(new MultiPath(path)))
                        : Collections.emptyMap());
            } else throw new AssertionError("[NBT] Json primitive '"+jsonPrimitive+"' not supported!");
        } else if (jsonElement instanceof JsonArray jsonArray) {
            NBTTagList nbtList = new NBTTagList();
            Map<String, List<MultiPath>> map = new HashMap<>();
            int length = jsonArray.size();
            for (int i = 0; i < length; i++) {
                List<IPath> _path = new ArrayList<>(path);
                _path.add(new ArrayPath(i));
                toDynamicNBT(_path, jsonArray.get(i), list).invoke((nbt, _map) -> {
                    _map.forEach((key, value) -> map.computeIfAbsent(key, k -> new ArrayList<>()).addAll(value));
                    nbtList.add(nbt);
                });
            }
            return system.toast(nbtList, map);
        } else if (jsonElement instanceof JsonObject jsonObject) {
            NBTTagCompound nbtCompound = new NBTTagCompound();
            Map<String, List<MultiPath>> map = new HashMap<>();
            jsonObject.entrySet().forEach(kv -> {
                List<IPath> _path = new ArrayList<>(path);
                _path.add(new ObjectPath(kv.getKey()));
                toDynamicNBT(_path, kv.getValue(), list).invoke((nbt, _map) -> {
                    _map.forEach((key, value) -> map.computeIfAbsent(key, k -> new ArrayList<>()).addAll(value));
                    nbtCompound.put(kv.getKey(), nbt);
                });
            });
            return system.toast(nbtCompound, map);
        } else if (jsonElement instanceof JsonNull) return system.toast(new NBTTagCompound(), Collections.emptyMap());
        else throw new AssertionError("[NBT] Json type '"+(jsonElement == null ? "<?~Empty>" : jsonElement.getClass().getName())+"' not supported!");
    }

    public static JsonObject toJson(NBTTagCompound nbt) { return (JsonObject)toJson((NBTBase) nbt); }
    public static JsonArray toJson(NBTList<?> nbt) { return (JsonArray)toJson((NBTBase) nbt); }
    public static JsonElement toJson(NBTBase nbt) {
        if (nbt instanceof NBTNumber nbtNumber) {
            return new JsonPrimitive(nbtNumber.getAsNumber());
        } else if (nbt instanceof NBTTagString nbtString) {
            return new JsonPrimitive(nbtString.getAsString());
        } else if (nbt instanceof NBTList<?> nbtList) {
            JsonArray jsonArray = new JsonArray();
            for (NBTBase nbtBase : nbtList) jsonArray.add(toJson(nbtBase));
            return jsonArray;
        } else if (nbt instanceof NBTTagCompound nbtCompound) {
            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<String, NBTBase> nbtEntry : nbtCompound.tags.entrySet())
                jsonObject.add(nbtEntry.getKey(), toJson(nbtEntry.getValue()));
            return jsonObject;
        } else if (nbt instanceof NBTTagEnd) {
            throw new AssertionError();
        }
        throw new UnsupportedOperationException();
    }
}
