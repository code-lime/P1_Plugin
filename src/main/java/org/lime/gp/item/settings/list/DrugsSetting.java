package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.List;

import org.lime.system;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.module.Drugs;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

@Setting(name = "drugs") public class DrugsSetting extends ItemSetting<JsonObject> {
    public final List<system.Toast2<ImmutableSet<Drugs.EffectType>, system.IRange>> effects = new ArrayList<>();
    public DrugsSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator, json);
        json.getAsJsonArray("effects")
                .forEach(item -> {
                    String effect = item.getAsString();
                    List<String> args = Lists.newArrayList(effect.split(" "));
                    system.IRange range = system.IRange.parse(args.remove(args.size() - 1));
                    effects.add(system.toast(args.stream().map(Drugs.EffectType::valueOf).collect(ImmutableSet.toImmutableSet()), range));
                });
        //this.time = json.has("time") ? json.get("time").getAsInt() : 0;
    }

    /*
    public final int time;
    @Override public int getTime() { return time; }
    @Override public void timeUse(Player player, Player target, ItemStack item) {
        Drugs.getGroupEffect(player.getUniqueId())
                .setup(effects);
    }
    @Override public boolean use(Player player, Player target, ItemStack item) { return UseSetting.ITimeUse.super.use(player, player, item); }
    */
}