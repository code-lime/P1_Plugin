package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.Rows;
import org.lime.gp.item.Items;
import org.lime.gp.item.Settings;
import org.lime.gp.lime;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.menu.Slot;
import org.lime.system;

import java.util.*;

public class Insert extends Base {
    public static core.element create() {
        return core.element.create(Insert.class)
                .withInit(Insert::init);
    }
    public static void init() {
        AnyEvent.addEvent("insert.give", AnyEvent.type.other, b -> b.createParam("[type]").createParam(Integer::parseUnsignedInt, "[cash:int]"), (p, type, cash) -> Items.dropGiveItem(p, Settings.InsertSetting.createOf(type, cash), false));
        lime.repeatTicks(Insert::tick, 1);
    }

    private static boolean filter(net.minecraft.world.item.ItemStack item, String type) {
        return Items.getOptional(Settings.InsertSetting.class, CraftItemStack.asBukkitCopy(item))
                .filter(v -> v.type.equals(type))
                .isPresent();
    }
    private static boolean openFilterInventory(Player player, CraftInventory inventory, String type) {
        player.closeInventory();
        return InterfaceManager.of(player, inventory)
                .filter(v -> filter(v, type))
                .open();
    }
    private static boolean open(Player player, Component title, int rows, system.Action1<Integer> callback, String type) {
        return openFilterInventory(player, inventoryWallets.compute(player.getUniqueId(), (uuid, inv) -> inv == null
                ? system.toast(new CraftInventoryCustom(null, rows * 9, title), callback)
                : inv
        ).val0, type);
    }
    private static final HashMap<UUID, system.Toast2<CraftInventory, system.Action1<Integer>>> inventoryWallets = new HashMap<>();
    private static void tick() {
        inventoryWallets.values().removeIf(kv -> {
            CraftInventory inventory = kv.val0;
            if (inventory.getViewers().size() != 0) return false;
            int cash = 0;
            for (ItemStack item : inventory)
                cash += Items.getOptional(Settings.InsertSetting.class, item).map(v -> v.weight).map(v -> v * item.getAmount()).orElse(0);
            kv.val1.invoke(cash);
            return true;
        });
    }

    public String title;
    public String type;
    public int rows;
    public List<Slot> output = new ArrayList<>();

    public Insert(JsonObject json) {
        super(json);
        title = json.has("title") ? json.get("title").getAsString() : "Chest";
        type = json.get("insert").getAsString();
        rows = json.has("rows") ? json.get("rows").getAsInt() : 3;
        if (json.has("output")) json.get("output").getAsJsonArray().forEach(kv -> output.add(Slot.parse(this, kv.getAsJsonObject())));
    }

    @Override protected void showGenerate(Rows.UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        open(player, ChatHelper.formatComponent(title, apply), rows, (cash) -> {
            apply.add("weight", cash + "");
            output.forEach(i -> i.invoke(player, apply, true));
        }, type);
    }
}

















