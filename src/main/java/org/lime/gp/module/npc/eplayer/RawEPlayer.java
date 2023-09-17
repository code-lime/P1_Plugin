package org.lime.gp.module.npc.eplayer;

import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.lime.gp.player.module.Skins;
import org.lime.system.execute.Action2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RawEPlayer(
        String key,
        Location location,
        Boolean single,
        Pose pose,
        Action2<Player, Boolean> onClick,
        List<Component> displayName,
        Map<EnumItemSlot, ItemStack> equipment,
        Skins.Property skin
) implements IEPlayer {
    @Override public boolean isShow(UUID uuid) { return true; }
    @Override public GameProfile setSkin(GameProfile profile) {
        if (skin == null) return profile;
        Skins.setProfile(profile, skin, false);
        return profile;
    }
    @Override public void click(Player player, boolean isShift) { onClick.invoke(player, isShift); }
    @Override public List<Component> getDisplayName(Player player) { return new ArrayList<>(displayName); }
    @Override public Map<EnumItemSlot, ItemStack> createEquipment() { return equipment; }
}
