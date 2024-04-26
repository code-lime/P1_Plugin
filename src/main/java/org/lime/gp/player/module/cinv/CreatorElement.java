package org.lime.gp.player.module.cinv;

import com.google.common.collect.ImmutableList;
import org.lime.gp.item.data.IItemCreator;
import org.lime.system.utils.IterableUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreatorElement {
    private final ImmutableList<GroupElement> groups;
    private final boolean simple;

    public CreatorElement(Collection<IItemCreator> creators, boolean simple) {
        this.simple = simple;
        groups = creators.stream()
                .map(v -> new ItemElement(v, simple))
                .filter(IterableUtils.distinctBy(ItemElement::key))
                .collect(Collectors.groupingBy(kv -> kv.key().contains(".") ? kv.key().split("\\.")[0].toLowerCase() : "item", ImmutableList.toImmutableList()))
                .entrySet()
                .stream()
                .map(kv -> new GroupElement(kv.getKey(), kv.getValue()))
                .collect(ImmutableList.toImmutableList());
    }

    public boolean isSimple() { return simple; }
    public int size() { return groups.size(); }
    public @Nullable GroupElement get(int index) { return index < 0 || index >= groups.size() ? null : groups.get(index); }

    public Stream<ItemElement> rawSearch(@Nullable String search) { return groups.stream().flatMap(v -> v.rawSearch(search)); }
}
