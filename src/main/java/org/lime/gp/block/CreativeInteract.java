package org.lime.gp.block;

import com.google.common.collect.Streams;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.commands.data.CommandData;
import net.minecraft.world.item.ItemHorseArmor;
import net.minecraft.world.item.ItemHorseArmorDyeable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.util.BlockIterator;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.block.component.data.OtherGenericInstance;
import org.lime.gp.block.component.display.CacheBlockDisplay;
import org.lime.gp.block.component.display.block.IBlock;
import org.lime.gp.block.component.list.LootComponent;
import org.lime.gp.chat.Apply;
import org.lime.gp.lime;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class CreativeInteract implements Listener {
    public static CoreElement create() {
        return CoreElement.create(CreativeInteract.class)
                .withInstance();
    }

    private static Stream<Block> line(LivingEntity living, int maxDistance) {
        return Streams.stream(new BlockIterator(living, Math.min(maxDistance, 120)));
    }

    @EventHandler public static void on(InventoryCreativeEvent e) {
        if (!(e.getWhoClicked() instanceof CraftPlayer player)) return;
        ItemStack item = CraftItemStack.asNMSCopy(e.getCursor());
        if (item.isEmpty()) return;
        NBTTagCompound tag = item.getTag();
        if (tag != null && (!tag.contains("BlockEntityTag", NBTBase.TAG_COMPOUND) || !tag.getCompound("BlockEntityTag").contains("PublicBukkitValues", NBTBase.TAG_COMPOUND) || !tag.getCompound("BlockEntityTag").getCompound("PublicBukkitValues").contains("lime:custom_block"))) return;

        UUID playerUUID = player.getUniqueId();
        UUID worldUUID = player.getWorld().getUID();

        line(e.getWhoClicked(), 6)
                .filter(v -> v.getType() == Material.SKELETON_SKULL)
                .map(Blocks::of)
                .flatMap(Optional::stream)
                .map(Blocks::customOf)
                .flatMap(Optional::stream)
                .flatMap(metadata -> CacheBlockDisplay.getCacheBlock(metadata.skull.getBlockPos(), worldUUID)
                        .map(v -> v.cache(playerUUID))
                        .flatMap(IBlock::data)
                        .map(IBlockData::getBukkitMaterial)
                        .filter(v -> e.getCursor().getType().equals(v))
                        .stream()
                        .map(v -> metadata.list(OtherGenericInstance.class).findFirst().flatMap(OtherGenericInstance::owner).flatMap(Blocks::customOf).orElse(metadata))
                )
                .flatMap(metadata -> metadata.list(LootComponent.class).flatMap(v -> v.generateItems(Apply.of())))
                .findFirst()
                .ifPresent(out -> {
                    e.setResult(Event.Result.ALLOW);
                    e.setCursor(out);
                    lime.nextTick(player.getHandle().inventoryMenu::broadcastFullState);
                });
                //.filter(ExtMethods.filterLog("[Click.0] {0}"))
                /*.flatMap(metadata -> metadata.list(BlockDisplay.Displayable.class)
                        .filter(ExtMethods.filterLog("[Click.1] {0}"))
                        .flatMap(v -> v.onDisplayAsync(player, metadata.skull.getLevel(), metadata.skull.getBlockPos(), metadata.skull.getBlockState()).stream())
                        .filter(ExtMethods.filterLog("[Click.2] {0}"))
                        .map(BlockDisplay.IBlock::data)
                        .filter(ExtMethods.filterLog("[Click.3] {0}"))
                        .flatMap(Optional::stream)
                        .filter(ExtMethods.filterLog("[Click.4] {0}"))
                        .findFirst()
                        .filter(ExtMethods.filterLog("[Click.5] {0}"))
                        .map(IBlockData::getBukkitMaterial)
                        .filter(ExtMethods.filterLog("[Click.6] {0}"))
                        .filter(v -> e.getCursor().getType().equals(v))
                        .map(v -> metadata.list(OtherGenericInstance.class).findFirst().flatMap(OtherGenericInstance::owner).flatMap(Blocks::customOf).orElse(metadata))
                )
                .flatMap(metadata -> metadata.list(Components.LootComponent.class).flatMap(v -> v.generateItems(Apply.of())).findFirst())
                .ifPresent(out -> {
                    e.setResult(Event.Result.ALLOW);
                    e.setCursor(out);
                    lime.nextTick(player.getHandle().inventoryMenu::broadcastFullState);
                });*/
    }
}

























