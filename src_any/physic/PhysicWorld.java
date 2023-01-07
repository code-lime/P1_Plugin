package org.lime.gp.physic;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;
import org.lime.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhysicWorld implements Disposable {
    private final static short GROUND_FLAG = 1 << 8;
    private final static short OBJECT_FLAG = 1 << 9;
    private final static short ALL_FLAG = -1;

    private int index = 0;
    private int nextIndex() { return index++; }

    private static class MyMotionState extends btMotionState {
        Matrix4 transform;
        @Override public void getWorldTransform(Matrix4 worldTrans) { worldTrans.set(transform); }
        @Override public void setWorldTransform(Matrix4 worldTrans) { transform.set(worldTrans); }
    }
    public class GameObject implements Disposable {
        public final btRigidBody body;
        private final MyMotionState motionState;
        public final btRigidBody.btRigidBodyConstructionInfo info;
        public final Matrix4 transform = new Matrix4();
        public final int index;

        private static btRigidBody.btRigidBodyConstructionInfo info(btCollisionShape shape, float mass) {
            Vector3 localInertia = new Vector3();
            if (mass > 0f) shape.calculateLocalInertia(mass, localInertia);
            else localInertia.set(0, 0, 0);
            return new btRigidBody.btRigidBodyConstructionInfo(mass, null, shape, localInertia);
        }
        public GameObject(btCollisionShape shape, float mass) {
            this(info(shape, mass));
        }
        public GameObject(btRigidBody.btRigidBodyConstructionInfo constructionInfo) {
            info = constructionInfo;
            motionState = new MyMotionState();
            motionState.transform = transform;
            body = new btRigidBody(constructionInfo);
            body.setMotionState(motionState);

            index = nextIndex();
            instances.put(index, this);
            dynamicsWorld.addRigidBody(body);
        }

        public GameObject setGround() {
            body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
            body.setContactCallbackFlag(GROUND_FLAG);
            body.setContactCallbackFilter(0);
            body.setActivationState(Collision.DISABLE_DEACTIVATION);
            return this;
        }
        public GameObject setMotion() {
            body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
            body.setContactCallbackFlag(OBJECT_FLAG);
            body.setContactCallbackFilter(GROUND_FLAG);
            return this;
        }
        public GameObject transform(system.Action1<Matrix4> modify) {
            modify.invoke(transform);
            return this;
        }

        @Override public void dispose() {
            instances.remove(index);
            dynamicsWorld.removeRigidBody(body);

            info.dispose();
            body.dispose();
            motionState.dispose();
        }
    }

    private final Map<Integer, GameObject> instances = new HashMap<>();

    public Map<Integer, GameObject> objects() { return new HashMap<>(instances); }

    private final btCollisionConfiguration collisionConfig;
    private final btDispatcher dispatcher;
    private final btBroadphaseInterface broadphase;
    private final btDynamicsWorld dynamicsWorld;
    private final btConstraintSolver constraintSolver;

    public final btCollisionShape BOX_COLLISION = new btBoxShape(new Vector3(1.0F, 1.0F, 1.0F));

    public PhysicWorld() {
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        constraintSolver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -10f, 0));
    }

    private long lastSim = System.nanoTime();
    public void tick() {
        long now = System.nanoTime();
        float delta = (float)(now - this.lastSim) / (float) TimeUnit.SECONDS.toNanos(1L);
        this.dynamicsWorld.stepSimulation(delta, 100);
        this.lastSim = now;
    }

    @Override public void dispose() {
        for (GameObject obj : new ArrayList<>(instances.values())) obj.dispose();

        dynamicsWorld.dispose();
        constraintSolver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfig.dispose();
        BOX_COLLISION.dispose();
    }
}


















