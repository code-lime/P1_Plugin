package org.lime.gp.player.menu.page.slot;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.gp.player.menu.Logged;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;

public class SelectSlot implements ISlot {
    public List<Toast2<String, ISlot>> slots = new ArrayList<>();

    private final Logged.ILoggedDelete base;
    private final Logged.ChildLoggedDeleteHandle deleteHandle;

    @Override public String getLoggedKey() { return base.getLoggedKey(); }
    @Override public boolean isLogged() { return base.isLogged(); }
    @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
    @Override public void delete() { deleteHandle.delete(); }


    private SelectSlot(Logged.ILoggedDelete base) {
        this.base = base;
        this.deleteHandle = new Logged.ChildLoggedDeleteHandle(base);
    }

    public static SelectSlot parse(Logged.ILoggedDelete base, JsonObject json) {
        SelectSlot slot = new SelectSlot(base);
        json.entrySet().forEach(kv -> slot.slots.add(Toast.of(kv.getKey(), ISlot.parse(slot, kv.getValue().getAsJsonObject()))));
        return slot;
    }

    private Optional<ISlot> getSelected(Apply apply) {
        if (isDeleted()) return Optional.empty();
        for (Toast2<String, ISlot> slot : slots) {
            if (ISlot.isTrue(slot.val0, apply))
                return Optional.of(slot.val1);
        }
        return Optional.empty();
    }

    public boolean tryIsShow(Apply apply) {
        return getSelected(apply)
                .filter(v -> v.tryIsShow(apply))
                .isPresent();
    }

    public Toast3<List<Toast2<String, String>>, Map<ClickType, List<ActionSlot>>, ItemStack> create(Apply apply) {
        return getSelected(apply)
                .map(v -> v.create(apply))
                .orElseGet(() -> Toast.of(new ArrayList<>(), new HashMap<>(), new ItemStack(Material.AIR)));
    }
}






















