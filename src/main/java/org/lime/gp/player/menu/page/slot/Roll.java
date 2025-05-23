package org.lime.gp.player.menu.page.slot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.rows.BaseRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.RandomUtils;

import java.util.*;
import java.util.stream.IntStream;

public class Roll implements Logged.ILoggedDelete {
    public List<Integer> slots = new ArrayList<>();
    public ActionSlot generate = ActionSlot.NONE;
    public ActionSlot tick = ActionSlot.NONE;
    public ActionSlot end = ActionSlot.NONE;
    public String data;
    public ISlot format;


    private final Logged.ILoggedDelete base;
    private final Logged.ChildLoggedDeleteHandle deleteHandle;

    @Override public String getLoggedKey() { return base.getLoggedKey(); }
    @Override public boolean isLogged() { return base.isLogged(); }
    @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
    @Override public void delete() { deleteHandle.delete(); }

    //https://cdn.discordapp.com/attachments/862771221963997207/1010605359725019286/unknown.png
    public static double VAR_K = 3;
    public static double VAR_L = -2;
    public static int VAR_S = 100;

    /*static {
        AnyEvent.addEvent("set.var", AnyEvent.type.owner_console, v -> v.createParam("k","l","s").createParam(Double::parseDouble, "[val]"), (p,name,val) -> {
            switch (name) {
                case "k" -> VAR_K = val;
                case "l" -> VAR_L = val;
                case "s" -> VAR_S = (int)(double)val;
            }
        });
    }*/

    protected Roll(Logged.ILoggedDelete base) {
        this.base = base;
        deleteHandle = new Logged.ChildLoggedDeleteHandle(base);
    }

    public static Roll parse(Logged.ILoggedDelete base, JsonObject json) {
        Roll roll = new Roll(base);
        json.get("slots").getAsJsonArray().forEach(kv -> {
            if (kv.isJsonArray()) {
                JsonArray arr = kv.getAsJsonArray();
                IntStream.range(arr.get(0).getAsInt(), arr.get(1).getAsInt() + 1).forEach(roll.slots::add);
            } else {
                roll.slots.add(kv.getAsInt());
            }
        });
        roll.format = ISlot.parse(roll, json.get("format"));
        roll.data = json.get("data").getAsString();
        if (json.has("generate")) roll.generate = ActionSlot.parse(roll, json.get("generate").getAsJsonObject());
        if (json.has("end")) roll.end = ActionSlot.parse(roll, json.get("end").getAsJsonObject());
        if (json.has("tick")) roll.tick = ActionSlot.parse(roll, json.get("tick").getAsJsonObject());
        if (roll.slots.isEmpty()) throw new IllegalArgumentException("ROLL.SLOTS SIZE ZERO");
        return roll;
    }

    public Action0 apply(Player player, Apply apply, BaseRow row, Map<Integer, Toast4<List<Toast2<String, String>>, Map<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, net.minecraft.world.item.ItemStack, BaseRow>> slotsData) {
        List<Toast2<HashMap<String, String>, Integer>> array = new ArrayList<>();
        Toast1<Integer> scale = Toast.of(0);
        json.parse(JavaScript.getJsString(apply.apply(data)).orElseThrow()).getAsJsonArray().forEach(item -> {
            JsonObject data = item.getAsJsonObject();
            HashMap<String, String> map = new HashMap<>();
            int _scale = data.get("scale").getAsInt();
            data.entrySet().forEach(kv -> map.put(kv.getKey(), kv.getValue().getAsString()));
            scale.val0 += _scale;
            array.add(Toast.of(map, _scale));
        });
        Map<ItemStack, Toast2<Integer, Apply>> values = new HashMap<>();
        array.forEach(kv -> {
            Apply _apply = apply.copy().add(kv.val0);
            values.put(format.create(_apply).val2, Toast.of(kv.val1, _apply));
        });
        int size = values.size();
        if (size == 0) throw new IllegalArgumentException("ROLL.SIZE_ZERO");
        int slots = this.slots.size();
        if (slots == 0) throw new IllegalArgumentException("ROLL.SLOTS_ZERO");
        return roll(player, scale.val0, values, row, slotsData);
    }
    private Action0 roll(Player player, int total, Map<ItemStack, Toast2<Integer, Apply>> values, BaseRow row, Map<Integer, Toast4<List<Toast2<String, String>>, Map<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, net.minecraft.world.item.ItemStack, BaseRow>> slotsData) {
        Toast1<Integer> result_index = Toast.of(RandomUtils.rand(0, total));
        return values.entrySet()
                .stream()
                .filter(kv -> {
                    boolean filter = result_index.val0 > 0;
                    result_index.val0 -= kv.getValue().val0;
                    return filter;
                })
                .map(kv -> Toast.of(kv.getKey(), kv.getValue().val1))
                .reduce((v1,v2) -> v2)
                .map(result -> {
                    generate.invoke(player, result.val1.copy(), true);

                    int size = slots.size();
                    int roll_size = VAR_S;

                    List<ItemStack> see = randomize(values, v -> v.val0, roll_size + size);
                    see.set(size / 2, result.val0);

                    List<BukkitTask> tasks = new ArrayList<>();

                    Toast1<Boolean> closed = Toast.of(false);
                    double wait = 0;
                    for (int i = roll_size - 1; i >= 0; i--) {
                        int _i = i;

                        double j = roll_size - i + VAR_L;
                        double k = -VAR_K;

                        tasks.add(lime.once(() -> {
                            if (closed.val0) return;
                            frame(player, row, slotsData, _i, size, see);
                            //lime.logOP("J: " + j + " | " + k + " | " + _i + " / " + roll_size);
                        }, wait));

                        wait += Math.max(0.05, k / (j - roll_size));
                        //if (progress < 0.5) wait += Math.min(0.05, 0.5 * progress * progress);
                        //else wait += Math.min(0.05, 0.5 * progress);*/
                    }
                    tasks.add(lime.once(() -> {
                        closed.val0 = true;
                        end.invoke(player, result.val1.copy().add("closed", "false"), true);
                    }, wait));

                    return Execute.action(() -> {
                        if (closed.val0) return;
                        closed.val0 = true;
                        tasks.forEach(BukkitTask::cancel);
                        end.invoke(player, result.val1.copy().add("closed", "true"), true);
                    });
                })
                .orElse(() -> {});
    }
    private static <T, V> List<T> randomize(Map<T, V> items, Func1<V, Integer> func, int size) {
        List<T> list = new ArrayList<>();
        while (list.size() < size) items.forEach((k, v) -> {
            for (int i = func.invoke(v) - 1; i >= 0; i--) list.add(k);
        });
        RandomUtils.randomize(list);
        return list;
    }
    private void frame(Player player, BaseRow row, Map<Integer, Toast4<List<Toast2<String, String>>, Map<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, net.minecraft.world.item.ItemStack, BaseRow>> slotsData, int index, int size, List<ItemStack> see) {
        for (int i = size - 1; i >= 0; i--) {
            slotsData.computeIfAbsent(slots.get(size - 1 - i), v -> Toast.of(Collections.emptyList(), Collections.emptyMap(), net.minecraft.world.item.ItemStack.EMPTY, row))
                    .val2 = CraftItemStack.asNMSCopy(see.get(index + i));
            //slotsData.setItem(slots.get(size - 1 - i), see.get(index + i));
        }
        ((CraftPlayer)player).getHandle().containerMenu.broadcastFullState();
        tick.invoke(player, Apply.of(), true);
    }
}


















