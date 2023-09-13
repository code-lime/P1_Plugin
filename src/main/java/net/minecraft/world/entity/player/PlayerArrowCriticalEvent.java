package net.minecraft.world.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerArrowCriticalEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private int damage;
    private int criticalAppend;
    private final boolean critical;
    private final EntityArrow arrow;
    private final MovingObjectPositionEntity hit;

    protected PlayerArrowCriticalEvent(boolean critical, int damage, EntityArrow arrow, MovingObjectPositionEntity hit) {
        this.damage = damage;
        this.criticalAppend = critical ? Entity.SHARED_RANDOM.nextInt(damage / 2 + 2) : 0;
        this.critical = critical;
        this.arrow = arrow;
        this.hit = hit;
    }

    public static int execute(boolean critical, int damage, EntityArrow arrow, MovingObjectPositionEntity hit) {
        PlayerArrowCriticalEvent event = new PlayerArrowCriticalEvent(critical, damage, arrow, hit);
        Bukkit.getPluginManager().callEvent(event);
        return (int)Math.min(event.getDamage() + (long)event.getCriticalAppend(), Integer.MAX_VALUE);
    }

    public boolean isCritical() { return critical; }
    public EntityArrow getArrow() { return arrow; }
    public MovingObjectPositionEntity getHit() { return hit; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public int getCriticalAppend() { return criticalAppend; }
    public void setCriticalAppend(int criticalAppend) { this.criticalAppend = damage; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
