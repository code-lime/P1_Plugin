package org.lime.gp.item;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.*;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.gp.item.weapon.WeaponData;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.menu.page.Menu;
import org.lime.gp.player.module.Drugs;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.gp.player.voice.MegaPhoneData;
import org.lime.gp.player.voice.RadioData;
import org.lime.gp.sound.SoundMaterial;
import org.lime.gp.sound.Sounds;
import org.lime.reflection;
import org.lime.system;
import org.lime.gp.player.module.Death;

import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Settings {
    @Retention(RetentionPolicy.RUNTIME) public @interface Setting { String name(); }
    public interface IItemSetting {
        Items.ItemCreator creator();
        String name();
        system.Toast2<ItemStack, Boolean> replace(ItemStack item);
        void apply(ItemStack item, ItemMeta meta, Apply apply);
        void appendArgs(ItemStack item, Apply apply);
    }
    public static abstract class ItemSetting<T extends JsonElement> implements IItemSetting {
        private final Items.ItemCreator _creator;
        public Items.ItemCreator creator() { return this._creator; }

        private String _name;
        public String name() { return _name; }
        public system.Toast2<ItemStack, Boolean> replace(ItemStack item) { return system.toast(item, false); }
        public void apply(ItemStack item, ItemMeta meta, Apply apply) { }
        public void appendArgs(ItemStack item, Apply apply) { }

        private static final Map<String, system.Func2<Items.ItemCreator, JsonElement, ItemSetting<?>>> settings;
        private static Optional<Constructor<?>> constructor(Class<?> tClass, Class<?>... args) {
            try { return Optional.of(reflection.access(tClass.getDeclaredConstructor(args))); }
            catch (Exception e) { return Optional.empty(); }
        }
        static {
            try {
                settings = Stream.of(Settings.class.getDeclaredClasses())
                        .filter(ItemSetting.class::isAssignableFrom)
                        .map(v -> constructor(v, Items.ItemCreator.class, JsonElement.class)
                                .or(() -> constructor(v, Items.ItemCreator.class, JsonArray.class))
                                .or(() -> constructor(v, Items.ItemCreator.class, JsonObject.class))
                                .or(() -> constructor(v, Items.ItemCreator.class, JsonPrimitive.class))
                                .or(() -> constructor(v, Items.ItemCreator.class, JsonNull.class))
                                .or(() -> constructor(v, Items.ItemCreator.class))
                                .map(c -> system.toast(v.getAnnotation(Setting.class), c))
                                .orElse(null)
                        )
                        .filter(Objects::nonNull)
                        .filter(kv -> kv.val0 != null)
                        .collect(Collectors.toMap(kv -> kv.val0.name(), kv -> (system.Func2<Items.ItemCreator, JsonElement, ItemSetting<?>>) (creator, json) -> {
                            try {
                                return (ItemSetting<?>)(kv.val1.getParameterCount() == 2 ? kv.val1.newInstance(creator, json) : kv.val1.newInstance(creator));
                            }
                            catch (InvocationTargetException e) {
                                lime.logStackTrace(e.getCause());
                                throw new IllegalArgumentException(e.getCause());
                            }
                            catch (Exception e) {
                                lime.logStackTrace(e);
                                throw new IllegalArgumentException(e);
                            }
                        }));
            } catch (Exception e) {
                lime.logStackTrace(e);
                throw e;
            }
            settings.keySet().forEach(k -> lime.logOP("Setting: " + k));
        }
        public ItemSetting(Items.ItemCreator creator) { this._creator = creator; }
        public ItemSetting(Items.ItemCreator creator, T json) { this(creator); }
        public static ItemSetting<?> parse(String key, Items.ItemCreator creator, JsonElement json) {
            ItemSetting<?> setting = settings.get(key).invoke(creator, json);
            setting._name = key;
            return setting;
        }
    }

    @Setting(name = "duplicate") public static class DuplicateSetting extends ItemSetting<JsonArray> {
        public DuplicateSetting(Items.ItemCreator creator, JsonArray json) {
            super(creator, json);
            json.forEach(item -> {
                int id = item.getAsInt();
                Items.creators.put(id, creator);
                Items.creatorNamesIDs.put(id, creator.getKey());
                try { Items.creatorMaterials.put(id, Material.valueOf(creator.item)); } catch (Exception ignored) { }
            });
        }
    }
    @Setting(name = "wallet") public static class WalletSetting extends ItemSetting<JsonNull> {
        public WalletSetting(Items.ItemCreator creator) { super(creator); }
    }
    @Setting(name = "card") public static class CardSetting extends ItemSetting<JsonNull> {
        public CardSetting(Items.ItemCreator creator) { super(creator); }
    }
    @Setting(name = "brush") public static class BrushSetting extends ItemSetting<JsonNull> {
        public BrushSetting(Items.ItemCreator creator) { super(creator); }
    }
    @Setting(name = "bucket") public static class BucketSetting extends ItemSetting<JsonNull> {
        public BucketSetting(Items.ItemCreator creator) { super(creator); }
    }
    @Setting(name = "cash") public static class CashSetting extends ItemSetting<JsonPrimitive> {
        public final int cash;
        public CashSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            cash = json.getAsInt();
        }
    }
    @Setting(name = "insert") public static class InsertSetting extends ItemSetting<JsonObject> {
        public final String type;
        public final int weight;
        public InsertSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            type = json.get("type").getAsString();
            weight = json.get("weight").getAsInt();
        }
        public static List<ItemStack> createOf(String type, int weight) {
            List<ItemStack> items = new ArrayList<>();
            if (weight <= 0) return items;
            HashMap<Items.IItemCreator, Integer> list = new HashMap<>();
            List<Items.IItemCreator> creators = new ArrayList<>(Items.creatorIDs.values());
            Collections.reverse(creators);
            for (Items.IItemCreator _v :creators) {
                if (!(_v instanceof Items.ItemCreator creator)) continue;
                Optional<Integer> _weight = creator.getOptional(InsertSetting.class).filter(v -> v.type.equals(type)).map(v -> v.weight);
                if (_weight.isEmpty()) continue;
                if (weight < _weight.get()) continue;
                list.put(creator, list.getOrDefault(creator, 0) + weight / _weight.get());
                weight %= _weight.get();
            }
            list.forEach((k,v) -> items.add(k.createItem(v)));
            return items;
        }
    }
    @Setting(name = "hummer") public static class HummerSetting extends ItemSetting<JsonNull> {
        public HummerSetting(Items.ItemCreator creator) { super(creator); }
    }
    @Setting(name = "clicker") public static class ClickerSetting extends ItemSetting<JsonPrimitive> {
        public final String type;
        public ClickerSetting(Items.ItemCreator creator, JsonPrimitive json) { super(creator); this.type = json.getAsString(); }
    }
    @Setting(name = "radio") public static class RadioSetting extends ItemSetting<JsonObject> {
        public final int min_level;// 1490,
        public final int def_level;
        public final int max_level;// 1740,
        public final int on;// 19,
        public final int off;// 20
        public final short total_distance;
        public final boolean is_on;
        public final RadioData.RadioState state;
        public RadioSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            min_level = json.get("min_level").getAsInt();
            def_level = json.get("def_level").getAsInt();
            max_level = json.get("max_level").getAsInt();
            total_distance = json.get("total_distance").getAsShort();
            on = json.get("on").getAsInt();
            off = json.get("off").getAsInt();
            is_on = !json.has("is_on") || json.get("is_on").getAsBoolean();
            state = json.has("state") ? RadioData.RadioState.valueOf(json.get("state").getAsString()) : RadioData.RadioState.all;
        }

        @Override public void apply(ItemStack item, ItemMeta meta, Apply apply) {
            List<system.Action1<RadioData>> modifyRadio = new ArrayList<>();
            apply.get("level").map(Integer::parseInt).ifPresent(level -> modifyRadio.add(data -> data.level = level));
            apply.get("state").map(Boolean::parseBoolean).ifPresent(state -> modifyRadio.add(data -> data.enable = state));
            if (!modifyRadio.isEmpty()) RadioData.modifyData(this, meta, data -> modifyRadio.forEach(action -> action.invoke(data)));
        }
    }
    @Setting(name = "megaphone") public static class MegaPhoneSetting extends ItemSetting<JsonObject> {
        public final short def_distance;// 16
        public final short min_distance;// 0
        public final short max_distance;// 32
        public MegaPhoneSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator);
            def_distance = json.get("def_distance").getAsShort();
            min_distance = json.get("min_distance").getAsShort();
            max_distance = json.get("max_distance").getAsShort();
        }

        @Override public void apply(ItemStack item, ItemMeta meta, Apply apply) {
            List<system.Action1<MegaPhoneData>> modify = new ArrayList<>();
            apply.get("distance").map(Short::parseShort).ifPresent(distance -> modify.add(data -> data.distance = distance));
            apply.get("volume").map(Integer::parseInt).ifPresent(volume -> modify.add(data -> data.volume = volume));
            if (!modify.isEmpty()) MegaPhoneData.modifyData(this, meta, data -> modify.forEach(action -> action.invoke(data)));
        }
    }
    @Setting(name = "baton") public static class BatonSetting extends ItemSetting<JsonPrimitive> {
        public final double chance;
        public BatonSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator);
            this.chance = json.getAsDouble();
        }
    }
    @Setting(name = "handcuffs") public static class HandcuffsSetting extends ItemSetting<JsonNull> {
        public HandcuffsSetting(Items.ItemCreator creator) { super(creator); }
    }
    @Setting(name = "magnifier") public static class MagnifierSetting extends ItemSetting<JsonNull> {
        public MagnifierSetting(Items.ItemCreator creator) { super(creator); }
    }
    @Setting(name = "heal") public static class HealSetting extends ItemSetting<JsonObject> implements UseSetting.ITimeUse {
        public final system.IRange heal;
        public final int time;
        public HealSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            this.heal = json.has("heal") ? system.IRange.parse(json.get("heal").getAsString()) : null;
            this.time = json.has("time") ? json.get("time").getAsInt() : 0;
        }

        @Override public EquipmentSlot arm() { return EquipmentSlot.HAND; }
        @Override public int getTime() { return time; }
        @Override public void timeUse(Player player, Player target, ItemStack item) {
            Death.up(target);
            double total = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double health = heal.getValue(total);
            target.setHealth(Math.max(0, Math.min(total, target.getHealth() + health)));
        }
    }
    @Setting(name = "use_to_next") public static class UseToNextSetting extends ItemSetting<JsonObject> implements UseSetting.ITimeUse {
        public final int time;
        public final Boolean shift;
        public final EquipmentSlot arm;
        public UseToNextSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            this.time = json.get("time").getAsInt();
            this.shift = json.get("shift").isJsonNull() ? null : json.get("shift").getAsBoolean();
            this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
        }
        @Override public EquipmentSlot arm() { return arm; }
        @Override public int getTime() { return time; }
        @Override public void timeUse(Player player, Player target, ItemStack item) { }
        @Override public boolean use(Player player, Player target, EquipmentSlot arm, boolean shift) {
            if (this.shift != null && this.shift != shift) return false;
            return UseSetting.ITimeUse.super.use(player, target, arm, shift);
        }
    }
    @Setting(name = "q_to_next") public static class QToNextSetting extends ItemSetting<JsonObject> {
        public final String sound;
        public QToNextSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            sound = json.has("sound") ? json.get("sound").getAsString() : null;
        }
    }
    @Setting(name = "next") public static class NextSetting extends ItemSetting<JsonPrimitive> {
        public final String next;
        public NextSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            this.next = json.getAsString();
        }
    }
    @Setting(name = "block") public static class BlockSetting extends ItemSetting<JsonObject> {
        public static final NamespacedKey BLOCK_DATA_KEY = new NamespacedKey(lime._plugin, "block_data");
        public final Map<InfoComponent.Rotation.Value, String> rotation = new LinkedHashMap<>();
        public final Map<String, JsonObject> block_args = new HashMap<>();

        public BlockSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            json.getAsJsonObject("rotation").entrySet().forEach(kv -> {
                String value = kv.getValue().getAsString();
                for (String key : kv.getKey().split("\\|")) rotation.put(InfoComponent.Rotation.Value.ofAngle(Integer.parseInt(key)), value);
            });
            if (json.has("block_args")) json.getAsJsonObject("block_args").entrySet().forEach(kv -> block_args.put(kv.getKey(), kv.getValue().getAsJsonObject()));
        }

        @Override public void apply(ItemStack item, ItemMeta meta, Apply apply) {
            JsonObject data = new JsonObject();
            apply.list().forEach((key, value) -> {
                if (key.startsWith("block_data.")) data.addProperty(key.substring(11), value);
            });
            if (data.size() <= 0) return;
            meta.getPersistentDataContainer().set(BLOCK_DATA_KEY, LimePersistentDataType.JSON_OBJECT, data);
        }

        public Map<String, JsonObject> blockArgs(ItemStack item) {
            Apply apply = Apply.of();
            Optional.of(item)
                    .map(ItemStack::getItemMeta)
                    .map(PersistentDataHolder::getPersistentDataContainer)
                    .map(v -> v.get(BLOCK_DATA_KEY, LimePersistentDataType.JSON_OBJECT))
                    .ifPresent(v -> v.entrySet().forEach(kv -> apply.add(kv.getKey(), kv.getValue().getAsString())));
            return block_args.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, kv -> system.EditStringToObject(kv.getValue().deepCopy(), text -> new JsonPrimitive(ChatHelper.formatText(text, apply)))));
        }
    }
    @Setting(name = "replace") public static class ReplaceSetting extends ItemSetting<JsonObject> {
        public final Material material;
        public final Integer id;
        public ReplaceSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            material = json.has("material") ? Material.valueOf(json.get("material").getAsString()) : null;
            id = json.has("id") ? json.get("id").getAsInt() : null;
        }

        @Override public system.Toast2<ItemStack, Boolean> replace(ItemStack item) {
            item = item.clone();
            ItemMeta meta = item.getItemMeta();
            boolean save = false;
            if (id != null) {
                meta.setCustomModelData(id);
                save = true;
            }
            if (material != null) {
                item.setType(material);
                save = true;
            }
            if (save) {
                item.setItemMeta(meta);
                return system.toast(item, true);
            }
            return system.toast(item, false);
        }
    }
    @Setting(name = "weapon") public static class WeaponSetting extends ItemSetting<JsonObject> {
        public static final NamespacedKey MAGAZINE_KEY = new NamespacedKey(lime._plugin, "magazine");

        public static class Pose {
            public final Vector offset;
            public final double range;
            public final double range_max;

            public double range(double recoil, double recoil_max) {
                return range + ((range_max - range) * (recoil / recoil_max));
            }

            public Pose(JsonObject json) {
                offset = system.getVector(json.get("offset").getAsString());
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
                            item.get("size").getAsInt()
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
        public final HashMap<WeaponData.Pose, Pose> poses = new HashMap<>();
        public final HashMap<WeaponData.State, UI> ui = new HashMap<>();

        public final String sound_shoot;
        public final String sound_load;
        public final String sound_unload;
        public final String sound_pose;
        public final String sound_state;

        public final String display;

        public WeaponSetting(Items.ItemCreator creator, JsonObject json) {
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

            display = json.has("display") ? json.get("display").getAsString() : ("" + creator.getID());
        }

        public static Optional<ItemStack> getMagazine(ItemStack weapon) {
            return Optional.of(weapon)
                    .map(ItemStack::getItemMeta)
                    .map(PersistentDataHolder::getPersistentDataContainer)
                    .map(v -> v.get(MAGAZINE_KEY, PersistentDataType.STRING))
                    .map(system::loadItem);
        }
        public static void setMagazine(ItemStack weapon, ItemStack magazine) {
            ItemMeta meta = weapon.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            Optional.ofNullable(system.saveItem(magazine))
                    .ifPresentOrElse(
                            _v -> container.set(MAGAZINE_KEY, PersistentDataType.STRING, _v),
                            () -> container.remove(MAGAZINE_KEY)
                    );
            weapon.setItemMeta(meta);

            Items.getItemCreator(weapon)
                    .map(v -> v instanceof Items.ItemCreator c ? c : null)
                    .ifPresent(v -> v.apply(weapon));
        }

        @Override public void appendArgs(ItemStack weapon, Apply apply) {
            apply.add("magazine_type", getMagazine(weapon)
                    .flatMap(magazine -> Items.getOptional(Settings.MagazineSetting.class, magazine))
                    .map(v -> v.magazine_type)
                    .orElse("")
            );
        }

        public int weaponDisplay(WeaponData.Pose pose, Integer magazine_id, int bullet_count) {
            return JavaScript.getJsInt(display
                    .replace("{pose}", pose.name())
                    .replace("{magazine_id}", magazine_id == null ? "" : ("" + magazine_id))
                    .replace("{bullet_count}", bullet_count + "")
            ).orElseGet(() -> creator().getID());
        }
    }
    @Setting(name = "magazine") public static class MagazineSetting extends ItemSetting<JsonObject> {
        public static final NamespacedKey BULLETS_KEY = new NamespacedKey(lime._plugin, "bullets");

        public final String bullet_type;
        public final String magazine_type;
        public final int size;

        public final String sound_load;
        public final String sound_unload;

        public MagazineSetting(Items.ItemCreator creator, JsonObject json) {
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
                    .map(PersistentDataHolder::getPersistentDataContainer)
                    .map(v -> v.get(BULLETS_KEY, PersistentDataType.STRING))
                    .map(v -> v.split(" "))
                    .map(Arrays::stream)
                    .map(v -> v.filter(_v -> !_v.equals("")))
                    .map(v -> v.map(system::loadItem).collect(Collectors.toList()));
        }
        public static void setBullets(ItemStack magazine, List<ItemStack> bullets) {
            ItemMeta meta = magazine.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(BULLETS_KEY, PersistentDataType.STRING, bullets.stream().map(system::saveItem).collect(Collectors.joining(" ")));
            magazine.setItemMeta(meta);

            Items.getItemCreator(magazine)
                    .map(v -> v instanceof Items.ItemCreator c ? c : null)
                    .ifPresent(v -> v.apply(magazine));
        }

        @Override public void appendArgs(ItemStack magazine, Apply apply) {
            List<ItemStack> bullets = getBullets(magazine).orElseGet(Collections::emptyList);
            apply.add("bullet_count", bullets.size() + "");
            apply.add("bullets", system.json.array()
                    .add(bullets, item -> Optional.ofNullable(item)
                            .flatMap(v -> Items.getOptional(Settings.BulletSetting.class, v))
                            .map(v -> system.json.object()
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
    }
    @Setting(name = "bullet") public static class BulletSetting extends ItemSetting<JsonObject> {
        public enum BulletAction {
            NONE,
            TASER,
            TRAUMATIC
        }

        public final String bullet_type;
        public final BulletAction bullet_action;
        public final int count;
        public final double damage;
        public final double time_sec;
        public final double time_damage_scale;

        public final HashMap<SoundMaterial, String> sound_hit = new HashMap<>();

        public BulletSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            bullet_type = json.get("bullet_type").getAsString();
            count = json.has("count") ? json.get("count").getAsInt() : 1;
            damage = json.get("damage").getAsDouble();
            if (json.has("time")) {
                JsonObject time = json.getAsJsonObject("time");
                time_sec = time.get("sec").getAsDouble();
                time_damage_scale = time.get("damage_scale").getAsDouble();
            } else {
                time_sec = Double.POSITIVE_INFINITY;
                time_damage_scale = 0;
            }

            bullet_action = json.has("bullet_action")
                    ? system.tryParse(BulletAction.class, json.get("bullet_action").getAsString())
                    .orElseThrow(() -> new IllegalArgumentException("bullet_action can't be '" + json.get("bullet_action").getAsString() + "'. Only: " + Arrays.stream(BulletAction.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")))
                    )
                    : BulletAction.NONE;

            if (json.has("sound_hit")) json.getAsJsonObject("sound_hit").entrySet().forEach(kv -> sound_hit.put(SoundMaterial.valueOf(kv.getKey()), kv.getValue().getAsString()));
        }

        public void playSound(Material material, Location location) {
            Sounds.playSound(sound_hit.get(SoundMaterial.of(material)), location);
        }
        public void playSound(IBlockData state, Location location) {
            Sounds.playSound(sound_hit.get(SoundMaterial.of(state)), location);
        }
    }
    @Setting(name = "stem") public static class StemSetting extends ItemSetting<JsonObject> {
        public final String key;
        public final String result;
        public StemSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            key = json.get("key").getAsString();
            result = json.get("result").getAsString();
        }
    }
    @Setting(name = "display") public static class DisplaySetting extends ItemSetting<JsonObject> {
        public String item;
        public DisplaySetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            item = json.get("item").getAsString();
        }

        public Optional<ItemStack> item(int original_id) {
            return Items.getItemCreator(item).map(v -> v.createItem(Apply.of().add("original_id", original_id + "")));
        }
    }
    @Setting(name = "lock_menu") public static class LockMenuSetting extends ItemSetting<JsonPrimitive> {
        public final boolean isLock;
        public LockMenuSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            isLock = json.getAsBoolean();
        }
    }
    @Setting(name = "hide_nick") public static class HideNickSetting extends ItemSetting<JsonPrimitive> {
        public final boolean isHide;
        public HideNickSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            isHide = json.getAsBoolean();
        }

        private static final List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        public static boolean isHide(Player player) {
            PlayerInventory inventory = player.getInventory();
            for (EquipmentSlot slot : slots)
                if (Items.getOptional(HideNickSetting.class, inventory.getItem(slot)).filter(v -> v.isHide).isPresent())
                    return true;
            return false;
        }
    }
    @Setting(name = "thirst") public static class ThirstSetting extends ItemSetting<JsonObject> {
        public static final Color DEFAULT_WATER_COLOR = Color.fromRGB(0x3F76E4);

        public static ItemStack createWaterBottle() {
            ItemStack item = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta)item.getItemMeta();
            meta.setBasePotionData(new PotionData(PotionType.WATER));
            item.setItemMeta(meta);
            return item;
        }
        public static ItemStack createClearBottle() {
            return Items.createItem("Potion.Clear_Water").orElseGet(ThirstSetting::createWaterBottle);
        }

        public final String type;
        public final Color color;
        public ThirstSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);

            type = json.get("type").getAsString();
            color = json.has("color") ? ChatColorHex.of(json.get("color").getAsString()).toBukkitColor() : null;
        }
    }
    @Setting(name = "equip") public static class EquipSetting extends ItemSetting<JsonPrimitive> {
        public final EnumItemSlot slot;
        public EquipSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            this.slot = EnumItemSlot.byName(json.getAsString().toLowerCase());
        }
    }
    @Setting(name = "vest") public static class VestSetting extends ItemSetting<JsonObject> {
        public final int rows;
        public final Component title;
        public final Map<Integer, Items.Checker> slots = new HashMap<>();

        public VestSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);

            this.rows = json.has("rows") ? json.get("rows").getAsInt() : 1;
            this.title = ChatHelper.formatComponent(json.get("title").getAsString());
            json.getAsJsonObject("slots").entrySet().forEach(kv -> {
                Items.Checker checker = Items.createCheck(kv.getValue().getAsString());
                Menu.rangeOf(kv.getKey()).forEach(slot -> this.slots.put(slot, checker));
            });
        }
    }
    @Setting(name = "slots") public static class SlotsSetting extends ItemSetting<JsonPrimitive> {
        public final int slots;

        public SlotsSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            this.slots = json.getAsInt();
        }
    }
    @Setting(name = "durability") public static class DurabilitySetting extends ItemSetting<JsonPrimitive> {
        public final int maxDurability;
        public DurabilitySetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            maxDurability = json.getAsInt();
        }
    }
    @Setting(name = "max_stack") public static class MaxStackSetting extends ItemSetting<JsonPrimitive> {
        public final int maxStack;
        public MaxStackSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            maxStack = json.getAsInt();
        }
    }
    @Setting(name = "sweep") public static class SweepSetting extends ItemSetting<JsonPrimitive> {
        public final boolean sweep;
        public SweepSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            sweep = json.getAsBoolean();
        }
    }
    @Setting(name = "dye_color") public static class DyeColorSetting extends ItemSetting<JsonPrimitive> {
        public final boolean dyeColor;
        public DyeColorSetting(Items.ItemCreator creator, JsonPrimitive json) {
            super(creator, json);
            dyeColor = json.getAsBoolean();
        }
    }
    @Setting(name = "drugs") public static class DrugsSetting extends ItemSetting<JsonObject> {
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
    @Setting(name = "musical_instrument") public static class MusicalInstrumentSetting extends ItemSetting<JsonObject> implements UseSetting.IUse {
        public final system.Action2<Location, Float> pitch;
        public final EquipmentSlot arm;
        public final double cooldown;
        public final String menu;
        public final HashMap<String, String> args = new HashMap<>();
        public MusicalInstrumentSetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            this.pitch = json.has("note") ? new system.Action2<>() {
                final org.bukkit.Sound sound = org.bukkit.Sound.valueOf("BLOCK_NOTE_BLOCK_" + json.get("note").getAsString());
                @Override public void invoke(Location location, Float pitch) {
                    location.getWorld().playSound(Sound.sound(sound, Sound.Source.PLAYER, 1.0f, pitch), location.getX(), location.getY(), location.getZ());
                }
            } : new system.Action2<>() {
                final String sound = json.get("sound").getAsString();
                @Override public void invoke(Location location, Float pitch) {
                    Sounds.playSound(sound, location);
                }
            };
            this.cooldown = json.has("cooldown") ? json.get("cooldown").getAsDouble() : 0;
            this.menu = json.has("menu") ? json.get("menu").getAsString() : null;
            this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
            if (json.has("args")) json.get("args").getAsJsonObject().entrySet().forEach(kv -> args.put(kv.getKey(), kv.getValue().getAsString()));
        }

        @Override public EquipmentSlot arm() { return arm; }
        @Override public boolean use(Player player, Player target, EquipmentSlot arm, boolean shift) {
            Location location = player.getLocation();
            if (cooldown > 0 && Cooldown.hasOrSetCooldown(player.getUniqueId(), creator().getKey(), cooldown)) return false;
            float pitch = (Math.min(30, Math.max(-20, player.getEyeLocation().getPitch())) + 20) / 50;
            this.pitch.invoke(location, pitch);
            if (this.menu != null) MenuCreator.show(player, this.menu, Apply.of().add(args));
            return false;
        }
    }
    @Setting(name = "de_key") public static class DeKeySetting extends ItemSetting<JsonNull> {
        public DeKeySetting(Items.ItemCreator creator) {
            super(creator);
        }
    }
    @Setting(name = "backpack_drop") public static class BackPackDropSetting extends ItemSetting<JsonNull> {
        public BackPackDropSetting(Items.ItemCreator creator) {
            super(creator);
        }
    }
    @Setting(name = "table_dispaly") public static class TableDisplaySetting extends ItemSetting<JsonObject> {
        public enum TableType {
            inventory,
            converter,
            laboratory,
            clicker,
            all;

            public static Stream<TableType> all() {
                return Stream.of(TableType.converter, TableType.laboratory, TableType.clicker);
            }
        }

        public static abstract class ITableInfo {
            public abstract net.minecraft.world.item.ItemStack display(ItemStack original);
            public abstract net.minecraft.world.item.ItemStack display(net.minecraft.world.item.ItemStack original);
            public abstract ITableInfo optimize();
        }
        public static class StaticTableInfo extends ITableInfo {
            public final Material material;
            public final int id;
            public final ItemStack bukkit_item;
            private final net.minecraft.world.item.ItemStack nms_item;

            public StaticTableInfo(Material material, int id) {
                this.material = material;
                this.id = id;

                bukkit_item = new ItemStack(material);
                ItemMeta meta = bukkit_item.getItemMeta();
                meta.setCustomModelData(id);
                bukkit_item.setItemMeta(meta);
                nms_item = CraftItemStack.asNMSCopy(bukkit_item);
            }
            @Override public net.minecraft.world.item.ItemStack display(ItemStack original) { return nms_item; }
            @Override public net.minecraft.world.item.ItemStack display(net.minecraft.world.item.ItemStack original) { return nms_item; }
            @Override public ITableInfo optimize() { return this; }
        }
        public static class DynamicTableInfo extends ITableInfo {
            public final Optional<Material> material;
            public final Optional<Integer> id;

            public DynamicTableInfo(JsonObject json) {
                material = json.has("material")
                        ? Optional.of(Material.valueOf(json.get("material").getAsString()))
                        : Optional.empty();
                id = json.has("id")
                        ? Optional.of(json.get("id").getAsInt())
                        : Optional.empty();
            }

            @Override public net.minecraft.world.item.ItemStack display(ItemStack original) {
                ItemStack item = new ItemStack(material.orElseGet(original::getType));
                ItemMeta meta = item.getItemMeta();
                meta.setCustomModelData(id.orElseGet(() -> {
                    ItemMeta _meta = original.getItemMeta();
                    return _meta.hasCustomModelData() ? _meta.getCustomModelData() : null;
                }));
                item.setItemMeta(meta);
                return CraftItemStack.asNMSCopy(item);
            }
            @Override public net.minecraft.world.item.ItemStack display(net.minecraft.world.item.ItemStack original) {
                ItemStack item = new ItemStack(material.orElseGet(() -> CraftMagicNumbers.getMaterial(original.getItem())));
                ItemMeta meta = item.getItemMeta();
                meta.setCustomModelData(id.orElseGet(() -> ItemNMS.getCustomModelData(original).orElse(null)));
                item.setItemMeta(meta);
                return CraftItemStack.asNMSCopy(item);
            }
            public Optional<StaticTableInfo> tryStatic() {
                return this.material.flatMap(material -> this.id.map(id -> new StaticTableInfo(material, id)));
            }
            @Override public ITableInfo optimize() { return tryStatic().<ITableInfo>map(v -> v).orElse(this); }
        }

        private final HashMap<system.Toast2<TableType, String>, ITableInfo> infos = new HashMap<>();

        public Optional<ITableInfo> of(TableType tableType, @Nullable String type) {
            return type == null
                    ? Optional.ofNullable(infos.get(system.toast(tableType, null)))
                    : Optional.ofNullable(infos.get(system.toast(tableType, type))).or(() -> Optional.ofNullable(infos.get(system.toast(tableType, null))));
        }

        public TableDisplaySetting(Items.ItemCreator creator, JsonObject json) {
            super(creator, json);
            json.entrySet().forEach(kv -> {
                String[] args = kv.getKey().split(":", 2);
                TableType type = TableType.valueOf(args[0]);
                ITableInfo info = new DynamicTableInfo(kv.getValue().getAsJsonObject()).optimize();
                (type == TableType.all ? TableType.all() : Stream.of(type))
                        .forEach(_type -> infos.put(system.toast(_type, args.length > 1 ? args[1] : null), info));
            });
        }
    }
}





































