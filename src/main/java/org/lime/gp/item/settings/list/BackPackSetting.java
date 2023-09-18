package org.lime.gp.item.settings.list;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.JObject;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

import com.google.gson.JsonObject;

@Setting(name = "backpack") public class BackPackSetting extends ItemSetting<JsonObject> {
    public enum PoseType {
        NONE,
        SHIFT;

        public static PoseType getPose(Player player) { return player.isSneaking() ? PoseType.SHIFT : PoseType.NONE; }
    }
    public HashMap<PoseType, net.minecraft.world.item.ItemStack> data = new HashMap<>();

    public BackPackSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        json.entrySet().forEach(kv -> {
            PoseType pose = PoseType.valueOf(kv.getKey());
            JsonObject value = kv.getValue().getAsJsonObject();
            int id = value.get("id").getAsInt();
            Material type = Material.valueOf(value.get("type").getAsString());
            ItemStack item = new ItemStack(type);
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(id);
            item.setItemMeta(meta);
            data.put(pose, CraftItemStack.asNMSCopy(item));
        });
        net.minecraft.world.item.ItemStack none = data.get(PoseType.NONE);
        for (PoseType pose : PoseType.values())
            data.putIfAbsent(pose, none);
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(), "*Не используется");
    }
}









