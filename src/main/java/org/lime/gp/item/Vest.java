package org.lime.gp.item;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Slot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventoryCustom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.module.PlayerData;
import org.lime.json.JsonArrayOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.execute.*;
import org.lime.system.utils.ItemUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Vest implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Vest.class)
                .withInit(Vest::init)
                .withInstance()
                .withUninit(Vest::uninit);
    }
    private record VestInventory(String item, VestSetting vest_data, CraftInventory inventory) {
        public void close() { inventory.close(); }
    }

    private static final HashMap<UUID, HashMap<EquipmentSlot, VestInventory>> inventoryVest = new HashMap<>();
    private static int dirtyCount = 0;
    private static int callIndex = 0;
    public static void init() {
        lime.repeatTicks(Vest::tick, 1);
    }
    private static List<ItemStack> dieTick(UUID die_uuid) {
        List<ItemStack> die_items = new ArrayList<>();
        inventoryVest.entrySet().removeIf(kv -> {
            PlayerData.JsonPersistentDataContainer container = PlayerData.getPlayerData(kv.getKey());
            kv.getValue().entrySet().removeIf(_kv -> {
                if (!_kv.getValue().inventory.getViewers().isEmpty()) return false;
                VestData.read(container, _kv.getKey()).setItems(_kv.getValue().inventory).save(container, _kv.getKey());
                return true;
            });
            return kv.getValue().isEmpty();
        });

        Optional.ofNullable(die_uuid)
                .ifPresent(uuid -> {
                    PlayerData.JsonPersistentDataContainer container = PlayerData.getPlayerData(uuid);
                    VestData.VEST_KEY_BIMAP.forEach((slot, slot_key) -> {
                        VestData vestData = VestData.read(container, slot);
                        Optional.ofNullable(inventoryVest.get(uuid)).map(v -> v.get(slot)).map(v -> {
                            vestData.setItems(v.inventory);
                            return v;
                        }).ifPresent(VestInventory::close);
                        die_items.addAll(vestData.getItems());
                        VestData.remove(container, slot);
                    });
                });
        return die_items;
    }
    private static void tick() {
        callIndex++;
        if (callIndex > 10 || dirtyCount > 0) {
            dirtyCount--;
            callIndex = 0;
        } else return;
        dieTick(null);
        Bukkit.getOnlinePlayers().forEach(player -> {
            UUID uuid = player.getUniqueId();
            PlayerData.JsonPersistentDataContainer container = PlayerData.getPlayerData(uuid);
            PlayerInventory inventory = player.getInventory();
            VestData.VEST_KEY_BIMAP.forEach((slot, slot_key) -> {
                VestData vestData = VestData.read(container, slot);
                String vest_slot_data = vestData.getSlotItem();
                String real_data = Items.getOptional(VestSetting.class, inventory.getItem(slot)).map(ItemSetting::creator).map(ItemCreator::getKey).orElse(null);
                if (Objects.equals(vest_slot_data, real_data)) return;
                Optional.ofNullable(inventoryVest.get(uuid)).map(v -> v.get(slot)).map(v -> {
                    vestData.setItems(v.inventory);
                    return v;
                }).ifPresent(VestInventory::close);
                if (vestData.hasSlotItem()) vestData.getItems().forEach(items -> Items.dropGiveItem(player, items, false));
                if (vest_slot_data == null) {
                    vestData.setItems(Collections.emptyList());
                    vestData.setSlotItem(real_data);
                    vestData.save(container, slot);
                    return;
                }
                if (real_data == null) {
                    VestData.remove(container, slot);
                } else {
                    vestData.setItems(Collections.emptyList());
                    vestData.setSlotItem(real_data);
                    vestData.save(container, slot);
                }
            });
        });
    }
    public static void uninit() {
        inventoryVest.values().forEach(map -> map.values().forEach(VestInventory::close));
        tick();
    }

    public static class VestData {
        private final JsonObjectOptional json;
        public VestData(JsonObjectOptional json) { this.json = json.deepCopy(); }
        public VestData() { this.json = new JsonObjectOptional(); }

        public String getSlotItem() { return json.getAsString("slot_item").orElse(null); }
        public boolean hasSlotItem() { return json.has("slot_item"); }
        public void setSlotItem(String slot) { json.addProperty("slot_item", slot); }

        public List<ItemStack> getItems() { return json.getAsJsonArray("items").map(VestData::loadItems).orElse(Collections.emptyList()); }
        private Optional<VestInventory> createInventory(Action1<List<ItemStack>> dropItems) {
            return Optional.ofNullable(getSlotItem())
                    .flatMap(slot_item -> Items.getItemCreator(slot_item)
                            .map(v -> v instanceof ItemCreator c ? c : null)
                            .flatMap(v -> v.getOptional(VestSetting.class))
                            .map(vest_data -> {
                                List<ItemStack> items = getItems();
                                CraftInventoryCustom inventory = new CraftInventoryCustom(null, vest_data.rows * 9, vest_data.title);
                                List<ItemStack> drops = new ArrayList<>();
                                for (int i = items.size() - inventory.getSize(); i > 0; i--) drops.add(items.remove(items.size() - 1));
                                Collection<Integer> slots = vest_data.slots.keySet();
                                IntStream.range(0, Math.min(items.size(), inventory.getSize()))
                                        .filter(v -> !slots.contains(v))
                                        .mapToObj(v -> items.set(v, new ItemStack(Material.AIR)))
                                        .filter(v -> !v.getType().isAir())
                                        .forEach(drops::add);

                                dropItems.invoke(drops);
                                inventory.setContents(items.toArray(ItemStack[]::new));
                                return new VestInventory(slot_item, vest_data, inventory);
                            })
                    );
        }
        public Optional<VestInventory> createInventory(Player owner) {
            return createInventory(drops -> Items.dropGiveItem(owner, drops, false));
        }

        public VestData setItems(List<ItemStack> items) {
            json.add("items", JsonArrayOptional.of(saveItems(items)));
            return this;
        }
        public VestData setItems(Inventory inventory) {
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack item : inventory) items.add(item == null ? new ItemStack(Material.AIR) : item);
            return setItems(items);
        }

        public static VestData read(PlayerData.JsonPersistentDataContainer container, EquipmentSlot slot) {
            return new VestData(JsonObjectOptional.of(Optional.ofNullable(container.getJson(VEST_KEY_BIMAP.get(slot))).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new)));
        }
        public static boolean has(PlayerData.JsonPersistentDataContainer container, EquipmentSlot slot) {
            return container.has(VEST_KEY_BIMAP.get(slot));
        }
        public static void remove(PlayerData.JsonPersistentDataContainer container, EquipmentSlot slot) {
            container.remove(VEST_KEY_BIMAP.get(slot));
        }
        public void save(PlayerData.JsonPersistentDataContainer container, EquipmentSlot slot) {
            container.setJson(VEST_KEY_BIMAP.get(slot), json.base());
        }

        public static final BiMap<EquipmentSlot, NamespacedKey> VEST_KEY_BIMAP = HashBiMap.create(Stream.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
                .collect(Collectors.toMap(slot -> slot, slot -> new NamespacedKey(lime._plugin, "vest_" + slot.name().toLowerCase())))
        );

        private static JsonArray saveItems(List<ItemStack> items) {
            JsonArray arr = new JsonArray();
            for (ItemStack item : items)
                arr.add(item != null && item.getType() != Material.AIR ? ItemUtils.saveItem(item) : null);
            return arr;
        }
        private static List<ItemStack> loadItems(JsonArrayOptional json) {
            List<ItemStack> items = new ArrayList<>();
            json.forEach(item -> items.add(item.getAsString().map(ItemUtils::loadItem).orElseGet(() -> new ItemStack(Material.AIR))));
            return items;
        }
    }

    private static boolean openFilterInventory(Player player, VestInventory vest, boolean readonly, Func1<net.minecraft.world.item.ItemStack, net.minecraft.world.item.ItemStack> filter, Func1<net.minecraft.world.item.ItemStack, Boolean> click) {
        //tick();
        dirtyCount = 4;
        player.closeInventory();
        Map<Integer, Checker> slots = vest.vest_data.slots;
        return InterfaceManager.of(player, vest.inventory)
                .slots(slot -> {
                    Slot _slot = slot;
                    Checker checker = slots.get(slot.index);
                    if (checker == null) return InterfaceManager.AbstractSlot.noneSlot(slot);
                    if (!readonly) return InterfaceManager.filterSlot(slot, checker::check);
                    Slot out = new Slot(slot.container, slot.slot, slot.x, slot.y) {
                        @Override public net.minecraft.world.item.ItemStack getItem() { return filter.invoke(super.getItem()); }
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                        @Override public boolean mayPickup(EntityHuman playerEntity) {
                            if (click.invoke(_slot.getItem())) _slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                            return false;
                        }
                    };
                    out.index = slot.index;
                    return out;
                })
                .open();
    }
    public static boolean openVest(Player player, EquipmentSlot slot) {
        UUID uuid = player.getUniqueId();
        VestInventory vest = inventoryVest
                .computeIfAbsent(uuid, (_uuid) -> new HashMap<>())
                .computeIfAbsent(slot, (_slot) -> VestData.read(PlayerData.getPlayerData(uuid), slot).createInventory(player).orElse(null));
        return vest != null && openFilterInventory(player, vest, false, item -> item, item -> false);
    }
    public static boolean openVestTarget(Player viewer, Player target, EquipmentSlot slot, boolean readonly, Func1<net.minecraft.world.item.ItemStack, net.minecraft.world.item.ItemStack> filter, Func1<net.minecraft.world.item.ItemStack, Boolean> click) {
        UUID uuid = target.getUniqueId();
        VestInventory vest = inventoryVest
                .computeIfAbsent(uuid, (_uuid) -> new HashMap<>())
                .computeIfAbsent(slot, (_slot) -> VestData.read(PlayerData.getPlayerData(uuid), slot).createInventory(target).orElse(null));
        return vest != null && openFilterInventory(viewer, vest, readonly, filter, click);
    }
    public static boolean hasVest(Player viewer, UUID target_uuid, EquipmentSlot slot) {
        HashMap<EquipmentSlot, VestInventory> map = inventoryVest.computeIfAbsent(target_uuid, (_uuid) -> new HashMap<>());
        return map.get(slot) != null || VestData.has(PlayerData.getPlayerData(target_uuid), slot);
    }
    public static boolean openVestAdmin(Player viewer, UUID target_uuid, EquipmentSlot slot) {
        VestInventory vest = inventoryVest
                .computeIfAbsent(target_uuid, (_uuid) -> new HashMap<>())
                .computeIfAbsent(slot, (_slot) -> VestData.read(PlayerData.getPlayerData(target_uuid), slot).createInventory(viewer).orElse(null));
        return vest != null && openFilterInventory(viewer, vest, false, item -> item, item -> false);
    }

    private static Optional<EquipmentSlot> getEquipmentSlot(PlayerInventory inventory, int slot) {
        if (inventory.getHeldItemSlot() == slot) return Optional.of(EquipmentSlot.HAND);
        if (net.minecraft.world.entity.player.PlayerInventory.SLOT_OFFHAND == slot) return Optional.of(EquipmentSlot.OFF_HAND);
        int size = inventory.getSize();
        if (size - 2 == slot) return Optional.of(EquipmentSlot.HEAD);
        if (size - 3 == slot) return Optional.of(EquipmentSlot.CHEST);
        if (size - 4 == slot) return Optional.of(EquipmentSlot.LEGS);
        if (size - 5 == slot) return Optional.of(EquipmentSlot.FEET);
        return Optional.empty();
    }

    @EventHandler(ignoreCancelled = true) public static void on(InventoryClickEvent e) {
        if (e.getClick() != ClickType.DROP) return;
        if (!(e.getView().getBottomInventory() instanceof PlayerInventory playerInventory)) return;
        if (playerInventory != e.getView().getInventory(e.getRawSlot())) return;
        getEquipmentSlot(playerInventory, e.getSlot()).ifPresent(slot -> {
            if (e.getView().getPlayer() instanceof Player player && openVest(player, slot))
                e.setCancelled(true);
        });
    }

    public static boolean extractItems(Player player, Action1<ItemStack> callback) {
        //tick();
        dirtyCount = 4;
        List<ItemStack> items = dieTick(player.getUniqueId());
        items.removeIf(v -> v.getType().isAir());
        if (items.isEmpty()) return false;
        items.forEach(callback);
        return true;
    }
    private static boolean tryRemoveSingle(CraftInventory inventory, Func1<ItemStack, Boolean> filter) {
        ItemStack[] items = inventory.getStorageContents();
        for (int slot = 0; slot < items.length; ++slot) {
            ItemStack item = items[slot];
            if (item == null || !filter.invoke(item)) continue;
            item.subtract();
            inventory.setItem(slot, item);
            return true;
        }
        return false;
    }
    public static boolean tryRemoveSingleItem(Player player, Func1<ItemStack, Boolean> filter) {
        UUID uuid = player.getUniqueId();
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            VestInventory vest = inventoryVest
                    .computeIfAbsent(uuid, (_uuid) -> new HashMap<>())
                    .computeIfAbsent(slot, (_slot) -> VestData.read(PlayerData.getPlayerData(uuid), slot).createInventory(player).orElse(null));
            if (vest == null) continue;
            if (tryRemoveSingle(vest.inventory, filter)) return true;
        }
        return false;
    }
}














