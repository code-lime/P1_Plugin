package org.lime.gp.block.component.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.PlayerInventory;
import org.lime.Position;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.Items;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.sound.Sounds;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@InfoComponent.Component(name = "menu")
public final class MenuComponent extends ComponentDynamic<JsonElement, MenuComponent.OpenInstance> {
    public static Optional<Apply> argsOf(Position position) {
        return Blocks.of(position.getBlock())
                .flatMap(Blocks::customOf)
                .map(MenuComponent::argsOf);
    }

    public static Apply argsOf(CustomTileMetadata metadata) {
        Position position = metadata.position();
        return Apply.of()
                .add(metadata.list(DisplayInstance.class).findAny().map(DisplayInstance::getAll).orElseGet(Collections::emptyMap))
                .add("block_uuid", metadata.key.uuid().toString())
                .add("block_pos", position.toSave())
                .add("block_pos_x", position.x + "")
                .add("block_pos_y", position.y + "")
                .add("block_pos_z", position.z + "");
    }

    public static class MenuData {
        public final String menu;
        public final String shift_menu;

        public String sound_open_any;
        public String sound_close_any;
        public String sound_open_once;
        public String sound_close_once;

        public final int open_timeout;
        public final HashMap<String, String> args = new HashMap<>();

        public MenuData(JsonObject json) {
            this.menu = json.get("menu").getAsString();
            this.shift_menu = json.has("shift_menu") ? json.get("shift_menu").isJsonNull() ? null : json.get("shift_menu").getAsString() : menu;
            this.open_timeout = json.has("open_timeout") ? json.get("open_timeout").getAsInt() : 10;

            this.sound_open_any = json.has("sound_open_any") ? json.get("sound_open_any").getAsString() : null;
            this.sound_close_any = json.has("sound_close_any") ? json.get("sound_close_any").getAsString() : null;
            this.sound_open_once = json.has("sound_open_once") ? json.get("sound_open_once").getAsString() : null;
            this.sound_close_once = json.has("sound_close_once") ? json.get("sound_close_once").getAsString() : null;

            if (json.has("args"))
                json.get("args").getAsJsonObject().entrySet().forEach(kv -> args.put(kv.getKey(), kv.getValue().getAsString()));
        }
    }

    public final HashMap<EnumDirection, MenuData> data = new HashMap<>();

    public MenuComponent(BlockInfo info, JsonElement json) {
        super(info, json);
        if (json instanceof JsonObject obj) {
            MenuData data = new MenuData(obj);
            for (EnumDirection item : EnumDirection.values())
                this.data.put(item, data);
        } else {
            json.getAsJsonArray().forEach(item -> {
                JsonObject _item = item.getAsJsonObject();
                MenuData data = new MenuData(_item);
                for (String str : _item.get("direction").getAsString().split(","))
                    this.data.put(EnumDirection.byName(str.toLowerCase()), data);
            });
        }
    }

    public final class OpenInstance extends BlockInstance implements CustomTileMetadata.Tickable, CustomTileMetadata.Interactable {
        public final HashMap<UUID, system.Toast2<Integer, MenuData>> open_list = new HashMap<>();
        private boolean init = true;

        public OpenInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
            super(component, metadata);
        }

        @Override
        public void read(JsonObjectOptional json) {
        }

        @Override
        public system.json.builder.object write() {
            return system.json.object();
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
            system.Toast1<String> sco = system.toast(null);
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
            MenuData menuData = MenuComponent.this.data.get(event.hit().getDirection());
            if (menuData == null) return EnumInteractionResult.PASS;
            PlayerInventory playerInventory = player.getInventory();
            Apply data = argsOf(metadata)
                    .add("mainhand_", Items.getStringData(playerInventory.getItemInMainHand()))
                    .add("offhand_", Items.getStringData(playerInventory.getItemInOffHand()))
                    .add("viewers", open_list.size() + "")
                    .add(menuData.args);
            //lime.logOP(system.toFormat(system.json.object().add(data.list()).build()));

            Location location = metadata.location(0.5, 0.5, 0.5);
            if (!MenuCreator.show(player, player.isSneaking() ? menuData.shift_menu : menuData.menu, data))
                return EnumInteractionResult.PASS;
            if (open_list.size() == 0) {
                Sounds.playSound(menuData.sound_open_once, location);
                metadata.list(DisplayInstance.class).forEach(variable -> variable.set("open", "true"));
            }
            Sounds.playSound(menuData.sound_open_any, location);
            open_list.put(player.getUniqueId(), system.toast(menuData.open_timeout, menuData));
            return EnumInteractionResult.CONSUME;
        }
    }

    @Override public OpenInstance createInstance(CustomTileMetadata metadata) { return new OpenInstance(this, metadata); }
    @Override public Class<OpenInstance> classInstance() { return OpenInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
