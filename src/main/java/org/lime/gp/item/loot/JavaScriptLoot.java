package org.lime.gp.item.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemCarrotStick;
import net.minecraft.world.item.ItemDebugStick;
import net.minecraft.world.item.ItemShears;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.persistence.CraftPersistentDataContainer;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.filter.data.AppendFunction;
import org.lime.gp.item.Items;
import org.lime.gp.module.JavaScript;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.module.loot.ModifyLootTable;
import org.lime.gp.module.loot.Parameters;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;

import java.util.*;

public class JavaScriptLoot implements ILoot {
    public final String code;
    public final JsonObjectOptional args;
    public final ILoot next;

    public JavaScriptLoot(JsonObject json) {
        code = json.get("code").getAsString();
        args = json.has("args") ? JsonObjectOptional.of(json.getAsJsonObject("args")) : new JsonObjectOptional();
        next = ILoot.parse(json.get("next"));
    }

    @Override public List<ItemStack> generateLoot(IPopulateLoot loot) {
        Map<String, Object> args = this.args.createObject();
        Map<String, Object> data = new HashMap<>();
        AppendFunction function = new AppendFunction() {
            @Override public void appendNan(String name) {}
            @Override public void appendWithoutValue(String name) {}
            @Override public void appendValue(String name, String value) { data.put(name, value); }
        };
        Parameters.filterInfo().getAllParams().forEach(info -> info.appendTo(loot, function));
        args.put("data", data);
        ModifyLootTable.getOwnerPlayer(loot).map(CraftEntity::getUniqueId).ifPresent(uuid -> args.put("uuid", uuid.toString()));
        List<ItemStack> items = new ArrayList<>(next.generateLoot(loot));
        args.put("variable", JsonObjectOptional.of(json.by(VariableLoot.exportVariablesWithRemove(items)).build()).createObject());
        Map<String, Object> _loot = new HashMap<>();
        items.forEach(item -> {
            Map<String, Object> itemInfo = new HashMap<>();
            _loot.put(String.valueOf(_loot.size()), itemInfo);
            itemInfo.put("id", Items.getGlobalKeyByItem(item).orElse("NULL"));
            itemInfo.compute("count", (key, value) -> (value instanceof Number val ? val.intValue() : 0) + item.getAmount());
            itemInfo.put("data", item.getItemMeta().getPersistentDataContainer() instanceof CraftPersistentDataContainer container ? container.serialize() : new HashMap<>());
        });
        args.put("loot", _loot);
        JavaScript.invoke(code, args);
        return items;
    }
}
