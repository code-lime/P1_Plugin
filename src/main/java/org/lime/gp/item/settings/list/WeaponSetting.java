package org.lime.gp.item.settings.list;

import java.util.*;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.lime;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;
import org.lime.gp.item.weapon.WeaponData;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.ui.ImageBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.format.TextColor;
import org.lime.system.utils.ItemUtils;
import org.lime.system.utils.MathUtils;

@Setting(name = "weapon") public class WeaponSetting extends ItemSetting<JsonObject> {
    public static final NamespacedKey MAGAZINE_KEY = new NamespacedKey(lime._plugin, "magazine");

    public static class Pose {
        public final Vector offset;
        public final double range;
        public final double range_max;

        public double range(double recoil, double recoil_max) {
            return range + ((range_max - range) * (recoil / recoil_max));
        }

        public Pose(JsonObject json) {
            offset = MathUtils.getVector(json.get("offset").getAsString());
            range = json.get("range").getAsDouble();
            range_max = json.get("range_max").getAsDouble();
        }
    }
    public class UI {
        public final List<ImageBuilder> percent = new ArrayList<>();

        public ImageBuilder of(int ammo, double cooldown) {
            return ammo == 0
                    ? percent.get(percent.size() - 1)
                    : percent.get((int)Math.round(Math.max(Math.min(cooldown * 20.0 / tick_speed, 1), 0) * (percent.size() - 1)));
        }

        public UI(JsonArray json) {
            json.forEach(_item -> {
                JsonObject item = _item.getAsJsonObject();
                ImageBuilder builder = ImageBuilder.of(
                        ChatHelper.formatComponent(item.get("image").getAsString()),
                        item.get("size").getAsInt(),
                        true
                );
                if (item.has("offset")) builder = builder.addOffset(item.get("offset").getAsInt());
                if (item.has("color")) builder = builder.withColor(TextColor.fromHexString("#" + item.get("color").getAsString()));
                percent.add(builder);
            });
        }
    }

    public final double bullet_speed;
    public final double bullet_down;

    public final String magazine_type;
    public final double recoil;
    public final double recoil_speed;
    public final double damage_scale;
    public final int tick_speed;
    public final double init_sec;

    public final List<WeaponData.State> states = new ArrayList<>();
    public final HashMap<WeaponData.Pose, WeaponSetting.Pose> poses = new HashMap<>();
    public final HashMap<WeaponData.State, UI> ui = new HashMap<>();

    public final String sound_shoot;
    public final String sound_load;
    public final String sound_unload;
    public final String sound_pose;
    public final String sound_state;

    public final String display;

    public WeaponSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        bullet_speed = json.get("bullet_speed").getAsDouble();
        bullet_down = json.get("bullet_down").getAsDouble();
        magazine_type = json.has("magazine_type") ? json.get("magazine_type").getAsString() : null;
        recoil = json.get("recoil").getAsDouble();
        recoil_speed = json.get("recoil_speed").getAsDouble();
        damage_scale = json.get("damage_scale").getAsDouble();
        tick_speed = json.get("tick_speed").getAsInt();
        init_sec = json.has("init_sec") ? json.get("init_sec").getAsDouble() : 2.5;

        json.getAsJsonArray("states").forEach(kv -> states.add(WeaponData.State.valueOf(kv.getAsString())));
        json.getAsJsonObject("poses").entrySet().forEach(kv -> poses.put(WeaponData.Pose.valueOf(kv.getKey()), new Pose(kv.getValue().getAsJsonObject())));
        if (json.has("ui")) json.getAsJsonObject("ui")
                .entrySet()
                .forEach(kv -> {
                    UI _ui = new UI(kv.getValue().getAsJsonArray());
                    if (_ui.percent.size() == 0) return;
                    ui.put(WeaponData.State.valueOf(kv.getKey()), _ui);
                });

        sound_shoot = json.has("sound_shoot") ? json.get("sound_shoot").getAsString() : null;
        sound_load = json.has("sound_load") ? json.get("sound_load").getAsString() : null;
        sound_unload = json.has("sound_unload") ? json.get("sound_unload").getAsString() : null;
        sound_pose = json.has("sound_pose") ? json.get("sound_pose").getAsString() : null;
        sound_state = json.has("sound_state") ? json.get("sound_state").getAsString() : null;

        display = json.has("display") ? json.get("display").getAsString() : String.valueOf(creator.getID());
    }

    public static Optional<ItemStack> getMagazine(ItemStack weapon) {
        return Optional.of(weapon)
                .map(ItemStack::getItemMeta)
                .flatMap(WeaponSetting::getMagazine);
    }
    public static Optional<ItemStack> getMagazine(ItemMeta weapon) {
        return Optional.of(weapon)
                .map(PersistentDataHolder::getPersistentDataContainer)
                .map(v -> v.get(MAGAZINE_KEY, PersistentDataType.STRING))
                .map(ItemUtils::loadItem);
    }
    public static void setMagazine(ItemStack weapon, ItemStack magazine) {
        ItemMeta meta = weapon.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Optional.ofNullable(ItemUtils.saveItem(magazine))
                .ifPresentOrElse(
                        _v -> container.set(MAGAZINE_KEY, PersistentDataType.STRING, _v),
                        () -> container.remove(MAGAZINE_KEY)
                );
        weapon.setItemMeta(meta);

        Items.getItemCreator(weapon)
                .map(v -> v instanceof ItemCreator c ? c : null)
                .ifPresent(v -> v.apply(weapon));
    }

    @Override public void appendArgs(ItemMeta meta, Apply apply) {
        apply.add("magazine_type", getMagazine(meta)
                .flatMap(magazine -> Items.getOptional(MagazineSetting.class, magazine))
                .map(v -> v.magazine_type)
                .orElse("")
        );
    }

    public int weaponDisplay(WeaponData.Pose pose, Integer magazine_id, int bullet_count) {
        Map<String, Object> args = Map.of(
                "pose", pose.name(),
                "magazine_id", magazine_id == null ? "" : String.valueOf(magazine_id),
                "bullet_count", String.valueOf(bullet_count)
        );
        String _display = display;
        for (var kv : args.entrySet()) _display = _display.replace("{" + kv.getKey() + "}", kv.getValue().toString());
        return JavaScript.getJsInt(_display, args).orElseGet(() -> creator().getID());
    }


    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup weapon_state = JsonEnumInfo.of("WEAPON_STATE", "weapon_state", WeaponData.State.class);
        IIndexGroup weapon_pose = JsonEnumInfo.of("WEAPON_POSE", "weapon_pose", WeaponData.Pose.class);
        IIndexGroup weapon_pose_info = JsonGroup.of("WEAPON_POSE_INFO", "weapon_pose_info", JObject.of(
                JProperty.require(IName.raw("offset"), IJElement.link(docs.vector()), IComment.text("Относительная точка вылета пули")),
                JProperty.require(IName.raw("range"), IJElement.raw(1.0), IComment.text("Уровень разброса")),
                JProperty.require(IName.raw("range_max"), IJElement.raw(1.0), IComment.text("Максимальный уровень разброса"))
        ));
        IIndexGroup weapon_ui_frame = JsonGroup.of("WEAPON_UI_FRAME", "weapon_ui_frame", JObject.of(
                JProperty.require(IName.raw("image"), IJElement.link(docs.formattedChat()), IComment.text("Отображаемое изображение")),
                JProperty.require(IName.raw("size"), IJElement.raw(10), IComment.empty()
                        .append(IComment.text("Ширина "))
                        .append(IComment.field("image"))
                        .append(IComment.text(" пикселей"))),
                JProperty.optional(IName.raw("offset"), IJElement.raw(10), IComment.text("Сдвиг изображения в пикселях")),
                JProperty.optional(IName.raw("color"), IJElement.raw("#FFFFFF"), IComment.text("Цвет изображения"))
        ));
        IIndexGroup weapon_ui = JsonGroup.of("WEAPON_UI", "weapon_ui", IJElement.anyList(IJElement.link(weapon_ui_frame)),IComment.text( "Набор кадров анимации"))
                .withChild(weapon_ui_frame);
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("bullet_speed"), IJElement.raw(1.0), IComment.raw("Скорость пули")),
                JProperty.require(IName.raw("bullet_down"), IJElement.raw(1.0), IComment.raw("Скорость падения пули")),
                JProperty.optional(IName.raw("magazine_type"), IJElement.raw("MAGAZINE_TYPE"), IComment.text("Пользвательский тип магазина. Если не указан - само оружие содержит магазин")),
                JProperty.require(IName.raw("recoil"), IJElement.raw(1.0), IComment.text("Максимальная отдача")),
                JProperty.require(IName.raw("recoil_speed"), IJElement.raw(1.0), IComment.text("Скорость увеличения отдачи")),
                JProperty.require(IName.raw("damage_scale"), IJElement.raw(1.0), IComment.text("Множитель урона")),
                JProperty.require(IName.raw("tick_speed"), IJElement.raw(5), IComment.text("Количество тиков между выстрелами")),
                JProperty.optional(IName.raw("init_sec"), IJElement.raw(2.5), IComment.text("Время в секундах. Скорость готовности оружия")),
                JProperty.require(IName.raw("states"), IJElement.anyList(IJElement.link(weapon_state)), IComment.text("Список возможных типов стрельбы")),
                JProperty.require(IName.raw("poses"), IJElement.anyObject(
                        JProperty.require(IName.link(weapon_pose), IJElement.link(weapon_pose_info))
                ), IComment.text("Список поз стрельбы")),
                JProperty.optional(IName.raw("ui"), IJElement.anyObject(
                        JProperty.require(IName.link(weapon_state), IJElement.link(weapon_ui))
                ), IComment.text("Отображаемый прогресс перезарядки для возможных типов стрельбы")),
                JProperty.optional(IName.raw("sound_shoot"), IJElement.link(docs.sound()), IComment.text("Звук выстрела")),
                JProperty.optional(IName.raw("sound_load"), IJElement.link(docs.sound()), IComment.text("Звук установки магазина")),
                JProperty.optional(IName.raw("sound_unload"), IJElement.link(docs.sound()), IComment.text("Звук доставания магазина")),
                JProperty.optional(IName.raw("sound_pose"), IJElement.link(docs.sound()), IComment.text("Звук смены позы")),
                JProperty.optional(IName.raw("sound_state"), IJElement.link(docs.sound()), IComment.text("Звук смены типа стрельбы")),
                JProperty.optional(IName.raw("display"), IJElement.link(docs.js()), IComment.empty()
                        .append(IComment.text("Изменение "))
                        .append(IComment.raw("id"))
                        .append(IComment.text(" предмета через JavaScript. Передаваемые параметры: "))
                        .append(IComment.raw("pose"))
                        .append(IComment.text(", "))
                        .append(IComment.raw("magazine_id"))
                        .append(IComment.text(", "))
                        .append(IComment.raw("bullet_count")))
        )).withChilds(weapon_state, weapon_pose, weapon_pose_info, weapon_ui);
    }
}