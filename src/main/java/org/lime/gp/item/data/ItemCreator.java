package org.lime.gp.item.data;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.minecraft.world.item.InstrumentSoundItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.lime.docs.IGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.chat.TextSplitRenderer;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.IItemSetting;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.list.MaxStackSetting;
import org.lime.gp.lime;
import org.lime.gp.player.menu.page.slot.ISlot;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.EnumUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemCreator extends IItemCreator {
    static final Map<String, Attribute> ATTRIBUTE_NAMES = map.<String, Attribute>of()
            .add(Arrays.asList(Attribute.values()), kv -> Arrays.stream(kv.getKey().getKey().split("\\.")).skip(1).collect(Collectors.joining(".")), kv -> kv)
            .build();

    private final String _key;
    private final int _id;

    public final String item;
    private final Material nullable_cache_item;
    public final String id;
    public final String name;
    public final List<String> lore = new ArrayList<>();
    public final List<ItemFlag> flags = new ArrayList<>();
    private final List<Toast2<String, String>> args = new ArrayList<>();

    public List<Toast2<String, String>> args(Apply apply) { return args; }

    public final List<String> charged = new ArrayList<>();
    public final HashMap<String, JsonElement> data = new HashMap<>();
    public final LinkedHashMultimap<Attribute, AttributeModifier> attributes = LinkedHashMultimap.create();
    public final HashMap<String, ItemSetting<?>> settings = new HashMap<>();

    public final String head_uuid;
    public final String head_data;
    public final String color;
    public final boolean is_stack;
    public record Instrument(String sound, float range, int cooldown) {
        public Instrument(JsonObject json) {
            this(json.get("sound").getAsString(), json.get("range").getAsFloat(), json.get("cooldown").getAsInt());
        }
    }
    public final Instrument instrument;

    public final HashMap<String, String> enchants = new HashMap<>();

    private final static HashMap<String, Enchantment> _enchants;
    static {
        _enchants = new HashMap<>();
        for (Field field : Enchantment.class.getFields()) {
            if (field.getType() == Enchantment.class) {
                try { _enchants.put(field.getName(), (Enchantment) field.get(null)); } catch (Exception e) { lime.logStackTrace(e); }
            }
        }
    }
    private static Enchantment getEnchantment(String name) { return _enchants.get(name); }

    @Override public boolean updateReplace() { return true; }

    @Override public String getKey() { return _key; }
    @Override public int getID() { return _id; }
    @Override public Stream<Material> getWhitelist() { return Stream.ofNullable(nullable_cache_item); }
    @Override public Optional<Integer> tryGetMaxStackSize() {
        return (is_stack ? getOptional(MaxStackSetting.class).map(_v -> _v.maxStack) : Optional.of(1))
                .or(() -> Optional.ofNullable(nullable_cache_item).map(Material::getMaxStackSize));
    }

    public final List<PotionEffect> potionEffects = new ArrayList<>();
    public ItemCreator(String key, JsonObject json) {
        this._key = key;

        this.item = json.get("item").getAsString();
        this.nullable_cache_item = EnumUtils.tryParse(Material.class, this.item).orElse(null);
        this.name = json.has("name") ? json.get("name").getAsString() : null;
        this.id = json.has("id") ? json.get("id").getAsString() : null;

        int _id = 0;
        try { _id = id == null ? 0 : Integer.parseInt(id); } catch (Exception ignored) { }
        this._id = _id;

        if (json.has("lore")) json.get("lore").getAsJsonArray().forEach(lore -> this.lore.add(lore.getAsString()));

        this.head_uuid = json.has("head_uuid") ? json.get("head_uuid").getAsString() : null;
        this.is_stack = !json.has("is_stack") || json.get("is_stack").getAsBoolean();
        this.head_data = json.has("head_data") ? json.get("head_data").getAsString() : null;
        this.color = json.has("color") ? json.get("color").getAsString() : null;
        if (json.has("args")) json.get("args")
                .getAsJsonObject()
                .entrySet()
                .forEach(arg -> {
                    JsonElement val = arg.getValue();
                    this.args.add(Toast.of(arg.getKey(), val.isJsonPrimitive()
                            ? val.getAsString()
                            : val.toString()));
                });
        if (json.has("data")) json.get("data").getAsJsonObject().entrySet().forEach(arg -> this.data.put(arg.getKey(), arg.getValue()));
        if (json.has("potion_effects")) json.get("potion_effects").getAsJsonArray().forEach(arg -> this.potionEffects.add(Items.parseEffect(arg.getAsJsonObject())));
        if (json.has("flags")) json.get("flags").getAsJsonArray().forEach(arg -> this.flags.add(ItemFlag.valueOf(arg.getAsString())));
        if (json.has("enchants")) json.get("enchants").getAsJsonObject().entrySet().forEach(kv -> enchants.put(kv.getKey(), kv.getValue().getAsString()));
        if (json.has("settings")) json.get("settings").getAsJsonObject().entrySet().forEach(kv -> {
            ItemSetting<?> setting = ItemSetting.parse(kv.getKey(), this, kv.getValue());
            settings.put(setting.name(), setting);
        });
        instrument = json.has("instrument") ? new Instrument(json.get("instrument").getAsJsonObject()) : null;
        if (json.has("charged")) json.get("charged").getAsJsonArray().forEach(item -> charged.add(item.getAsString()));
        if (json.has("attributes")) json.getAsJsonArray("attributes")
                .forEach(item -> {
                    String value = item.getAsString();
                    String[] value_args = value.split(":");
                    Attribute attribute = ATTRIBUTE_NAMES.get(value_args[0]);
                    if (attribute == null) throw new IllegalArgumentException("Attribute '"+value_args[0]+"' not founded!");
                    List<EquipmentSlot> slots = Arrays.stream(value_args[1].split(",")).map(EquipmentSlot::valueOf).toList();
                    int sign = 1;
                    AttributeModifier.Operation operation = switch (value_args[2].charAt(0)) {
                        case '+' -> AttributeModifier.Operation.ADD_NUMBER;
                        case '-' -> {
                            sign = -1;
                            yield AttributeModifier.Operation.ADD_NUMBER;
                        }
                        case '*' -> AttributeModifier.Operation.ADD_SCALAR;
                        case 'x' -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                        default -> throw new IllegalStateException("Unexpected operation in attribute: " + value_args[2].charAt(0));
                    };
                    double amount = sign * Double.parseDouble(value_args[2].substring(1));
                    slots.forEach(slot -> attributes.put(attribute, Items.generate(attribute, amount, operation, slot)));
                });
    }
    public static IGroup docs(String title, IDocsLink docs) {
        return JsonGroup.of(title, title, JObject.of(
                JProperty.require(IName.raw("item"), IJElement.link(docs.vanillaMaterial()), IComment.text("Тип основы предмета")),
                JProperty.optional(IName.raw("name"), IJElement.link(docs.formattedChat()), IComment.text("Отображаемое название предмета")),
                JProperty.optional(IName.raw("id"), IJElement.raw(10).or(IJElement.link(docs.formattedText())), IComment.empty()
                        .append(IComment.text("Уникальный индентификатор предмета. После установки ЗАПРЕЩЕНО менять. Используется в ресурспаке как "))
                        .append(IComment.raw("CustomModelData"))),
                JProperty.optional(IName.raw("lore"), IJElement.anyList(IJElement.link(docs.formattedChat())), IComment.text("Описание предмета")),
                JProperty.optional(IName.raw("head_uuid"), IJElement.link(docs.formattedText()), IComment.empty()
                        .append(IComment.raw("UUID"))
                        .append(IComment.text(" игрока, текстуру головы которого требуется установить. Работает только при "))
                        .append(IComment.field("item"))
                        .append(IComment.text(" - "))
                        .append(IComment.raw(Material.PLAYER_HEAD))),
                JProperty.optional(IName.raw("head_data"), IJElement.link(docs.formattedText()), IComment.empty()
                        .append(IComment.text("Прямые данные на текстуру головы игрока, которую требуется установить. Работает только при "))
                        .append(IComment.field("item"))
                        .append(IComment.text(" - "))
                        .append(IComment.raw(Material.PLAYER_HEAD))),
                JProperty.optional(IName.raw("is_stack"), IJElement.bool(), IComment.empty()
                        .append(IComment.raw("Указывается, возможно ли стакание предмета. "))
                        .append(IComment.warning("ВНИМАНИЕ! НЕ РЕКОМЕНДУЕТСЯ ИСПОЛЬЗОВАТЬ! ПРИОРИТЕТ К ИСПОЛЬЗОВАНИЮ: "))
                        .append(IComment.link(docs.settingsLink(MaxStackSetting.class)))),
                JProperty.optional(IName.raw("color"), IJElement.link(docs.formattedText()), IComment.empty()
                        .append(IComment.text("Устанавливает цвет предмета. Цвет в HEX-формате "))
                        .append(IComment.raw("#FFFFFF"))),
                JProperty.optional(IName.raw("args"),
                        IJElement.anyObject(JProperty.require(IName.raw("KEY"), IJElement.link(docs.formattedText()))),
                        IComment.empty()
                                .append(IComment.raw("args"))
                                .append(IComment.text(" предмета. Данные "))
                                .append(IComment.raw("args"))
                                .append(IComment.text(" заменяют собой передаваемые "))
                                .append(IComment.raw("args"))),
                JProperty.optional(IName.raw("data"),
                        IJElement.anyObject(JProperty.require(IName.raw("KEY"), IJElement.link(docs.formattedText()))),
                        IComment.empty()
                                .append(IComment.text("Сохраняемые данные внутри предмета"))),
                JProperty.optional(IName.raw("potion_effects"),
                        IJElement.anyList(IJElement.link(docs.potionEffect())),
                        IComment.empty()
                                .append(IComment.text("Эффекты устанавливаемые при выпивании зелья. Работает только при "))
                                .append(IComment.field("item"))
                                .append(IComment.text(" - "))
                                .append(IComment.or(
                                        IComment.raw(Material.POTION),
                                        IComment.raw(Material.SPLASH_POTION),
                                        IComment.raw(Material.LINGERING_POTION)
                                ))),
                JProperty.optional(IName.raw("flags"),
                        IJElement.anyList(IJElement.link(docs.itemFlag())),
                        IComment.empty()
                                .append(IComment.text("Изменяет флаговые характеристики предмета"))),
                JProperty.optional(IName.raw("enchants"),
                        IJElement.anyObject(JProperty.require(
                                IName.link(docs.enchantment()),
                                IJElement.raw(0).or(IJElement.link(docs.formattedText()))
                        )),
                        IComment.empty()
                                .append(IComment.text("Зачарования устанавливаемые на предмет"))),
                JProperty.optional(IName.raw("charged"),
                        IJElement.anyList(IJElement.link(docs.regexItem())),
                        IComment.empty()
                                .append(IComment.text("Указывает список заряженых предметов в предмете. "))
                                .append(IComment.text("Пример использования: Арбалет с заряженной стрелой").italic())
                                .append(IComment.text("Работает только при "))
                                .append(IComment.field("item"))
                                .append(IComment.text(" - "))
                                .append(IComment.raw(Material.CROSSBOW))),
                JProperty.optional(IName.raw("attributes"),
                        IJElement.anyList(IJElement.link(docs.attribute())),
                        IComment.empty()
                                .append(IComment.text("Устанавливает аттрибуты предмета"))),
                JProperty.optional(IName.raw("settings"),
                        IJElement.anyObject(JProperty.require(IName.raw("SETTING_NAME"), IJElement.link(docs.setting()))),
                        IComment.empty()
                                .append(IComment.text("Указывает список настроек для конкретного предмета"))),
                JProperty.optional(IName.raw("instrument"),
                        IJElement.link(docs.instrument()),
                        IComment.empty()
                                .append(IComment.text("Устанавливает звук воспроизводимый при взаимодействии"))
                                .append(IComment.text("Работает только при "))
                                .append(IComment.field("item"))
                                .append(IComment.text(" - "))
                                .append(IComment.raw(Material.GOAT_HORN)))
        ));
    }

    public static ItemCreator parse(JsonObject json) {
        return new ItemCreator(null, json);
    }

    public ItemStack createItem(int count, Apply apply) {
        ItemStack item = CraftItemStack.asCraftMirror(CraftItemStack.asNMSCopy(new ItemStack(Material.valueOf(ChatHelper.formatText(this.item, ISlot.createArgs(args(apply), apply))))));
        return apply(item, count, apply);
    }
    private List<Component> applyLore(Apply apply) {
        List<Component> lore = new ArrayList<>();
        this.lore.forEach(line -> lore.addAll(TextSplitRenderer.split(ChatHelper.formatComponent(ChatHelper.formatText(line, apply)), "\n")));
        return lore;
    }

    public void update(ItemMeta meta, Apply apply, IUpdate lists) {
        Apply _apply = ISlot.createArgs(args(apply), apply);
        settings.values().forEach(setting -> setting.appendArgs(meta, _apply));
        if (lists.is(UpdateType.NAME)) {
            if (name != null)
                meta.displayName(ChatHelper.formatComponent(name, _apply));
        }
        if (lists.is(UpdateType.LORE)) meta.lore(applyLore(_apply));
        if (lists.is(UpdateType.ID)) {
            if (id != null) {
                String _id = _apply.apply(id);
                if (_id != null) meta.setCustomModelData(Integer.parseInt(_id));
            }
        }
        if (lists.is(UpdateType.HEAD)) {
            if (head_uuid != null && meta instanceof SkullMeta skull) {
                String uuid = ChatHelper.formatText(head_uuid, _apply);
                if (!uuid.isEmpty()) skull.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
            }
            if (head_data != null && meta instanceof SkullMeta skull) {
                String data = ChatHelper.formatText(head_data, _apply);
                if (!data.isEmpty()) {
                    CraftPlayerProfile profile = new CraftPlayerProfile(UUID.randomUUID(), null);
                    profile.setProperty(new ProfileProperty("textures", data));
                    skull.setPlayerProfile(profile);
                }
            }
        }
        if (lists.is(UpdateType.COLOR)) {
            if (color != null && meta instanceof LeatherArmorMeta leather) leather.setColor(ChatColorHex.of(ChatHelper.formatText(color, _apply)).toBukkitColor());
            if (color != null && meta instanceof PotionMeta potion) potion.setColor(ChatColorHex.of(ChatHelper.formatText(color, _apply)).toBukkitColor());
        }
        if (lists.is(UpdateType.CHARGED)) {
            if (charged.size() != 0 && meta instanceof CrossbowMeta crossbow)
                crossbow.setChargedProjectiles(charged.stream().map(Checker::createCheck).flatMap(v -> v.getRandomCreator().stream()).map(IItemCreator::createItem).toList());
        }
        if (lists.is(UpdateType.POTION)) {
            if (potionEffects.size() != 0 && meta instanceof PotionMeta potion)
                potionEffects.forEach(effect -> potion.addCustomEffect(effect, false));
        }
        if (lists.is(UpdateType.ATTRIBUTES)) {
            meta.setAttributeModifiers(attributes);
        }
        if (lists.any(UpdateType.DATA, UpdateType.STACK)) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (lists.is(UpdateType.DATA)) {
                data.forEach((k, v) -> JManager.set(container, k, setArgs(v, _apply)));
            }
            if (lists.is(UpdateType.STACK)) {
                if (!is_stack) JManager.set(container, "uuid", new JsonPrimitive(UUID.randomUUID().toString()));
                else JManager.del(container, "uuid");
            }
        }
        if (lists.is(UpdateType.FLAGS)) {
            flags.forEach(meta::addItemFlags);
        }
        if (lists.is(UpdateType.ENCHANTS)) {
            if (enchants.size() != 0) enchants.forEach((k, v) -> {
                Enchantment enchantment = getEnchantment(_apply.apply(k));
                try { meta.addEnchant(enchantment, Integer.parseInt(_apply.apply(v)), true); } catch (Exception ignored) { }
            });
        }
        if (lists.is(UpdateType.SETTINGS)) {
            settings.values().forEach(setting -> setting.apply(meta, _apply));
        }
        if (lists.is(UpdateType.INSTRUMENT)) {
            if (instrument != null)
                InstrumentSoundItem.setInstrument(ItemNMS.getUnhandledTags(meta), instrument.sound(), instrument.range(), instrument.cooldown());
        }
    }

    public ItemStack apply(ItemStack item) { return apply(item, Apply.of()); }
    public ItemStack apply(ItemStack item, Apply apply) { return apply(item, null, apply); }
    public ItemStack apply(ItemStack item, Integer count, Apply apply) {
        int maxDamage = Items.getMaxDamage(item);
        if (count != null) item.setAmount(count);

        ItemMeta meta = item.getItemMeta();
        update(meta, apply, IUpdate.all());

        /*
        Apply _apply = ISlot.createArgs(args(apply), apply);
        ItemMeta meta = item.getItemMeta();
        settings.values().forEach(setting -> setting.appendArgs(meta, _apply));
        if (name != null) meta.displayName(ChatHelper.formatComponent(name, _apply));
        meta.lore(applyLore(_apply));
        if (id != null) {
            String _id = _apply.apply(id);
            if (_id != null) meta.setCustomModelData(Integer.parseInt(_id));
        }
        if (head_uuid != null && meta instanceof SkullMeta skull) {
            String uuid = ChatHelper.formatText(head_uuid, _apply);
            if (!uuid.isEmpty()) skull.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
        }
        if (head_data != null && meta instanceof SkullMeta skull) {
            String data = ChatHelper.formatText(head_data, _apply);
            if (!data.isEmpty()) {
                CraftPlayerProfile profile = new CraftPlayerProfile(UUID.randomUUID(), null);
                profile.setProperty(new ProfileProperty("textures", data));
                skull.setPlayerProfile(profile);
            }
        }
        if (color != null && meta instanceof LeatherArmorMeta leather) leather.setColor(ChatColorHex.of(ChatHelper.formatText(color, _apply)).toBukkitColor());
        if (color != null && meta instanceof PotionMeta potion) potion.setColor(ChatColorHex.of(ChatHelper.formatText(color, _apply)).toBukkitColor());
        if (charged.size() != 0 && meta instanceof CrossbowMeta crossbow) crossbow.setChargedProjectiles(charged.stream().map(Items::createItem).filter(Optional::isPresent).map(Optional::get).toList());
        if (potionEffects.size() != 0 && meta instanceof PotionMeta potion) potionEffects.forEach(effect -> potion.addCustomEffect(effect, false));
        if (meta instanceof Damageable damageable && maxDamage > 0) {
            double damage = Math.min(1, Math.max(0, damageable.getDamage() / (double)maxDamage));
            maxDamage = Items.getMaxDamage(item.getType(), meta);
            damageable.setDamage((int)Math.round(damage * maxDamage));
        }
        meta.setAttributeModifiers(attributes);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        data.forEach((k, v) -> JManager.set(container, k, setArgs(v, _apply)));
        if (!is_stack) JManager.set(container, "uuid", new JsonPrimitive(UUID.randomUUID().toString()));
        flags.forEach(meta::addItemFlags);
        if (enchants.size() != 0) enchants.forEach((k,v) -> {
            Enchantment enchantment = getEnchantment(_apply.apply(k));
            try { meta.addEnchant(enchantment, Integer.parseInt(_apply.apply(v)), true); }
            catch (Exception ignored) { }
        });
        settings.values().forEach(setting -> setting.apply(meta, _apply));
        if (instrument != null) InstrumentSoundItem.setInstrument(ItemNMS.getUnhandledTags(meta), instrument.sound(), instrument.range(), instrument.cooldown());
        */

        if (meta instanceof Damageable damageable && maxDamage > 0) {
            double damage = Math.min(1, Math.max(0, damageable.getDamage() / (double)maxDamage));
            int newMaxDamage = Items.getMaxDamage(item.getType(), meta);
            int newDamage = (int)Math.round(damage * newMaxDamage);
            /*lime.logOP("Change damage:\n   " + String.join("\n   ",
                    "Old damage: " + damageable.getDamage() + " / " + maxDamage,
                    "New damage: " + newDamage + " / " + newMaxDamage,
                    "Delta damage: " + damage
            ));*/
            damageable.setDamage(newDamage);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static JsonElement setArgs(JsonElement json, Apply apply) {
        if (json.isJsonNull()) return json;
        else if (json.isJsonPrimitive()) {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            return primitive.isString() ? new JsonPrimitive(ChatHelper.formatText(primitive.getAsString(), apply)) : primitive;
        } else if (json.isJsonArray()) {
            JsonArray arr = new JsonArray();
            json.getAsJsonArray().forEach(item -> arr.add(setArgs(item, apply)));
            return arr;
        } else if (json.isJsonObject()) {
            JsonObject obj = new JsonObject();
            json.getAsJsonObject().entrySet().forEach(kv -> obj.add(kv.getKey(), setArgs(kv.getValue(), apply)));
            return obj;
        } else throw new UnsupportedOperationException("Unsupported element: " + json);
    }

    
    @SuppressWarnings("unchecked")
    public <T extends IItemSetting>Optional<T> getOptional(Class<T> tClass) {
        return settings.values()
                .stream()
                .filter(tClass::isInstance)
                .map(v -> (T)v)
                .findFirst();
    }
    @SuppressWarnings("unchecked")
    public <T extends IItemSetting>List<T> getAll(Class<T> tClass) {
        return settings.values()
                .stream()
                .filter(tClass::isInstance)
                .map(v -> (T)v)
                .collect(Collectors.toList());
    }
    public boolean has(Class<? extends IItemSetting> tClass) {
        return settings.values()
                .stream()
                .anyMatch(tClass::isInstance);
    }
}