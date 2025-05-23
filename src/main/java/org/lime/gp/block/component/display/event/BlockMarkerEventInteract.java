package org.lime.gp.block.component.display.event;

import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.world.EnumHand;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.display.Displays;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.component.display.display.BlockModelDisplay;

import java.util.Optional;

public class BlockMarkerEventInteract extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final TileEntityLimeSkull skull;
    private final int entityID;
    private final boolean isAttack;
    private final EquipmentSlot hand;
    private final boolean isPlayerSneaking;
    private final BaseChildDisplay<?, ?, ?> clickDisplay;
    private final BlockModelDisplay parentDisplay;

    protected BlockMarkerEventInteract(TileEntityLimeSkull skull, BaseChildDisplay<?, ?, ?> clickDisplay, BlockModelDisplay parentDisplay, Player player, int entityID, boolean isAttack, EquipmentSlot hand, boolean isPlayerSneaking) {
        super(player);
        this.skull = skull;
        this.clickDisplay = clickDisplay;
        this.parentDisplay = parentDisplay;
        this.entityID = entityID;
        this.isAttack = isAttack;
        this.hand = hand;
        this.isPlayerSneaking = isPlayerSneaking;
    }
    public static void execute(Player player, PacketPlayInUseEntity packet) {
        packet.dispatch(new PacketPlayInUseEntity.c() {
            @Override public void onAttack() { onInteraction(EnumHand.MAIN_HAND); }
            @Override public void onInteraction(EnumHand enumHand) { onInteraction(enumHand, Vec3D.ZERO); }
            @Override public void onInteraction(EnumHand enumHand, Vec3D vec3D) {
                Displays.byID(BaseChildDisplay.class, packet.getEntityId())
                        .flatMap(clickDisplay -> Optional.of(clickDisplay.objectParent())
                                .map(v -> v instanceof BlockModelDisplay bmd ? bmd : null)
                                .flatMap(parentDisplay -> Blocks.of(parentDisplay.key.block_position().getBlock())
                                        .flatMap(Blocks::customOf)
                                        .map(metadata -> new BlockMarkerEventInteract(
                                                metadata.skull,
                                                clickDisplay,
                                                parentDisplay,
                                                player,
                                                packet.getEntityId(),
                                                packet.isAttack(),
                                                enumHand == EnumHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND,
                                                packet.isUsingSecondaryAction())
                                        )
                                )
                        )
                        .ifPresent(Bukkit.getPluginManager()::callEvent);
            }
        });
    }

    public TileEntityLimeSkull getSkull() { return skull; }
    public int getEntityID() { return entityID; }
    public boolean isAttack() { return isAttack; }
    public EquipmentSlot getHand() { return hand; }
    public boolean isPlayerSneaking() { return isPlayerSneaking; }
    public BaseChildDisplay<?, ?, ?> getClickDisplay() { return clickDisplay; }
    public BlockModelDisplay getParentDisplay() { return parentDisplay; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}