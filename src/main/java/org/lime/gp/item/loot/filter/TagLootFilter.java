package org.lime.gp.item.loot.filter;

import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.module.loot.PopulateLootEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class TagLootFilter implements ILootFilter {
    public List<String> tags = new ArrayList<>();
    public TagLootFilter(String argLine) {
        tags.addAll(Arrays.asList(argLine.split(Pattern.quote(";"))));
    }
    @Override public boolean isFilter(IPopulateLoot loot) {
        return loot.getOptional(LootContextParameters.THIS_ENTITY)
                .map(v -> v.getTags().containsAll(tags))
                .orElse(false);
    }
}
