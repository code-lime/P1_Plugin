package net.minecraft.server.commands;

import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class CommandFillEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final CommandListenerWrapper source;
    private final StructureBoundingBox range;
    private final ArgumentTileLocation block;
    private final @Nullable Predicate<ShapeDetectorBlock> filter;

    private boolean cancelled = false;

    protected CommandFillEvent(CommandListenerWrapper source, StructureBoundingBox range, ArgumentTileLocation block, @Nullable Predicate<ShapeDetectorBlock> filter) {
        this.source = source;
        this.range = range;
        this.block = block;
        this.filter = filter;
    }
    public static boolean execute(CommandListenerWrapper source, StructureBoundingBox range, ArgumentTileLocation block, @Nullable Predicate<ShapeDetectorBlock> filter) {
        CommandFillEvent event = new CommandFillEvent(source, range, block, filter);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    public CommandListenerWrapper source() { return this.source; }
    public StructureBoundingBox range() { return this.range; }
    public ArgumentTileLocation block() { return this.block; }
    public Optional<Predicate<ShapeDetectorBlock>> filter() { return Optional.ofNullable(this.filter); }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override public @Nonnull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
