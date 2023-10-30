package org.lime.gp.entity.component.display.partial;

import com.google.gson.JsonObject;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.display.partial.list.NonePartial;
import org.lime.gp.lime;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import java.util.*;
import java.util.stream.Collectors;

public class PartialLoader {

    private static class Builder {
        public final int distanceChunk;
        public Partial base = null;
        public final List<Toast2<Partial, List<Toast2<String, List<String>>>>> childs = new LinkedList<>();

        public Builder(int distanceChunk) {
            this.distanceChunk = distanceChunk;
        }
        public void append(Partial partial, List<Toast2<String, List<String>>> variable) {
            if (variable.size() == 0) base = partial;
            else childs.add(Toast.of(partial, variable));
        }
        public Partial build() {
            Partial base = this.base == null ? new NonePartial(distanceChunk) : this.base;
            childs.forEach(child -> base.variables().add(new Variable(child.val0, child.val1)));
            return base;
        }
    }

    public static double load(EntityInfo creator, JsonObject json, List<Partial> partials, Map<UUID, Partial> partialMap) {
        HashMap<Integer, Builder> distanceBuilder = new HashMap<>();
        json.entrySet().forEach(kv -> {
            if (kv.getKey().equals("animation")) return;
            String[] _arr = kv.getKey().split("\\?");
            int distanceChunk = BlockDisplay.getChunkSize(Double.parseDouble(_arr[0]));
            String[] _args = Arrays.stream(_arr).skip(1).collect(Collectors.joining("?")).replace('?', '&').split("&");
            List<Toast2<String, List<String>>> map = new ArrayList<>();
            for (String _arg : _args) {
                String[] _kv = _arg.split("=");
                if (_kv.length == 1) {
                    if (_kv[0].length() > 0) lime.logOP("[Warning] Key '"+_kv[0]+"' of Partial '"+kv.getKey()+"' in block '"+creator.getKey()+"' is empty. Skipped");
                    continue;
                }
                map.add(Toast.of(_kv[0], Arrays.asList(Arrays.stream(_kv).skip(1).collect(Collectors.joining("=")).split(","))));
            }
            distanceBuilder.compute(distanceChunk, (k,v) -> {
                if (v == null) v = new Builder(k);
                v.append(Partial.parse(distanceChunk, kv.getValue().getAsJsonObject()), map);
                return v;
            });
        });
        distanceBuilder.values()
                .stream()
                .map(Builder::build)
                .sorted(Comparator.comparingInt(Partial::distanceChunk).reversed())
                .forEach(partials::add);
        partials.stream()
                .map(Partial::partials)
                .flatMap(Collection::stream)
                .forEach(partial -> partialMap.put(partial.uuid(), partial));
        return partials.size() == 0 ? -1 : partials.get(0).distanceChunk();
    }
}












