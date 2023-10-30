package org.lime.gp.entity.event;

import net.minecraft.network.protocol.game.PacketPlayInSteerVehicle;
import net.minecraft.world.entity.EntityLimeMarker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.lime.display.Displays;
import org.lime.display.Passenger;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.gp.entity.Entities;
import org.lime.gp.entity.component.display.display.EntityModelDisplay;

import java.util.Optional;

public class EntityMarkerEventInput extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final EntityLimeMarker marker;
    private final int entityID;
    private final float sidewaysSpeed;
    private final float forwardSpeed;
    private final boolean jumping;
    private final boolean sneaking;
    private final BaseChildDisplay<?, ?, ?> inputDisplay;
    private final EntityModelDisplay parentDisplay;

    protected EntityMarkerEventInput(EntityLimeMarker marker, BaseChildDisplay<?, ?, ?> inputDisplay, EntityModelDisplay parentDisplay, Player player, int entityID, float sidewaysSpeed, float forwardSpeed, boolean jumping, boolean sneaking) {
        super(player);
        this.marker = marker;
        this.inputDisplay = inputDisplay;
        this.parentDisplay = parentDisplay;
        this.entityID = entityID;
        this.sidewaysSpeed = sidewaysSpeed;
        this.forwardSpeed = forwardSpeed;
        this.jumping = jumping;
        this.sneaking = sneaking;
    }
    public static void execute(Player player, PacketPlayInSteerVehicle packet) {
        Passenger.getVehicle(player.getEntityId())
                .flatMap(id -> Displays.byID(BaseChildDisplay.class, id)
                        .flatMap(inputDisplay -> Optional.of(inputDisplay.objectParent())
                                .map(v -> v instanceof EntityModelDisplay emd ? emd : null)
                                .flatMap(parentDisplay -> Optional.ofNullable(Bukkit.getEntity(parentDisplay.key.entity()))
                                        .map(v -> v instanceof Marker marker ? marker : null)
                                        .flatMap(Entities::of)
                                        .flatMap(Entities::of)
                                        .map(metadata -> new EntityMarkerEventInput(
                                                metadata.marker,
                                                inputDisplay,
                                                parentDisplay,
                                                player,
                                                id,
                                                packet.getXxa(),
                                                packet.getZza(),
                                                packet.isJumping(),
                                                packet.isShiftKeyDown()
                                        ))
                                )
                        )
                )
                .ifPresent(Bukkit.getPluginManager()::callEvent);
    }

    public EntityLimeMarker getMarker() { return marker; }
    public int getEntityID() { return entityID; }
    public float getSidewaysSpeed() { return sidewaysSpeed; }
    public float getForwardSpeed() { return forwardSpeed; }
    public boolean isJumping() { return jumping; }
    public boolean isSneaking() { return sneaking; }
    public BaseChildDisplay<?, ?, ?> getInputDisplay() { return inputDisplay; }
    public EntityModelDisplay getParentDisplay() { return parentDisplay; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}























