package org.lime.gp.player.menu.page.slot;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.player.menu.Logged;
import org.lime.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class SelectSlot implements ISlot {
    public List<system.Toast2<String, ISlot>> slots = new ArrayList<>();

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
        json.entrySet().forEach(kv -> slot.slots.add(system.toast(kv.getKey(), ISlot.parse(slot, kv.getValue().getAsJsonObject()))));
        return slot;
    }

    private Optional<ISlot> getSelected(Apply apply) {
        if (isDeleted()) return Optional.empty();
        for (system.Toast2<String, ISlot> slot : slots) {
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

    public system.Toast3<List<system.Toast2<String, String>>, HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> create(Apply apply) {
        return getSelected(apply)
                .map(v -> v.create(apply))
                .orElseGet(() -> system.toast(new ArrayList<>(), new HashMap<>(), new ItemStack(Material.AIR)));
    }
}






















