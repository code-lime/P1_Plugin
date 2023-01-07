package org.lime.gp.physic;

import com.google.common.collect.Streams;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.display.Displays;
import org.lime.gp.lime;
import org.lime.system;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class Physics2 {
    public static core.element create() {
        return core.element.create(Physics2.class)
                .disable()
                .withInit(Physics2::init)
                .withUninit(Physics2::uninit);
    }
    private static final World world = new World();
    public static void init() {
        Car car = world.addCar(new Vector(-461, 72, -204), new Vector(1, 2, 3), Arrays.asList(
                new Car.Wheel(new Vector(0.5, -0.8, 1.5), true, 0.25),
                new Car.Wheel(new Vector(-0.5, -0.8, 1.5), true, 0.25),
                new Car.Wheel(new Vector(0.5, -0.8, -1.5), false, 0.25),
                new Car.Wheel(new Vector(-0.5, -0.8, -1.5), false, 0.25)
        ));
        AnyEvent.addEvent("tmp.input", AnyEvent.type.owner, v -> v.createParam("a","z",",",".","t","pause"), (p, cmd) -> {
            switch (cmd) {
                case "a" -> car.speed.edit0(v -> v + 0.3);
                case "z" -> car.speed.edit0(v -> v - 0.3);
                case "," -> car.rotate.edit0(v -> v - 0.5);
                case "." -> car.rotate.edit0(v -> v + 0.5);
                case "t" -> {
                    car.speed.set0(0.0);
                    car.rotate.set0(0.0);
                }
                case "pause" -> pause = !pause;
            }
        });
        /*lime.repeat(() -> {
            WorldServer world = ((CraftWorld)lime.MainWorld).getHandle();
            BlockPosition center = new BlockPosition(-461, 72, -204);
            for (int x = -10; x <= 10; x++)
                for (int y = -10; y <= 10; y++)
                    for (int z = -10; z <= 10; z++) {
                        BlockPosition pos = center.offset(x,y,z);
                        VoxelShape shape = world.getBlockState(pos).getShape(world, pos);
                        if (shape.isEmpty()) Physics2.world.worldBlocks.remove(pos);
                        else Physics2.world.worldBlocks.put(pos, shape.bounds());
                    }
        }, 0, 1);*/
        lime.timer().setAsync().withLoopTicks(1).withCallback(world::tick).run();
    }
    public static void uninit() {
        world.close();
    }

    public static boolean pause = false;

    public record GameObject(String name, DGeom geom, DBody body, DHinge2Joint joint) implements Closeable {
        public static GameObject create(DWorld world, DGeom geom, double mass) {
            return create(UUID.randomUUID().toString(), world, geom, mass);
        }
        public static GameObject create(String name, DWorld world, DGeom geom, double mass) {
            DBody body = OdeHelper.createBody(world);
            if (mass > 0) {
                DMass m = OdeHelper.createMass();
                m.adjust(mass);
                body.setMass(m);
            }
            geom.setBody(body);
            return new GameObject(name, geom, body, OdeHelper.createHinge2Joint (world,null));
        }

        public GameObject quaternion(system.Func1<DQuaternion, DQuaternion> func) {
            DQuaternion q = new DQuaternion();
            q.set(body.getQuaternion());
            body.setQuaternion(func.invoke(q));
            return this;
        }
        public GameObject position(system.Func1<DVector3C, DVector3C> func) {
            body.setPosition(func.invoke(body.getPosition()));
            return this;
        }
        public GameObject position(DVector3C pos) {
            body.setPosition(pos);
            return this;
        }

        public GameObject jointWith(GameObject gameObject, DVector3C anchor, boolean lockRotate) {
            joint.attach(gameObject.body, this.body);
            joint.setAnchor(anchor);
            joint.setAxis1(0,0,1);
            joint.setAxis2(0,1,0);
            joint.setParamSuspensionERP(0.4);
            joint.setParamSuspensionCFM(0.8);
            if (lockRotate) {
                joint.setParamLoStop(0);
                joint.setParamHiStop(0);
            }
            return this;
        }

        public void draw(double delta) {
            drawPoint(body.getPosition(), delta);
        }

        private static void drawPoint(DVector3C pos, double delta) {
            //Displays.drawPoint(new Vector(pos.get0() - 461, pos.get2() + 73, pos.get1() - 204), delta);
            Displays.drawPoint(new Vector(pos.get0(), pos.get2(), pos.get1()), delta);
        }

        private static void drawBox(DGeom geom, DVector3C size, double delta) {
            drawPoint(geom.getPosition(), delta);
            /*double sizeX = size.get0() / 2;
            double sizeY = size.get1() / 2;
            double sizeZ = size.get2() / 2;
            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = 0; z <= 1; z++) {
                        DVector3 pos = new DVector3();
                        geom.vectorToWorld(
                                (x == 0 ? -1 : 1) * sizeX,
                                (y == 0 ? -1 : 1) * sizeY,
                                (z == 0 ? -1 : 1) * sizeZ,
                                pos);
                        drawPoint(pos, delta);
                    }
                }
            }*/
        }
        private static void drawBox(DBody body, DVector3C size, double delta) {
            drawPoint(body.getPosition(), delta);
            /*double sizeX = size.get0() / 2;
            double sizeY = size.get1() / 2;
            double sizeZ = size.get2() / 2;
            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = 0; z <= 1; z++) {
                        DVector3 pos = new DVector3();
                        body.vectorToWorld(
                                (x == 0 ? -1 : 1) * sizeX,
                                (y == 0 ? -1 : 1) * sizeY,
                                (z == 0 ? -1 : 1) * sizeZ,
                                pos);
                        drawPoint(pos, delta);
                    }
                }
            }*/
        }


        @Override public void close() {
            geom.destroy();
            body.destroy();
            joint.destroy();
        }
    }

    public interface GameObjectParent {
        Stream<GameObject> gameObjects();
    }

    public static class Car implements GameObjectParent, Closeable {
        @Override public Stream<GameObject> gameObjects() { return objects.stream(); }

        public record DWheel(DVector3C pos, boolean isMotor, double radius) {}
        public record Wheel(Vector pos, boolean isMotor, double radius) {
            public DWheel dWheel() { return new DWheel(new DVector3(pos.getX(), pos.getZ(), pos.getY()), isMotor, radius); }
        }
        private final DWorld world;
        private final DSpace space;

        public final system.LockToast1<Double> speed = system.toast(0.0).lock();
        public final system.LockToast1<Double> rotate = system.toast(0.0).lock();

        private final List<GameObject> objects = new ArrayList<>();

        public final GameObject body;
        public final List<GameObject> move_wheels = new ArrayList<>();
        public final List<GameObject> motor_wheels = new ArrayList<>();

        public Car(DWorld world, DHashSpace space, DVector3C pos, DVector3C size, List<DWheel> wheels) {
            this.world = world;

            this.space = OdeHelper.createSimpleSpace(space);
            this.space.setCleanup(false);

            objects.add(body = GameObject.create("car.body", world, OdeHelper.createBox(null, size.get0(), size.get1(), size.get2()), 1).position(pos));

            wheels.forEach(wheel -> {
                GameObject wheel_object = GameObject.create("car.wheel." + (wheel.isMotor ? "motor" : "move"), world, OdeHelper.createSphere(null, wheel.radius), 0.2)
                        .quaternion(q -> {
                            q.setZero();
                            OdeMath.dQFromAxisAndAngle(q, 1, 0, 0, Math.PI * 0.5);
                            return q;
                        })
                        .position(wheel.pos)
                        .jointWith(body, wheel.pos, !wheel.isMotor);
                objects.add(wheel_object);
                (wheel.isMotor ? this.motor_wheels : this.move_wheels).add(wheel_object);
            });

            this.objects.forEach(v -> this.space.add(v.geom));
        }

        public void draw() {
            body.draw(0);
            motor_wheels.forEach(v -> v.draw(0.5));
            move_wheels.forEach(v -> v.draw(1));
        }

        @Override public void close() {
            this.objects.removeIf(v -> {
                v.close();
                return true;
            });
            this.space.destroy();
        }
    }
    public static class Block implements GameObjectParent, Closeable {
        private final DWorld world;
        
        private final GameObject object;
        public final AxisAlignedBB aabb;
        
        public Block(DWorld world, DHashSpace space, int x, int y, int z, AxisAlignedBB aabb) {
            this.world = world;
            this.aabb = aabb;
            Vec3D center = aabb.getCenter();
            object = GameObject.create("block_"+x+"_"+y+"_"+z, world, OdeHelper.createBox(space, aabb.getXsize(), aabb.getZsize(), aabb.getYsize()), 0)
                    .position(new DVector3(x + center.x, z + center.z, y + center.z));
        }

        @Override public void close() { this.object.close(); }
        @Override public Stream<GameObject> gameObjects() { return Stream.of(object); }
    }
    public static class World implements GameObjectParent, Closeable {
        public final ConcurrentHashMap<BlockPosition, AxisAlignedBB> worldBlocks = new ConcurrentHashMap<>();

        private final List<Car> cars = new ArrayList<>();
        private final Map<BlockPosition, Block> blocks = new HashMap<>();
        private static DJointGroup contactgroup;
        private final DWorld world;
        private final DHashSpace space;

        public World() {
            OdeHelper.initODE2(0);
            world = OdeHelper.createWorld();
            space = OdeHelper.createHashSpace(null);
            contactgroup = OdeHelper.createJointGroup();
            world.setGravity(0, 0, -0.5);
        }

        public Car addCar(Vector pos, Vector size, List<Car.Wheel> wheels) {
            Car car = new Car(world, space, new DVector3(pos.getX(), pos.getZ(), pos.getY()), new DVector3(size.getX(), size.getZ(), size.getY()), wheels.stream().map(Car.Wheel::dWheel).toList());
            cars.add(car);
            return car;
        }

        private void nearCallback(Object data, DGeom o1, DGeom o2) {
            int i,n;
            final int N = 10;
            DContactBuffer contacts = new DContactBuffer(N);
            n = OdeHelper.collide(o1,o2,N,contacts.getGeomBuffer());
            if (n > 0) {
                for (i=0; i<n; i++) {
                    DContact contact = contacts.get(i);
                    contact.surface.mode = OdeConstants.dContactSlip1
                            | OdeConstants.dContactSlip2
                            | OdeConstants.dContactSoftERP
                            | OdeConstants.dContactSoftCFM
                            | OdeConstants.dContactApprox1;
                    contact.surface.mu = OdeConstants.dInfinity;
                    contact.surface.slip1 = 0.1;
                    contact.surface.slip2 = 0.1;
                    contact.surface.soft_erp = 0.5;
                    contact.surface.soft_cfm = 0.3;
                    DJoint c = OdeHelper.createContactJoint(world, contactgroup, contact);
                    c.attach(contact.geom.g1.getBody(), contact.geom.g2.getBody());
                }
            }
        }
        public void tick(double delta) {
            if (!pause) {
                cars.forEach(car -> car.motor_wheels.forEach(motor_wheel -> {
                    motor_wheel.joint.setParamVel2(-car.speed.get0());
                    motor_wheel.joint.setParamFMax2(0.1);

                    // steering
                    double v = car.rotate.get0() - motor_wheel.joint.getAngle1();
                    if (v > 0.1) v = 0.1;
                    if (v < -0.1) v = -0.1;
                    v *= 10.0;
                    motor_wheel.joint.setParamVel(v);
                    motor_wheel.joint.setParamFMax(0.2);
                    motor_wheel.joint.setParamLoStop(-0.75);
                    motor_wheel.joint.setParamHiStop(0.75);
                    motor_wheel.joint.setParamFudgeFactor(0.1);
                }));
                List<BlockPosition> removed = new ArrayList<>(blocks.keySet());
                worldBlocks.forEach((pos, aabb) -> {
                    removed.remove(pos);
                    Block block = blocks.get(pos);
                    if (block != null) {
                        if (block.aabb.equals(aabb)) return;
                        blocks.remove(pos).close();
                    }
                    blocks.put(pos, new Block(world, space, pos.getX(), pos.getY(), pos.getZ(), aabb));
                });
                removed.forEach(pos -> blocks.remove(pos).close());

                space.collide(null, this::nearCallback);
                world.step(delta);

                contactgroup.empty();
            }

            cars.forEach(Car::draw);
        }

        @Override public void close() {
            this.cars.removeIf(v -> {
                v.close();
                return true;
            });
            this.blocks.values().removeIf(v -> {
                v.close();
                return true;
            });
        }
        @Override public Stream<GameObject> gameObjects() {
            return Streams.<GameObjectParent>concat(cars.stream(), blocks.values().stream()).flatMap(GameObjectParent::gameObjects);
        }
    }

    public static class Demo implements Closeable {
        // some constants

        private final double LENGTH = 0.7;	// chassis length
        private final double WIDTH = 0.5;	// chassis width
        private final double HEIGHT = 0.2;	// chassis height
        private final float RADIUS = 0.18f;	// wheel radius
        private final double STARTZ = 0.5;	// starting height of chassis
        private final double CMASS = 1;		// chassis mass
        private final double WMASS = 0.2;	// wheel mass

        // dynamics and collision objects (chassis, 3 wheels, environment)

        private static DWorld world;
        private static DSpace space;
        private static DBody[] body = new DBody[4];
        private static DHinge2Joint[] joint = new DHinge2Joint[3];	// joint[0] is the front wheel
        private static DJointGroup contactgroup;
        private static DPlane ground;
        private static DSpace car_space;
        private static DBox[] box = new DBox[1];
        private static DSphere[] sphere = new DSphere[3];
        private static DBox ground_box;


        // things that the user controls

        public static system.LockToast2<Double, Double> speed_steer = system.toast(0.0, 0.0).lock();  // user commands


        public Demo() {
            int i;
            DMass m = OdeHelper.createMass();

            // create world
            OdeHelper.initODE2(0);
            world = OdeHelper.createWorld();
            space = OdeHelper.createHashSpace(null);
            contactgroup = OdeHelper.createJointGroup();
            world.setGravity (0,0,-0.5);
            ground = OdeHelper.createPlane(space,0,0,1,0);

            // chassis body
            body[0] = OdeHelper.createBody(world);
            body[0].setPosition(0, 0, STARTZ);
            m.setBox(1, LENGTH, WIDTH, HEIGHT);
            m.adjust(CMASS);
            body[0].setMass(m);
            box[0] = OdeHelper.createBox (null,LENGTH,WIDTH,HEIGHT);
            box[0].setBody(body[0]);

            // wheel bodies
            for (i=1; i<=3; i++) {
                body[i] = OdeHelper.createBody(world);
                DQuaternion q = new DQuaternion();
                OdeMath.dQFromAxisAndAngle (q,1,0,0,Math.PI*0.5);
                body[i].setQuaternion(q);
                m.setSphere(1,RADIUS);
                m.adjust(WMASS);
                body[i].setMass(m);
                sphere[i-1] = OdeHelper.createSphere (null,RADIUS);
                sphere[i-1].setBody(body[i]);
            }
            body[1].setPosition(0.5*LENGTH,0,STARTZ-HEIGHT*0.5);
            body[2].setPosition(-0.5*LENGTH, WIDTH*0.5,STARTZ-HEIGHT*0.5);
            body[3].setPosition(-0.5*LENGTH,-WIDTH*0.5,STARTZ-HEIGHT*0.5);

            // front and back wheel hinges
            for (i=0; i<3; i++) {
                joint[i] = OdeHelper.createHinge2Joint (world,null);
                joint[i].attach(body[0],body[i+1]);
                final DVector3C a = body[i+1].getPosition();
                DHinge2Joint h2 = joint[i];
                h2.setAnchor (a);
                h2.setAxis1 (0,0,1);
                h2.setAxis2 (0,1,0);
            }

            // set joint suspension
            for (i=0; i<3; i++) {
                joint[i].setParamSuspensionERP (0.4);
                joint[i].setParamSuspensionCFM (0.8);
            }

            // lock back wheels along the steering axis
            for (i=1; i<3; i++) {
                // set stops to make sure wheels always stay in alignment
                joint[i].setParamLoStop (0);
                joint[i].setParamHiStop (0);
                // the following alternative method is no good as the wheels may get out
                // of alignment:
                //   dJointSetHinge2Param (joint[i],dParamVel,0);
                //   dJointSetHinge2Param (joint[i],dParamFMax,dInfinity);
            }

            // create car space and add it to the top level space
            car_space = OdeHelper.createSimpleSpace(space);
            car_space.setCleanup(false);
            car_space.add (box[0]);
            car_space.add (sphere[0]);
            car_space.add (sphere[1]);
            car_space.add (sphere[2]);

            // environment
            ground_box = OdeHelper.createBox (space,2,1.5,1);
            DMatrix3 R = new DMatrix3();
            OdeMath.dRFromAxisAndAngle (R,0,1,0,-0.15);
            ground_box.setPosition(2,0,-0.34);
            ground_box.setRotation(R);

        }

        private static void nearCallback (Object data, DGeom o1, DGeom o2) {
            int i,n;

            // only collide things with the ground
            boolean g1 = (o1 == ground || o1 == ground_box);
            boolean g2 = (o2 == ground || o2 == ground_box);
            if (g1 == g2) return;

            final int N = 10;
            //dContact contact[N];
            DContactBuffer contacts = new DContactBuffer(N);
//		n = dCollide (o1,o2,N,&contact[0].geom,sizeof(dContact));
            n = OdeHelper.collide (o1,o2,N,contacts.getGeomBuffer());//[0].geom,sizeof(dContact));
            if (n > 0) {
                for (i=0; i<n; i++) {
                    DContact contact = contacts.get(i);
                    contact.surface.mode = OdeConstants.dContactSlip1 | OdeConstants.dContactSlip2 |
                            OdeConstants.dContactSoftERP | OdeConstants.dContactSoftCFM | OdeConstants.dContactApprox1;
                    contact.surface.mu = OdeConstants.dInfinity;
                    contact.surface.slip1 = 0.1;
                    contact.surface.slip2 = 0.1;
                    contact.surface.soft_erp = 0.5;
                    contact.surface.soft_cfm = 0.3;
                    DJoint c = OdeHelper.createContactJoint (world,contactgroup,contact);
                    c.attach(
                            contact.geom.g1.getBody(),
                            contact.geom.g2.getBody());
                }
            }
        }

        public void tick(double delta) {
            int i;
            if (!pause) {
                // motor
                joint[0].setParamVel2 (-speed_steer.get0());
                joint[0].setParamFMax2 (0.1);

                // steering
                double v = speed_steer.get1() - joint[0].getAngle1();
                if (v > 0.1) v = 0.1;
                if (v < -0.1) v = -0.1;
                v *= 10.0;
                joint[0].setParamVel (v);
                joint[0].setParamFMax (0.2);
                joint[0].setParamLoStop (-0.75);
                joint[0].setParamHiStop (0.75);
                joint[0].setParamFudgeFactor (0.1);

                space.collide(null, Demo::nearCallback);
                world.step(0.05);

                // remove all contact joints
                contactgroup.empty();
            }

            drawBox(body[0], new DVector3(LENGTH,WIDTH,HEIGHT), 0);
            for (i=1; i<=3; i++) drawBox(body[i], new DVector3(RADIUS * 2, 0.02f, RADIUS * 2), 0.5);

            drawBox(ground_box, ground_box.getLengths(), 1);
        }

        private static void drawPoint(DVector3C pos, double delta) {
            Displays.drawPoint(new Vector(pos.get0() - 461, pos.get2() + 73, pos.get1() - 204), delta);
        }

        private static void drawBox(DGeom geom, DVector3C size, double delta) {
            drawPoint(geom.getPosition(), delta);
            /*double sizeX = size.get0() / 2;
            double sizeY = size.get1() / 2;
            double sizeZ = size.get2() / 2;
            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = 0; z <= 1; z++) {
                        DVector3 pos = new DVector3();
                        geom.vectorToWorld(
                                (x == 0 ? -1 : 1) * sizeX,
                                (y == 0 ? -1 : 1) * sizeY,
                                (z == 0 ? -1 : 1) * sizeZ,
                                pos);
                        drawPoint(pos, delta);
                    }
                }
            }*/
        }
        private static void drawBox(DBody body, DVector3C size, double delta) {
            drawPoint(body.getPosition(), delta);
            /*double sizeX = size.get0() / 2;
            double sizeY = size.get1() / 2;
            double sizeZ = size.get2() / 2;
            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = 0; z <= 1; z++) {
                        DVector3 pos = new DVector3();
                        body.vectorToWorld(
                                (x == 0 ? -1 : 1) * sizeX,
                                (y == 0 ? -1 : 1) * sizeY,
                                (z == 0 ? -1 : 1) * sizeZ,
                                pos);
                        drawPoint(pos, delta);
                    }
                }
            }*/
        }

        @Override public void close() {
            box[0].destroy();
            sphere[0].destroy();
            sphere[1].destroy();
            sphere[2].destroy();
            contactgroup.destroy();
            space.destroy();
            world.destroy();
            OdeHelper.closeODE();
        }
    }
}











