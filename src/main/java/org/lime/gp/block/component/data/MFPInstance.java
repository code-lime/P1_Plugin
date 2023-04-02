package org.lime.gp.block.component.data;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.util.Vector;
import org.lime.system;
import org.lime.display.Models;
import org.lime.display.transform.LocalLocation;
import org.lime.display.transform.Transform;
import org.lime.gp.lime;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.block.IModelBlock;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.MFPComponent;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.BookPaper;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.json.JsonObjectOptional;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.Vector3f;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;

public class MFPInstance extends BlockComponentInstance<MFPComponent> implements CustomTileMetadata.Damageable, CustomTileMetadata.Tickable, BlockDisplay.Displayable, CustomTileMetadata.Lootable {
    public MFPInstance(MFPComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        builder = lime.models.builder(EntityTypes.ARMOR_STAND)
                .local(new LocalLocation(component.offset).add(0, -0.4, -0.5, 0, 0))
                .nbt(() -> {
                    EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, lime.MainWorld.getHandle());
                    stand.setNoBasePlate(true);
                    stand.setSmall(true);
                    stand.setInvisible(true);
                    stand.setInvulnerable(true);
                    stand.setMarker(true);
                    stand.setHeadPose(new Vector3f(90, 0, 0));
                    return stand;
                });
        setItem(null, false);
    }

    private final Models.Builder builder;

    public final system.LockToast1<Models.Model> model = system.<Models.Model>toast(null).lock();
    private ItemStack head;

    public void setItem(ItemStack item, boolean save) {
        if (item == null) head = new ItemStack(Material.AIR);
        else head = item.clone();
        model.set0(builder.addEquipment(EnumItemSlot.HEAD, Items.getOptional(TableDisplaySetting.class, head)
                .flatMap(v -> v.of(TableDisplaySetting.TableType.converter, null))
                .map(v -> v.display(head))
                .orElseGet(() -> CraftItemStack.asNMSCopy(head))
        ).build());
        if (save) saveData();
        syncDisplayVariable();
    }

    @Override public void read(JsonObjectOptional json) {
        setItem(json.getAsString("item").map(system::loadItem).orElse(null), false);
    }
    @Override public system.json.builder.object write() {
        return system.json.object()
                .add("item", head.getType().isAir() ? null : system.saveItem(head));
    }

    @Override public void onDamage(CustomTileMetadata metadata, BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && !head.getType().isAir()) {
            Items.dropGiveItem(player, head, true);
            setItem(null, true);
        }
    }
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (head.getType().isAir()) {
            Location location = metadata().location(0.5, 1, 0.5);
            for (Item drop : location.getNearbyEntitiesByType(Item.class, 0.5)) {
                if (drop == null) continue;
                ItemStack item = drop.getItemStack();
                if (!item.hasItemMeta() || !(item.getItemMeta() instanceof BookMeta meta)) continue;
                ItemStack _item = item.asOne();
                item.subtract(1);
                if (item.getAmount() <= 0) PacketManager.getEntityHandle(drop).kill();
                setItem(_item, true);
                return;
            }
        }
    }
    @Override public Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data) {
        return Optional.of(IModelBlock.of(null, model.get0(), BlockDisplay.getChunkSize(10)));
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        if (!head.getType().isAir()) event.addItem(head);
    }
    
    private static final Pattern REMOVE_FORMATS = Pattern.compile("§.");
    private void syncDisplayVariable() {
        MFPComponent component = component();
        Optional<system.Toast4<String, Optional<String>, Optional<String>, Integer>> text = Optional.ofNullable(head)
            .filter(ItemStack::hasItemMeta)
            .map(ItemStack::getItemMeta)
            .map(v -> v instanceof BookMeta m ? m : null)
            .map(v -> system.toast(
                v.pages()
                    .stream()
                    .map(ChatHelper::getLegacyText)
                    .map(_v -> _v.replace("\t", ""))
                    .collect(Collectors.joining("\t")),
                Optional.ofNullable(v.author()).map(ChatHelper::getLegacyText),
                Optional.ofNullable(v.displayName())
                    .or(() -> Optional.ofNullable(v.title()))
                    .map(ChatHelper::getLegacyText)
                    .map(_v -> String.join("", REMOVE_FORMATS.split(_v))),
                BookPaper.getAuthorID(v)
            ));
        metadata()
            .list(DisplayInstance.class)
            .findAny()
            .ifPresent(display -> {
                display.set("mfp.has_book", text.isPresent() ? "true" : "false");
                display.set("mfp.book", text.map(v -> v.val0).orElse(""));
                display.set("mfp.book.author", text.flatMap(v -> v.val1).orElse(""));
                display.set("mfp.book.author_id", text.map(v -> v.val3).orElse(-1) + "");
                display.set("mfp.book.title", text.flatMap(v -> v.val2).orElse(""));

                double rotation = display.getRotation().orElse(InfoComponent.Rotation.Value.ANGLE_0).angle / 360.0;
                rotation += component.out_rotation / 360.0;
                rotation = (rotation % 1) * 360;
                if (rotation > 180) rotation -= 360;
                Vector rotated_offset = Transform.toWorld(new Location(null, 0, 0, 0, (float) rotation, 0), new LocalLocation(component.out_offset)).toVector();

                display.set("mfp.out", system.getString(metadata()
                    .location(rotated_offset.getX() + 0.5, rotated_offset.getY() + 0.5, rotated_offset.getZ() + 0.5)
                    .toVector()));
            });
    }
}













