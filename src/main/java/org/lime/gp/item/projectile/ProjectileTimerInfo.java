package org.lime.gp.item.projectile;

public record ProjectileTimerInfo(int ticks, String item) {
    public ProjectileTimerInfo tick(int deltaTicks) { return new ProjectileTimerInfo(Math.max(ticks - deltaTicks, 0), item); }
}
