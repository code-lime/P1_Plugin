package org.lime.gp.physic;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.EulerAngle;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class PhysicsModule implements Listener {
    private static boolean trackExplosions = true;
    private static boolean ENABLE = false;

    public static core.element create() {
        if (ENABLE) {
            PhysicsModule Instance = new PhysicsModule();
            return core.element.create(PhysicsModule.class)
                    .withInstance(Instance)
                    .withInit(Instance::onEnable)
                    .withUninit(Instance::onDisable);
        } else {
            return core.element.create(PhysicsModule.class)
                    .disable();
        }
    }

    private static final float armorBlockSize = 0.625F;
    private static final float armorBlockSizeH;

    public static float getArmorBlockSize() {
        return armorBlockSize;
    }

    public static float getArmorBlockSizeH() {
        return armorBlockSizeH;
    }

    public static EulerAngle quatToEul(Quaternion q) {
        float sqw = q.w * q.w;
        float sqx = q.x * q.x;
        float sqy = q.y * q.y;
        float sqz = q.z * q.z;
        float unit = sqx + sqy + sqz + sqw;
        float test = q.x * q.y + q.z * q.w;
        if ((double)test > 0.499D * (double)unit) {
            return new EulerAngle(1.5707963267948966D, (double)2 * Math.atan2(q.x, q.w), 0.0D);
        } else {
            return (double)test < -0.499D * (double)unit ? new EulerAngle(-1.5707963267948966D, (double)-2 * Math.atan2(q.x, q.w), 0.0D) : new EulerAngle(Math.atan2((float)2 * q.y * q.w - (float)2 * q.x * q.z, sqx - sqy - sqz + sqw), -Math.atan2((float)2 * q.x * q.w - (float)2 * q.y * q.z, -sqx + sqy - sqz + sqw), -Math.asin((float)2 * test / unit));
        }
    }

    static {
        armorBlockSizeH = armorBlockSize / (float)2;
    }

    private btDefaultCollisionConfiguration collisionConfig;
    private btCollisionDispatcher dispatcher;
    private btDbvtBroadphase broadphase;
    private btSequentialImpulseConstraintSolver solver;
    private btDiscreteDynamicsWorld dynamicsWorld;
    private final btBoxShape boxCollision;
    private final Vector3 boxInertia;
    private final btBoxShape boxStaticCollision;
    private final btBoxShape playerCollision;
    private final ArrayList<PBlock> blocks;
    private final HashMap<Player, btRigidBody> players;
    private long lastSim;
    private final ArrayList<btRigidBody> pool;
    private final HashSet<Location> visited;

    public btDefaultCollisionConfiguration getCollisionConfig() {
        return this.collisionConfig;
    }

    public void setCollisionConfig(btDefaultCollisionConfiguration var1) {
        this.collisionConfig = var1;
    }

    public btCollisionDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public void setDispatcher(btCollisionDispatcher var1) {
        this.dispatcher = var1;
    }

    public btDbvtBroadphase getBroadphase() {
        return this.broadphase;
    }

    public void setBroadphase(btDbvtBroadphase var1) {
        this.broadphase = var1;
    }

    public btSequentialImpulseConstraintSolver getSolver() {
        return this.solver;
    }

    public void setSolver(btSequentialImpulseConstraintSolver var1) {
        this.solver = var1;
    }

    public btDiscreteDynamicsWorld getDynamicsWorld() {
        return this.dynamicsWorld;
    }

    public void setDynamicsWorld(btDiscreteDynamicsWorld var1) {
        this.dynamicsWorld = var1;
    }

    public btBoxShape getBoxCollision() {
        return this.boxCollision;
    }

    public Vector3 getBoxInertia() {
        return this.boxInertia;
    }

    public btBoxShape getBoxStaticCollision() {
        return this.boxStaticCollision;
    }

    public btBoxShape getPlayerCollision() {
        return this.playerCollision;
    }

    public ArrayList<PBlock> getBlocks() {
        return this.blocks;
    }

    public HashMap<Player, btRigidBody> getPlayers() {
        return this.players;
    }

    public long getLastSim() {
        return this.lastSim;
    }

    public void setLastSim(long var1) {
        this.lastSim = var1;
    }

    public boolean getTrackExplosions() {
        return this.trackExplosions;
    }

    public void setTrackExplosions(boolean var1) {
        this.trackExplosions = var1;
    }

    public void onEnable() {
        this.dynamicsWorld.setGravity(new Vector3(0.0F, -10.0F, 0.0F));
        this.boxCollision.calculateLocalInertia(30.0F, this.boxInertia);
        lime.repeatTicks(PhysicsModule.this::stepSimulation, 1);
    }

    public void onDisable() {
        blocks.forEach(PBlock::kill);
    }

    public PhysicsAPI getAPI(Plugin owner) {
        return new PhysicsAPI(owner, this);
    }

    @EventHandler public void explode(final EntityExplodeEvent e) {
        if (this.trackExplosions) {
            Location var10000 = e.getLocation();
            final Location loc = var10000;
            this.blocks.forEach(it -> {
                Location location = it.getStand().getLocation();
                if (loc.distanceSquared(location) < (double)(e.getYield() * e.getYield() * e.getYield())) {
                    Vector3 vec = new Vector3((float)(loc.getX() - location.getX()), (float)(loc.getY() - location.getY()), (float)(loc.getZ() - location.getZ()));
                    vec.nor();
                    vec.scl(-e.getYield() * (float)30);
                    it.getBody().setLinearVelocity(vec);
                }
            });
            ListIterator<Block> ite = e.blockList().listIterator();

            while(ite.hasNext()) {
                Block it = ite.next();
                if (it.getType() != Material.TNT) {
                    var10000 = it.getLocation().add(0.5D, 0.5D, 0.5D);
                    Location l = var10000;
                    BlockState var10004 = it.getState();
                    org.lime.gp.physic.PBlock bl = new org.lime.gp.physic.PBlock(this, l, var10004);
                    this.blocks.add(bl);
                    Vector3 vec = new Vector3((float)(loc.getX() - l.getX()), (float)(loc.getY() - l.getY()), (float)(loc.getZ() - l.getZ()));
                    vec.nor();
                    vec.scl(-e.getYield() * (float)30);
                    bl.getBody().setLinearVelocity(vec);
                    it.setType(Material.AIR);
                    ite.remove();
                }
            }

        }
    }
    @EventHandler public void playerJoin(PlayerJoinEvent e) {
        Location loc = e.getPlayer().getLocation();
        Matrix4 transform = new Matrix4();
        transform.idt();
        transform.setTranslation((float)loc.getX(), (float)loc.getY() + 0.9F, (float)loc.getZ());
        btDefaultMotionState motionState = new btDefaultMotionState(transform);
        btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0.0F, motionState, this.playerCollision, new Vector3(0.0F, 0.0F, 0.0F));
        btRigidBody body = new btRigidBody(info);
        this.dynamicsWorld.addRigidBody(body);
        this.players.put(e.getPlayer(), body);
    }
    @EventHandler public void playerLeave(PlayerQuitEvent e) {
        this.dynamicsWorld.removeRigidBody(this.players.remove(e.getPlayer()));
    }

    public ArrayList<btRigidBody> getPool() {
        return this.pool;
    }

    public HashSet<Location> getVisited() {
        return this.visited;
    }

    public void stepSimulation() {
        system.Toast1<Integer> offset = system.toast(0);
        this.blocks.forEach(it -> {
            if (it.getBody().isActive()) {
                int y = -2;

                for(byte var3 = 2; y <= var3; ++y) {
                    int z = -2;

                    for(byte var5 = 2; z <= var5; ++z) {
                        int x = -2;

                        for(byte var7 = 2; x <= var7; ++x) {
                            Location loc = it.getLocation().clone().add(x, y, z);
                            Block b = loc.getBlock();
                            Location bloc = b.getLocation(loc);
                            if (b.getType() != Material.AIR && b.getType().isOccluding() && !PhysicsModule.this.getVisited().contains(bloc)) {
                                PhysicsModule.this.getVisited().add(bloc);
                                Matrix4 transform = new Matrix4();
                                transform.idt();
                                transform.setTranslation((float)loc.getBlockX() + 0.5F, (float)loc.getBlockY() + 0.5F, (float)loc.getBlockZ() + 0.5F);
                                if (offset.val0 >= PhysicsModule.this.getPool().size()) {
                                    btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(0.0F, null, PhysicsModule.this.getBoxStaticCollision(), new Vector3(0.0F, 0.0F, 0.0F));
                                    btRigidBody body = new btRigidBody(info);
                                    body.setActivationState(0);
                                    body.setCollisionFlags(body.getCollisionFlags() | 1);
                                    PhysicsModule.this.getPool().add(body);
                                }

                                btRigidBody bodyx = PhysicsModule.this.getPool().get(offset.val0);
                                bodyx.setWorldTransform(transform);
                                PhysicsModule.this.getDynamicsWorld().removeRigidBody(bodyx);
                                PhysicsModule.this.getDynamicsWorld().addRigidBody(bodyx);
                                bodyx.setActivationState(0);
                                offset.val0++;
                            }
                        }
                    }
                }

            }
        });
        int i = offset.val0;
        int var3 = this.pool.size() - 1;
        if (i <= var3) {
            while(true) {
                this.pool.get(i).setActivationState(5);
                if (i == var3) break;
                ++i;
            }
        }

        this.players.forEach((p, body) -> {
            Location loc = p.getLocation();
            Matrix4 t = body.getCenterOfMassTransform();
            t.setTranslation(
                    (float) loc.getX(),
                    (float)loc.getY() + 0.9f,
                    (float)loc.getZ()
            );
            body.setCenterOfMassTransform(t);
            body.setActivationState(CollisionConstants.ACTIVE_TAG);
        });
        long now = System.nanoTime();
        float delta = (float)(now - this.lastSim) / (float)TimeUnit.SECONDS.toNanos(1L);
        this.dynamicsWorld.stepSimulation(delta, 100);
        this.lastSim = now;
        Iterator<PBlock> ite = this.blocks.iterator();

        while(ite.hasNext()) {
            org.lime.gp.physic.PBlock it = ite.next();
            it.tick(delta);
            if (!it.getBody().isActive() || it.getStand().isDead()) {
                it.kill();
            }

            if (it.getBody().isDisposed()) {
                ite.remove();
            }
        }

        this.visited.clear();
    }

    public PhysicsModule() {
        Bullet.init();
        this.collisionConfig = new btDefaultCollisionConfiguration();
        this.dispatcher = new btCollisionDispatcher(this.collisionConfig);
        this.broadphase = new btDbvtBroadphase();
        this.solver = new btSequentialImpulseConstraintSolver();
        this.dynamicsWorld = new btDiscreteDynamicsWorld(this.dispatcher, this.broadphase, this.solver, this.collisionConfig);
        this.boxCollision = new btBoxShape(new Vector3(getArmorBlockSizeH(), getArmorBlockSizeH(), getArmorBlockSizeH()));
        this.boxInertia = new Vector3(0.0F, 0.0F, 0.0F);
        this.boxStaticCollision = new btBoxShape(new Vector3(1.0F, 1.0F, 1.0F));
        this.playerCollision = new btBoxShape(new Vector3(0.15F, 0.9F, 0.15F));
        this.blocks = new ArrayList<>();
        this.players = new HashMap<>();
        this.lastSim = System.nanoTime();
        this.trackExplosions = true;
        this.pool = new ArrayList<>();
        this.visited = new HashSet<>();
    }
}
