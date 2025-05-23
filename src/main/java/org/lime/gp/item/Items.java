package org.lime.gp.item;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.coreprotect.event.AsyncItemInfoEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.EnumHand;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityProjectile;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.lime;
import org.lime.gp.module.ArrowBow;
import org.lime.system.Regex;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.json;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.data.Builder;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.player.inventory.WalletInventory;
import org.lime.system.utils.EnumUtils;
import org.lime.system.utils.RandomUtils;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("deprecation")
public class Items implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Items.class)
                .withInstance()
                .withInit(Items::init)
                .<JsonObject>addConfig("items", v -> v.withInvoke(Items::config).withDefault(new JsonObject()));
    }
    /*private static UUID toUUID(double value, Attribute attribute, AttributeModifier.Operation operation, EquipmentSlot slot) {
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
    }*/
    public static AttributeModifier generate(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {
        return new AttributeModifier(UUID.randomUUID() /*toUUID(amount, attribute, operation, slot)*/, attribute.getKey().getKey() /*attribute.name().toLowerCase().replace('_', '.')*/, amount, operation, slot);
    }

    public static ItemStack empty() {
        return new ItemStack(Material.STONE, 0);
    }

    public static void init() {
        Items.addHardcodeItem("tmp.backpack", json.object()
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

        AnyEvent.addEvent("give.item", AnyEvent.type.other, builder -> builder.createParam(Checker::createCheck, () -> creatorIDs.keySet().stream().filter(v -> !v.startsWith("Minecraft.")).toList()), (player, _creators) -> {
            _creators.getWhitelistKeys()
                    .map(Items.creatorIDs::get)
                    .forEach(creator -> dropGiveItem(player, creator.createItem(b -> b.addApply(UserRow.getBy(player.getUniqueId()).map(v -> Apply.of().add(v)).orElseGet(Apply::of))), false));
        });
        AnyEvent.addEvent("item.ids.all", AnyEvent.type.owner_console, (player) -> {
            List<Toast2<Integer, Integer>> idGoups = new ArrayList<>();
            Toast2<Integer, Integer> last = Toast.of(null, null);
            creatorNamesIDs.keySet().stream().sorted().forEach(id -> {
                if (last.val0 == null || last.val1 == null) {
                    last.val0 = id;
                } else if (last.val1 + 1 != id) {
                    idGoups.add(Toast.of(last.val0, last.val1));
                    last.val0 = id;
                }
                last.val1 = id;
            });
            if (last.val0 != null && last.val1 != null)
                idGoups.add(last);
            ;
            lime.logOP(Component.text("List of IDs: ").append(json.formatComponent(json.by(idGoups.stream()
                    .map(v -> Objects.equals(v.val0, v.val1) ? String.valueOf(v.val0) : (v.val0 + ".." + v.val1))
                    .toList()).build(), null)));
        });
        AnyEvent.addEvent("item.ids.duplicate", AnyEvent.type.owner_console, (player) -> {
            Map<Integer, Set<String>> ids = creatorIDs
                    .values()
                    .stream()
                    .collect(Collectors.groupingBy(IItemCreator::getID, Collectors.mapping(IItemCreator::getKey, Collectors.toSet())));
            ids.entrySet().removeIf(v -> v.getValue().size() <= 1 || v.getKey() == 0);
            lime.logOP(Component.text("Data of duplicates: ").append(json.formatComponent(json.by(ids).build())));
        });
        AnyEvent.addEvent("give.item", AnyEvent.type.other, builder -> builder.createParam(Checker::createCheck, () -> creatorIDs.keySet().stream().filter(v -> !v.startsWith("Minecraft.")).toList()).createParam(t -> json.parse(t).getAsJsonObject().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().isJsonPrimitive() ? kv.getValue().getAsString() : kv.getValue().toString())), "[args:json]"), (player, _creators, args) -> {
            _creators.getWhitelistKeys().map(Items.creatorIDs::get).forEach(creator -> dropGiveItem(player, creator.createItem(b -> b.addApply(UserRow.getBy(player.getUniqueId()).map(v -> Apply.of().add(v)).orElseGet(Apply::of).add(args))), false));
        });
        AnyEvent.addEvent("drop.item", AnyEvent.type.other, builder -> builder
            .createParam(Double::parseDouble, "[x]")
            .createParam(Double::parseDouble, "[y]")
            .createParam(Double::parseDouble, "[z]")
            .createParam(Checker::createCheck, () -> creatorIDs.keySet().stream().filter(v -> !v.startsWith("Minecraft.")).toList()),
             (p, x, y, z, _creators) -> {
                _creators.getWhitelistKeys()
                    .map(Items.creatorIDs::get)
                    .forEach(creator -> Items.dropItem(new Location(p.getWorld(), x, y, z), creator.createItem()));
            });
        AnyEvent.addEvent("drop.item", AnyEvent.type.other, builder -> builder
            .createParam(Double::parseDouble, "[x]")
            .createParam(Double::parseDouble, "[y]")
            .createParam(Double::parseDouble, "[z]")
            .createParam(Checker::createCheck, () -> creatorIDs.keySet().stream().filter(v -> !v.startsWith("Minecraft.")).toList())
            .createParam(t -> json.parse(t).getAsJsonObject().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().isJsonPrimitive() ? kv.getValue().getAsString() : kv.getValue().toString())), "[args:json]"), 
            (p, x, y, z, _creators, args) -> {
                _creators.getWhitelistKeys()
                    .map(Items.creatorIDs::get)
                    .forEach(creator -> Items.dropItem(new Location(p.getWorld(), x, y, z), creator.createItem(v -> v.addApply(Apply.of().add(args)))));
            });
    }

    public static final String AIR = Items.getMaterialKey(Material.AIR);

    public static final HashMap<String, JsonObject> hardcode_items = new HashMap<>();
    public static void addHardcodeItem(String key, JsonObject json) {
        hardcode_items.put(key, json);
    }
    static int loaded_index = 0;
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
        overrides.clear();

        JsonObject _new = lime.combineParent(json, true, false);
        hardcode_items.forEach(_new::add);
        _new.entrySet().forEach(kv -> {
            String key = kv.getKey();
            JsonObject value = kv.getValue().getAsJsonObject();
            ItemCreator creator = new ItemCreator(key, value);
            validateItemKey(key.toLowerCase());
            overrides.put(key, value);
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

    public static final LinkedHashMap<String, IItemCreator> creatorIDs = new LinkedHashMap<>();
    public static final LinkedHashMap<Integer, IItemCreator> creators = new LinkedHashMap<>();
    public static final LinkedHashMap<String, JsonObject> overrides = new LinkedHashMap<>();
    public static final LinkedHashMap<Integer, Material> creatorMaterials = new LinkedHashMap<>();
    public static final LinkedHashMap<Integer, String> creatorNamesIDs = new LinkedHashMap<>();
    public static final LinkedHashMap<Integer, String> creatorCoreProtectIDs = new LinkedHashMap<>();
    public static final LinkedHashMap<String, Integer> creatorIDsNames = new LinkedHashMap<>();

    public static List<String> getKeysRegex(String regex) {
        return creatorIDs.keySet().stream().filter(key -> key.equals(regex) || Regex.compareRegex(key, regex)).collect(Collectors.toList());
    }
    public static List<IItemCreator> getValuesRegex(String regex) {
        return creatorIDs.entrySet().stream().filter(kv -> kv.getKey().equals(regex) || Regex.compareRegex(kv.getKey(), regex)).map(Map.Entry::getValue).collect(Collectors.toList());
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

        if (json.has("ambient")) effect = effect.withAmbient(json.get("ambient").getAsBoolean());
        if (json.has("icon")) effect = effect.withIcon(json.get("icon").getAsBoolean());
        if (json.has("particles")) effect = effect.withParticles(json.get("particles").getAsBoolean());
        return effect;
    }

    @EventHandler public static void onAsync(AsyncItemInfoEvent e) {
        Integer cmd = e.getCustomModelData();
        if (cmd == null) return;
        String name = creatorCoreProtectIDs.getOrDefault(cmd, null);
        if (name == null) return;
        e.setDisplayName(name);
    }

    /*public static List<ItemStack> byMaxStack(ItemStack item) {
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
    }*/
    public static Stream<ItemStack> splitOptimize(ItemStack item) {
        int maxCount = item.getMaxStackSize();
        int currentCount = item.getAmount();
        if (maxCount <= 0 || currentCount <= maxCount) return Stream.of(item);
        return Streams.stream(new Iterator<>() {
            int count = currentCount;
            @Override public boolean hasNext() {
                return count > 0;
            }
            @Override public ItemStack next() {
                ItemStack out;
                if (count > maxCount) {
                    out = item.asQuantity(maxCount);
                    count -= maxCount;
                } else {
                    out = item.asQuantity(count);
                    count = 0;
                }
                return out;
            }
        });
    }
    public static Stream<net.minecraft.world.item.ItemStack> splitOptimize(net.minecraft.world.item.ItemStack item) {
        int maxCount = item.getMaxStackSize();
        int currentCount = item.getCount();
        if (maxCount <= 0 || currentCount <= maxCount) return Stream.of(item);
        return Streams.stream(new Iterator<>() {
            int count = currentCount;
            @Override public boolean hasNext() {
                return count > 0;
            }
            @Override public net.minecraft.world.item.ItemStack next() {
                net.minecraft.world.item.ItemStack out;
                if (count > maxCount) {
                    out = item.copyWithCount(maxCount);
                    count -= maxCount;
                } else {
                    out = item.copyWithCount(count);
                    count = 0;
                }
                return out;
            }
        });
    }

    public static void dropGiveItem(Player player, List<ItemStack> items, boolean log) {
        items.forEach(item -> dropGiveItem(player, item, log));
    }
    public static void dropGiveItem(Player player, ItemStack item, boolean log) {
        if (isAir(item)) return;
        splitOptimize(item).forEach(singleItem -> {
            EntityPlayer serverPlayer = ((CraftPlayer)player).getHandle();
            net.minecraft.world.item.ItemStack itemStack = CraftItemStack.asNMSCopy(singleItem);
            boolean bl = serverPlayer.getInventory().add(itemStack);
            Location location = player.getLocation();
            if (log) CoreProtectHandle.logPickUp(location, player, singleItem);
            if (bl && itemStack.isEmpty()) {
                serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEffects.ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((serverPlayer.getRandom().nextFloat() - serverPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                serverPlayer.containerMenu.broadcastChanges();
            } else {
                ItemStack _item = CraftItemStack.asBukkitCopy(itemStack);
                List<ItemStack> out = WalletInventory.tryAddToWallet(player, _item);
                if (!out.isEmpty()) lime.once(() -> {
                    out.forEach(__item -> CoreProtectHandle.logDrop(location, player, __item));
                    dropItem(location, out);
                }, 1);
            }
        });
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
        World world = location.getWorld();
        splitOptimize(item).forEach(splitItem -> world.dropItemNaturally(location, splitItem));
    }
    public static void dropBlockItem(Location location, List<ItemStack> items) {
        World world = location.getWorld();
        Location dropLoc = dropBlockPosition(location);
        items.stream().flatMap(Items::splitOptimize).forEach(item -> {
            if (isAir(item)) return;
            world.dropItem(dropLoc, item);
        });
    }
    public static void dropBlockItem(Location location, ItemStack item) {
        if (isAir(item)) return;
        World world = location.getWorld();
        Location dropLoc = dropBlockPosition(location);
        splitOptimize(item).forEach(splitItem -> world.dropItem(dropLoc, splitItem));
    }

    public static Optional<Material> getMaterialKey(String key) {
        return Optional.ofNullable(key)
                .filter(Items::isMaterialKey)
                .map(v -> Material.valueOf(v.substring(10)));
    }
    public static String getMaterialKey(Material material) { return "Minecraft." + material; }
    public static boolean isMaterialKey(String key) { return key.startsWith("Minecraft."); }
    /*public static String getCategoryKey(Material material) {
        return "Category." + Optional.ofNullable(material)
            .map(CraftMagicNumbers::getItem)
            .map(Item::getItemCategory)
            .map(CraftCreativeCategory::fromNMS)
            .map(Enum::name)
            .orElse("OTHER");
    }*/

    public static Optional<String> getGlobalKeyByItem(ItemStack item) {
        if (item == null) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return Optional.of(Items.getMaterialKey(item.getType()));
        return Optional.ofNullable(creatorNamesIDs.get(meta.getCustomModelData()));
    }
    public static Optional<String> getGlobalKeyByItem(net.minecraft.world.item.ItemStack item) {
        if (item == null) return Optional.empty();
        return getIDByItem(item)
                .map(creatorNamesIDs::get)
                .or(() -> Optional.of(CraftMagicNumbers.getMaterial(item.getItem())).map(Items::getMaterialKey));
    }
    public static Optional<String> getKeyBy(Material material, @Nullable Integer customModelData) {
        return customModelData == null
                ? Optional.of(getMaterialKey(material))
                : Optional.ofNullable(creatorNamesIDs.get(customModelData));
    }
    public static Optional<String> getKeyByCustomModelData(int id) {
        return Optional.ofNullable(creatorNamesIDs.get(id));
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
    public static boolean hasIDByItem(ItemStack item) {
        return Optional.ofNullable(item)
                .map(ItemStack::getItemMeta)
                .map(Items::hasIDByMeta)
                .orElse(false);
    }
    public static boolean hasIDByItem(net.minecraft.world.item.ItemStack item) {
        return Optional.ofNullable(item)
                .filter(net.minecraft.world.item.ItemStack::hasTag)
                .map(net.minecraft.world.item.ItemStack::getTag)
                .map(v -> v.contains("CustomModelData"))
                .orElse(false);
    }
    public static boolean hasIDByMeta(ItemMeta meta) {
        return Optional.ofNullable(meta)
                .map(ItemMeta::hasCustomModelData)
                .orElse(false);
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
    public static Optional<ItemStack> createItem(String key, Func1<Builder, Builder> builder) {
        return Optional.ofNullable(creatorIDs.get(key)).map(v -> v.createItem(builder));
    }
    public static Optional<ItemStack> createItem(int id, Func1<Builder, Builder> builder) {
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
                .or(() -> EnumUtils.tryParse(Material.class, Items.isMaterialKey(key) ? key.substring(10) : key).map(ItemCreator::byMaterial));
    }
    public static Optional<IItemCreator> getItemCreator(ItemStack item) {
        return Optional.ofNullable(item)
                .map(ItemStack::getItemMeta)
                .map(v -> v.hasCustomModelData() ? v.getCustomModelData() : null)
                .map(creators::get);
    }
    public static Optional<IItemCreator> getItemCreator(ItemMeta meta) {
        return Optional.ofNullable(meta)
                .map(v -> v.hasCustomModelData() ? v.getCustomModelData() : null)
                .map(creators::get);
    }
    public static Optional<IItemCreator> getItemCreator(net.minecraft.world.item.ItemStack item) {
        return getIDByItem(item)
                .map(creators::get);
    }
    public static Optional<IItemCreator> getItemCreator(Material material, @Nullable Integer customModelData) {
        return customModelData == null
                ? Optional.of(ItemCreator.byMaterial(material))
                : Optional.ofNullable(creators.get(customModelData));
    }

    public static Map<String, JsonElement> getData(ItemStack item) {
        return Optional.ofNullable(item)
                .map(ItemStack::getItemMeta)
                .map(PersistentDataHolder::getPersistentDataContainer)
                .flatMap(container -> getItemCreator(item)
                        .map(v -> v instanceof ItemCreator c ? c : null)
                        .map(v -> v.data)
                        .map(v -> map.<String, JsonElement>of().add(v.entrySet(), Map.Entry::getKey, kv -> JManager.get(JsonElement.class, container, kv.getKey(), kv.getValue())).build())
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
    public static int getMaxDamage(Material material, ItemMeta item) {
        return Items.getItemCreator(item)
                .flatMap(v -> v instanceof ItemCreator c ? c.getOptional(DurabilitySetting.class).map(_v -> (short)_v.maxDurability) : Optional.empty())
                .orElseGet(material::getMaxDurability);
    }

    public static boolean hurtRemove(net.minecraft.world.item.ItemStack item, int amount) {
        return item.isDamageableItem() && item.hurt(amount, lime.MainWorld.getHandle().getRandom(), null);
    }
    public static void hurt(net.minecraft.world.item.ItemStack item, EntityPlayer player, int amount, EnumItemSlot slot) {
        item.hurtAndBreak(amount, player, e2 -> e2.broadcastBreakEvent(slot));
    }
    public static void hurt(ItemStack item, Player player, int amount, @Nullable EquipmentSlot slot) {
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
        hurt(nms, ((CraftPlayer)player).getHandle(), amount, slot == null ? EnumItemSlot.MAINHAND : switch (slot) {
            case OFF_HAND -> EnumItemSlot.OFFHAND;
            case FEET -> EnumItemSlot.FEET;
            case LEGS -> EnumItemSlot.LEGS;
            case CHEST -> EnumItemSlot.CHEST;
            case HEAD -> EnumItemSlot.HEAD;
            default -> EnumItemSlot.MAINHAND;
        });
        ExecuteItem.replace(item, CraftItemStack.asBukkitCopy(nms));
    }

    public static void hurt(net.minecraft.world.item.ItemStack item, EntityPlayer player, int amount) {
        hurt(item, player, amount, EnumItemSlot.MAINHAND);
    }
    public static void hurt(ItemStack item, Player player, int amount) {
        hurt(item, player, amount, EquipmentSlot.HAND);
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
        lime.once(player::updateInventory, 0.5);
    }
    /*
    @EventHandler public static void on(PlayerAttackStrengthResetEvent e) {
        if (!(e.getHuman() instanceof EntityPlayer player)) return;
        net.minecraft.world.item.ItemStack itemstack = player.getMainHandItem();
        net.minecraft.world.item.ItemStack lastItemInMainHand = ReflectionAccess.lastItemInMainHand_EntityHuman.get(player);

        if (net.minecraft.world.item.ItemStack.matches(lastItemInMainHand, itemstack)) return;
        if (!net.minecraft.world.item.ItemStack.isSame(lastItemInMainHand, itemstack) || itemstack.isEmpty()) return;
        if (!Items.has(AttackFixSetting.class, lastItemInMainHand) && !Items.has(AttackFixSetting.class, itemstack)) return;
        PlayerInventory inventory = player.getInventory();
        PlayerConnection connection = player.connection;

        int slot = inventory.items.size() + inventory.selected;
        int stateId = player.containerMenu.getStateId();
        connection.send(new PacketPlayOutSetSlot(0, stateId, slot, net.minecraft.world.item.ItemStack.EMPTY));
        lime.nextTick(player.inventoryMenu::broadcastFullState);
    }
    */

    @EventHandler private static void on(DamageSourceBlockEvent e) {
        if (!e.isBlocking()) return;
        net.minecraft.world.item.ItemStack shield = e.getShield();
        Entity damageEntity = e.getSource().getEntity();
        if (damageEntity instanceof EntityHuman human && !(e.getSource().getDirectEntity() instanceof EntityArrow) && human.getAttackStrengthScale(0.5f) < 1)
            return;
        net.minecraft.world.item.ItemStack attack;
        if (damageEntity instanceof EntityThrownTrident trident) attack = trident.getPickupItem();
        else if (damageEntity instanceof IProjectile projectile) attack = ArrowBow.getBowItem(projectile);
        else if (damageEntity instanceof EntityLiving living) attack = living.getMainHandItem();
        else attack = net.minecraft.world.item.ItemStack.EMPTY;
        if (attack.getUseAnimation() == EnumAnimation.BLOCK) attack = net.minecraft.world.item.ItemStack.EMPTY;
        double chance = Items.getOptional(ShieldIgnoreSetting.class, shield)
                .map(v -> v.chance)
                .orElse(1.0)
                * Items.getOptional(ShieldIgnoreSetting.class, attack)
                .map(v -> v.chance)
                .orElse(0.0);
        if (RandomUtils.rand_is(chance))
            e.setBlocking(false);
    }
    @EventHandler private static void on(EntityShootBowEvent e) {
        Items.getOptional(ArrowSetting.class, e.getBow())
                .ifPresent(v -> {
                    if (!(e.getProjectile() instanceof CraftEntity ce) || !(ce.getHandle() instanceof EntityArrow arrow)) return;
                    arrow.setBaseDamage(arrow.getBaseDamage() * v.damageMultiply);
                });
    }

    @EventHandler private static void on(PlayerAttackMultiplyCriticalEvent e) {
        e.setMultiply(1);
    }
    @EventHandler private static void on(PlayerArrowCriticalEvent e) {
        e.setCriticalAppend(0);
        if (e.isCritical()) return;
        e.setDamage(Math.round((e.getDamage() * 2f - 4f) / 3f));
    }
    @EventHandler private static void on(PlayerJumpEvent e) {
        Player player = e.getPlayer();
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getArmorContents()) {
            Items.getOptional(ArmorFoodSetting.class, item).ifPresent(armor -> armor.change(player));
        }
        for (ItemStack item : List.of(inventory.getItem(EquipmentSlot.HAND), inventory.getItem(EquipmentSlot.OFF_HAND))) {
            Items.getOptional(ArmorFoodSetting.class, item).filter(v -> v.inHand).ifPresent(armor -> armor.change(player));
        }
    }
}




















