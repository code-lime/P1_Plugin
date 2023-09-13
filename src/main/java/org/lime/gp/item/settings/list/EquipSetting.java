package org.lime.gp.item.settings.list;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.entity.EnumItemSlot;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonEnumInfo;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

import java.util.stream.Stream;

@Setting(name = "equip") public class EquipSetting extends ItemSetting<JsonPrimitive> {
    public final EnumItemSlot slot;
    public EquipSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.slot = EnumItemSlot.byName(json.getAsString().toLowerCase());
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup equip_slot = JsonEnumInfo.of("EQUIP_SLOT", "equip_slot",
                Stream.of(EnumItemSlot.HEAD, EnumItemSlot.CHEST, EnumItemSlot.LEGS, EnumItemSlot.FEET)
                        .map(v -> IJElement.raw(v.getName()))
                        .collect(ImmutableList.toImmutableList())
        );
        return JsonGroup.of(index, index, IJElement.link(equip_slot), "Позволяет ложить предмет в определенный слот брони")
                .withChilds(equip_slot);
    }
}






