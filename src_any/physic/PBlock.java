// PBlock.java
package org.lime.gp.physic;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.Collection;

public final class PBlock {
   private final ArmorStand stand;
   private final btRigidBody body;
   private final Matrix4 transform;
   private Collection<ItemStack> drops;
   private ItemStack headItem;
   private float life;
   private final PhysicsModule plugin;
   private final Location location;

   public ArmorStand getStand() {
      return this.stand;
   }

   public btRigidBody getBody() {
      return this.body;
   }

   public Matrix4 getTransform() {
      return this.transform;
   }

   public Collection<ItemStack> getDrops() {
      return this.drops;
   }

   public void setDrops(Collection<ItemStack> var1) {
      this.drops = var1;
   }

   public ItemStack getHeadItem() {
      return this.headItem;
   }

   public void setHeadItem(ItemStack var1) {
      this.headItem = var1;
   }

   public float getLife() {
      return this.life;
   }

   public void setLife(float var1) {
      this.life = var1;
   }

   public void tick(float delta) {
      this.body.getMotionState().getWorldTransform(this.transform);
      Quaternion rot = new Quaternion();
      this.transform.getRotation(rot);
      EulerAngle eul = PhysicsModule.quatToEul(rot);
      this.stand.setHeadPose(eul);
      Vector3 origin = this.transform.getTranslation(new Vector3());
      this.location.setX((double)origin.x);
      this.location.setY((double)origin.y - (double)1.8F + (double)0.05F);
      this.location.setZ((double)origin.z);
      this.location.add(-Math.sin(eul.getZ()) * (double)(PhysicsModule.getArmorBlockSizeH() / (float)2), -Math.cos(eul.getX()) * (double)(PhysicsModule.getArmorBlockSizeH() / (float)2) - Math.cos(eul.getZ()) * (double)(PhysicsModule.getArmorBlockSizeH() / (float)2), -Math.sin(eul.getX()) * (double)(PhysicsModule.getArmorBlockSizeH() / (float)2));
      this.life -= delta;
      if (this.location.getY() < (double)-10 || this.location.getY() > (double)300 || this.stand.isDead() || this.life < (float)0) {
         this.stand.remove();
      }

      this.stand.teleport(this.location);
   }

   public void kill() {
      if (!this.body.isDisposed()) {
         this.stand.remove();
         this.plugin.getDynamicsWorld().removeRigidBody(this.body);
         final Location loc = this.stand.getLocation();
         this.drops.forEach(it -> loc.getWorld().dropItemNaturally(loc, it));
         this.body.dispose();
      }
   }

   public PhysicsModule getPlugin() {
      return this.plugin;
   }

   public Location getLocation() {
      return this.location;
   }

   private PBlock(PhysicsModule plugin, Location location) {
      this.plugin = plugin;
      this.location = location;
      this.transform = new Matrix4();
      this.drops = new ArrayList<>();
      this.headItem = new ItemStack(Material.STONE);
      this.life = 60.0F;
      this.location.setYaw(0.0F);
      this.location.setPitch(0.0F);
      this.location.add(0.0D, -1.8D, 0.0D);
      this.stand = this.location.getWorld().spawn(this.location, ArmorStand.class);
      this.stand.setGravity(false);
      this.stand.setHelmet(this.headItem);
      this.stand.setVisible(false);
      this.transform.idt();
      this.transform.setTranslation((float)this.location.getX(), (float)this.location.getY() + 1.8F, (float)this.location.getZ());
      btDefaultMotionState motionState = new btDefaultMotionState(this.transform);
      btRigidBodyConstructionInfo info = new btRigidBodyConstructionInfo(30.0F, (btMotionState)motionState, (btCollisionShape)this.plugin.getBoxCollision(), this.plugin.getBoxInertia());
      info.setAdditionalDamping(true);
      this.body = new btRigidBody(info);
      info.dispose();
      this.plugin.getDynamicsWorld().addRigidBody(this.body);
   }

   public PBlock(PhysicsModule plugin, Location location, BlockState block) {
      this(plugin, location);
      this.drops = block.getBlock().getDrops();
      this.headItem = block.getData().toItemStack();
      this.stand.setHelmet(this.headItem);
   }

   public PBlock(PhysicsModule plugin, Location location, ItemStack head) {
      this(plugin, location);
      this.headItem = head;
      this.stand.setHelmet(this.headItem);
   }

   public PBlock(PhysicsModule plugin, Location location, ItemStack head, Collection<ItemStack> drops) {
      this(plugin, location);
      this.headItem = head;
      this.drops = drops;
      this.stand.setHelmet(this.headItem);
   }
}