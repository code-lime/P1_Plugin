package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import net.minecraft.world.inventory.ContainerChest;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventoryView;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.plugin.CoreElement;
import org.lime.system.utils.ItemUtils;

import java.util.Optional;

@Setting(name = "zipper") public class ZipperSetting extends ItemSetting<JsonObject> {
    public static CoreElement create() {
        return CoreElement.create(ZipperSetting.class)
                .withInstance(new Listener() {
                    @EventHandler public static void onClick(InventoryClickEvent e) {
                        if (e.getView() instanceof CraftInventoryView view
                                && view.getHandle() instanceof ContainerChest containerChest
                                && containerChest.getContainer() instanceof ReadonlyInventory) return;
                        if (!(e.getWhoClicked() instanceof Player player)) return;
                        switch (e.getClick()) {
                            case RIGHT, LEFT, SHIFT_RIGHT, SHIFT_LEFT -> {
                                ItemStack cursor = e.getCursor();
                                ItemStack item = e.getCurrentItem();
                                if (cursor == null || item == null || item.isEmpty()) return;
                                Items.getOptional(ZipperSetting.class, cursor)
                                        .ifPresent(zipper -> {
                                            if (Items.has(ZipperSetting.class, item) || Items.has(ZipSetting.class, item)) return;
                                            zipper.createZip(player, item, e.isShiftClick())
                                                    .ifPresent(zip -> {
                                                        cursor.subtract(1);
                                                        item.setAmount(0);
                                                        Items.dropGiveItem(player, zip, false);
                                                        e.setCancelled(true);
                                                    });
                                        });
                            }
                        }
                    }
                });
    }

    public final Checker zip;
    public final Checker shiftZip;

    public ZipperSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        zip = Checker.createCheck(json.get("zip").getAsString());
        shiftZip = json.has("shift_zip") ? Checker.createCheck(json.get("shift_zip").getAsString()) : zip;
    }

    public Optional<ItemStack> createZip(Player player, ItemStack present, boolean shift) {
        return present.isEmpty()
                ? Optional.empty()
                : (shift ? shiftZip : zip)
                .getRandomCreator()
                .map(v -> {
                    Apply apply = Apply.of();
                    apply = UserRow.getBy(player)
                            .map(apply::add)
                            .orElse(apply)
                            .add("zip.item", ItemUtils.saveItem(present));
                    return v.createItem(apply);
                });
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("zip"), IJElement.link(docs.regexItem()), IComment.raw("Предмет, в который запакуется")),
                JProperty.optional(IName.raw("shift_zip"), IJElement.link(docs.regexItem()), IComment.raw("Предмет, в который запакуется (при нажатии SHIFT)"))
        ));
    }
}
