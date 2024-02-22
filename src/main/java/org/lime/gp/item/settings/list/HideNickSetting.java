package org.lime.gp.item.settings.list;

import java.util.List;

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
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "hide_nick") public class HideNickSetting extends ItemSetting<JsonPrimitive> {
    public final boolean isHide;
    public HideNickSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        isHide = json.getAsBoolean();
    }

    private static final List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public static boolean isHide(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (EquipmentSlot slot : slots)
            if (Items.getOptional(HideNickSetting.class, inventory.getItem(slot)).filter(v -> v.isHide).isPresent())
                return true;
        return false;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.bool(), IComment.text("Указывает, скрывать ли ник игрока при надевании"));
    }
}