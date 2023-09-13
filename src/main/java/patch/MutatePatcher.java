package patch;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.data.worldgen.BiomeSettings;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.commands.CommandHelp;
import net.minecraft.server.commands.CommandMe;
import net.minecraft.server.commands.CommandTeamMsg;
import net.minecraft.server.commands.CommandTell;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.EntityCaveSpider;
import net.minecraft.world.entity.monster.EntityStrider;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerArrowCriticalEvent;
import net.minecraft.world.entity.player.PlayerAttackMultiplyCriticalEvent;
import net.minecraft.world.entity.player.PlayerAttackStrengthResetEvent;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.entity.projectile.EntityTridentBaseDamageEvent;
import net.minecraft.world.food.FoodMetaData;
import net.minecraft.world.food.IFoodNative;
import net.minecraft.world.inventory.ContainerGrindstone;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeRepair;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.SnowAccumulationHeightEvent;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeTemperatureEvent;
import net.minecraft.world.level.block.BlockSkull;
import net.minecraft.world.level.block.BlockSnow;
import net.minecraft.world.level.block.BlockSnowTickEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.lime.system;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import java.util.Arrays;
import java.util.List;

class MutatePatcher {
    private static final String STATIC_CONSTRUCTOR = "<clinit>";
    private static final String OBJECT_CONSTRUCTOR = "<init>";

    public static void patch(JarArchive version_archive, JarArchive bukkit_archive, JarArchive plugin_archive) {
        Native.log("TEST: " + Native.infoFromLambda(system.func(ItemCustomTool::new)));

        Native.log("Modify "+bukkit_archive.name+" jar...");
        bukkit_archive
                .patchMethod(IMethodFilter.of(system.func(org.bukkit.inventory.ItemStack::getMaxStackSize)),
                        MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                visitor.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "net/minecraft/world/item/ItemStackSizeEvent",
                                        "call_getMaxStackSize",
                                        Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(org.bukkit.inventory.ItemStack.class)),
                                        false
                                );
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();
                            }
                        }));

        Native.log("Modify "+version_archive.name+" jar...");
        version_archive
                .patchMethod(IMethodFilter.of(LootTable.class, OBJECT_CONSTRUCTOR, false),
                        MethodPatcher.none().access(v -> v | Opcodes.ACC_PUBLIC))
                .patchMethod(IMethodFilter.of(Blocks.class, STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            private final String from = BlockSkull.class.getName().replace('.', '/');
                            private final String to = from.replace("BlockSkull", "BlockLimeSkull");

                            @Override public void visitTypeInsn(int opcode, String type) {
                                if (type.equals(from)) {
                                    Native.log("Replace '" + from + "' to '" + to + "'");
                                    type = to;
                                }
                                super.visitTypeInsn(opcode, type);
                            }
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (owner.equals(from)) {
                                    Native.log("Replace '" + from + "' to '" + to + "' in method " + owner + "." + name + descriptor);
                                    owner = to;
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))
                .patchMethod(IMethodFilter.of(TileEntityTypes.class, STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
                                super.visitInvokeDynamicInsn(name, descriptor, handle, Arrays.stream(args).map(v -> {
                                    if (v instanceof Handle _handle && _handle.getOwner().equals(Type.getInternalName(TileEntitySkull.class))) {
                                        Native.log("Replace 'TileEntitySkull' to 'TileEntityLimeSkull' in " + v);
                                        return new Handle(_handle.getTag(), Type.getInternalName(TileEntityLimeSkull.class), _handle.getName(), _handle.getDesc(), _handle.isInterface());
                                    }
                                    else if (v instanceof Type _type) {
                                        Method method = new Method("tmp", _type.getDescriptor());
                                        if (method.getReturnType().equals(Type.getType(TileEntitySkull.class))) {
                                            Native.log("Replace 'TileEntitySkull' to 'TileEntityLimeSkull' in " + v);
                                            return Type.getMethodType(Type.getType(TileEntityLimeSkull.class), method.getArgumentTypes());
                                        }
                                    }
                                    return v;
                                }).toArray());
                            }
                        }))
                .patchMethod(IMethodFilter.of(EntityTypes.class, STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {

                            @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
                                super.visitInvokeDynamicInsn(name, descriptor, handle, Arrays.stream(args).map(v -> {
                                    if (v instanceof Handle _handle && _handle.getOwner().equals(Type.getInternalName(Marker.class))) {
                                        Native.log("Replace 'Marker' to 'EntityLimeMarker' in " + v);
                                        return new Handle(_handle.getTag(), Type.getInternalName(EntityLimeMarker.class), _handle.getName(), _handle.getDesc(), _handle.isInterface());
                                    }
                                    else if (v instanceof Type _type) {
                                        Method method = new Method("tmp", _type.getDescriptor());
                                        if (method.getReturnType().equals(Type.getType(Marker.class))) {
                                            Native.log("Replace 'Marker' to 'EntityLimeMarker' in " + v);
                                            return Type.getMethodType(Type.getType(EntityLimeMarker.class), method.getArgumentTypes());
                                        }
                                    }
                                    return v;
                                }).toArray());
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.func(CraftItemStack::getMaxStackSize)),
                        MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                Native.writeMethod(system.func(ItemStackSizeEvent::call_getMaxStackSizeBukkit), visitor::visitMethodInsn);
                                /*visitor.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "net/minecraft/world/item/ItemStackSizeEvent",
                                        "call_getMaxStackSizeBukkit",
                                        Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(org.bukkit.inventory.ItemStack.class)),
                                        false
                                );*/
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                            }
                        }));

        version_archive
                .of(net.minecraft.world.item.ItemStack.class)
                .patchMethod(IMethodFilter.of(system.func(net.minecraft.world.item.ItemStack::getMaxStackSize)),
                        MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                Native.writeMethod(system.func(ItemStackSizeEvent::call_getMaxStackSizeNMS), visitor::visitMethodInsn);
                                /*visitor.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "net/minecraft/world/item/ItemStackSizeEvent",
                                        "call_getMaxStackSizeNMS",
                                        Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(net.minecraft.world.item.ItemStack.class)),
                                        false
                                );*/
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.func(net.minecraft.world.item.ItemStack::getMaxDamage)),
                        MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                Native.writeMethod(system.func(ItemMaxDamageEvent::call_getMaxDamage), visitor::visitMethodInsn);
                                /*visitor.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "net/minecraft/world/item/ItemMaxDamageEvent",
                                        "call_getMaxDamage",
                                        Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(net.minecraft.world.item.ItemStack.class)),
                                        false
                                );*/
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                            }
                        }))
                .patch();

        version_archive
                .of(EntityLiving.class)
                .patchMethod(IMethodFilter.of(system.func(EntityLiving::getEquipmentSlotForItem)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            @Override public void visitInsn(int opcode) {
                                if (opcode == Opcodes.ARETURN) {
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    Native.writeMethod(system.func(EntityEquipmentSlotEvent::execute), super::visitMethodInsn);
                                    /*super.visitMethodInsn(
                                            Opcodes.INVOKESTATIC,
                                            "net/minecraft/world/entity/EntityEquipmentSlotEvent",
                                            "execute",
                                            Type.getMethodDescriptor(Type.getType(EnumItemSlot.class), Type.getType(EnumItemSlot.class), Type.getType(net.minecraft.world.item.ItemStack.class)),
                                            false
                                    );*/
                                    Native.log("Event added");
                                }
                                super.visitInsn(opcode);
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.func(EntityLiving::isDamageSourceBlocked)),
                        MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                            @Override public void visitCode() {
                                visitor.visitCode();

                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                Native.writeMethod(system.func(DamageSourceBlockEvent::execute), visitor::visitMethodInsn);
                                /*visitor.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "net/minecraft/world/entity/DamageSourceBlockEvent",
                                        "execute",
                                        Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(EntityLiving.class), Type.getType(DamageSource.class)),
                                        false
                                );*/

                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.func(EntityLiving::canDisableShield)),
                        MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitInsn(Opcodes.ICONST_0);
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                            }
                        }))
                .patch();

        version_archive
                .of(EntityHuman.class)
                .patchMethod(IMethodFilter.of(system.action(EntityHuman::attack)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            private int index = 0;
                            private int var_flag = 0;
                            @Override public void visitLineNumber(int line, Label start) {
                                super.visitLineNumber(line, start);
                                if (index == 2) {
                                    index++;
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitVarInsn(Opcodes.ILOAD, var_flag);
                                    Native.writeMethod(system.func(EntityAttackSweepEvent::execute), super::visitMethodInsn);
                                    super.visitVarInsn(Opcodes.ISTORE, var_flag);
                                    Native.log("Event added `EntityAttackSweepEvent`");
                                }
                            }
                            @Override public void visitVarInsn(int opcode, int var) {
                                if (opcode == Opcodes.ISTORE && index == 1)  {
                                    index++;
                                    var_flag = var;
                                }
                                super.visitVarInsn(opcode, var);
                            }
                            @Override public void visitTypeInsn(int opcode, String type) {
                                if (opcode == Opcodes.INSTANCEOF && index == 0 && type.equals(Type.getInternalName(ItemSword.class))) index++;
                                super.visitTypeInsn(opcode, type);
                            }

                            private int critical = 0;
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                if (critical == 0 && Native.isMethod(system.func(EntityHuman::isSprinting), owner, name, descriptor)) {
                                    critical = 1;
                                }
                            }

                            @Override public void visitLdcInsn(Object value) {
                                super.visitLdcInsn(value);
                                if (critical == 1 && value instanceof Float raw && raw == 1.5) {
                                    critical = 2;
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitVarInsn(Opcodes.ALOAD, 1);
                                    Native.writeMethod(system.func(PlayerAttackMultiplyCriticalEvent::execute), super::visitMethodInsn);
                                    Native.log("Event added `PlayerAttackMultiplyCriticalEvent`");
                                }
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.action(EntityHuman::tick)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (Native.isMethod(system.func(net.minecraft.world.item.ItemStack::isSame), owner, name, descriptor)) {
                                    super.visitInsn(Opcodes.POP2);
                                    super.visitInsn(Opcodes.ICONST_0);
                                    Native.log("Same removed");
                                } else {
                                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                }
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.action(EntityHuman::resetAttackStrengthTicker)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            @Override public void visitInsn(int opcode) {
                                if (opcode == Opcodes.RETURN) {
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    Native.writeMethod(system.action(PlayerAttackStrengthResetEvent::execute), super::visitMethodInsn);
                                    /*super.visitMethodInsn(
                                            Opcodes.INVOKESTATIC,
                                            "net/minecraft/world/entity/player/PlayerAttackStrengthResetEvent",
                                            "execute",
                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(EntityHuman.class)),
                                            false
                                    );*/
                                    Native.log("Event added");
                                }
                                super.visitInsn(opcode);
                            }
                        }))
                .patch();

        version_archive
                .patchMethod(IMethodFilter.of(system.func(EntityCaveSpider::doHurtTarget)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            @Override public void visitIntInsn(int opcode, int operand) {
                                if (opcode == Opcodes.BIPUSH) {
                                    Native.log("Replace value from '"+operand+"' to '0'");
                                    operand = 0;
                                }
                                super.visitIntInsn(opcode, operand);
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.func(RecipeRepair::assemble)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            private int index = 0;
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (opcode == Opcodes.INVOKEVIRTUAL && Native.isMethod(system.func(Item::canBeDepleted), owner, name, descriptor)) {
                                    index++;
                                } else if (opcode == Opcodes.INVOKEVIRTUAL && index == 2 && Native.isMethod(system.func(net.minecraft.world.item.ItemStack::getItem), owner, name, descriptor)) {
                                    index++;
                                    Native.writeMethod(system.func(ItemMaxDamageEvent::call_getMaxDamageItem), super::visitMethodInsn);
                                    /*super.visitMethodInsn(
                                            Opcodes.INVOKESTATIC,
                                            "net/minecraft/world/item/ItemMaxDamageEvent",
                                            "call_getMaxDamageItem",
                                            Type.getMethodDescriptor(Type.getType(Item.class), Type.getType(net.minecraft.world.item.ItemStack.class)),
                                            false
                                    );*/
                                    Native.log("Event added");
                                    return;
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))
                .patchMethod(IMethodFilter.of(ContainerGrindstone.class, "createResult", Type.getMethodType(Type.VOID_TYPE), true),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            private int index = 0;
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (opcode == Opcodes.INVOKEVIRTUAL && Native.isMethod(system.action(ContainerGrindstone::broadcastChanges), owner, name, descriptor)) {
                                    index++;
                                } else if (opcode == Opcodes.INVOKEVIRTUAL && index == 2 && Native.isMethod(system.func(net.minecraft.world.item.ItemStack::getItem), owner, name, descriptor)) {
                                    index++;
                                    Native.writeMethod(system.func(ItemMaxDamageEvent::call_getMaxDamageItem), super::visitMethodInsn);
                                    /*super.visitMethodInsn(
                                            Opcodes.INVOKESTATIC,
                                            "net/minecraft/world/item/ItemMaxDamageEvent",
                                            "call_getMaxDamageItem",
                                            Type.getMethodDescriptor(Type.getType(Item.class), Type.getType(net.minecraft.world.item.ItemStack.class)),
                                            false
                                    );*/
                                    Native.log("Event added");
                                    return;
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.action(BiomeSettings::addDefaultMonsterRoom)),
                        MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitInsn(Opcodes.RETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                            }
                        }));

        version_archive
                .of(FoodMetaData.class)
                .addInterface("net/minecraft/world/food/IFoodNative")
                .addField(Opcodes.ACC_PRIVATE, "nativeData", Type.getDescriptor(NBTTagCompound.class), "", null)
                .addMethod(MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitIntInsn(Opcodes.ALOAD, 0);
                        super.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/world/food/FoodMetaData", "nativeData", Type.getDescriptor(NBTTagCompound.class));
                        super.visitInsn(Opcodes.ARETURN);
                        super.visitMaxs(0, 0);
                        super.visitEnd();
                    }
                }), Opcodes.ACC_PUBLIC, "nativeData", Type.getMethodDescriptor(Type.getType(NBTTagCompound.class)), "", new String[0])
                .addMethod(MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitIntInsn(Opcodes.ALOAD, 0);
                        super.visitIntInsn(Opcodes.ALOAD, 1);
                        super.visitFieldInsn(Opcodes.PUTFIELD, "net/minecraft/world/food/FoodMetaData", "nativeData", Type.getDescriptor(NBTTagCompound.class));
                        super.visitInsn(Opcodes.RETURN);
                        super.visitMaxs(0, 0);
                        super.visitEnd();
                    }
                }), Opcodes.ACC_PUBLIC, "nativeData", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(NBTTagCompound.class)), "", new String[0])
                .patchMethod(IMethodFilter.of(system.action(FoodMetaData::readAdditionalSaveData)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            private int index = 0;
                            @Override public void visitLineNumber(int line, Label start) {
                                super.visitLineNumber(line, start);
                                if (index != 0) return;
                                index++;
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitVarInsn(Opcodes.ALOAD, 1);
                                Native.writeMethod(system.action(IFoodNative::readNativeSaveData), super::visitMethodInsn);
                                /*super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "net/minecraft/world/food/IFoodNative",
                                        "readNativeSaveData",
                                        Type.getMethodDescriptor(Type.VOID_TYPE, Native.replaceDescriptor(Type.getType(FoodMetaData.class), "FoodMetaData", "IFoodNative"), Type.getType(NBTTagCompound.class)),
                                        true
                                );*/
                                Native.log("Native added");
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.action(FoodMetaData::addAdditionalSaveData)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            private int index = 0;
                            @Override public void visitLineNumber(int line, Label start) {
                                super.visitLineNumber(line, start);
                                if (index != 0) return;
                                index++;
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitVarInsn(Opcodes.ALOAD, 1);
                                Native.writeMethod(system.action(IFoodNative::addNativeSaveData), super::visitMethodInsn);
                                /*super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "net/minecraft/world/food/IFoodNative",
                                        "addNativeSaveData",
                                        Type.getMethodDescriptor(Type.VOID_TYPE, Native.replaceDescriptor(Type.getType(FoodMetaData.class), "FoodMetaData", "IFoodNative"), Type.getType(NBTTagCompound.class)),
                                        true
                                );*/
                                Native.log("Native added");
                            }
                        }))
                .patch();

        version_archive
                .patchMethod(IMethodFilter.of(system.action(BlockSnow::randomTick)),
                        MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                visitor.visitIntInsn(Opcodes.ALOAD, 1);
                                visitor.visitIntInsn(Opcodes.ALOAD, 2);
                                visitor.visitIntInsn(Opcodes.ALOAD, 3);
                                visitor.visitIntInsn(Opcodes.ALOAD, 4);
                                Native.writeMethod(system.action(BlockSnowTickEvent::execute), visitor::visitMethodInsn);
                                /*visitor.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "net/minecraft/world/level/block/BlockSnowTickEvent",
                                        "execute",
                                        Type.getMethodDescriptor(Type.VOID_TYPE,
                                                Type.getType(BlockSnow.class),
                                                Type.getType(IBlockData.class),
                                                Type.getType(WorldServer.class),
                                                Type.getType(BlockPosition.class),
                                                Type.getType(RandomSource.class)
                                        ),
                                        false
                                );*/
                                visitor.visitInsn(Opcodes.RETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.action(WorldServer::tickChunk)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            private int snowAccumulation = 0;
                            @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                if (opcode == Opcodes.GETSTATIC) {
                                    if (Native.isField(system.func(() -> Blocks.ICE), owner, name, descriptor)) {
                                        Native.FieldInfo FROSTED_ICE = Native.infoFromField(system.func(() -> Blocks.FROSTED_ICE));
                                        super.visitFieldInsn(opcode, FROSTED_ICE.owner(), FROSTED_ICE.name(), FROSTED_ICE.descriptor());
                                        Native.log("Replace ice");
                                        return;
                                    } else if (snowAccumulation == 0 && Native.isField(system.func(() -> GameRules.RULE_SNOW_ACCUMULATION_HEIGHT), owner, name, descriptor)) {
                                        snowAccumulation = 1;
                                        Native.log("SnowAccumulation: 0 -> 1");
                                    }
                                }
                                super.visitFieldInsn(opcode, owner, name, descriptor);
                            }

                            private int maxHeightVar = -1;
                            @Override public void visitVarInsn(int opcode, int varIndex) {
                                if (snowAccumulation == 1 && opcode == Opcodes.ISTORE) {
                                    maxHeightVar = varIndex;
                                    snowAccumulation = 2;
                                    Native.log("SnowAccumulation: 1 -> 2");
                                }
                                super.visitVarInsn(opcode, varIndex);
                            }
                            @Override public void visitInsn(int opcode) {
                                if (snowAccumulation == 2 && opcode == Opcodes.POP) {
                                    super.visitIntInsn(Opcodes.ALOAD, 0);
                                    super.visitIntInsn(Opcodes.ALOAD, 1);
                                    super.visitIntInsn(Opcodes.ILOAD, maxHeightVar);
                                    Native.writeMethod(system.action(SnowAccumulationHeightEvent::execute), super::visitMethodInsn);
                                    super.visitIntInsn(Opcodes.ISTORE, maxHeightVar);
                                    snowAccumulation = 3;
                                    Native.log("SnowAccumulation: 2 -> 3");
                                    Native.log("Event SnowAccumulationHeightEvent added");
                                    return;
                                }
                                super.visitInsn(opcode);
                            }
                        }))
                .patchMethod(IMethodFilter.of(system.func(BiomeBase::getTemperature)),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            @Override public void visitInsn(int opcode) {
                                if (opcode == Opcodes.FRETURN) {
                                    super.visitIntInsn(Opcodes.ALOAD, 0);
                                    super.visitIntInsn(Opcodes.ALOAD, 1);
                                    Native.writeMethod(system.action(BiomeTemperatureEvent::execute), super::visitMethodInsn);
                                    /*super.visitMethodInsn(
                                            Opcodes.INVOKESTATIC,
                                            "net/minecraft/world/level/biome/BiomeTemperatureEvent",
                                            "execute",
                                            Type.getMethodDescriptor(Type.FLOAT_TYPE,
                                                    Type.FLOAT_TYPE,
                                                    Type.getType(BiomeBase.class),
                                                    Type.getType(BlockPosition.class)
                                            ),
                                            false
                                    );*/
                                    Native.log("Event added");
                                }
                                super.visitInsn(opcode);
                            }
                        }))
                .patchMethod(IMethodFilter.of(Items.class, STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            private boolean isWarped = false;

                            @Override public void visitLdcInsn(Object value) {
                                if ("warped_fungus_on_a_stick".equals(value)) isWarped = true;
                                super.visitLdcInsn(value);
                            }

                            @Override public void visitTypeInsn(int opcode, String type) {
                                if (opcode == Opcodes.NEW && type.equals(Type.getInternalName(InstrumentItem.class))) {
                                    type = Type.getInternalName(InstrumentSoundItem.class);
                                    Native.log("Replace `NEW` instrument instance");
                                } else if (isWarped && opcode == Opcodes.NEW && type.equals(Type.getInternalName(ItemCarrotStick.class))) {
                                    type = Type.getInternalName(ItemCustomTool.class);
                                    Native.log("Replace `NEW` carrot stick instance");
                                }
                                super.visitTypeInsn(opcode, type);
                            }

                            private interface CarrotAction<T extends Entity & ISteerable>
                                    extends system.Func3<Item.Info, EntityTypes<T>, Integer, ItemCarrotStick<T>> { }

                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (opcode == Opcodes.INVOKESPECIAL && Native.isMethod(system.func(InstrumentItem::new), owner, name, descriptor)) {
                                    Native.writeMethod(system.func(InstrumentSoundItem::new), super::visitMethodInsn);
                                    Native.log("Replace 'InstrumentItem::new' to 'InstrumentSoundItem::new'");
                                    return;
                                } else if (isWarped && opcode == Opcodes.INVOKESPECIAL && Native.isMethod((CarrotAction<?>)ItemCarrotStick::new, owner, name, descriptor)) {
                                    Native.writeMethod(system.func(ItemCustomTool::new), super::visitMethodInsn);
                                    Native.log("Replace 'ItemCarrotStick::new' to 'ItemCustomTool::new'");
                                    isWarped = false;
                                    return;
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))
                .patchMethod(IMethodFilter.of(EntityThrownTrident.class, "onHitEntity", Type.getMethodType(Type.VOID_TYPE, Type.getType(MovingObjectPositionEntity.class)), true),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            @Override public void visitLdcInsn(Object value) {
                                if (value instanceof Float raw && raw == 8.0) {
                                    super.visitIntInsn(Opcodes.ALOAD, 0);
                                    super.visitIntInsn(Opcodes.ALOAD, 1);
                                    Native.writeMethod(system.func(EntityTridentBaseDamageEvent::execute), super::visitMethodInsn);
                                    /*super.visitMethodInsn(
                                            Opcodes.INVOKESTATIC,
                                            "net/minecraft/world/entity/projectile/EntityTridentBaseDamageEvent",
                                            "execute",
                                            Type.getMethodDescriptor(Type.FLOAT_TYPE,
                                                    Type.getType(EntityThrownTrident.class),
                                                    Type.getType(MovingObjectPositionEntity.class)
                                            ),
                                            false
                                    );*/
                                    Native.log("Event added");
                                    return;
                                }
                                super.visitLdcInsn(value);
                            }
                        }))
                .patchMethod(IMethodFilter.of(EntityArrow.class, "onHitEntity", Type.getMethodType(Type.VOID_TYPE, Type.getType(MovingObjectPositionEntity.class)), true),
                        MethodPatcher.mutate(v -> new MethodVisitor(Opcodes.ASM9, v) {
                            int critical = 0;
                            int var_damage = -1;
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                if (critical == 0 && Native.isMethod(system.func(EntityArrow::isCritArrow), owner, name, descriptor))
                                    critical = 1;
                            }

                            @Override public void visitJumpInsn(int opcode, Label label) {
                                if (critical == 1 && opcode == Opcodes.IFEQ) {
                                    critical = 2;
                                    opcode = Opcodes.GOTO;
                                }
                                super.visitJumpInsn(opcode, label);
                            }

                            @Override public void visitVarInsn(int opcode, int varIndex) {
                                if (critical == 2 && opcode == Opcodes.ISTORE) {
                                    critical = 3;
                                    var_damage = varIndex;
                                }
                                super.visitVarInsn(opcode, varIndex);
                            }
                            @Override public void visitLineNumber(int line, Label start) {
                                super.visitLineNumber(line, start);
                                if (critical == 3) {
                                    critical = 4;
                                    super.visitVarInsn(Opcodes.ILOAD, var_damage);
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitVarInsn(Opcodes.ALOAD, 1);
                                    Native.writeMethod(system.func(PlayerArrowCriticalEvent::execute), super::visitMethodInsn);
                                    Native.log("Event added `PlayerArrowCriticalEvent`");
                                    super.visitVarInsn(Opcodes.ISTORE, var_damage);
                                }
                            }
                            /*
FRAME SAME
    ALOAD 0
    INVOKEVIRTUAL net/minecraft/world/entity/projectile/EntityArrow.isCritArrow ()Z
    IFEQ L14
L15
    LINENUMBER 371 L15
    ALOAD 0
    GETFIELD net/minecraft/world/entity/Entity.random : Lnet/minecraft/util/RandomSource;
    ILOAD 4
    ICONST_2
    IDIV
    ICONST_2
    IADD
    INVOKEINTERFACE net/minecraft/util/RandomSource.nextInt (I)I (itf)
    I2L
    LSTORE 5
L16
    LINENUMBER 373 L16
    LLOAD 5
    ILOAD 4
    I2L
    LADD
    LDC 2147483647
    INVOKESTATIC java/lang/Math.min (JJ)J
    L2I
    ISTORE 4
L14
    LINENUMBER 376 L14
FRAME SAME
                            */
                        }))
                .patchMethod(IMethodFilter.of(system.func(EntityStrider::getControllingPassenger)),
                        MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitInsn(Opcodes.ACONST_NULL);
                                visitor.visitInsn(Opcodes.ARETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                            }
                        }));

        for (system.Action1<CommandDispatcher<CommandListenerWrapper>> method : List.<system.Action1<CommandDispatcher<CommandListenerWrapper>>>of(
                CommandMe::register,
                CommandTell::register,
                CommandTeamMsg::register,
                CommandHelp::register
        )) version_archive
                .patchMethod(IMethodFilter.of(method), MethodPatcher.mutate(visitor -> new MethodVisitor(Opcodes.ASM9, null) {
                    @Override public void visitCode() {
                        visitor.visitCode();
                        visitor.visitInsn(Opcodes.RETURN);
                        visitor.visitMaxs(0, 0);
                        visitor.visitEnd();
                        Native.log("Clear command method");
                    }
                }));
    }
}
