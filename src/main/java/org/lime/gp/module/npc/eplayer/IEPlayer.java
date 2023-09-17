package org.lime.gp.module.npc.eplayer;

import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IEPlayer {
    String key();
    Location location();

    boolean isShow(UUID uuid);
    GameProfile setSkin(GameProfile profile);
    Boolean single();
    Pose pose();
    void click(Player player, boolean isShift);
    List<Component> getDisplayName(Player player);
    Map<EnumItemSlot, ItemStack> createEquipment();
}
