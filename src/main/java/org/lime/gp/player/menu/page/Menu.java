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
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.menu.page.slot.*;
import org.lime.system.range.IRange;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.inventory.MainPlayerInventory;
import org.lime.system.utils.IterableUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Menu extends Base {
    public String title;
    public ItemCreator background;
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
            IRange.parse(kv.getKey()).getAllInts(size).forEach(i -> slots.put(i, slot));
            //rangeOf(kv.getKey()).forEach(i -> slots.put(i, slot));
        });
        background = json.has("background") ? StaticSlot.parse(json.get("background").getAsJsonObject()) : null;
        if (json.has("tables")) json.get("tables").getAsJsonObject().entrySet().forEach(kv -> tables.put(kv.getKey(), Table.parse(this, kv.getValue().getAsJsonObject())));
        if (json.has("rolls")) json.get("rolls").getAsJsonArray().forEach(kv -> rolls.add(Roll.parse(this, kv.getAsJsonObject())));
    }
/*
    public static List<Integer> rangeOf(String range) {
        List<Integer> list = new ArrayList<>();
        for (String arg : range.split(",")) {
            String[] keys = arg.replace("..", ".").split("\\.");
            if (keys.length == 1) list.add(Integer.parseInt(keys[0]));
            else IntStream.range(Integer.parseInt(keys[0]), Integer.parseInt(keys[1]) + 1).forEach(list::add);
        }
        return list;
    }
*/
    @Override protected void showGenerate(UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        List<Toast2<String, Table>> _tables = tables.entrySet().stream().map(v -> Toast.of(ChatHelper.formatText(v.getKey(), apply), v.getValue())).collect(Collectors.toList());
        IterableUtils.waitAllAnyAsyns(_tables, (String table, Action1<ITable<? extends BaseRow>> callback) -> Tables
                .getTable(table, callback)
                .withSQL((sql) -> Logged.log(player, sql, this)), tableData -> {
            if (isDeleted()) {
                player.closeInventory();
                return;
            }
            if (tableData.size() > 0) {
                Toast3<String, Table, ITable<? extends BaseRow>> first = tableData.get(0);
                Table table = first.val1;
                int count = first.val2.getRows().size() + table.adds.size();
                if (count == 0) {
                    apply.add("max_page", "0");
                } else {
                    int pageSize = table.slots.size();
                    int maxPage = count / pageSize;
                    if (count % pageSize == 0) maxPage--;
                    apply.add("max_page", String.valueOf(maxPage));
                }
            }
            List<Action0> onClose = new ArrayList<>();
            HashMap<Integer, Toast3<List<Toast2<String, String>>, HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, BaseRow>> onClickEvents = new HashMap<>();
            InterfaceManager.GUI gui = InterfaceManager.create(ChatHelper.formatComponent(title, apply), size, new InterfaceManager.IGUI() {
                @Override public void init(InterfaceManager.GUI gui) {
                    if (isDeleted()) {
                        player.closeInventory();
                        return;
                    }
                    Apply init_apply = apply.copy().add(row);
                    slots.forEach((k, v) -> {
                        if (v.tryIsShow(init_apply)) {
                            Toast3<List<Toast2<String, String>>, HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> _slot = v.create(init_apply);
                            gui.inventory.setItem(k, _slot.val2);
                            onClickEvents.put(k, Toast.of(_slot.val0, _slot.val1, row));
                        }
                    });
                    Toast1<Boolean> isFirst = new Toast1<>(true);
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
                                Toast3<List<Toast2<String, String>>, HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> _slot = slot.create(init_apply);
                                gui.inventory.setItem(index, _slot.val2);
                                onClickEvents.put(index, Toast.of(_slot.val0, _slot.val1, row));
                            } else {
                                BaseRow _row = rows.get(i);
                                Apply row_apply = init_apply.copy().add(_row);
                                if (v.format.tryIsShow(row_apply)) {
                                    Toast3<List<Toast2<String, String>>, HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> _slot = v.format.create(row_apply);
                                    gui.inventory.setItem(index, _slot.val2);
                                    onClickEvents.put(index, Toast.of(_slot.val0, _slot.val1, _row));
                                }
                            }
                        }
                    });
                }
                @Override public void onClick(InterfaceManager.GUI gui, Player player, Integer slot, Inventory inventory, ItemStack item, ClickType click, Action1<ItemStack> setCursor) {
                    if (isDeleted()) {
                        player.closeInventory();
                        return;
                    }
                    if (inventory.getType() != InventoryType.CHEST) return;
                    Toast3<List<Toast2<String, String>>, HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, BaseRow> actions = onClickEvents.getOrDefault(slot, null);
                    if (actions == null) return;
                    List<org.lime.gp.player.menu.ActionSlot> action = actions.val1.getOrDefault(click, null);
                    if (action == null) return;
                    Apply slot_apply = apply.copy().add(actions.val2);
                    for (Toast2<String, String> kv : actions.val0) slot_apply = slot_apply.add(kv.val0, ChatHelper.formatText(kv.val1, slot_apply));
                    for (org.lime.gp.player.menu.ActionSlot v : action) v.invoke(player, slot_apply, true);
                }
                @Override
                public void onClose(InterfaceManager.GUI gui, Player player) {
                    onClose.forEach(Action0::invoke);
                }
            });
            if (background != null) MainPlayerInventory.setBackground(player, gui.inventory, background.createItem());
            else MainPlayerInventory.clearBackground(player);
            this.rolls.forEach(roll -> onClose.add(roll.apply(player, gui.inventory, apply, onClickEvents)));
            gui.show(player);
        });
    }
}




















