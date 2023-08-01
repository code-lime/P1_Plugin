package org.lime.gp.module.loot;

import com.google.gson.JsonElement;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.item.loot.ILoot;
import org.lime.gp.item.loot.MultiLoot;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;

public enum LootModifyAction {
    NONE("n"),
    APPEND("a"),
    REPLACE("r"),
    APPEND_IF_NOT_EMPTY("ane"),
    REPLACE_IF_NOT_EMPTY("rne");

    public final String fastPrefix;

    LootModifyAction(String fp) {
        this.fastPrefix = fp;
    }

    private boolean isPostfix(String postfix) {
        return postfix.equalsIgnoreCase(fastPrefix) ||
                postfix.equalsIgnoreCase(name());
    }

    public void modifyLoot(PopulateLootEvent e, ILoot loot) {
        List<ItemStack> items = loot.generateLoot(e);
        switch (this) {
            case APPEND -> e.addItems(items);
            case APPEND_IF_NOT_EMPTY -> {
                if (!items.isEmpty()) e.addItems(items);
            }
            case REPLACE -> e.setItems(items);
            case REPLACE_IF_NOT_EMPTY -> {
                if (!items.isEmpty()) e.setItems(items);
            }
            default -> {
            }
        }
    }

    public ILoot changeLoot(ILoot base, ILoot other) {
        return switch (this) {
            case APPEND -> new MultiLoot(List.of(base, other));
            case APPEND_IF_NOT_EMPTY -> loot -> {
                List<ItemStack> items = new ArrayList<>(base.generateLoot(loot));
                if (!items.isEmpty()) items.addAll(other.generateLoot(loot));
                return items;
            };
            case REPLACE -> other;
            case REPLACE_IF_NOT_EMPTY -> loot -> {
                List<ItemStack> items = new ArrayList<>(base.generateLoot(loot));
                return items.isEmpty() ? items : other.generateLoot(loot);
            };
            default -> base;
        };
    }

    public static LootModifyAction byPostfix(String postfix) {
        for (LootModifyAction action : values()) {
            if (action.isPostfix(postfix))
                return action;
        }
        return LootModifyAction.NONE;
    }

    public static system.Toast3<String, ILoot, LootModifyAction> parse(String key, JsonElement value) {
        String[] keys = key.split("#", 2);
        return system.toast(keys[0], ILoot.parse(value), LootModifyAction.byPostfix(keys[1]));
    }
}
