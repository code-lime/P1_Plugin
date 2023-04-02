package org.lime.gp.item;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.coreprotect.event.AsyncItemInfoEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.core.IRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumHand;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.EntityAttackSweepEvent;
import net.minecraft.world.entity.EntityEquipmentSlotEvent;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemMaxDamageEvent;
import net.minecraft.world.item.ItemStackSizeEvent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.core;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.TextSplitRenderer;
import org.lime.gp.database.Rows;
import org.lime.gp.lime;
import org.lime.gp.player.menu.page.slot.ISlot;
import org.lime.system;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.settings.*;
import org.lime.gp.item.settings.list.DurabilitySetting;
import org.lime.gp.item.settings.list.EquipSetting;
import org.lime.gp.item.settings.list.MaxStackSetting;
import org.lime.gp.item.settings.list.SweepSetting;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.gp.player.inventory.WalletInventory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("deprecation")
public class Items implements Listener {
    public static core.element create() {
        return core.element.create(Items.class)
                .withInstance()
                .withInit(Items::init)
                .<JsonObject>addConfig("items", v -> v.withInvoke(Items::config).withDefault(new JsonObject()));
    }
    private static UUID toUUID(double value, Attribute attribute, AttributeModifier.Operation operation, EquipmentSlot slot) {
        int _value = (int)Math.round(value * 10000);
        if (_value > 999999999) _value = 999999999;
        else if (_value < -999999999) _value = -999999999;

        byte[] bytes = ByteBuffer.allocate(4).putInt(_value).array();
        byte[] data = new byte[8];
        byte _attribute = (byte)attribute.ordinal();
        byte _operation = (byte)operation.ordinal();
        byte _slot = (byte)slot.ordinal();
        System.arraycopy(bytes, 0, data, 0, 4);
        data[4] = _attribute;
        data[5] = _operation;
        data[6] = _slot;
        return new UUID(2030, ByteBuffer.wrap(data).getLong());
    }
    public static AttributeModifier generate(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {
        return new AttributeModifier(toUUID(amount, attribute, operation, slot), attribute.name().toLowerCase().replace('_', '.'), amount, operation, slot);
    }
    private static final Map<String, Attribute> ATTRIBUTE_NAMES = system.map.<String, Attribute>of()
            .add(Arrays.asList(Attribute.values()), kv -> Arrays.stream(kv.getKey().getKey().split("\\.")).skip(1).collect(Collectors.joining(".")), kv -> kv)
            .build();

    public static ItemStack empty() {
        return new ItemStack(Material.STONE, 0);
    }

    public static void init() {
        
        Items.addHardcodeItem("tmp.backpack", system.json.object()
            .add("item", Material.STONE.name())
            .add("id", -50)
            .addObject("settings", v -> v
                .add("equip", "CHEST")
                .addObject("backpack", _v -> _v
                    .addObject("NONE", __v -> __v
                        .add("id", 0)
                        .add("type", Material.FURNACE.name())
                        .add("offset", "0 0 0")
                    )
                    .addObject("SHIFT", __v -> __v
                        .add("id", 0)
                        .add("type", Material.BIRCH_DOOR.name())
                        .add("offset", "0 0 0")
                    )
                )
            )
            .build());

        lime.logOP("Feather max stack size: " + new ItemStack(Material.FEATHER).getMaxStackSize());

        AnyEvent.addEvent("give.item", AnyEvent.type.other, builder -> builder.createParam(Items::createCheck, () -> creatorIDs.keySet().stream().filter(v -> !v.startsWith("Minecraft.")).toList()), (player, _creators) -> {
            _creators.getWhitelistKeys()
                .map(Items.creatorIDs::get)
                .forEach(creator -> dropGiveItem(player, creator.createItem(b -> b.addApply(Rows.UserRow.getBy(player.getUniqueId()).map(v -> Apply.of().add(v)).orElseGet(Apply::of))), false));
        });
        AnyEvent.addEvent("give.item", AnyEvent.type.other, builder -> builder.createParam(Items::createCheck, () -> creatorIDs.keySet().stream().filter(v -> !v.startsWith("Minecraft.")).toList()).createParam(t -> system.json.parse(t).getAsJsonObject().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().isJsonPrimitive() ? kv.getValue().getAsString() : kv.getValue().toString())), "[args:json]"), (player, _creators, args) -> {
            _creators.getWhitelistKeys().map(Items.creatorIDs::get).forEach(creator -> dropGiveItem(player, creator.createItem(b -> b.addApply(Rows.UserRow.getBy(player.getUniqueId()).map(v -> Apply.of().add(v)).orElseGet(Apply::of).add(args))), false));
        });
        AnyEvent.addEvent("drop.item", AnyEvent.type.owner_console, builder -> builder
            .createParam(Double::parseDouble, "[x]")
            .createParam(Double::parseDouble, "[y]")
            .createParam(Double::parseDouble, "[z]")
            .createParam(Items::createCheck, () -> creatorIDs.keySet().stream().filter(v -> !v.startsWith("Minecraft.")).toList()),
             (s, x, y, z, _creators) -> {
                _creators.getWhitelistKeys()
                    .map(Items.creatorIDs::get)
                    .forEach(creator -> Items.dropItem(new Location(lime.MainWorld, x, y, z), creator.createItem()));
            });
        AnyEvent.addEvent("drop.item", AnyEvent.type.owner_console, builder -> builder
            .createParam(Double::parseDouble, "[x]")
            .createParam(Double::parseDouble, "[y]")
            .createParam(Double::parseDouble, "[z]")
            .createParam(Items::createCheck, () -> creatorIDs.keySet().stream().filter(v -> !v.startsWith("Minecraft.")).toList())
            .createParam(t -> system.json.parse(t).getAsJsonObject().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().isJsonPrimitive() ? kv.getValue().getAsString() : kv.getValue().toString())), "[args:json]"), 
            (s, x, y, z, _creators, args) -> {
                _creators.getWhitelistKeys()
                    .map(Items.creatorIDs::get)
                    .forEach(creator -> Items.dropItem(new Location(lime.MainWorld, x, y, z), creator.createItem(v -> v.addApply(Apply.of().add(args)))));
            });
    }

    public static final String AIR = Items.getMaterialKey(Material.AIR);

    public static final HashMap<String, JsonObject> hardcode_items = new HashMap<>();
    public static void addHardcodeItem(String key, JsonObject json) {
        hardcode_items.put(key, json);
    }
    private static int loaded_index = 0;
    public static int getLoadedIndex() { return loaded_index; }
    public static void validateItemKey(String key) {
        for (int i2 = 0; i2 < key.length(); ++i2) {
            if (MinecraftKey.validPathChar(key.charAt(i2))) continue;
            throw new ResourceKeyInvalidException("Non [a-z0-9/._-] character in 'items.json' of location: '" + StringUtils.normalizeSpace(key) + "' in pos '"+i2+"' symbol '" + key.charAt(i2) + "'");
        }
    }
    public static void config(JsonObject json) {
        loaded_index++;
        creatorIDs.values().forEach(creator -> creator.isDestroy = true);
        creatorIDs.clear();
        creators.clear();
        creatorIDsNames.clear();
        creatorCoreProtectIDs.clear();

        JsonObject _new = lime.combineParent(json, true, false);
        hardcode_items.forEach(_new::add);
        _new.entrySet().forEach(kv -> {
            String key = kv.getKey();
            JsonObject value = kv.getValue().getAsJsonObject();
            ItemCreator creator = new ItemCreator(key, value);
            validateItemKey(key.toLowerCase());
            creatorIDs.put(key, creator);
            if (creator.id != null) {
                int id;
                try { id = Integer.parseInt(creator.id); } catch (Exception ignored) { return; }
                creators.put(id, creator);
                creatorIDsNames.put(key, id);
                creatorNamesIDs.put(id, key);
                creatorCoreProtectIDs.put(id, key.toLowerCase().replace(".", "_"));
                try { creatorMaterials.put(id, Material.valueOf(creator.item)); } catch (Exception ignored) { }
            }
        });

        for (Material material : Material.values()) creatorIDs.put(Items.getMaterialKey(material), IItemCreator.byMaterial(material));
    }

    public static abstract class IItemCreator {
        public boolean isDestroy = false;
        public abstract boolean updateReplace();
        public abstract String getKey();
        public abstract int getID();
        public abstract ItemStack createItem(int count, Apply apply);
        public abstract Stream<Material> getWhitelist();

        public final String stack;

        public IItemCreator() {
            stack = Stream.of(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
        }

        public ItemStack createItem() { return createItem(1); }
        public ItemStack createItem(int count) { return createItem(count, Apply.of()); }
        public ItemStack createItem(Apply apply) { return createItem(1, apply); }
        public ItemStack createItem(system.Func1<Builder, Builder> builder) { return builder == null ? this.createItem(1) : builder.invoke(new Builder(this)).create(); }

        public static IItemCreator byMaterial(Material material) { return new MaterialCreator(material); }

        @Override public String toString() { return getClass().getSimpleName() + "^" + getKey(); }
    }
    public static class MaterialCreator extends IItemCreator {
        public final Material material;
        @Override public boolean updateReplace() { return false; }
        @Override public String getKey() { return Items.getMaterialKey(material); }
        @Override public int getID() { return 0; }
        @Override public Stream<Material> getWhitelist() { return Stream.of(material); }

        public MaterialCreator(Material material) { this.material = material; }

        @Override public ItemStack createItem(int count, Apply apply) { return new ItemStack(this.material, count); }
    }
    public static class ItemCreator extends IItemCreator {
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

        public final List<PotionEffect> potionEffects = new ArrayList<>();
        protected ItemCreator(String key, JsonObject json) {
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
            if (json.has("potion_effects")) json.get("potion_effects").getAsJsonArray().forEach(arg -> this.potionEffects.add(parseEffect(arg.getAsJsonObject())));
            if (json.has("flags")) json.get("flags").getAsJsonArray().forEach(arg -> this.flags.add(ItemFlag.valueOf(arg.getAsString())));
            if (json.has("enchants")) json.get("enchants").getAsJsonObject().entrySet().forEach(kv -> enchants.put(kv.getKey(), kv.getValue().getAsString()));
            if (json.has("settings")) json.get("settings").getAsJsonObject().entrySet().forEach(kv -> {
                ItemSetting<?> setting = ItemSetting.parse(kv.getKey(), this, kv.getValue());
                settings.put(setting.name(), setting);
            });
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
                        slots.forEach(slot -> attributes.put(attribute, generate(attribute, amount, operation, slot)));
                    });
        }

        public static ItemCreator parse(JsonObject json) {
            return new ItemCreator(null, json);
        }

        public ItemStack createItem(int count, Apply apply) {
            ItemStack item = new ItemStack(Material.valueOf(ChatHelper.formatText(this.item, apply)));
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

    public static class Builder {
        private final IItemCreator creator;
        private Apply apply = Apply.of();
        private int count = 1;
        protected Builder(IItemCreator creator) {
            this.creator = creator;
        }
        public Builder addApply(Apply apply) {
            this.apply = this.apply.copy().join(apply);
            return this;
        }
        public Builder setCount(int count) {
            this.count = count;
            return this;
        }
        protected ItemStack create() {
            return creator.createItem(count, apply);
        }
    }


    public static final LinkedHashMap<String, IItemCreator> creatorIDs = new LinkedHashMap<>();
    public static final LinkedHashMap<Integer, IItemCreator> creators = new LinkedHashMap<>();
    public static final LinkedHashMap<Integer, Material> creatorMaterials = new LinkedHashMap<>();
    public static final LinkedHashMap<Integer, String> creatorNamesIDs = new LinkedHashMap<>();
    public static final LinkedHashMap<Integer, String> creatorCoreProtectIDs = new LinkedHashMap<>();
    public static final LinkedHashMap<String, Integer> creatorIDsNames = new LinkedHashMap<>();

    public static List<String> getKeysRegex(String regex) {
        return creatorIDs.keySet().stream().filter(key -> key.equals(regex) || system.compareRegex(key, regex)).collect(Collectors.toList());
    }
    public static List<IItemCreator> getValuesRegex(String regex) {
        return creatorIDs.entrySet().stream().filter(kv -> kv.getKey().equals(regex) || system.compareRegex(kv.getKey(), regex)).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    private static final HashMap<String, PotionEffectType> potionEffectTypes = new HashMap<>();
    static {
        for (Map.Entry<ResourceKey<MobEffectList>, MobEffectList> kv : BuiltInRegistries.MOB_EFFECT.entrySet()) {
            String key = kv.getKey().location().getPath();
            PotionEffectType effect = PotionEffectType.getById(MobEffectList.getId(kv.getValue()));
            potionEffectTypes.put(key, effect);
        }
        for (PotionEffectType effect : PotionEffectType.values()) {
            potionEffectTypes.put(effect.getName(), effect);
        }
    }
    public static PotionEffect parseEffect(JsonObject json) {
        PotionEffect effect = new PotionEffect(
                potionEffectTypes.get(json.get("type").getAsString()),
                json.get("duration").getAsInt(),
                json.get("amplifier").getAsInt());

        if (json.has("ambient")) effect.withAmbient(json.get("ambient").getAsBoolean());
        if (json.has("icon")) effect.withIcon(json.get("icon").getAsBoolean());
        if (json.has("particles")) effect.withParticles(json.get("particles").getAsBoolean());
        return effect;
    }

    @EventHandler public static void onAsync(AsyncItemInfoEvent e) {
        Integer cmd = e.getCustomModelData();
        if (cmd == null) return;
        String name = creatorCoreProtectIDs.getOrDefault(cmd, null);
        if (name == null) return;
        e.setDisplayName(name);
    }

    public static List<ItemStack> byMaxStack(ItemStack item) {
        int maxStack = item.getMaxStackSize();
        if (maxStack <= 0) return Collections.singletonList(item);
        int count = item.getAmount();
        List<ItemStack> items = new ArrayList<>();
        while (count > maxStack) {
            count -= maxStack;
            items.add(item.asQuantity(maxStack));
        }
        if (count > 0) items.add(item.asQuantity(count));
        return items;
    }

    public interface Checker {
        default boolean check(ItemStack item) { return getGlobalKeyByItem(item).map(this::check).orElse(false); }
        default boolean check(net.minecraft.world.item.ItemStack item) { return getGlobalKeyByItem(item).map(this::check).orElse(false); }
        boolean check(String key);
        Stream<String> getWhitelistKeys();
        Stream<Material> getWhitelist();

        static Checker empty() {
            return new Checker() {
                @Override public boolean check(String key) { return false; }
                @Override public Stream<String> getWhitelistKeys() { return Stream.empty(); }
                @Override public Stream<Material> getWhitelist() { return Stream.empty(); }
            };
        }
    }
    public static Checker createCheck(String regex) {
        Map<Material, String> materials = Arrays
                .stream(Material.values())
                .map(v -> system.toast(v, Items.getMaterialKey(v), Items.getCategoryKey(v)))
                .filter(kv -> system.compareRegex(kv.val1, regex) || system.compareRegex(kv.val2, regex))
                .collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1));

        return new Checker() {
            private Set<String> keys = Collections.emptySet();
            private Set<Material> whitelist = Collections.emptySet();

            private int loaded_index = -1;
            private void tryReload() {
                if (loaded_index == Items.loaded_index) return;
                loaded_index = Items.loaded_index;
                this.keys = Streams.concat(materials.values().stream(), creatorIDs.keySet().stream().filter(v -> system.compareRegex(v, regex))).collect(Collectors.toSet());
                this.whitelist = Streams.concat(materials.keySet().stream(), creatorIDs.values().stream().flatMap(IItemCreator::getWhitelist)).collect(Collectors.toSet());
            }

            @Override public boolean check(String key) {
                tryReload();
                return this.keys.contains(key);
            }
            @Override public Stream<String> getWhitelistKeys() {
                tryReload();
                return this.keys.stream();
            }
            @Override public Stream<Material> getWhitelist() {
                tryReload();
                return this.whitelist.stream();
            }
        };
    }
    public static Checker createCheck(Collection<String> regexList) {
        Map<Material, String> materials = Arrays
                .stream(Material.values())
                .map(v -> system.toast(v, Items.getMaterialKey(v), Items.getCategoryKey(v)))
                .filter(kv -> regexList.stream().anyMatch(regex -> system.compareRegex(kv.val1, regex) || system.compareRegex(kv.val2, regex)))
                .collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1));

        return new Checker() {
            private Set<String> keys = Collections.emptySet();
            private Set<Material> whitelist = Collections.emptySet();

            private int loaded_index = -1;
            private void tryReload() {
                if (loaded_index == Items.loaded_index) return;
                loaded_index = Items.loaded_index;
                this.keys = Streams.concat(materials.values().stream(), creatorIDs.keySet().stream().filter(v -> regexList.stream().anyMatch(regex -> system.compareRegex(v, regex)))).collect(Collectors.toSet());
                this.whitelist = Streams.concat(materials.keySet().stream(), creatorIDs.values().stream().flatMap(IItemCreator::getWhitelist)).collect(Collectors.toSet());
            }

            @Override public boolean check(String key) {
                tryReload();
                return this.keys.contains(key);
            }
            @Override public Stream<String> getWhitelistKeys() {
                tryReload();
                return this.keys.stream();
            }
            @Override public Stream<Material> getWhitelist() {
                tryReload();
                return this.whitelist.stream();
            }
        };
    }

    public static void dropGiveItem(Player player, List<ItemStack> items, boolean log) {
        items.forEach(item -> dropGiveItem(player, item, log));
    }
    public static void dropGiveItem(Player player, ItemStack item, boolean log) {
        if (isAir(item)) return;
        EntityPlayer serverPlayer = ((CraftPlayer)player).getHandle();
        net.minecraft.world.item.ItemStack itemStack = CraftItemStack.asNMSCopy(item);
        boolean bl = serverPlayer.getInventory().add(itemStack);
        Location location = player.getLocation();
        if (log) CoreProtectHandle.logPickUp(location, player, item);
        if (bl && itemStack.isEmpty()) {
            serverPlayer.level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEffects.ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((serverPlayer.getRandom().nextFloat() - serverPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            serverPlayer.containerMenu.broadcastChanges();
        } else {
            ItemStack _item = CraftItemStack.asBukkitCopy(itemStack);
            List<ItemStack> out = WalletInventory.tryAddToWallet(player, _item);
            if (!out.isEmpty()) lime.once(() -> {
                out.forEach(__item -> CoreProtectHandle.logDrop(location, player, __item));
                dropItem(location, out);
            }, 1);
        }
    }

    @SuppressWarnings("all")
    private static Location dropBlockPosition(Location location) {
        World world = location.getWorld();
        RandomSource random = ((CraftWorld)world).getHandle().random;

        float f = EntityTypes.ITEM.getHeight() / 2.0F;
        double d0 = (double)location.getBlockX() + 0.5D + MathHelper.nextDouble(random, -0.25D, 0.25D);
        double d1 = (double)location.getBlockY() + 0.5D + MathHelper.nextDouble(random, -0.25D, 0.25D) - (double)f;
        double d2 = (double)location.getBlockZ() + 0.5D + MathHelper.nextDouble(random, -0.25D, 0.25D);

        return new Location(world, d0, d1, d2);
    }

    private static boolean isAir(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }
    public static void dropItem(Location location, List<ItemStack> items) {
        World world = location.getWorld();
        items.forEach(item -> {
            if (isAir(item)) return;
            world.dropItemNaturally(location, item);
        });
    }
    public static void dropItem(Location location, ItemStack item) {
        if (isAir(item)) return;
        location.getWorld().dropItemNaturally(location, item);
    }
    public static void dropBlockItem(Location location, List<ItemStack> items) {
        World world = location.getWorld();
        Location dropLoc = dropBlockPosition(location);
        items.forEach(item -> {
            if (isAir(item)) return;
            world.dropItem(dropLoc, item);
        });
    }
    public static void dropBlockItem(Location location, ItemStack item) {
        if (isAir(item)) return;
        location.getWorld().dropItem(dropBlockPosition(location), item);
    }

    public static Optional<Material> getMaterialKey(String key) {
        return Optional.ofNullable(key)
                .filter(Items::isMaterialKey)
                .map(v -> Material.valueOf(v.substring(10)));
    }
    public static String getMaterialKey(Material material) { return "Minecraft." + material; }
    public static boolean isMaterialKey(String key) { return key.startsWith("Minecraft."); }
    /* TODO */
    public static String getCategoryKey(Material material) {
        /*return "Category." + Optional.ofNullable(material)
            .map(CraftMagicNumbers::getItem)
            .map(Item::getItemCategory)
            .map(CraftCreativeCategory::fromNMS)
            .map(Enum::name)
            .orElse("OTHER");*/
        return "NONE";
    }

    public static Optional<String> getGlobalKeyByItem(ItemStack item) {
        if (item == null) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return Optional.of(Items.getMaterialKey(item.getType()));
        return Optional.ofNullable(creatorNamesIDs.get(meta.getCustomModelData()));
    }
    public static Optional<String> getGlobalKeyByItem(net.minecraft.world.item.ItemStack item) {
        if (item == null) return Optional.empty();
        return getIDByItem(item)
                .map(v -> Optional.ofNullable(creatorNamesIDs.get(v)))
                .orElseGet(() -> Optional.of(CraftMagicNumbers.getMaterial(item.getItem())).map(Items::getMaterialKey));
    }
    public static Optional<String> getKeyByItem(ItemStack item) {
        return getIDByItem(item).map(creatorNamesIDs::get);
    }
    public static Optional<String> getKeyByItem(net.minecraft.world.item.ItemStack item) {
        return getIDByItem(item).map(creatorNamesIDs::get);
    }
    public static Optional<Integer> getIDByItem(ItemStack item) {
        return Optional.ofNullable(item)
                .map(ItemStack::getItemMeta)
                .flatMap(Items::getIDByMeta);
    }
    public static Optional<Integer> getIDByItem(net.minecraft.world.item.ItemStack item) {
        return Optional.ofNullable(item)
                .filter(net.minecraft.world.item.ItemStack::hasTag)
                .map(net.minecraft.world.item.ItemStack::getTag)
                .filter(v -> v.contains("CustomModelData"))
                .map(v -> v.getInt("CustomModelData"));
    }
    public static Optional<Integer> getIDByMeta(ItemMeta meta) {
        return Optional.ofNullable(meta)
                .filter(ItemMeta::hasCustomModelData)
                .map(ItemMeta::getCustomModelData);
    }
    public static boolean hasItem(String key) {
        return creatorIDs.containsKey(key);
    }
    public static Optional<ItemStack> createItem(String key) {
        return createItem(key, null);
    }
    public static Optional<ItemStack> createItem(int id) {
        return createItem(id, null);
    }
    public static Optional<ItemStack> createItem(String key, system.Func1<Builder, Builder> builder) {
        return Optional.ofNullable(creatorIDs.get(key)).map(v -> v.createItem(builder));
    }
    public static Optional<ItemStack> createItem(int id, system.Func1<Builder, Builder> builder) {
        return Optional.ofNullable(creators.get(id)).map(v -> v.createItem(builder));
    }

    public static <T extends IItemSetting>Optional<T> getOptional(Class<T> tClass, ItemStack item) {
        return getItemCreator(item)
                .map(v -> v instanceof ItemCreator creator ? creator : null)
                .flatMap(v -> v.getOptional(tClass));
    }
    public static <T extends IItemSetting>Optional<T> getOptional(Class<T> tClass, net.minecraft.world.item.ItemStack item) {
        return getItemCreator(item)
                .map(v -> v instanceof ItemCreator creator ? creator : null)
                .flatMap(v -> v.getOptional(tClass));
    }
    public static <T extends IItemSetting>Optional<T> getOptional(Class<T> tClass, ItemCreator creator) {
        return Optional.ofNullable(creator)
                .flatMap(v -> v.getOptional(tClass));
    }

    public static <T extends IItemSetting>List<T> getAll(Class<T> tClass, ItemStack item) {
        return getItemCreator(item)
                .map(v -> v instanceof ItemCreator creator ? creator : null)
                .map(v -> v.getAll(tClass))
                .orElseGet(Collections::emptyList);
    }
    public static <T extends IItemSetting>List<T> getAll(Class<T> tClass, net.minecraft.world.item.ItemStack item) {
        return getItemCreator(item)
                .map(v -> v instanceof ItemCreator creator ? creator : null)
                .map(v -> v.getAll(tClass))
                .orElseGet(Collections::emptyList);
    }
    public static <T extends IItemSetting>List<T> getAll(Class<T> tClass, ItemCreator creator) {
        return Optional.ofNullable(creator)
                .map(v -> v.getAll(tClass))
                .orElseGet(Collections::emptyList);
    }

    public static boolean has(Class<? extends IItemSetting> tClass, ItemStack item) {
        return getItemCreator(item)
                .map(v -> v instanceof ItemCreator creator ? creator : null)
                .map(v -> has(tClass, v))
                .orElse(false);
    }
    public static boolean has(Class<? extends IItemSetting> tClass, net.minecraft.world.item.ItemStack item) {
        return getItemCreator(item)
                .map(v -> v instanceof ItemCreator creator ? creator : null)
                .map(v -> has(tClass, v))
                .orElse(false);
    }
    public static boolean has(Class<? extends IItemSetting> tClass, ItemCreator creator) { return creator != null && creator.has(tClass); }

    public static Optional<IItemCreator> getItemCreator(String key) {
        return Optional.ofNullable(creatorIDs.get(key))
                .or(() -> system.tryParse(Material.class, Items.isMaterialKey(key) ? key.substring(10) : key).map(ItemCreator::byMaterial));
    }
    public static Optional<IItemCreator> getItemCreator(ItemStack item) {
        return Optional.ofNullable(item)
                .map(ItemStack::getItemMeta)
                .map(v -> v.hasCustomModelData() ? v.getCustomModelData() : null)
                .map(creators::get);
    }
    public static Optional<IItemCreator> getItemCreator(net.minecraft.world.item.ItemStack item) {
        return getIDByItem(item)
                .map(creators::get);
    }

    public static Map<String, JsonElement> getData(ItemStack item) {
        return Optional.ofNullable(item)
                .map(ItemStack::getItemMeta)
                .map(PersistentDataHolder::getPersistentDataContainer)
                .flatMap(container -> getItemCreator(item)
                        .map(v -> v instanceof ItemCreator c ? c : null)
                        .map(v -> v.data)
                        .map(v -> system.map.<String, JsonElement>of().add(v.entrySet(), Map.Entry::getKey, kv -> JManager.get(JsonElement.class, container, kv.getKey(), kv.getValue())).build())
                        .map(v -> (Map<String, JsonElement>)v)
                )
                .orElseGet(Collections::emptyMap);
    }
    public static Map<String, String> getStringData(ItemStack item) {
        return getData(item).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().isJsonPrimitive() ? kv.getValue().getAsString() : kv.getValue().toString()));
    }

    public static int getMaxDamage(ItemStack item) {
        return CraftItemStack.asNMSCopy(item).getMaxDamage();
    }
    public static void hurt(net.minecraft.world.item.ItemStack item, EntityPlayer player, int amount) {
        item.hurtAndBreak(amount, player, e2 -> e2.broadcastBreakEvent(EnumItemSlot.MAINHAND));
    }
    public static void hurt(ItemStack item, Player player, int amount) {
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
        hurt(nms, ((CraftPlayer)player).getHandle(), amount);
        ExecuteItem.replace(item, CraftItemStack.asBukkitCopy(nms));
    }

    @EventHandler public static void on(ItemStackSizeEvent e) {
        //lime.logOP("Item '"+e.getType()+"' has max size is " + e.getMaxItemStack() + " of CMD '" + e.getCustomModelData() + "'");
        e.getCustomModelData()
                .map(creators::get)
                .flatMap(v -> v instanceof ItemCreator c ? c.is_stack ? c.getOptional(MaxStackSetting.class).map(_v -> _v.maxStack) : Optional.of(1) : Optional.empty())
                .ifPresent(e::setMaxItemStack);
    }
    @EventHandler public static void on(ItemMaxDamageEvent e) {
        //lime.logOP("Item '"+e.getType()+"' has max damage is " + e.getMaxDamage() + " of CMD '" + e.getCustomModelData() + "'");
        e.getCustomModelData()
                .map(creators::get)
                .flatMap(v -> v instanceof ItemCreator c ? c.getOptional(DurabilitySetting.class).map(_v -> _v.maxDurability) : Optional.empty())
                .ifPresent(e::setMaxDamage);
    }
    @EventHandler public static void on(EntityAttackSweepEvent e) {
        //lime.logOP("Entity " + e.getHuman() + " attack " + (e.getSweep() ? "with" : "without") + " sweep");
        Items.getOptional(SweepSetting.class, e.getHuman().getItemInHand(EnumHand.MAIN_HAND))
                .map(v -> v.sweep)
                .ifPresent(e::setSweep);
    }
    @EventHandler public static void on(EntityEquipmentSlotEvent e) {
        getOptional(EquipSetting.class, e.getItemStack())
                .map(v -> v.slot)
                .ifPresent(e::setSlot);
    }
    @EventHandler public static void on(PlayerArmorChangeEvent e) {
        if (!(e.getPlayer() instanceof CraftPlayer player)) return;
        lime.nextTick(player::updateInventory);
    }
}




















