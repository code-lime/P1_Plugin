package patch.gp;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableEvent;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.lime.gp.lime;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Execute;
import org.lime.system.execute.Func3;
import org.lime.system.execute.ICallable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import patch.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class MutatePatcher extends BasePluginPatcher {
    public static void register() {
        Patcher.addPatcher(new MutatePatcher());
    }

    private MutatePatcher() {
        super(lime.class);
    }

    @Override public void patch(JarArchive versionArchive, JarArchive bukkitArchive) {
        Native.log("Modify "+bukkitArchive.name+" jar...");
        bukkitArchive
                .patchMethod(IMethodFilter.of(Execute.func(org.bukkit.inventory.ItemStack::getMaxStackSize)),
                        MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.ItemStackSizeEvent");
                            }

                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                Native.writeMethod(Execute.func(ItemStackSizeEvent::call_getMaxStackSizeBukkit), visitor::visitMethodInsn);
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                setProgress("Event.ItemStackSizeEvent");
                            }
                        }));

        Native.log("Modify "+versionArchive.name+" jar...");
        versionArchive
                //.patchMethod(IMethodFilter.of(LootTable.class, OBJECT_CONSTRUCTOR, false),
                //        MethodPatcher.none().access(v -> v | Opcodes.ACC_PUBLIC))
                .patchMethod(IMethodFilter.of(Execute.<LootTable, LootTableInfo, Consumer<ItemStack>>action(LootTable::getRandomItemsRaw)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.LootTableEvent");
                            }
                            private int event = 0;

                            @Override public void visitVarInsn(int opcode, int varIndex) {
                                if (event == 0 && opcode == Opcodes.ALOAD && varIndex == 0) {
                                    event = 1;
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitVarInsn(Opcodes.ALOAD, 1);
                                    super.visitVarInsn(Opcodes.ALOAD, 2);
                                    Native.writeMethod(Execute.func(LootTableEvent::execute), super::visitMethodInsn);
                                    Label label = new Label();
                                    super.visitJumpInsn(Opcodes.IFNE, label);
                                    super.visitInsn(Opcodes.RETURN);
                                    super.visitLabel(label);
                                    setProgress("Event.LootTableEvent");
                                }
                                super.visitVarInsn(opcode, varIndex);
                            }
                        }))
                .patchMethod(IMethodFilter.of(Blocks.class, IMethodInfo.STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Replace.Type", "Replace.Method");
                            }

                            private final String from = Type.getInternalName(BlockSkull.class);
                            private final String to = Type.getInternalName(BlockLimeSkull.class);

                            @Override public void visitTypeInsn(int opcode, String type) {
                                if (type.equals(from)) {
                                    Native.log("Replace '" + from + "' to '" + to + "'");
                                    type = to;
                                    setProgressDuplicate("Replace.Type");
                                }
                                super.visitTypeInsn(opcode, type);
                            }
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (owner.equals(from)) {
                                    Native.log("Replace '" + from + "' to '" + to + "' in method " + owner + "." + name + descriptor);
                                    owner = to;
                                    setProgressDuplicate("Replace.Method");
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))
                .patchMethod(IMethodFilter.of(TileEntityTypes.class, IMethodInfo.STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Replace.Method");
                            }

                            @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
                                super.visitInvokeDynamicInsn(name, descriptor, handle, Arrays.stream(args).map(v -> {
                                    if (v instanceof Handle _handle && _handle.getOwner().equals(Type.getInternalName(TileEntitySkull.class))) {
                                        Native.log("Replace 'TileEntitySkull' to 'TileEntityLimeSkull' in " + v);
                                        setProgressDuplicate("Replace.Method");
                                        return new Handle(_handle.getTag(), Type.getInternalName(TileEntityLimeSkull.class), _handle.getName(), _handle.getDesc(), _handle.isInterface());
                                    }
                                    else if (v instanceof Type _type) {
                                        Method method = new Method("tmp", _type.getDescriptor());
                                        if (method.getReturnType().equals(Type.getType(TileEntitySkull.class))) {
                                            Native.log("Replace 'TileEntitySkull' to 'TileEntityLimeSkull' in " + v);
                                            setProgressDuplicate("Replace.Method");
                                            return Type.getMethodType(Type.getType(TileEntityLimeSkull.class), method.getArgumentTypes());
                                        }
                                    }
                                    return v;
                                }).toArray());
                            }
                        }))
                .patchMethod(IMethodFilter.of(EntityTypes.class, IMethodInfo.STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Replace.Method");
                            }

                            @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
                                super.visitInvokeDynamicInsn(name, descriptor, handle, Arrays.stream(args).map(v -> {
                                    if (v instanceof Handle _handle && _handle.getOwner().equals(Type.getInternalName(Marker.class))) {
                                        Native.log("Replace 'Marker' to 'EntityLimeMarker' in " + v);
                                        setProgressDuplicate("Replace.Method");
                                        return new Handle(_handle.getTag(), Type.getInternalName(EntityLimeMarker.class), _handle.getName(), _handle.getDesc(), _handle.isInterface());
                                    }
                                    else if (v instanceof Type _type) {
                                        Method method = new Method("tmp", _type.getDescriptor());
                                        if (method.getReturnType().equals(Type.getType(Marker.class))) {
                                            Native.log("Replace 'Marker' to 'EntityLimeMarker' in " + v);
                                            setProgressDuplicate("Replace.Method");
                                            return Type.getMethodType(Type.getType(EntityLimeMarker.class), method.getArgumentTypes());
                                        }
                                    }
                                    return v;
                                }).toArray());
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.func(CraftItemStack::getMaxStackSize)),
                        MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.ItemStackSizeEvent.Bukkit");
                            }

                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                Native.writeMethod(Execute.func(ItemStackSizeEvent::call_getMaxStackSizeBukkit), visitor::visitMethodInsn);
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");

                                setProgress("Event.ItemStackSizeEvent.Bukkit");
                            }
                        }));

        versionArchive
                .of(net.minecraft.world.item.ItemStack.class)
                .patchMethod(IMethodFilter.of(Execute.func(net.minecraft.world.item.ItemStack::getMaxStackSize)),
                        MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.ItemStackSizeEvent.NMS");
                            }

                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                Native.writeMethod(Execute.func(ItemStackSizeEvent::call_getMaxStackSizeNMS), visitor::visitMethodInsn);
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                                setProgress("Event.ItemStackSizeEvent.NMS");
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.func(net.minecraft.world.item.ItemStack::getMaxDamage)),
                        MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.ItemMaxDamageEvent");
                            }

                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                Native.writeMethod(Execute.func(ItemMaxDamageEvent::call_getMaxDamage), visitor::visitMethodInsn);
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                                setProgress("Event.ItemMaxDamageEvent");
                            }
                        }))
                .patch();

        versionArchive
                .of(EntityLiving.class)
                .patchMethod(IMethodFilter.of(Execute.func(EntityLiving::getEquipmentSlotForItem)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.EntityEquipmentSlotEvent");
                            }

                            @Override public void visitInsn(int opcode) {
                                if (opcode == Opcodes.ARETURN) {
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    Native.writeMethod(Execute.func(EntityEquipmentSlotEvent::execute), super::visitMethodInsn);
                                    Native.log("Event added");
                                    setProgress("Event.EntityEquipmentSlotEvent");
                                }
                                super.visitInsn(opcode);
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.func(EntityLiving::isDamageSourceBlocked)),
                        MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.DamageSourceBlockEvent");
                            }

                            @Override public void visitCode() {
                                visitor.visitCode();

                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                Native.writeMethod(Execute.func(DamageSourceBlockEvent::execute), visitor::visitMethodInsn);

                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                                setProgress("Event.DamageSourceBlockEvent");
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.func(EntityLiving::canDisableShield)),
                        MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Static.DisableShield");
                            }

                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitInsn(Opcodes.ICONST_0);
                                visitor.visitInsn(Opcodes.IRETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                                setProgress("Static.DisableShield");
                            }
                        }))
                .patch();

        versionArchive
                .of(EntityHuman.class)
                .patchMethod(IMethodFilter.of(Execute.action(EntityHuman::attack)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of(
                                        "Event.EntityAttackSweepEvent",
                                        "Event.PlayerAttackMultiplyCriticalEvent",
                                        "Static.Strength"
                                );
                            }

                            private int index = 0;
                            private int var_flag = 0;
                            @Override public void visitLineNumber(int line, Label start) {
                                super.visitLineNumber(line, start);
                                if (index == 2) {
                                    index++;
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitVarInsn(Opcodes.ILOAD, var_flag);
                                    Native.writeMethod(Execute.func(EntityAttackSweepEvent::execute), super::visitMethodInsn);
                                    super.visitVarInsn(Opcodes.ISTORE, var_flag);
                                    Native.log("Event added `EntityAttackSweepEvent`");
                                    setProgress("Event.EntityAttackSweepEvent");
                                }
                            }
                            @Override public void visitVarInsn(int opcode, int var) {
                                if (opcode == Opcodes.ISTORE && index == 1) {
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
                                if (critical == 0 && Native.isMethod(Execute.func(EntityHuman::isSprinting), owner, name, descriptor)) {
                                    critical = 1;
                                }
                            }

                            private int strength = 0;
                            @Override public void visitLdcInsn(Object value) {
                                if (strength == 0 && value instanceof Float raw && raw == 0.2f) {
                                    strength = 1;
                                    Native.log("Strength change 0.2+0.8x to 0.0+0.8x");
                                    super.visitLdcInsn(0f);
                                    return;
                                }
                                if (strength == 1 && value instanceof Float raw && raw == 0.8f) {
                                    strength = 2;
                                    Native.log("Strength change 0.2+0.8x to 0.0+1.0x");
                                    super.visitLdcInsn(1f);
                                    setProgress("Static.Strength");
                                    return;
                                }
                                super.visitLdcInsn(value);
                                if (critical == 1 && value instanceof Float raw && raw == 1.5f) {
                                    critical = 2;
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitVarInsn(Opcodes.ALOAD, 1);
                                    Native.writeMethod(Execute.func(PlayerAttackMultiplyCriticalEvent::execute), super::visitMethodInsn);
                                    Native.log("Event added `PlayerAttackMultiplyCriticalEvent`");
                                    setProgress("Event.PlayerAttackMultiplyCriticalEvent");
                                }
                            }
                        }))
                /*.patchMethod(IMethodFilter.of(Execute.action(EntityHuman::tick)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(Opcodes.ASM9, v) {
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (Native.isMethod(Execute.func(net.minecraft.world.item.ItemStack::isSameItem), owner, name, descriptor)) {
                                    super.visitInsn(Opcodes.POP2);
                                    super.visitInsn(Opcodes.ICONST_0);
                                    Native.log("Same removed");
                                } else {
                                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                }
                            }
                        }))*/
                .patchMethod(IMethodFilter.of(Execute.action(EntityHuman::resetAttackStrengthTicker)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.PlayerAttackStrengthResetEvent");
                            }

                            @Override public void visitInsn(int opcode) {
                                if (opcode == Opcodes.RETURN) {
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    Native.writeMethod(Execute.action(PlayerAttackStrengthResetEvent::execute), super::visitMethodInsn);
                                    Native.log("Event added");
                                    setProgress("Event.PlayerAttackStrengthResetEvent");
                                }
                                super.visitInsn(opcode);
                            }
                        }))
                .patchMethod(IMethodFilter.of(EntityHuman.class, "hurtCurrentlyUsedShield", Type.getMethodType(Type.VOID_TYPE, Type.FLOAT_TYPE), true),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Static.MinDamageItem");
                            }
                            private boolean changed = false;
                            @Override public void visitLdcInsn(Object value) {
                                if (!changed && value instanceof Float raw && raw == 3.0f) {
                                    changed = true;
                                    value = 0.001f;
                                    setProgress("Static.MinDamageItem");
                                }
                                super.visitLdcInsn(value);
                            }
                        }))
                .patch();

        versionArchive
                .patchMethod(IMethodFilter.of(Execute.func(EntityCaveSpider::doHurtTarget)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Static.DisableSpiderDamageEffect");
                            }

                            @Override public void visitIntInsn(int opcode, int operand) {
                                if (opcode == Opcodes.BIPUSH) {
                                    Native.log("Replace value from '"+operand+"' to '0'");
                                    operand = 0;
                                    setProgressDuplicate("Static.DisableSpiderDamageEffect");
                                }
                                super.visitIntInsn(opcode, operand);
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.func(RecipeRepair::assemble)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.ItemMaxDamageEvent");
                            }

                            private int index = 0;
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (opcode == Opcodes.INVOKEVIRTUAL && Native.isMethod(Execute.func(Item::canBeDepleted), owner, name, descriptor)) {
                                    index++;
                                } else if (opcode == Opcodes.INVOKEVIRTUAL && index == 2 && Native.isMethod(Execute.func(net.minecraft.world.item.ItemStack::getItem), owner, name, descriptor)) {
                                    index++;
                                    Native.writeMethod(Execute.func(ItemMaxDamageEvent::call_getMaxDamageItem), super::visitMethodInsn);
                                    Native.log("Event added");
                                    setProgress("Event.ItemMaxDamageEvent");
                                    return;
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))
                .patchMethod(IMethodFilter.of(ContainerGrindstone.class, "createResult", Type.getMethodType(Type.VOID_TYPE), true),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.ItemMaxDamageEvent");
                            }

                            private int index = 0;
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (opcode == Opcodes.INVOKEVIRTUAL && Native.isMethod(Execute.action(ContainerGrindstone::broadcastChanges), owner, name, descriptor)) {
                                    index++;
                                } else if (opcode == Opcodes.INVOKEVIRTUAL && index == 2 && Native.isMethod(Execute.func(net.minecraft.world.item.ItemStack::getItem), owner, name, descriptor)) {
                                    index++;
                                    Native.writeMethod(Execute.func(ItemMaxDamageEvent::call_getMaxDamageItem), super::visitMethodInsn);
                                    Native.log("Event added");
                                    setProgress("Event.ItemMaxDamageEvent");
                                    return;
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.action(BiomeSettings::addDefaultMonsterRoom)),
                        MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Disable.SpawnerRoom");
                            }

                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitInsn(Opcodes.RETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                                setProgress("Disable.SpawnerRoom");
                            }
                        }));

        versionArchive
                .of(FoodMetaData.class)
                .addInterface(Type.getInternalName(IFoodNative.class))
                .addField(Opcodes.ACC_PRIVATE, "nativeData", Type.getDescriptor(NBTTagCompound.class), "", null)
                .addMethod(MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                    @Override protected List<String> createProgressList() {
                        return List.of("Method.Append");
                    }

                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitIntInsn(Opcodes.ALOAD, 0);
                        super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(FoodMetaData.class), "nativeData", Type.getDescriptor(NBTTagCompound.class));
                        super.visitInsn(Opcodes.ARETURN);
                        super.visitMaxs(0, 0);
                        super.visitEnd();

                        setProgress("Method.Append");
                    }
                }), Opcodes.ACC_PUBLIC, "nativeData", Type.getMethodDescriptor(Type.getType(NBTTagCompound.class)), "", new String[0])
                .addMethod(MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                    @Override protected List<String> createProgressList() {
                        return List.of("Method.Append");
                    }

                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitIntInsn(Opcodes.ALOAD, 0);
                        super.visitIntInsn(Opcodes.ALOAD, 1);
                        super.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(FoodMetaData.class), "nativeData", Type.getDescriptor(NBTTagCompound.class));
                        super.visitInsn(Opcodes.RETURN);
                        super.visitMaxs(0, 0);
                        super.visitEnd();

                        setProgress("Method.Append");
                    }
                }), Opcodes.ACC_PUBLIC, "nativeData", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(NBTTagCompound.class)), "", new String[0])
                .patchMethod(IMethodFilter.of(Execute.action(FoodMetaData::readAdditionalSaveData)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Method.readNativeSaveData");
                            }

                            private int index = 0;
                            @Override public void visitLineNumber(int line, Label start) {
                                super.visitLineNumber(line, start);
                                if (index != 0) return;
                                index++;
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitVarInsn(Opcodes.ALOAD, 1);
                                Native.writeMethod(Execute.action(IFoodNative::readNativeSaveData), super::visitMethodInsn);
                                Native.log("Native added");
                                setProgress("Method.readNativeSaveData");
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.action(FoodMetaData::addAdditionalSaveData)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Method.addNativeSaveData");
                            }

                            private int index = 0;
                            @Override public void visitLineNumber(int line, Label start) {
                                super.visitLineNumber(line, start);
                                if (index != 0) return;
                                index++;
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitVarInsn(Opcodes.ALOAD, 1);
                                Native.writeMethod(Execute.action(IFoodNative::addNativeSaveData), super::visitMethodInsn);
                                Native.log("Native added");
                                setProgress("Method.addNativeSaveData");
                            }
                        }))
                .patch();

        versionArchive
                .patchMethod(IMethodFilter.of(Execute.action(BlockSnow::randomTick)),
                        MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.BlockSnowTickEvent");
                            }

                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                visitor.visitIntInsn(Opcodes.ALOAD, 1);
                                visitor.visitIntInsn(Opcodes.ALOAD, 2);
                                visitor.visitIntInsn(Opcodes.ALOAD, 3);
                                visitor.visitIntInsn(Opcodes.ALOAD, 4);
                                Native.writeMethod(Execute.action(BlockSnowTickEvent::execute), visitor::visitMethodInsn);
                                visitor.visitInsn(Opcodes.RETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                                setProgress("Event.BlockSnowTickEvent");
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.action(WorldServer::tickChunk)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of(
                                        "Replace.ICE",
                                        "Event.SnowAccumulationHeightEvent"
                                );
                            }

                            private int snowAccumulation = 0;
                            @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                if (opcode == Opcodes.GETSTATIC) {
                                    if (Native.isField(Execute.func(() -> Blocks.ICE), owner, name, descriptor)) {
                                        Native.FieldInfo FROSTED_ICE = Native.infoFromField(Execute.func(() -> Blocks.FROSTED_ICE));
                                        super.visitFieldInsn(opcode, FROSTED_ICE.owner(), FROSTED_ICE.name(), FROSTED_ICE.descriptor());
                                        Native.log("Replace ice");
                                        setProgress("Replace.ICE");
                                        return;
                                    } else if (snowAccumulation == 0 && Native.isField(Execute.func(() -> GameRules.RULE_SNOW_ACCUMULATION_HEIGHT), owner, name, descriptor)) {
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
                                    Native.writeMethod(Execute.action(SnowAccumulationHeightEvent::execute), super::visitMethodInsn);
                                    super.visitIntInsn(Opcodes.ISTORE, maxHeightVar);
                                    snowAccumulation = 3;
                                    Native.log("SnowAccumulation: 2 -> 3");
                                    Native.log("Event SnowAccumulationHeightEvent added");
                                    setProgress("Event.SnowAccumulationHeightEvent");
                                    return;
                                }
                                super.visitInsn(opcode);
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.func(BiomeBase::getTemperature)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.BiomeTemperatureEvent");
                            }

                            @Override public void visitInsn(int opcode) {
                                if (opcode == Opcodes.FRETURN) {
                                    super.visitIntInsn(Opcodes.ALOAD, 0);
                                    super.visitIntInsn(Opcodes.ALOAD, 1);
                                    Native.writeMethod(Execute.action(BiomeTemperatureEvent::execute), super::visitMethodInsn);
                                    Native.log("Event added");
                                    setProgressDuplicate("Event.BiomeTemperatureEvent");
                                }
                                super.visitInsn(opcode);
                            }
                        }))
                .patchMethod(IMethodFilter.of(Items.class, IMethodInfo.STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of(
                                        "Swap.NEW.InstrumentItem -> InstrumentSoundItem",
                                        "Swap.NEW.ItemCarrotStick -> ItemCustomTool",
                                        "Swap.CTOR.InstrumentItem -> InstrumentSoundItem",
                                        "Swap.CTOR.ItemCarrotStick -> ItemCustomTool"
                                );
                            }

                            private boolean isWarped = false;

                            @Override public void visitLdcInsn(Object value) {
                                if ("warped_fungus_on_a_stick".equals(value)) isWarped = true;
                                super.visitLdcInsn(value);
                            }

                            @Override public void visitTypeInsn(int opcode, String type) {
                                if (opcode == Opcodes.NEW && type.equals(Type.getInternalName(InstrumentItem.class))) {
                                    type = Type.getInternalName(InstrumentSoundItem.class);
                                    Native.log("Replace `NEW` instrument instance");
                                    setProgress("Swap.NEW.InstrumentItem -> InstrumentSoundItem");
                                } else if (isWarped && opcode == Opcodes.NEW && type.equals(Type.getInternalName(ItemCarrotStick.class))) {
                                    type = Type.getInternalName(ItemCustomTool.class);
                                    Native.log("Replace `NEW` carrot stick instance");
                                    setProgress("Swap.NEW.ItemCarrotStick -> ItemCustomTool");
                                }
                                super.visitTypeInsn(opcode, type);
                            }

                            private interface CarrotAction<T extends Entity & ISteerable>
                                    extends Func3<Item.Info, EntityTypes<T>, Integer, ItemCarrotStick<T>> { }

                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (opcode == Opcodes.INVOKESPECIAL && Native.isMethod(Execute.func(InstrumentItem::new), owner, name, descriptor)) {
                                    Native.writeMethod(Execute.func(InstrumentSoundItem::new), super::visitMethodInsn);
                                    Native.log("Replace 'InstrumentItem::new' to 'InstrumentSoundItem::new'");
                                    setProgress("Swap.CTOR.InstrumentItem -> InstrumentSoundItem");
                                    return;
                                } else if (isWarped && opcode == Opcodes.INVOKESPECIAL && Native.isMethod((CarrotAction<?>)ItemCarrotStick::new, owner, name, descriptor)) {
                                    Native.writeMethod(Execute.func(ItemCustomTool::new), super::visitMethodInsn);
                                    Native.log("Replace 'ItemCarrotStick::new' to 'ItemCustomTool::new'");
                                    setProgress("Swap.CTOR.ItemCarrotStick -> ItemCustomTool");
                                    isWarped = false;
                                    return;
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.func(ItemTool::hurtEnemy)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Fix.ToolDamageX2");
                            }

                            @Override public void visitInsn(int opcode) {
                                if (opcode == Opcodes.ICONST_2) {
                                    opcode = Opcodes.ICONST_1;
                                    setProgress("Fix.ToolDamageX2");
                                }
                                super.visitInsn(opcode);
                            }
                        }))

                .patchMethod(IMethodFilter.of(Execute.func(ItemMonsterEgg::useOn)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.EggSpawnEvent");
                            }

                            private <T extends Entity>ICallable getEntityCreateMethod() {
                                return Execute.<EntityTypes<T>, WorldServer, ItemStack, EntityHuman, BlockPosition, EnumMobSpawn, Boolean, Boolean, T>func(EntityTypes::spawn);
                            }
                            private <T extends Entity>ICallable getEventSpawnMethod() {
                                return Execute.<EntityTypes<T>, WorldServer, ItemStack, EntityHuman, BlockPosition, EnumMobSpawn, Boolean, Boolean, T>func(EggSpawnEvent::execute);
                            }

                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (Native.isMethod(getEntityCreateMethod(), owner, name, descriptor)) {
                                    Native.writeMethod(getEventSpawnMethod(), super::visitMethodInsn);
                                    setProgress("Event.EggSpawnEvent");
                                    return;
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.func(ItemMonsterEgg::use)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.EggSpawnEvent");
                            }

                            private <T extends Entity>ICallable getEntityCreateMethod() {
                                return Execute.<EntityTypes<T>, WorldServer, ItemStack, EntityHuman, BlockPosition, EnumMobSpawn, Boolean, Boolean, T>func(EntityTypes::spawn);
                            }
                            private <T extends Entity>ICallable getEventSpawnMethod() {
                                return Execute.<EntityTypes<T>, WorldServer, ItemStack, EntityHuman, BlockPosition, EnumMobSpawn, Boolean, Boolean, T>func(EggSpawnEvent::execute);
                            }

                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (Native.isMethod(getEntityCreateMethod(), owner, name, descriptor)) {
                                    Native.writeMethod(getEventSpawnMethod(), super::visitMethodInsn);
                                    setProgress("Event.EggSpawnEvent");
                                    return;
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }))

                .patchMethod(IMethodFilter.of(EntityThrownTrident.class, "onHitEntity", Type.getMethodType(Type.VOID_TYPE, Type.getType(MovingObjectPositionEntity.class)), true),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.EntityTridentBaseDamageEvent");
                            }
                            @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                super.visitFieldInsn(opcode, owner, name, descriptor);

                                Native.log(owner +"."+ Native.getSpigotName(EntityArrow.class, name, descriptor, false) + descriptor);

                                if (owner.equals(Type.getInternalName(EntityArrow.class))
                                        && Native.getSpigotName(EntityArrow.class, name, descriptor, false).equals("baseDamage")
                                        && descriptor.equals(Type.getDescriptor(double.class)))
                                {
                                    super.visitInsn(Opcodes.D2F);
                                    super.visitIntInsn(Opcodes.ALOAD, 0);
                                    super.visitIntInsn(Opcodes.ALOAD, 1);
                                    Native.writeMethod(Execute.func(EntityTridentBaseDamageEvent::execute), super::visitMethodInsn);
                                    Native.log("Event added");
                                    setProgress("Event.EntityTridentBaseDamageEvent");
                                }
                            }
                        }))
                .patchMethod(IMethodFilter.of(EntityArrow.class, "onHitEntity", Type.getMethodType(Type.VOID_TYPE, Type.getType(MovingObjectPositionEntity.class)), true),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Event.PlayerArrowCriticalEvent", "THROW");
                            }

                            int critical = 0;
                            int var_damage = -1;
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                if (critical == 0 && Native.isMethod(Execute.func(EntityArrow::isCritArrow), owner, name, descriptor))
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
                                    Native.writeMethod(Execute.func(PlayerArrowCriticalEvent::execute), super::visitMethodInsn);
                                    Native.log("Event added `PlayerArrowCriticalEvent`");
                                    super.visitVarInsn(Opcodes.ISTORE, var_damage);
                                    setProgress("Event.PlayerArrowCriticalEvent");
                                }
                            }
                        }))
                .patchMethod(IMethodFilter.of(Execute.func(EntityStrider::getControllingPassenger)),
                        MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Replace.StriderPassenger");
                            }

                            @Override public void visitCode() {
                                visitor.visitCode();
                                visitor.visitInsn(Opcodes.ACONST_NULL);
                                visitor.visitInsn(Opcodes.ARETURN);
                                visitor.visitMaxs(0, 0);
                                visitor.visitEnd();

                                Native.log("Replace full method");
                                setProgress("Replace.StriderPassenger");
                            }
                        }));

        for (Action1<CommandDispatcher<CommandListenerWrapper>> method : List.<Action1<CommandDispatcher<CommandListenerWrapper>>>of(
                CommandMe::register,
                CommandTell::register,
                CommandTeamMsg::register,
                CommandHelp::register
        )) versionArchive
                .patchMethod(IMethodFilter.of(method), MethodPatcher.mutate(visitor -> new ProgressMethodVisitor(visitor, null) {
                    @Override protected List<String> createProgressList() {
                        return List.of("Clear.Command");
                    }
                    @Override public void visitCode() {
                        visitor.visitCode();
                        visitor.visitInsn(Opcodes.RETURN);
                        visitor.visitMaxs(0, 0);
                        visitor.visitEnd();
                        Native.log("Clear command method");
                        setProgress("Clear.Command");
                    }
                }));
    }
}
