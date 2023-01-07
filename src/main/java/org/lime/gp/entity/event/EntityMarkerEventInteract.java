package org.lime.gp.entity.event;

import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.display.Displays;
import org.lime.display.Models;
import org.lime.gp.entity.Entities;
import org.lime.gp.entity.component.display.EntityModelDisplay;

import java.util.Optional;

public class EntityMarkerEventInteract extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final EntityLimeMarker marker;
    private final int entityID;
    private final boolean isAttack;
    private final EquipmentSlot hand;
    private final boolean isPlayerSneaking;
    private final Models.Model.ChildDisplay<?> clickDisplay;
    private final EntityModelDisplay parentDisplay;

    protected EntityMarkerEventInteract(EntityLimeMarker marker, Models.Model.ChildDisplay<?> clickDisplay, EntityModelDisplay parentDisplay, Player player, int entityID, boolean isAttack, EquipmentSlot hand, boolean isPlayerSneaking) {
        super(player);
        this.marker = marker;
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
                Displays.byID(Models.Model.ChildDisplay.class, packet.getEntityId())
                        .flatMap(clickDisplay -> Optional.of(clickDisplay.objectParent())
                                .map(v -> v instanceof EntityModelDisplay emd ? emd : null)
                                .flatMap(parentDisplay -> Optional.ofNullable(Bukkit.getEntity(parentDisplay.key.entity_uuid()))
                                        .map(v -> v instanceof Marker marker ? marker : null)
                                        .flatMap(Entities::of)
                                        .flatMap(Entities::of)
                                        .map(metadata -> new EntityMarkerEventInteract(
                                                metadata.marker,
                                                clickDisplay,
                                                parentDisplay,
                                                player,
                                                packet.getEntityId(),
                                                packet.getActionType() == PacketPlayInUseEntity.b.ATTACK,
                                                enumHand == EnumHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND,
                                                packet.isUsingSecondaryAction())
                                        )
                                )
                        )
                        .ifPresent(Bukkit.getPluginManager()::callEvent);
            }
        });
    }

    public EntityLimeMarker getMarker() { return marker; }
    public int getEntityID() { return entityID; }
    public boolean isAttack() { return isAttack; }
    public EquipmentSlot getHand() { return hand; }
    public boolean isPlayerSneaking() { return isPlayerSneaking; }
    public Models.Model.ChildDisplay<?> getClickDisplay() { return clickDisplay; }
    public EntityModelDisplay getParentDisplay() { return parentDisplay; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}























