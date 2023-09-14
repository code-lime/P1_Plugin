package org.lime.gp.item.cinv;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ITileEntityContainer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.persistence.PersistentDataContainer;
import org.lime.core;
import org.lime.gp.extension.JManager;
import org.lime.json.JsonObjectOptional;
import org.lime.plugin.CoreElement;
import org.lime.gp.item.Items;

public class CreativeInventory {
    public static CoreElement create() {
        return CoreElement.create(CreativeInventory.class)
                .addCommand("cinv", v -> v.withTab().withCheck(ServerOperator::isOp).withExecutor(sender -> {
                    if (!(sender instanceof CraftPlayer player)) return false;
                    /*Apply apply = UserRow.getBy(player.getUniqueId()).map(_v -> Apply.of().add(_v)).orElseGet(Apply::of);
                    List<Toast2<String, List<ItemStack>>> items = Items.creators.values()
                            .stream()
                            .map(creator -> Toast.of(creator.getKey(), system.funcEx(() -> creator.createItem(1, apply)).optional().invoke().orElse(null)))
                            .filter(_v -> _v.val1 != null && _v.val0 != null)
                            .collect(Collectors.groupingBy(kv -> kv.val0.contains(".") ? kv.val0.split("\\.")[0].toLowerCase() : "item"))
                            .entrySet()
                            .stream()
                            .map(kv -> Toast.of(kv.getKey(), kv.getValue().stream().map(_v -> _v.val1).toList()))
                            .collect(Collectors.toList());*/
                    return open(player);
                }));
    }

    private static ITileInventory getMenuProvider(ITileEntityContainer init, IChatBaseComponent CONTAINER_TITLE) {
        return new TileInventory(init, CONTAINER_TITLE);
    }

    public static boolean open(Player player) {
        if (!player.isOp() || !(player instanceof CraftPlayer cplayer)) return false;
        cplayer.getHandle()
                .openMenu(getMenuProvider(
                        (syncId, inv, player1) -> ViewContainer.create(syncId, inv, new CreatorElement(Items.creators.values()), NonNullList.withSize(9*6, new ItemStack(Material.AIR)), ViewData.load(player1)),
                        IChatBaseComponent.literal("cinv")
                ));
        return true;
    }
    public static boolean openSearch(Player player) {
        if (!player.isOp() || !(player instanceof CraftPlayer cplayer)) return false;
        EntityPlayer handler = cplayer.getHandle();
        ViewContainer container = ViewContainer.create(handler.nextContainerCounter(), handler.getInventory(), new CreatorElement(Items.creators.values()), NonNullList.withSize(9*6, new ItemStack(Material.AIR)), ViewData.load(handler));
        container.setTitle(IChatBaseComponent.literal("cinv"));
        Search.openSearch(container, handler);
        return true;
    }
}
