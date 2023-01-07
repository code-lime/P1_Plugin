package org.lime.gp.physic;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.display.Displays;
import org.lime.gp.lime;

import java.util.ArrayList;
import java.util.List;

public class TempPhysics {
    public static core.element create() {
        return core.element.create(TempPhysics.class)
                .disable()
                .withInit(TempPhysics::init);
    }
    public static final Position position = new Position(lime.MainWorld, -461, 72, -204);
    public static class Block implements Physics.PhysicElement {
        public final Position position;
        public Block(Position position) {
            this.position = position;
        }

        public int index = -1;
        @Override public int index() {
            return index;
        }
        @Override public PhysicWorld.GameObject load(PhysicWorld world, PhysicWorld.GameObject object) {
            if (object == null) {
                object = world.new GameObject(world.BOX_COLLISION, 0).setGround().transform(transform -> transform.setTranslation(position.x, position.y, position.z));
            }
            return object;
        }
    }
    public static class Entity implements Physics.PhysicElement {
        public final Location location;
        public Entity(Location location) {
            this.location = location;
        }

        public int index = -1;
        @Override public int index() {
            return index;
        }
        @Override public PhysicWorld.GameObject load(PhysicWorld world, PhysicWorld.GameObject object) {
            if (object == null) {
                object = world.new GameObject(world.BOX_COLLISION, 5).setMotion().transform(transform -> transform.setTranslation((float)location.getX(), (float)location.getY(), (float)location.getZ()));
            }

            Vector3 pos = object.transform.getTranslation(new Vector3());
            Quaternion rot = object.transform.getRotation(new Quaternion());
            location.set(pos.x, pos.y, pos.z);
            location.setYaw(rot.getYaw());
            location.setPitch(rot.getPitch());

            if (location.getY() < position.y) entities.remove(this);

            Displays.drawPoint(location.toVector());
            return object;
        }
    }
    public static final List<Entity> entities = new ArrayList<>();
    public static void init() {
        AnyEvent.addEvent("tmp.ph.entity", AnyEvent.type.owner, p -> {
             entities.add(new Entity(p.getLocation()));
        });
        List<Block> blocks = new ArrayList<>();
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -1; z <= 4; z++) {
                    blocks.add(new Block(position.add(x,y,z)));
                }
            }
        }
        Physics.add(() -> blocks.stream().map(v -> v));
        Physics.add(() -> entities.stream().map(v -> v));
    }
}





















