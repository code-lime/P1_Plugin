package net.minecraft.server.commands;

import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class CommandSetBlockEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final CommandListenerWrapper source;
    private final BlockPosition pos;
    private final ArgumentTileLocation block;
    private final CommandSetBlock.Mode mode;
    private final @Nullable Predicate<ShapeDetectorBlock> condition;

    private boolean cancelled = false;

    protected CommandSetBlockEvent(CommandListenerWrapper source, BlockPosition pos, ArgumentTileLocation block, CommandSetBlock.Mode mode, @Nullable Predicate<ShapeDetectorBlock> condition) {
        this.source = source;
        this.pos = pos;
        this.block = block;
        this.mode = mode;
        this.condition = condition;
    }
    public static boolean execute(CommandListenerWrapper source, BlockPosition pos, ArgumentTileLocation block, CommandSetBlock.Mode mode, @Nullable Predicate<ShapeDetectorBlock> condition) {
        CommandSetBlockEvent event = new CommandSetBlockEvent(source, pos, block, mode, condition);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    public CommandListenerWrapper source() { return this.source; }
    public BlockPosition pos() { return this.pos; }
    public ArgumentTileLocation block() { return this.block; }
    public CommandSetBlock.Mode mode() { return this.mode; }
    public Optional<Predicate<ShapeDetectorBlock>> condition() { return Optional.ofNullable(this.condition); }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override public @Nonnull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
