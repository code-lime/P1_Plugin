package net.minecraft.world.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.item.EnumAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DamageSourceBlockEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final EntityLiving entity;
    private final DamageSource source;
    private boolean blocking;

    protected DamageSourceBlockEvent(EntityLiving entity, DamageSource source, boolean blocking) {
        this.entity = entity;
        this.source = source;
        this.blocking = blocking;
    }

    public static boolean execute(EntityLiving entityLiving, DamageSource source) {
        boolean flag = source.getDirectEntity() instanceof EntityArrow arrow && arrow.getPierceLevel() > 0;
        Vec3D vec3d = source.getSourcePosition();
        DamageSourceBlockEvent event = new DamageSourceBlockEvent(entityLiving, source, !source.is(DamageTypeTags.BYPASSES_SHIELD) && entityLiving.isBlocking() && !flag && vec3d != null);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isBlocking()) {
            Vec3D vec3d1 = entityLiving.getViewVector(1.0f);
            Vec3D vec3d2 = vec3d.vectorTo(entityLiving.position()).normalize();
            vec3d2 = new Vec3D(vec3d2.x, 0.0, vec3d2.z);
            return vec3d2.dot(vec3d1) < 0.0;
        }
        return false;
    }

    public EntityLiving getEntity() { return entity; }
    public DamageSource getSource() { return source; }
    public ItemStack getShield() {
        if (!entity.isUsingItem()) return ItemStack.EMPTY;
        ItemStack shield = entity.useItem;
        if (shield.isEmpty()) return ItemStack.EMPTY;
        if (shield.getUseAnimation() != EnumAnimation.BLOCK) return ItemStack.EMPTY;
        return shield;
    }
    public boolean isBlocking() { return blocking; }
    public void setBlocking(boolean blocking) { this.blocking = blocking; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
