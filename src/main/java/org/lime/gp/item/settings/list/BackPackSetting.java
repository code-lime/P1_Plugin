package org.lime.gp.item.settings.list;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.lime.system;
import org.lime.display.Models.Model;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.lime;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

import com.google.gson.JsonObject;

import net.minecraft.core.Vector3f;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;

@Setting(name = "backpack") public class BackPackSetting extends ItemSetting<JsonObject> {
    public enum PoseType {
        NONE,
        SHIFT;

        public static PoseType getPose(Player player) {
            return player.isSneaking() ? PoseType.SHIFT : PoseType.NONE;
        }
    }
    public HashMap<PoseType, Model> data = new HashMap<>();

    public BackPackSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator, json);
        json.entrySet().forEach(kv -> {
            PoseType pose = PoseType.valueOf(kv.getKey());
            JsonObject value = kv.getValue().getAsJsonObject();
            int id = value.get("id").getAsInt();
            Material type = Material.valueOf(value.get("type").getAsString());
            Vector offset = system.getVector(value.get("offset").getAsString());
            ItemStack item = new ItemStack(type);
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(id);
            item.setItemMeta(meta);
            data.put(pose, lime.models.builder(EntityTypes.ARMOR_STAND)
                .local(new LocalLocation(offset))
                .nbt(() -> {
                    EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, lime.MainWorld.getHandle());
                    stand.setNoBasePlate(true);
                    stand.setSmall(true);
                    stand.setInvisible(true);
                    stand.setInvulnerable(true);
                    stand.setMarker(true);
                    stand.setHeadPose(new Vector3f(90, 0, 0));
                    return stand;
                })
                .addEquipment(EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(item))
                .build()
            );
        });
        Model none = data.get(PoseType.NONE);
        for (PoseType pose : PoseType.values())
            data.putIfAbsent(pose, none);
    }
}