package org.lime.gp.module.npc.eplayer;

import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.lime.gp.lime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestEPlayer implements IEPlayer {
    public static final TestEPlayer Instance = new TestEPlayer();
    private TestEPlayer(){}

    @Override public String key() { return "...test..."; }
    @Override public Location location() { return new Location(lime.MainWorld, 5763, 73, -2909); }
    @Override public boolean isShow(UUID uuid) { return true; }
    @Override public GameProfile setSkin(GameProfile profile) { return profile; }
    @Override public Boolean single() { return null; }
    @Override public Pose pose() { return Pose.CRAWL; }
    @Override public void click(Player player, boolean isShift) {
        lime.logOP("Click: " + player.getName() + " - " + isShift);
    }
    @Override public List<Component> getDisplayName(Player player) { return List.of(Component.text("TEST CRAWL")); }
    @Override public Map<EnumItemSlot, ItemStack> createEquipment() { return Collections.emptyMap(); }
}
