package org.lime.gp.player.voice;

import org.bukkit.block.Block;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.component.data.voice.RadioInstance;
import org.lime.gp.block.component.list.RadioComponent;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.HashMap;
import java.util.Optional;

public class DistanceData {
    public short distance;
    public final short min_distance;
    public final short max_distance;
    public final short def_distance;

    private short clampDistance(short distance) {
        return (short)Math.min(Math.max(distance, min_distance), max_distance);
    }

    public DistanceData(RadioComponent component) {
        this(component.min_distance, component.max_distance, component.def_distance);
    }
    public DistanceData(short min_distance, short max_distance, short def_distance) {
        this.min_distance = min_distance;
        this.max_distance = max_distance;
        this.def_distance = def_distance;

        this.distance = clampDistance(def_distance);
    }

    public HashMap<String, String> map() {
        return system.map.<String, String>of()
                .add("distance", String.valueOf(distance))
                .add("min_distance", String.valueOf(min_distance))
                .add("max_distance", String.valueOf(max_distance))
                .build();
    }

    public static Optional<DistanceData> getData(Block block) {
        return Blocks.of(block)
                .flatMap(Blocks::customOf)
                .flatMap(v -> v.list(RadioInstance.class).findFirst())
                .map(v -> v.distanceData);
    }
    public static void modifyData(Block block, system.Action1<DistanceData> modify) {
        Blocks.of(block)
                .flatMap(Blocks::customOf)
                .flatMap(v -> v.list(RadioInstance.class).findFirst())
                .ifPresent(instance -> {
                    modify.invoke(instance.distanceData);
                    instance.distanceData.distance = instance.distanceData.clampDistance(instance.distanceData.distance);
                    instance.saveData();
                });
    }

    public void read(JsonObjectOptional json) {
        distance = clampDistance(json.getAsShort("distance").orElse(def_distance));
    }
    public system.json.builder.object write() {
        return system.json.object()
                .add("distance", distance);
    }
}
