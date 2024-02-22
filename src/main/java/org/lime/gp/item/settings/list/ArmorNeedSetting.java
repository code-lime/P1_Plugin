package org.lime.gp.item.settings.list;

import com.google.gson.JsonArray;
import org.bukkit.entity.Player;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.player.module.needs.INeedEffect;
import org.lime.gp.player.module.needs.NeedSystem;
import org.lime.plugin.CoreElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Setting(name = "armor_need") public class ArmorNeedSetting extends ItemSetting<JsonArray> {
    public static CoreElement create() {
        return CoreElement.create(ArmorNeedSetting.class)
                .withInit(ArmorNeedSetting::init);
    }

    private static void init() {
        NeedSystem.register(ArmorNeedSetting::getArmorNeeds);
    }

    public final List<INeedEffect<?>> needs = new ArrayList<>();
    public ArmorNeedSetting(ItemCreator creator, JsonArray json) {
        super(creator, json);
        json.forEach(item -> needs.add(INeedEffect.parse(item.getAsJsonObject())));
    }

    private static Stream<INeedEffect<?>> getArmorNeeds(Player player) {
        return Arrays.stream(player.getInventory().getArmorContents())
                .flatMap(v -> Items.getOptional(ArmorNeedSetting.class, v).stream())
                .flatMap(v -> v.needs.stream());
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, IJElement.anyList(IJElement.link(docs.need())), IComment.text("Добавляет потребность при надетом предмете"));
    }
}






