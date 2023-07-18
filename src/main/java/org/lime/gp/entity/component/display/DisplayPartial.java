package org.lime.gp.entity.component.display;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.EntityLimeMarker;
import org.bukkit.entity.Player;
import org.lime.display.models.Model;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;
import java.util.stream.Collectors;

public class DisplayPartial {
    public enum TypePartial {
        None,
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

        public UUID unique() { return uuid; }

        public Partial(double distance, JsonObject json) {
            this.uuid = UUID.randomUUID();
            this.distanceSquared = distance > 0 ? (distance * distance) : 0;

            if (json.has("variable")) json.getAsJsonArray("variable").forEach(variable -> {
                JsonObject owner = json.deepCopy();
                owner.remove("variable");
                variables.add(new Variable(distance, owner, variable.getAsJsonObject()));
            });
        }

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
            else return new NonePartial(distance, json);
        }
    }
    public static class ModelPartial extends Partial implements EntityDisplay.Displayable {
        private final String model;
        private Model generic = null;

        public ModelPartial(double distance, JsonObject json) {
            super(distance, json);
            this.model = parseModel(json.get("model"));
        }
        public ModelPartial(double distance, Model model) {
            super(distance, new JsonObject());
            this.model = "#generic";
            this.generic = model;
        }

        private String parseModel(JsonElement json) {
            if (json.isJsonPrimitive()) return json.getAsString();
            generic = lime.models.parse(json.getAsJsonObject());
            return "#generic";
        }

        public Optional<Model> model() {
            return Optional.ofNullable(generic).or(() -> lime.models.get(model));
        }

        @Override public TypePartial type() { return TypePartial.Model; }
        @Override public String toString() { return model; }

        @Override public Optional<EntityDisplay.IEntity> onDisplay(Player player, EntityLimeMarker marker) {
            return model().map(EntityDisplay.IEntity::of);
        }
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

    public static double load(EntityInfo creator, JsonObject json, List<Partial> partials, Map<UUID, Partial> partialMap) {
        HashMap<Double, Builder> distanceBuilder = new HashMap<>();
        json.entrySet().forEach(kv -> {
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
}












