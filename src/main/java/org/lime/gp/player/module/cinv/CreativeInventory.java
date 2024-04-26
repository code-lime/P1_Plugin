package org.lime.gp.player.module.cinv;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.inventory.ITileEntityContainer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.ServerOperator;
import org.lime.gp.admin.AnyEvent;
import org.lime.plugin.CoreElement;
import org.lime.gp.item.Items;

import javax.annotation.Nullable;

public class CreativeInventory {
    public static CoreElement create() {
        return CoreElement.create(CreativeInventory.class)
                .withInit(CreativeInventory::init)
                .addCommand("cinv", v -> v.withTab().withCheck(ServerOperator::isOp).withExecutor(sender -> {
                    if (!(sender instanceof CraftPlayer player)) return false;
                    return open(player);
                }));
    }

    private static void init() {
        AnyEvent.addEvent("cinv.search.open", AnyEvent.type.other, v -> v.createParam("simple", "full"), (p, v) -> {
            switch (v) {
                case "simple" -> openSearch(p, true);
                case "full" -> openSearch(p, false);
            }
        });
        AnyEvent.addEvent("cinv.search.open", AnyEvent.type.other, v -> v.createParam("simple", "full").createParam("REGEX"), (p, v, regex) -> {
            switch (v) {
                case "simple" -> openSearch(p, true, regex);
                case "full" -> openSearch(p, false, regex);
            }
        });
    }

    private static ITileInventory getMenuProvider(ITileEntityContainer init, IChatBaseComponent CONTAINER_TITLE) {
        return new TileInventory(init, CONTAINER_TITLE);
    }

    public static boolean open(Player player) {
        if (!player.isOp() || !(player instanceof CraftPlayer cplayer)) return false;
        cplayer.getHandle()
                .openMenu(getMenuProvider(
                        (syncId, inv, player1) -> ViewContainer.create(syncId, inv, new CreatorElement(Items.creators.values(), false), NonNullList.withSize(9*6, new ItemStack(Material.AIR)), ViewData.load(player1)),
                        IChatBaseComponent.literal("cinv")
                ));
        return true;
    }
    public static boolean openSearch(Player player, boolean simple) {
        return openSearch(player, simple, null);
    }
    public static boolean openSearch(Player player, boolean simple, @Nullable String regex) {
        if (!(simple || player.isOp()) || !(player instanceof CraftPlayer cplayer)) return false;
        EntityPlayer handler = cplayer.getHandle();
        ViewContainer container = ViewContainer.create(handler.nextContainerCounter(), handler.getInventory(), new CreatorElement(regex == null ? Items.creators.values() : Items.getValuesRegex(regex), simple), NonNullList.withSize(9*6, new ItemStack(Material.AIR)), ViewData.load(handler));
        container.setTitle(IChatBaseComponent.literal("cinv"));
        SearchQuery.openSearch(container, handler);
        return true;
    }
}
