package net.minecraft.world.entity.player;

import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerAttackMultiplyCriticalEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private float multiply;
    private final EntityHuman human;
    private final Entity target;

    protected PlayerAttackMultiplyCriticalEvent(float multiply, EntityHuman human, Entity target) {
        this.multiply = multiply;
        this.human = human;
        this.target = target;
    }
    public static float execute(float multiply, EntityHuman human, Entity target) {
        PlayerAttackMultiplyCriticalEvent event = new PlayerAttackMultiplyCriticalEvent(multiply, human, target);
        Bukkit.getPluginManager().callEvent(event);
        return event.getMultiply();
    }

    public EntityHuman getHuman() { return human; }
    public Entity getTarget() { return target; }
    public float getMultiply() { return multiply; }
    public void setMultiply(float multiply) { this.multiply = multiply; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
