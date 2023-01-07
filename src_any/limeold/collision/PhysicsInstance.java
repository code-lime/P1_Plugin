package org.lime.gp.entity.component.data;

import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.display.transform.LocalLocation;
import org.lime.gp.display.transform.Transform;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInstance;
import org.lime.gp.entity.collision.Collider;
import org.lime.gp.entity.collision.HeightReader;
import org.lime.gp.entity.component.Components;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.lime;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PhysicsInstance extends EntityInstance implements CustomEntityMetadata.LazyTickable {
    public static core.element create() {
        return core.element.create(PhysicsInstance.class)
                .withInit(PhysicsInstance::init);
    }
    public static void init() {
        lime.nextTick(() -> {
            for (int i = 0; i < 100; i++) {
                LocalLocation local = new LocalLocation(system.rand(-100.0, 100.0), system.rand(-100.0, 100.0), system.rand(-100.0, 100.0), system.rand(0, 360), system.rand(0, 360));
                Location parent = new Location(null, system.rand(-200.0, 200.0), system.rand(-200.0, 200.0), system.rand(-200.0, 200.0), system.rand(0, 360), system.rand(0, 360));
                LocalLocation local_out = Transform.toLocal(parent, Transform.toWorld(parent, local));//Transform.toLocal(parent, Transform.toWorld(parent, Transform.toLocal(parent, Transform.toWorld(parent, local))));
                local.notEquals(local_out).ifPresent(lime::logOP);
            }
        });
    }

    @Override public Components.PhysicsComponent component() { return (Components.PhysicsComponent)super.component(); }
    public PhysicsInstance(Components.PhysicsComponent component, CustomEntityMetadata metadata) { super(component, metadata); }

    private interface Velocity {
        Location globalTo(Location base);
    }
    public void velocity(UUID uuid, LocalLocation local) {
        velocity.put(uuid, (base) -> Transform.toWorld(base, local));
    }
    public void velocity(UUID uuid, Location global) {
        velocity.put(uuid, (base) -> new Location(base.getWorld(),
                global.getX() + base.getX(), global.getY() + base.getY(), global.getZ() + base.getZ(),
                global.getYaw() + base.getYaw(), global.getPitch() + base.getPitch())
        );
    }
    public void velocityTo(UUID uuid, Location global) {
        velocity.put(uuid, (base) -> global.clone());
    }

    private final ConcurrentHashMap<UUID, Velocity> velocity = new ConcurrentHashMap<>();

    @Override public void onLazyTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        Components.PhysicsComponent component = component();

        component.collider.draw(metadata.location());
        Location location = metadata.location();

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = (location.getYaw() / 360) % 1;
        float pitch = (location.getPitch() / 360) % 1;

        velocity(this.unique(), new Location(null, 0, -component.gravity, 0));
        List<Location> moveToList = new ArrayList<>();
        this.velocity.values().removeIf(v -> { moveToList.add(v.globalTo(location)); return true; });
        for (Location moveToItem : moveToList) {
            x += moveToItem.getX();
            y += moveToItem.getY();
            z += moveToItem.getZ();
            yaw = (yaw + moveToItem.getYaw() / 360) % 1;
            pitch = (pitch + moveToItem.getPitch() / 360) % 1;
        }

        int mtlSize = moveToList.size() + 1;

        x /= mtlSize;
        y /= mtlSize;
        z /= mtlSize;
        yaw /= mtlSize;
        pitch /= mtlSize;

        Vector moveToDelta = new Vector(x,y,z).subtract(location.toVector());
        int length = (int)Math.ceil(moveToDelta.length());
        if (length != 0) {
            moveToDelta.divide(new Vector(length, length, length));
            yaw /= length;
            pitch /= length;
        } else {
            length = 1;
        }

        Location moveTo = location;

        for (int i = 0; i < length; i++) {
            moveTo.add(moveToDelta);
            moveTo.setYaw(yaw * 360 + moveTo.getYaw());
            moveTo.setPitch(pitch * 360 + moveTo.getPitch());
            metadata.moveTo(moveTo = component.collider.collide(new HeightReader(event.getWorld()), moveTo));
        }

        /*component.collider.collide(new HeightReader(event.getWorld()), velocity, component.gravity, metadata.location()).invoke((location) -> {
            metadata.moveTo(location);
        });*/
    }

    @Override public void read(JsonObject json) { }
    @Override public system.json.builder.object write() { return system.json.object(); }
}













