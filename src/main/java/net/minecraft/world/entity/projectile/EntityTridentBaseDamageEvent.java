package net.minecraft.world.entity.projectile;

import net.minecraft.world.phys.MovingObjectPositionEntity;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EntityTridentBaseDamageEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final EntityThrownTrident trident;
    private final MovingObjectPositionEntity hit;
    private float damage = 8.0f;

    protected EntityTridentBaseDamageEvent(EntityThrownTrident trident, MovingObjectPositionEntity hit) {
        this.trident = trident;
        this.hit = hit;
    }

    public static float execute(EntityThrownTrident trident, MovingObjectPositionEntity hit) {
        EntityTridentBaseDamageEvent event = new EntityTridentBaseDamageEvent(trident, hit);
        Bukkit.getPluginManager().callEvent(event);
        return event.damage;
    }

    public EntityThrownTrident getTrident() { return trident; }
    public MovingObjectPositionEntity getHit() { return hit; }
    public float getDamage() { return damage; }
    public void setDamage(float damage) { this.damage = damage; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

}
