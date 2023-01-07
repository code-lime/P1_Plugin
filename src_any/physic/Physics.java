package org.lime.gp.physic;

import com.badlogic.gdx.physics.bullet.Bullet;
import org.lime.core;
import org.lime.gp.lime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Physics {
    public static core.element create() {
        return core.element.create(Physics.class)
                .disable()
                .withInit(Physics::init)
                .withUninit(Physics::uninit);
    }
    public interface PhysicListener {
        Stream<PhysicElement> elements();
    }
    public interface PhysicElement {
        int index();
        PhysicWorld.GameObject load(PhysicWorld world, PhysicWorld.GameObject object);
    }
    private static final List<PhysicListener> listeners = new ArrayList<>();
    public static void add(PhysicListener listener) {
        listeners.add(listener);
    }

    public static PhysicWorld world;

    public static void init() {
        Bullet.init();
        world = new PhysicWorld();
        lime.repeatTicks(Physics::tick, 1);
    }
    public static void tick() {
        List<Integer> indexes = new ArrayList<>();
        Map<Integer, PhysicWorld.GameObject> objects = world.objects();
        listeners.stream().flatMap(PhysicListener::elements).forEach(element -> {
            int index = element.index();
            indexes.add(element.load(world, index == -1 ? null : objects.get(index)).index);
        });
        world.objects().forEach((id, object) -> {
            if (indexes.contains(id)) return;
            object.dispose();
        });
        world.tick();
    }
    public static void uninit() {
        world.dispose();
    }
}






























