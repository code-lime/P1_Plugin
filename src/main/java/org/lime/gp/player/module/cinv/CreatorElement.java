package org.lime.gp.player.module.cinv;

import com.google.common.collect.ImmutableList;
import org.lime.gp.item.data.IItemCreator;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreatorElement {
    private final ImmutableList<GroupElement> groups;

    public CreatorElement(Collection<IItemCreator> creators) {
        groups = creators.stream()
                .map(ItemElement::new)
                .collect(Collectors.groupingBy(kv -> kv.key().contains(".") ? kv.key().split("\\.")[0].toLowerCase() : "item", ImmutableList.toImmutableList()))
                .entrySet()
                .stream()
                .map(kv -> new GroupElement(kv.getKey(), kv.getValue()))
                .collect(ImmutableList.toImmutableList());
    }

    public int size() { return groups.size(); }
    public @Nullable GroupElement get(int index) { return index < 0 || index >= groups.size() ? null : groups.get(index); }

    public Stream<ItemElement> rawSearch(String search) { return groups.stream().flatMap(v -> v.rawSearch(search)); }
}
