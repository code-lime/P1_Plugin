package org.lime.gp.item;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.lime;
import org.lime.gp.module.DrawMap;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.system;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CartographyBrush extends system.IJson.ILoad<JsonObject> implements system.IJson<JsonObject> {
    public static core.element create() {
        return core.element.create(CartographyBrush.class)
                .withInstance(new Listener() {
                    @EventHandler public void on(PlayerInteractEvent e) {
                        if (e.getHand() != EquipmentSlot.HAND) return;
                        switch (e.getAction()) {
                            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                                if (e.getPlayer().isSneaking() && (openBrushMenu(e.getPlayer()))) {
                                    e.setCancelled(true);
                                    return;
                                }
                            }
                            default -> {}
                        }
                    }
                })
                .withInit(CartographyBrush::init);
    }
    public static void init() {
        AnyEvent.addEvent("brush.set", AnyEvent.type.other, builder -> builder.createParam("color_id", "size").createParam("[value]"), (p, state, value) -> {
            switch (state) {
                case "color_id":
                    CartographyBrush.modifyData(p.getInventory().getItemInMainHand(), v -> {
                        try { v.color = (byte)Integer.parseUnsignedInt(value); } catch (Exception ignored) { }
                    });
                    return;
                case "size":
                    CartographyBrush.modifyData(p.getInventory().getItemInMainHand(), v -> {
                        try { v.size = (byte)Integer.parseUnsignedInt(value); } catch (Exception ignored) { }
                    });
                    return;
            }
        });
        AnyEvent.addEvent("brush.get", AnyEvent.type.other, CartographyBrush::openBrushMenu);

        lime.repeat(CartographyBrush::update, 0.1);
    }
    public static final ConcurrentHashMap<UUID, CartographyBrush> current_brush = new ConcurrentHashMap<>();
    public static void update() {
        HashMap<UUID, CartographyBrush> data = new HashMap<>();
        Bukkit.getOnlinePlayers().forEach(p -> CartographyBrush.getData(p.getInventory().getItemInMainHand()).ifPresent(brush -> data.put(p.getUniqueId(), brush)));
        current_brush.putAll(data);
        current_brush.entrySet().removeIf(kv -> !data.containsKey(kv.getKey()));
    }
    private static boolean openBrushMenu(Player player) {
        return CartographyBrush.getData(player.getInventory().getItemInMainHand())
                .map(data -> {
                    MenuCreator.show(player, "brush.menu", data.args());
                    return data;
                })
                .isPresent();
    }

    public byte color;
    public byte size;

    public HashMap<String, String> map() {
        return system.map.<String, String>of()
                .add("color", String.valueOf(color))
                .add("hex", DrawMap.toHex(color))
                .add("size", String.valueOf(size))
                .build();
    }
    public Apply args() {
        return Apply.of().add(map());
    }
    protected CartographyBrush(JsonObject json) {
        super(json);

        color = json.has("color") ? json.get("color").getAsByte() : 0;
        size = json.has("size") ? json.get("size").getAsByte() : 0;
    }
    @Override public JsonObject toJson() {
        return system.json.object()
                .add("color", color)
                .add("size", size)
                .build();
    }

    public static Optional<CartographyBrush> getData(UUID uuid) {
        return Optional.ofNullable(current_brush.get(uuid));
    }
    public static Optional<CartographyBrush> getData(ItemStack item) {
        if (!Items.has(BrushSetting.class, item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        JsonObject json = JManager.get(JsonObject.class, meta.getPersistentDataContainer(), "brush.data", null);
        return Optional.of(new CartographyBrush(json == null ? new JsonObject() : json));
    }
    public static void modifyData(ItemStack item, system.Action1<CartographyBrush> modify) {
        Items.getItemCreator(item).map(v -> v instanceof Items.ItemCreator creator ? creator : null).ifPresent(creator -> {
            if (!creator.has(BrushSetting.class)) return;
            ItemMeta meta = item.getItemMeta();
            JsonObject json = JManager.get(JsonObject.class, meta.getPersistentDataContainer(), "brush.data", null);
            CartographyBrush data = new CartographyBrush(json == null ? new JsonObject() : json);
            modify.invoke(data);
            creator.apply(item, item.getAmount(), data.args());
            meta = item.getItemMeta();
            JManager.set(meta.getPersistentDataContainer(), "brush.data", data.toJson());
            item.setItemMeta(meta);
        });
    }
}























