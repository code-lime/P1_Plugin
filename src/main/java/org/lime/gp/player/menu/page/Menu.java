package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.InventoryClickType;
import net.minecraft.world.inventory.Slot;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.rows.BaseRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.ITable;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.lime;
import org.lime.gp.player.inventory.gui.ContainerGUI;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.menu.page.slot.*;
import org.lime.gp.player.ui.EditorUI;
import org.lime.system.range.IRange;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.gp.player.inventory.gui.InterfaceManager;
import org.lime.gp.player.inventory.MainPlayerInventory;
import org.lime.system.utils.IterableUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class Menu extends Base {
    public String title;
    public ItemCreator background;
    public int rows;
    public HashMap<Integer, ISlot> slots = new HashMap<>();
    public List<Toast2<String, Table>> tables = new ArrayList<>();
    public List<Roll> rolls = new ArrayList<>();

    private static int getRows(int slots) {
        if (slots % 9 != 0) throw new IllegalArgumentException("Slots '"+slots+"' % 9 != 0");
        return slots / 9;
    }

    public Menu(JsonObject json) {
        super(json);
        title = json.get("title").getAsString();
        rows = json.has("rows") ? json.get("rows").getAsInt() : getRows(json.get("size").getAsInt());
        json.get("slots").getAsJsonObject().entrySet().forEach(kv -> {
            ISlot slot = ISlot.parse(this, kv.getValue().getAsJsonObject());
            IRange.parse(kv.getKey()).getAllInts(rows * 9).forEach(i -> slots.put(i, slot));
            //rangeOf(kv.getKey()).forEach(i -> slots.put(i, slot));
        });
        background = json.has("background") ? StaticSlot.parse(json.get("background").getAsJsonObject()) : null;
        if (json.has("tables")) json.get("tables").getAsJsonObject().entrySet().forEach(kv -> tables.add(Toast.of(kv.getKey(), Table.parse(this, kv.getValue().getAsJsonObject()))));
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
    @Override protected void showGenerate(UserRow row, @Nullable Player player, int page, Apply apply) {
        if (player == null) {
            lime.logOP("Menu '"+getKey()+"' not called! User is NULL");
            return;
        }
        List<Toast2<String, Table>> _tables = tables.stream().map(v -> Toast.of(ChatHelper.formatText(v.val0, apply), v.val1)).collect(Collectors.toList());
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
            Map<Integer, Toast4<List<Toast2<String, String>>, Map<ClickType, List<ActionSlot>>, net.minecraft.world.item.ItemStack, BaseRow>> slotsData = new HashMap<>();

            if (isDeleted()) {
                player.closeInventory();
                return;
            }
            Apply init_apply = apply.copy().add(row);
            slots.forEach((k, v) -> {
                if (v.tryIsShow(init_apply)) {
                    Toast3<List<Toast2<String, String>>, Map<ClickType, List<ActionSlot>>, ItemStack> _slot = v.create(init_apply);
                    slotsData.put(k, Toast.of(_slot.val0, _slot.val1, CraftItemStack.asNMSCopy(_slot.val2), row));
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
                        Toast3<List<Toast2<String, String>>, Map<ClickType, List<ActionSlot>>, ItemStack> _slot = slot.create(init_apply);
                         slotsData.put(index, Toast.of(_slot.val0, _slot.val1, CraftItemStack.asNMSCopy(_slot.val2), row));
                    } else {
                        BaseRow _row = rows.get(i);
                        Apply row_apply = init_apply.copy().add(_row);
                        if (v.format.tryIsShow(row_apply)) {
                            Toast3<List<Toast2<String, String>>, Map<ClickType, List<ActionSlot>>, ItemStack> _slot = v.format.create(row_apply);
                            slotsData.put(index, Toast.of(_slot.val0, _slot.val1, CraftItemStack.asNMSCopy(_slot.val2), _row));
                        }
                    }
                }
            });

            if (EditorUI.openRaw(player, ChatHelper.formatComponent(title, apply), (syncId, playerInventory, human) -> new ContainerGUI(syncId, playerInventory, rows) {
                @Override public void onClose(EntityHuman human) {
                    onClose.forEach(Action0::invoke);
                }
                @Override public Slot createInventorySlot(Slot slot) {
                    return new InterfaceManager.BasePacketSlot(slot) {
                        @Override public net.minecraft.world.item.ItemStack getItem() {
                            var slot = slotsData.get(index);
                            return slot == null ? net.minecraft.world.item.ItemStack.EMPTY : slot.val2;
                        }
                        @Override public void onSlotClick(EntityHuman human, InventoryClickType type, ClickType click) {
                            if (isDeleted()) {
                                removed(human);
                                return;
                            }
                            var slot = slotsData.get(index);
                            if (slot == null) return;
                            List<org.lime.gp.player.menu.ActionSlot> action = slot.val1.getOrDefault(click, null);
                            if (action == null) return;
                            Apply slot_apply = apply.copy().add(slot.val3);
                            for (Toast2<String, String> kv : slot.val0)
                                slot_apply = slot_apply.add(kv.val0, ChatHelper.formatText(kv.val1, slot_apply));
                            if (human instanceof EntityPlayer player) {
                                CraftPlayer craftPlayer = player.getBukkitEntity();
                                for (org.lime.gp.player.menu.ActionSlot v : action)
                                    v.invoke(craftPlayer, slot_apply, true);
                            }
                        }
                    };
                }
            })) {
                if (background != null) MainPlayerInventory.setBackground(player, player.getOpenInventory().getTopInventory(), background.createItem());
                else MainPlayerInventory.clearBackground(player);
            }
            this.rolls.forEach(roll -> onClose.add(roll.apply(player, apply, row, slotsData)));
        });
    }
}




















