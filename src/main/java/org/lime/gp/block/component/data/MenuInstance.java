package org.lime.gp.block.component.data;

import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.PlayerInventory;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.MenuComponent;
import org.lime.gp.block.component.list.SitComponent;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.sound.Sounds;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;
import org.lime.system.toast.Toast2;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public final class MenuInstance extends BlockComponentInstance<MenuComponent> implements CustomTileMetadata.Tickable, CustomTileMetadata.Interactable {
    public final HashMap<UUID, Toast2<Integer, MenuComponent.MenuData>> open_list = new HashMap<>();
    private boolean init = true;

    public MenuInstance(MenuComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    @Override
    public void read(JsonObjectOptional json) {
    }

    @Override
    public json.builder.object write() {
        return json.object();
    }

    @Override
    public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (init) {
            metadata.list(DisplayInstance.class).forEach(variable -> variable.set("open", "false"));
            init = false;
        }
        Location location = metadata.location(0.5, 0.5, 0.5);
        open_list.forEach((key, value) -> Optional.ofNullable(Bukkit.getPlayer(key))
                .filter(v -> v.getOpenInventory().getTopInventory().getType() == InventoryType.CHEST)
                .ifPresentOrElse(player -> value.val0 = value.val1.open_timeout, () -> value.val0--));
        Toast1<String> sco = Toast.of(null);
        if (open_list.values().removeIf(v -> {
            boolean remove = v.val0 <= 0;
            if (remove) {
                Sounds.playSound(v.val1.sound_close_any, location);
                sco.val0 = v.val1.sound_close_once;
            }
            return remove;
        }) && open_list.size() == 0) {
            Sounds.playSound(sco.val0, location);
            metadata.list(DisplayInstance.class).forEach(variable -> variable.set("open", "false"));
        }
    }

    @Override
    public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        if (!(event.player().getBukkitEntity() instanceof Player player)) return EnumInteractionResult.PASS;
        MenuComponent.MenuData menuData = component().data.get(event.hit().getDirection());
        if (menuData == null) return EnumInteractionResult.PASS;
        PlayerInventory playerInventory = player.getInventory();

        boolean isBlockSit = metadata.list(OtherGenericInstance.class)
                .findAny()
                .map(OtherGenericInstance::getStructureBlocks)
                .orElseGet(() -> metadata.list(MultiBlockInstance.class).flatMap(MultiBlockInstance::getStructureBlocks))
                .flatMap(v -> Blocks.customOf(v).stream())
                .anyMatch(v -> v.list(SitComponent.class).anyMatch(_v -> _v.isSit(player, v)));

        Apply data = MenuComponent.argsOf(metadata)
                .add("mainhand_", Items.getStringData(playerInventory.getItemInMainHand()))
                .add("offhand_", Items.getStringData(playerInventory.getItemInOffHand()))
                .add("viewers", String.valueOf(open_list.size()))
                .add("is_block_sit", String.valueOf(isBlockSit))
                .add(menuData.args);
        //lime.logOP(system.toFormat(json.object().add(data.list()).build()));

        Location location = metadata.location(0.5, 0.5, 0.5);
        if (!MenuCreator.show(player, player.isSneaking() ? menuData.shift_menu : menuData.menu, data))
            return EnumInteractionResult.PASS;
        if (open_list.size() == 0) {
            Sounds.playSound(menuData.sound_open_once, location);
            metadata.list(DisplayInstance.class).forEach(variable -> variable.set("open", "true"));
        }
        Sounds.playSound(menuData.sound_open_any, location);
        open_list.put(player.getUniqueId(), Toast.of(menuData.open_timeout, menuData));
        return EnumInteractionResult.CONSUME;
    }
}
