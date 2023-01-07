package org.lime.gp.physic;

import com.badlogic.gdx.math.Vector3;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class PhysicsBlock {
    private final PBlock block;
    private final PhysicsAPI api;

    public Location getLocation() {
        return this.block.getLocation().clone();
    }

    public void setLocation(Location loc) {
        this.block.getLocation().setX(loc.getX());
        this.block.getLocation().setY(loc.getY());
        this.block.getLocation().setZ(loc.getZ());
        this.block.getTransform().idt();
        this.block.getTransform().setTranslation((float)this.block.getLocation().getX(), (float)this.block.getLocation().getY() + 1.8F, (float)this.block.getLocation().getZ());
        this.block.getBody().setWorldTransform(this.block.getTransform());
    }

    public float getLife() {
        return this.block.getLife();
    }

    public void setLife(float life) {
        this.block.setLife(life);
    }

    public void applyForce(Vector vec) {
        this.block.getBody().setLinearVelocity(new Vector3((float)vec.getX(), (float)vec.getY(), (float)vec.getZ()));
    }

    public void kill() {
        this.block.getStand().remove();
    }

    public PhysicsBlock(PBlock block, PhysicsAPI api) {
        this.block = block;
        this.api = api;
    }
}
