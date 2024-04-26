package org.lime.gp.item.settings.list;

import com.google.gson.JsonPrimitive;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.system.utils.RandomUtils;

import java.util.stream.Stream;

@Setting(name = "shield_ignore") public class ShieldIgnoreSetting extends ItemSetting<JsonPrimitive> {
    public final double chance;

    public ShieldIgnoreSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.chance = json.getAsDouble();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.raw(1.0),
                IComment.text("Шанс пробития/защиты щита"),
                IComment.text("ШансЗащиты(default: 1) * ШансПробития(default: 0) = ИтоговыйШансПробития")
        );
    }

    public static boolean isBlocking(ItemStack shield, ItemStack... attacks) {
        double chance = Items.getOptional(ShieldIgnoreSetting.class, shield)
                .map(v -> v.chance)
                .orElse(1.0)
                * Stream.of(attacks)
                .flatMap(v -> Items.getOptional(ShieldIgnoreSetting.class, v).stream())
                .mapToDouble(v -> v.chance)
                .reduce((a,b) -> a * b)
                .orElse(0.0);

        return !RandomUtils.rand_is(chance);
    }
}








