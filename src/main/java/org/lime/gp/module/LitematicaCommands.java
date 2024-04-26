package org.lime.gp.module;

import com.google.gson.JsonObject;
import io.papermc.paper.adventure.AdventureComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.commands.CommandFillEvent;
import net.minecraft.server.commands.CommandSetBlockEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lime.Position;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.Regex;
import org.lime.system.execute.Func1;
import org.lime.system.execute.Func2;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class LitematicaCommands implements Listener {
    public static CoreElement create() {
        return CoreElement.create(LitematicaCommands.class)
                .<JsonObject>addConfig("litematica", v -> v
                        .withInvoke(LitematicaCommands::config)
                        .withDefault(() -> json.object()
                                .addArray("blacklist", _v -> _v
                                        .add("(.*)COMMAND_BLOCK")
                                        .add(Material.STRUCTURE_BLOCK)
                                        .add(Material.STRUCTURE_VOID)
                                        .add(Material.JIGSAW))
                                .build()))
                .withInit(LitematicaCommands::init)
                .withInstance();
    }

    private static final HashSet<Material> blackList = new HashSet<>();

    private enum CacheType {
        NONE,
        DESTROYED,
        PLACED
    }
    private static final HashMap<Position, Player> cacheDestroy = new HashMap<>();

    private static void init() {
        lime.repeatTicks(LitematicaCommands::update, 1);
    }
    private static void config(JsonObject json) {
        HashSet<Material> blackList = new HashSet<>();
        List<Material> allMaterials = List.of(Material.values());
        json.getAsJsonArray("blacklist")
                .forEach(v -> Regex.filterRegex(allMaterials, Enum::name, v.getAsString())
                        .forEach(blackList::add));

        LitematicaCommands.blackList.clear();
        LitematicaCommands.blackList.addAll(blackList);
    }
    private static void update() {
        cacheDestroy.forEach((pos, player) -> CoreProtectHandle.logPlace(pos.getBlock(), player));
        cacheDestroy.clear();
    }

    private static <T>void onExecute(Cancellable event, CommandListenerWrapper wrapper, ArgumentTileLocation tile, Func2<WorldBorder, T, Boolean> isWithinBounds, T element) {
        CommandSender sender = wrapper.getBukkitSender();
        if (sender.isOp()) return;

        if (sender instanceof Player player && player.getGameMode() != GameMode.CREATIVE) {
            wrapper.sendFailure(new AdventureComponent(Component.text("Невозможно размещать блоки в этом игровом режиме").color(NamedTextColor.RED)));
            event.setCancelled(true);
            return;
        }

        if (!isWithinBounds.invoke(wrapper.getLevel().getWorldBorder(), element)) {
            wrapper.sendFailure(new AdventureComponent(Component.text("Невозможно размещать блоки за границей мира").color(NamedTextColor.RED)));
            event.setCancelled(true);
            return;
        }
        Block blockType = tile.getState().getBlock();
        if (blackList.contains(CraftMagicNumbers.getMaterial(blockType))) {
            wrapper.sendFailure(new AdventureComponent(Component.empty()
                    .append(Component.text("Блок "))
                    .append(Component.text(blockType.getDescriptionId()).color(NamedTextColor.AQUA))
                    .append(Component.text(" запрещен для размещения"))
                    .color(NamedTextColor.RED)));
            event.setCancelled(true);
            return;
        }
    }
    private static <T>void onLog(CommandListenerWrapper wrapper, Func1<T, Stream<BlockPosition>> getter, T element) {
        CommandSender sender = wrapper.getBukkitSender();
        if (!(sender instanceof Player player))
            return;

        World world = wrapper.getBukkitWorld();
        getter.invoke(element)
                .forEach(pos -> {
                    Position position = new Position(world, pos.getX(), pos.getY(), pos.getZ());
                    cacheDestroy.put(position, player);
                    CoreProtectHandle.logDestroy(position.getBlock(), player);
                });
    }

    @EventHandler public static void on(CommandSetBlockEvent e) {
        onLog(e.source(), Stream::of, e.pos());
        onExecute(e, e.source(), e.block(), WorldBorder::isWithinBounds, e.pos());
    }
    @EventHandler public static void on(CommandFillEvent e) {
        onLog(e.source(), BlockPosition::betweenClosedStream, e.range());
        onExecute(e, e.source(), e.block(), (a,b) -> {
            Toast1<Boolean> outside = Toast.of(false);
            b.forAllCorners(corner -> {
                if (outside.val0) return;
                if (a.isWithinBounds(corner)) return;
                outside.val0 = true;
            });
            return !outside.val0;
        }, e.range());
    }
}
