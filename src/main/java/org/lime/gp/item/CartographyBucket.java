package org.lime.gp.item;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.core;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.JManager;
import org.lime.system;

import java.util.HashMap;
import java.util.Optional;

public class CartographyBucket extends system.IJson.ILoad<JsonObject> implements system.IJson<JsonObject> {
    public static core.element create() {
        return core.element.create(CartographyBucket.class)
                .withInstance(new Listener() {
                    private system.Toast3<Integer, Integer, Integer> ofColor(int x, int y, int z) {
                        x = x == 0 ? 1 : x;
                        y = y == 0 ? 1 : y;
                        z = z == 0 ? 1 : z;
                        double length = x + y + z;
                        double total = Math.pow(2, 14);
                        return system.toast((int)((x / length) * total), (int)((y / length) * total), (int)((z / length) * total));
                    }
                    private final HashMap<Material, system.Toast3<Integer, Integer, Integer>> itemColors = system.map.<Material, system.Toast3<Integer, Integer, Integer>>of()
                            .add(Material.WHITE_DYE, ofColor(255, 255, 255))
                            .add(Material.ORANGE_DYE, ofColor(255, 165, 0))
                            .add(Material.MAGENTA_DYE, ofColor(255, 0, 255))
                            .add(Material.LIGHT_BLUE_DYE, ofColor(173, 216, 230))
                            .add(Material.YELLOW_DYE, ofColor(255, 255, 0))
                            .add(Material.LIME_DYE, ofColor(0, 255, 0))
                            .add(Material.PINK_DYE, ofColor(255, 192, 203))
                            .add(Material.GRAY_DYE, ofColor(128, 128, 128))
                            .add(Material.LIGHT_GRAY_DYE, ofColor(211, 211, 211))
                            .add(Material.CYAN_DYE, ofColor(0, 255, 255))
                            .add(Material.PURPLE_DYE, ofColor(128, 0, 128))
                            .add(Material.BLUE_DYE, ofColor(0, 0, 255))
                            .add(Material.BROWN_DYE, ofColor(165, 42, 42))
                            .add(Material.GREEN_DYE, ofColor(0, 128, 0))
                            .add(Material.RED_DYE, ofColor(255, 0, 0))
                            .add(Material.BLACK_DYE, ofColor(0, 0, 0))
                            .build();
                    @EventHandler public void on(InventoryClickEvent e) {
                        switch (e.getClick()) {
                            case RIGHT:
                            case LEFT:
                                ItemStack cursor = e.getCursor();
                                ItemStack item = e.getCurrentItem();
                                if (cursor == null || item == null || cursor.getType().isAir() || item.getType().isAir()) return;
                                system.Toast3<Integer, Integer, Integer> color = itemColors.getOrDefault(cursor.getType(), null);
                                if (color == null) return;
                                int count = cursor.getAmount();
                                CartographyBucket.modifyData(item, data -> {
                                    data.r += color.val0 * count;
                                    data.g += color.val1 * count;
                                    data.b += color.val2 * count;
                                    cursor.setAmount(0);
                                    e.setCancelled(true);
                                });
                                break;
                            default:
                                break;
                        }
                    }
                });
    }
    public int r;
    public int g;
    public int b;

    public HashMap<String, String> map() {
        return system.map.<String, String>of()
                .add("r", String.valueOf(r))
                .add("g", String.valueOf(g))
                .add("b", String.valueOf(b))
                .build();
    }
    public Apply args() {
        return Apply.of().add(map());
    }
    protected CartographyBucket(JsonObject json) {
        super(json);

        r = json.has("r") ? json.get("r").getAsInt() : 0;
        g = json.has("g") ? json.get("g").getAsInt() : 0;
        b = json.has("b") ? json.get("b").getAsInt() : 0;
    }
    @Override public JsonObject toJson() { return system.json.object().add(map()).build(); }

    public static Optional<CartographyBucket> getData(ItemStack item) {
        if (!Items.has(Settings.BucketSetting.class, item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        JsonObject json = JManager.get(JsonObject.class, meta.getPersistentDataContainer(), "bucket.data", null);
        return Optional.of(new CartographyBucket(json == null ? new JsonObject() : json));
    }
    public static void modifyData(ItemStack item, system.Action1<CartographyBucket> modify) {
        CartographyBucket.modifyData(item, (v) -> { modify.invoke(v); return null; });
    }
    public static <T> Optional<T> modifyData(ItemStack item, system.Func1<CartographyBucket, T> modify) {
        return Items.getItemCreator(item).map(v -> v instanceof Items.ItemCreator creator ? creator : null).map(creator -> {
            if (!creator.has(Settings.BucketSetting.class)) return null;
            ItemMeta meta = item.getItemMeta();
            JsonObject json = JManager.get(JsonObject.class, meta.getPersistentDataContainer(), "bucket.data", null);
            CartographyBucket data = new CartographyBucket(json == null ? new JsonObject() : json);
            T val = modify.invoke(data);
            creator.apply(item, item.getAmount(), data.args());
            meta = item.getItemMeta();
            JManager.set(meta.getPersistentDataContainer(), "bucket.data", data.toJson());
            item.setItemMeta(meta);
            return val;
        });
    }
}