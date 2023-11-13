package org.lime.gp.item.settings.list;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import net.minecraft.world.entity.EnumItemSlot;
import org.bukkit.entity.Player;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonEnumInfo;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.player.module.needs.INeedEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Setting(name = "armor_need") public class ArmorNeedSetting extends ItemSetting<JsonArray> {
    public final List<INeedEffect<?>> needs = new ArrayList<>();
    public ArmorNeedSetting(ItemCreator creator, JsonArray json) {
        super(creator, json);
        json.forEach(item -> needs.add(INeedEffect.parse(item.getAsJsonObject())));
    }

    public static Stream<INeedEffect<?>> getArmorNeeds(Player player) {
        return Arrays.stream(player.getInventory().getArmorContents())
                .flatMap(v -> Items.getOptional(ArmorNeedSetting.class, v).stream())
                .flatMap(v -> v.needs.stream());
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        /*IIndexGroup equip_slot = JsonEnumInfo.of("EQUIP_SLOT", "equip_slot",
                Stream.of(EnumItemSlot.HEAD, EnumItemSlot.CHEST, EnumItemSlot.LEGS, EnumItemSlot.FEET)
                        .map(v -> IJElement.raw(v.getName()))
                        .collect(ImmutableList.toImmutableList())
        );
        return JsonGroup.of(index, index, IJElement.link(equip_slot), "Позволяет ложить предмет в определенный слот брони")
                .withChilds(equip_slot);*/
        throw new IllegalArgumentException("TODO");
    }
}






