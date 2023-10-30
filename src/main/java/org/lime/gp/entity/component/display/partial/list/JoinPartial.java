package org.lime.gp.entity.component.display.partial.list;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.lime.display.models.shadow.BaseBuilder;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.entity.component.display.partial.Partial;
import org.lime.gp.entity.component.display.partial.PartialEnum;
import org.lime.gp.lime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JoinPartial extends Partial {
    private final LinkedList<Partial> join = new LinkedList<>();

    public JoinPartial(int distanceChunk, JsonObject json) {
        super(distanceChunk, json);

        json.getAsJsonArray("join")
                .forEach(element -> join.add(Partial.parse(distanceChunk, element.getAsJsonObject())));
    }

    @Override public PartialEnum type() { return PartialEnum.Join; }

    private final ConcurrentHashMap<ImmutableSet<UUID>, ModelPartial> cacheModels = new ConcurrentHashMap<>();

    @Override public Partial partial(Map<String, String> values) {
        Partial checkPartial = super.partial(values);
        if (checkPartial != this) return checkPartial;
        ImmutableSet.Builder<UUID> cacheKey = ImmutableSet.builder();
        List<IBuilder> builders = new ArrayList<>();
        for (Partial partial : join) {
            if (partial.partial(values) instanceof ModelPartial modelPartial) {
                modelPartial.model().ifPresent(builder -> {
                    cacheKey.add(builder.unique());
                    builders.add(builder);
                });
            }
        }
        return cacheModels.computeIfAbsent(cacheKey.build(), v -> {
            BaseBuilder<?,?> builder = lime.models.builder().group();
            for (IBuilder model : builders)
                builder = builder.addChild(model);
            return new ModelPartial(distanceChunk(), builder);
        });
    }
}
