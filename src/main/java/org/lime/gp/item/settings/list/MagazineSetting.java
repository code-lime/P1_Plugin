package org.lime.gp.item.settings.list;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.gp.lime;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;
import org.lime.system.utils.ItemUtils;

@Setting(name = "magazine") public class MagazineSetting extends ItemSetting<JsonObject> {
    public static final NamespacedKey BULLETS_KEY = new NamespacedKey(lime._plugin, "bullets");

    public final String bullet_type;
    public final String magazine_type;
    public final int size;

    public final String sound_load;
    public final String sound_unload;

    public MagazineSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        magazine_type = json.has("magazine_type") ? json.get("magazine_type").getAsString() : null;
        bullet_type = json.get("bullet_type").getAsString();
        size = json.get("size").getAsInt();

        sound_load = json.has("sound_load") ? json.get("sound_load").getAsString() : null;
        sound_unload = json.has("sound_unload") ? json.get("sound_unload").getAsString() : null;
    }

    public static Optional<List<ItemStack>> getBullets(ItemStack magazine) {
        return Optional.of(magazine)
                .map(ItemStack::getItemMeta)
                .flatMap(MagazineSetting::getBullets);
    }
    public static Optional<List<ItemStack>> getBullets(ItemMeta magazine) {
        return Optional.of(magazine)
                .map(PersistentDataHolder::getPersistentDataContainer)
                .map(v -> v.get(BULLETS_KEY, PersistentDataType.STRING))
                .map(v -> v.split(" "))
                .map(Arrays::stream)
                .map(v -> v.filter(_v -> !_v.equals("")))
                .map(v -> v.map(ItemUtils::loadItem).collect(Collectors.toList()));
    }
    public static void setBullets(ItemStack magazine, List<ItemStack> bullets) {
        ItemMeta meta = magazine.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(BULLETS_KEY, PersistentDataType.STRING, bullets.stream().map(ItemUtils::saveItem).collect(Collectors.joining(" ")));
        magazine.setItemMeta(meta);

        Items.getItemCreator(magazine)
                .map(v -> v instanceof ItemCreator c ? c : null)
                .ifPresent(v -> v.apply(magazine));
    }

    @Override public void appendArgs(ItemMeta meta, Apply apply) {
        List<ItemStack> bullets = getBullets(meta).orElseGet(Collections::emptyList);
        apply.add("bullet_count", String.valueOf(bullets.size()));
        apply.add("bullets", json.array()
                .add(bullets, item -> Optional.ofNullable(item)
                        .flatMap(v -> Items.getOptional(BulletSetting.class, v))
                        .map(v -> json.object()
                                .add("bullet_name", v.creator().name)
                                .add("bullet_type", v.bullet_type)
                                .add("damage", v.damage)
                        )
                        .orElse(null)
                )
                .build()
                .toString()
        );
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.optional(IName.raw("magazine_type"), IJElement.raw("MAGAZINE_TYPE"), IComment.text("Пользвательский тип магазина. Если не указан - само оружие содержит магазин")),
                JProperty.require(IName.raw("bullet_type"), IJElement.raw("BULLET_TYPE"), IComment.text("Пользвательский тип патрона")),
                JProperty.require(IName.raw("size"), IJElement.raw(10), IComment.text("Количество хранимых патронов")),
                JProperty.optional(IName.raw("sound_load"), IJElement.link(docs.sound()), IComment.text("Устанавливает звук добавления патрона")),
                JProperty.optional(IName.raw("sound_unload"), IJElement.link(docs.sound()), IComment.text("Устанавливает звук доставания патрона"))
        ), IComment.text("Используется в меню"), IComment.text("Передаваемые `args` в предмет: `bullet_count` и `bullets`"));
    }
}












