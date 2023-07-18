package org.lime.gp.item.loot.filter;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.Holder;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import org.lime.gp.item.data.Checker;
import org.lime.gp.module.ArrowBow;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class TagLootFilter implements ILootFilter {
    public List<String> tags = new ArrayList<>();
    public TagLootFilter(String argLine) {
        tags.addAll(Arrays.asList(argLine.split(Pattern.quote(";"))));
    }
    @Override public boolean isFilter(PopulateLootEvent loot) {
        return loot.getOptional(LootContextParameters.THIS_ENTITY)
                .map(v -> v.getTags().containsAll(tags))
                .orElse(false);
    }
}
