package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.rows.BaseRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.ITable;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.item.Items;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.menu.page.slot.*;
import org.lime.system;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.inventory.MainPlayerInventory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Menu extends Base {
    public String title;
    public Items.ItemCreator background;
    public int size;
    public HashMap<Integer, ISlot> slots = new HashMap<>();
    public HashMap<String, Table> tables = new HashMap<>();
    public List<Roll> rolls = new ArrayList<>();

    public Menu(JsonObject json) {
        super(json);
        title = json.get("title").getAsString();
        size = json.get("size").getAsInt();
        json.get("slots").getAsJsonObject().entrySet().forEach(kv -> {
            ISlot slot = ISlot.parse(this, kv.getValue().getAsJsonObject());
            rangeOf(kv.getKey()).forEach(i -> slots.put(i, slot));
        });
        background = json.has("background") ? StaticSlot.parse(json.get("background").getAsJsonObject()) : null;
        if (json.has("tables")) json.get("tables").getAsJsonObject().entrySet().forEach(kv -> tables.put(kv.getKey(), Table.parse(this, kv.getValue().getAsJsonObject())));
        if (json.has("rolls")) json.get("rolls").getAsJsonArray().forEach(kv -> rolls.add(Roll.parse(this, kv.getAsJsonObject())));
    }

    public static List<Integer> rangeOf(String range) {
        List<Integer> list = new ArrayList<>();
        for (String arg : range.split(",")) {
            String[] keys = arg.replace("..", ".").split("\\.");
            if (keys.length == 1) list.add(Integer.parseInt(keys[0]));
            else IntStream.range(Integer.parseInt(keys[0]), Integer.parseInt(keys[1]) + 1).forEach(list::add);
        }
        return list;
    }

    @Override protected void showGenerate(UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        List<system.Toast2<String, Table>> _tables = tables.entrySet().stream().map(v -> system.toast(ChatHelper.formatText(v.getKey(), apply), v.getValue())).collect(Collectors.toList());
        system.waitAllAnyAsyns(_tables, (String table, system.Action1<ITable<? extends BaseRow>> callback) -> Tables
                .getTable(table, callback)
                .withSQL((sql) -> Logged.log(player, sql, this)), tableData -> {
            if (isDeleted()) {
                player.closeInventory();
                return;
            }
            if (tableData.size() > 0) {
                system.Toast3<String, Table, ITable<? extends BaseRow>> first = tableData.get(0);
                Table table = first.val1;
                int count = first.val2.getRows().size() + table.adds.size();
                apply.add("max_page", String.valueOf(count / table.slots.size()));
            }
            List<system.Action0> onClose = new ArrayList<>();
            HashMap<Integer, system.Toast2<HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, BaseRow>> onClickEvents = new HashMap<>();
            InterfaceManager.GUI gui = InterfaceManager.create(ChatHelper.formatComponent(title, apply), size, new InterfaceManager.IGUI() {
                @Override public void init(InterfaceManager.GUI gui) {
                    if (isDeleted()) {
                        player.closeInventory();
                        return;
                    }
                    Apply init_apply = apply.copy().add(row);
                    slots.forEach((k, v) -> {
                        if (v.tryIsShow(init_apply)) {
                            system.Toast2<HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> _slot = v.create(init_apply);
                            gui.inventory.setItem(k, _slot.val1);
                            onClickEvents.put(k, system.toast(_slot.val0, row));
                        }
                    });
                    system.Toast1<Boolean> isFirst = new system.Toast1<>(true);
                    tableData.forEach((kv) -> {
                        ITable<? extends BaseRow> k = kv.val2;
                        Table v = kv.val1;
                        List<? extends BaseRow> rows = v.getList(k, init_apply);
                        int count = v.slots.size();
                        int _page = isFirst.val0 ? page : 0;
                        isFirst.val0 = false;
                        int startIndex = _page * count;
                        int rowCount = rows.size();
                        int size = Math.min(rowCount + v.adds.size(), (_page + 1) * count);
                        for (int i = startIndex; i < size; i++) {
                            int index = v.slots.get(i - startIndex);
                            if (i >= rowCount) {
                                ISlot slot = v.adds.get(i - rowCount);
                                system.Toast2<HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> _slot = slot.create(init_apply);
                                gui.inventory.setItem(index, _slot.val1);
                                onClickEvents.put(index, system.toast(_slot.val0, row));
                            } else {
                                BaseRow _row = rows.get(i);
                                Apply row_apply = init_apply.copy().add(_row);
                                if (v.format.tryIsShow(row_apply)) {
                                    system.Toast2<HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> _slot = v.format.create(row_apply);
                                    gui.inventory.setItem(index, _slot.val1);
                                    onClickEvents.put(index, system.toast(_slot.val0, _row));
                                }
                            }
                        }
                    });
                }
                @Override public void onClick(InterfaceManager.GUI gui, Player player, Integer slot, Inventory inventory, ItemStack item, ClickType click, system.Action1<ItemStack> setCursor) {
                    if (isDeleted()) {
                        player.closeInventory();
                        return;
                    }
                    if (inventory.getType() != InventoryType.CHEST) return;
                    system.Toast2<HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, BaseRow> actions = onClickEvents.getOrDefault(slot, null);
                    if (actions == null) return;
                    List<org.lime.gp.player.menu.ActionSlot> action = actions.val0.getOrDefault(click, null);
                    if (action == null) return;
                    Apply slot_apply = apply.copy().add(actions.val1);
                    action.forEach(v -> v.invoke(player, slot_apply, true));
                }
                @Override
                public void onClose(InterfaceManager.GUI gui, Player player) {
                    onClose.forEach(system.Action0::invoke);
                }
            });
            if (background != null) MainPlayerInventory.setBackground(player, gui.inventory, background.createItem());
            else MainPlayerInventory.clearBackground(player);
            this.rolls.forEach(roll -> onClose.add(roll.apply(player, gui.inventory, apply, onClickEvents)));
            gui.show(player);
        });
    }
}




















