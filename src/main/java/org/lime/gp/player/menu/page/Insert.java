package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.lime;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.system.Func1;
import org.lime.system;

import java.util.*;

public class Insert extends Base {
    public static CoreElement create() {
        return CoreElement.create(Insert.class)
                .withInit(Insert::init);
    }
    public static void init() {
        AnyEvent.addEvent("insert.give", AnyEvent.type.other, b -> b.createParam("[type]").createParam(Integer::parseUnsignedInt, "[cash:int]"), (p, type, cash) -> Items.dropGiveItem(p, InsertSetting.createOf(type, cash), false));
        lime.repeatTicks(Insert::tick, 1);
    }

    private static Func1<net.minecraft.world.item.ItemStack, Boolean> filter(String type) {
        return item -> Items.getOptional(InsertSetting.class, CraftItemStack.asBukkitCopy(item))
                .filter(v -> v.type.equals(type))
                .isPresent();
    }
    private static Func1<net.minecraft.world.item.ItemStack, Boolean> filter(List<String> items) {
        Checker checker = Checker.createCheck(items);
        return checker::check;
    }

    private static boolean openFilterInventory(Player player, CraftInventory inventory, Func1<net.minecraft.world.item.ItemStack, Boolean> filter) {
        player.closeInventory();
        return InterfaceManager.of(player, inventory)
                .filter(filter)
                .open();
    }
    private static boolean open(Player player, Component title, int rows, system.Action1<Integer> callback, Func1<net.minecraft.world.item.ItemStack, Boolean> filter) {
        return openFilterInventory(player, inventoryWallets.compute(player.getUniqueId(), (uuid, inv) -> inv == null
                ? system.toast(new CraftInventoryCustom(null, rows * 9, title), callback)
                : inv
        ).val0, filter);
    }
    private static final HashMap<UUID, system.Toast2<CraftInventory, system.Action1<Integer>>> inventoryWallets = new HashMap<>();
    private static void tick() {
        inventoryWallets.values().removeIf(kv -> {
            CraftInventory inventory = kv.val0;
            if (inventory.getViewers().size() != 0) return false;
            int cash = 0;
            for (ItemStack item : inventory)
                cash += Items.getOptional(InsertSetting.class, item)
                    .map(v -> v.weight)
                    .orElse(1) * Optional.ofNullable(item).map(ItemStack::getAmount).orElse(0);
            kv.val1.invoke(cash);
            return true;
        });
    }

    public String title;
    public Func1<net.minecraft.world.item.ItemStack, Boolean> filter;
    public int rows;
    public List<ActionSlot> output = new ArrayList<>();

    public Insert(JsonObject json) {
        super(json);
        title = json.has("title") ? json.get("title").getAsString() : "Chest";
        if (json.has("insert")) {
            filter = Insert.filter(json.get("insert").getAsString());
        } else {
            filter = Insert.filter(system.list.<String>of().add(json.get("regex").getAsJsonArray(), v -> v.getAsString()).build());
        }
        rows = json.has("rows") ? json.get("rows").getAsInt() : 3;
        if (json.has("output")) json.get("output").getAsJsonArray().forEach(kv -> output.add(ActionSlot.parse(this, kv.getAsJsonObject())));
    }

    @Override protected void showGenerate(UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        open(player, ChatHelper.formatComponent(title, apply), rows, (cash) -> {
            apply.add("weight", cash + "");
            output.forEach(i -> i.invoke(player, apply, true));
        }, filter);
    }
}

















