package org.lime.gp.item.settings.list;

import com.google.gson.JsonPrimitive;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

import java.util.List;

@Setting(name = "block_eyes") public class BlockEyesSetting extends ItemSetting<JsonPrimitive> {
    public final boolean isBlock;
    public BlockEyesSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        isBlock = json.getAsBoolean();
    }

    private static final List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public static boolean isBlock(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (EquipmentSlot slot : slots)
            if (Items.getOptional(BlockEyesSetting.class, inventory.getItem(slot)).filter(v -> v.isBlock).isPresent())
                return true;
        return false;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.bool(), IComment.text("При надевании дает эффект слепоты и тьмы"));
    }
}