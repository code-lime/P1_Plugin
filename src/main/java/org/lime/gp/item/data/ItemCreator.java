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
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.chat.TextSplitRenderer;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.IItemSetting;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.list.MaxStackSetting;
import org.lime.gp.lime;
import org.lime.gp.player.menu.page.slot.ISlot;
import org.lime.system;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemCreator extends IItemCreator {
    static final Map<String, Attribute> ATTRIBUTE_NAMES = system.map.<String, Attribute>of()
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
    public final List<system.Toast2<String, String>> args = new ArrayList<>();
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
        this.nullable_cache_item = system.tryParse(Material.class, this.item).orElse(null);
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
        if (json.has("args")) json.get("args").getAsJsonObject().entrySet().forEach(arg -> this.args.add(system.toast(arg.getKey(), arg.getValue().getAsString())));
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
                    AttributeModifier.Operation operation = switch (value_args[2].charAt(0)) {
                        case '+' -> AttributeModifier.Operation.ADD_NUMBER;
                        case '*' -> AttributeModifier.Operation.ADD_SCALAR;
                        case 'x' -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                        default -> throw new IllegalStateException("Unexpected operation in attribute: " + value_args[2].charAt(0));
                    };
                    double amount = Double.parseDouble(value_args[2].substring(1));
                    slots.forEach(slot -> attributes.put(attribute, Items.generate(attribute, amount, operation, slot)));
                });
    }

    public static ItemCreator parse(JsonObject json) {
        return new ItemCreator(null, json);
    }

    public ItemStack createItem(int count, Apply apply) {
        ItemStack item = CraftItemStack.asCraftMirror(CraftItemStack.asNMSCopy(new ItemStack(Material.valueOf(ChatHelper.formatText(this.item, apply)))));
        return apply(item, count, apply);
    }
    public List<Component> createLore(Apply apply) {
        List<Component> lore = new ArrayList<>();
        this.lore.forEach(line -> lore.addAll(TextSplitRenderer.split(ChatHelper.formatComponent(ChatHelper.formatText(line, apply)), "\n")));
        return lore;
    }

    public ItemStack apply(ItemStack item) { return apply(item, Apply.of()); }
    public ItemStack apply(ItemStack item, Apply apply) { return apply(item, null, apply); }
    public ItemStack apply(ItemStack item, Integer count, Apply apply) {
        int maxDamage = Items.getMaxDamage(item);
        if (count != null) item.setAmount(count);
        Apply _apply = ISlot.createArgs(this.args, apply);
        settings.values().forEach(setting -> setting.appendArgs(item, _apply));
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.displayName(ChatHelper.formatComponent(name, _apply));
        meta.lore(createLore(_apply));
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
        settings.values().forEach(setting -> setting.apply(item, meta, _apply));
        if (instrument != null)
            InstrumentSoundItem.setInstrument(ItemNMS.getUnhandledTags(meta), instrument.sound(), instrument.range(), instrument.cooldown());
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