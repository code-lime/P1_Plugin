package patch;

import net.minecraft.world.level.block.Blocks;
import org.lime.system;

/*
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.papermc.paper.util.ObfHelper;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.minecraft.core.BlockPosition;
import net.minecraft.data.worldgen.BiomeSettings;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.Main;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.EntityCaveSpider;
import net.minecraft.world.entity.monster.EntityStrider;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.food.FoodMetaData;
import net.minecraft.world.inventory.ContainerGrindstone;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeRepair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockSkull;
import net.minecraft.world.level.block.BlockSnow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.lime.system;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

@SuppressWarnings("all")
*/
public class patch {
    void test() {
        Native.isField(system.func(() -> Blocks.ICE), "owner", "name", "descriptor");
    }
    /*
    private enum JarType {
        VersionBase,
        PaperAPI
    }
    private static void log(String log) {
        System.out.println(log);
    }
    public static void patch(URL url) {
        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            patch.throwablePatch(connection.getInputStream().readAllBytes());
        }
        catch (Throwable e) { throw new IllegalArgumentException(e); }
    }
    public static void patch(byte[] resource) {
        try { patch.throwablePatch(resource); }
        catch (Throwable e) { throw new IllegalArgumentException(e); }
    }


    private static void patchLootTable(JarArchive version) {
        String name = classFile(LootTable.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresent(bytes -> {
                    log("Patch LootTable...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (name.equals("<init>")) access = access | Opcodes.ACC_PUBLIC;
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                });
    }
    private static void patchBlocks(JarArchive version) {
        String name = classFile(Blocks.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresent(bytes -> {
                    log("Patch Blocks...");

                    String from = BlockSkull.class.getName().replace('.', '/');
                    String to = from.replace("BlockSkull", "BlockLimeSkull");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (name.equals("<clinit>")) return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                @Override public void visitTypeInsn(int opcode, String type) {
                                    if (type.equals(from)) type = to;
                                    super.visitTypeInsn(opcode, type);
                                }
                                @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                    if (owner.equals(from)) owner = to;
                                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                }
                            };
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                });
    }
    private static void patchTileEntities(JarArchive version) {
        String name = classFile(TileEntityTypes.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresent(bytes -> {
                    log("Patch TileEntities...");

                    String from = TileEntitySkull.class.getName().replace('.', '/');
                    String to = from.replace("TileEntitySkull", "TileEntityLimeSkull");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (name.equals("<clinit>")) return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
                                    super.visitInvokeDynamicInsn(name, descriptor, handle, Arrays.stream(args).map(v -> {
                                        if (v instanceof Handle _handle && _handle.getOwner().equals(from)) return new Handle(_handle.getTag(), to, _handle.getName(), _handle.getDesc(), _handle.isInterface());
                                        else if (v instanceof Type _type) {
                                            Method method = new Method("tmp", _type.getDescriptor());
                                            if (method.getReturnType().equals(Type.getType(TileEntitySkull.class)))
                                                return Type.getMethodType(replaceDescriptor(Type.getType(TileEntitySkull.class), "TileEntitySkull", "TileEntityLimeSkull"), method.getArgumentTypes());
                                        }
                                        return v;
                                    }).toArray());
                                }
                            };
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                });
    }
    private static void patchEntities(JarArchive version) {
        String name = classFile(EntityTypes.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresent(bytes -> {
                    log("Patch Entities...");

                    String from = Marker.class.getName().replace('.', '/');
                    String to = from.replace("Marker", "EntityLimeMarker");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (name.equals("<clinit>")) return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
                                    super.visitInvokeDynamicInsn(name, descriptor, handle, Arrays.stream(args).map(v -> {
                                        if (v instanceof Handle _handle && _handle.getOwner().equals(from)) return new Handle(_handle.getTag(), to, _handle.getName(), _handle.getDesc(), _handle.isInterface());
                                        else if (v instanceof Type _type) {
                                            Method method = new Method("tmp", _type.getDescriptor());
                                            if (method.getReturnType().equals(Type.getType(Marker.class)))
                                                return Type.getMethodType(replaceDescriptor(Type.getType(Marker.class), "Marker", "EntityLimeMarker"), method.getArgumentTypes());
                                        }
                                        return v;
                                    }).toArray());
                                }
                            };
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                });
    }
    private static void patchBukkitItemStack(JarArchive bukkit) {
        String name = classFile(ItemStack.class);
        Optional.ofNullable(bukkit.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch BukkitItemStack...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (name.equals("getMaxStackSize") && Type.getType(descriptor).equals(Type.getMethodType(Type.INT_TYPE))) {
                                log("   Modify method: int getMaxStackSize()");
                                MethodVisitor visitor = writer.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, null) {
                                    @Override public void visitCode() {
                                        visitor.visitCode();
                                        visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                        visitor.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                "net/minecraft/world/item/ItemStackSizeEvent",
                                                "call_getMaxStackSize",
                                                Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(ItemStack.class)),
                                                false
                                        );
                                        visitor.visitInsn(Opcodes.IRETURN);
                                        visitor.visitMaxs(0, 0);
                                        visitor.visitEnd();
                                        log("Patch BukkitItemStack...OK!");
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    bukkit.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchCraftItemStack(JarArchive version) {
        String name = classFile(CraftItemStack.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch CraftItemStack...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (name.equals("getMaxStackSize") && Type.getType(descriptor).equals(Type.getMethodType(Type.INT_TYPE))) {
                                log("   Modify method: int getMaxStackSize()");
                                MethodVisitor visitor = writer.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, null) {
                                    @Override public void visitCode() {
                                        visitor.visitCode();
                                        visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                        visitor.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                "net/minecraft/world/item/ItemStackSizeEvent",
                                                "call_getMaxStackSize",
                                                Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(ItemStack.class)),
                                                false
                                        );
                                        visitor.visitInsn(Opcodes.IRETURN);
                                        visitor.visitMaxs(0, 0);
                                        visitor.visitEnd();
                                        log("Patch CraftItemStack...OK!");
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchNMSItemStack(JarArchive version) {
        String name = classFile(net.minecraft.world.item.ItemStack.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch NMSItemStack...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(net.minecraft.world.item.ItemStack.class, "getMaxStackSize", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.INT_TYPE))) {
                                log("   Modify method: int getMaxStackSize()");
                                MethodVisitor visitor = writer.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, null) {
                                    @Override public void visitCode() {
                                        visitor.visitCode();
                                        visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                        visitor.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                "net/minecraft/world/item/ItemStackSizeEvent",
                                                "call_getMaxStackSize",
                                                Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(net.minecraft.world.item.ItemStack.class)),
                                                false
                                        );
                                        visitor.visitInsn(Opcodes.IRETURN);
                                        visitor.visitMaxs(0, 0);
                                        visitor.visitEnd();
                                        log("Patch NMSItemStack...OK!");
                                    }
                                };
                            }
                            else if (ofMojang(net.minecraft.world.item.ItemStack.class, "getMaxDamage", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.INT_TYPE))) {
                                log("   Modify method: int getMaxDamage()");
                                MethodVisitor visitor = writer.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, null) {
                                    @Override public void visitCode() {
                                        visitor.visitCode();
                                        visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                        visitor.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                "net/minecraft/world/item/ItemMaxDamageEvent",
                                                "call_getMaxDamage",
                                                Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(net.minecraft.world.item.ItemStack.class)),
                                                false
                                        );
                                        visitor.visitInsn(Opcodes.IRETURN);
                                        visitor.visitMaxs(0, 0);
                                        visitor.visitEnd();
                                        log("Patch NMSItemStack...OK!");
                                    }
                                };
                            } else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchEntityLiving(JarArchive version) {
        String name = classFile(EntityLiving.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch EntityLiving...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(EntityLiving.class, "getEquipmentSlotForItem", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.getType(EnumItemSlot.class), Type.getType(net.minecraft.world.item.ItemStack.class)))) {
                                log("   Modify method: EnumItemSlot getEquipmentSlotForItem(ItemStack stack)");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitInsn(int opcode) {
                                        if (opcode == Opcodes.ARETURN) {
                                            super.visitVarInsn(Opcodes.ALOAD, 0);
                                            super.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    "net/minecraft/world/entity/EntityEquipmentSlotEvent",
                                                    "execute",
                                                    Type.getMethodDescriptor(Type.getType(EnumItemSlot.class), Type.getType(EnumItemSlot.class), Type.getType(net.minecraft.world.item.ItemStack.class)),
                                                    false
                                            );
                                            log("Patch EntityLiving...OK!");
                                        }
                                        super.visitInsn(opcode);
                                    }
                                };
                            }
                            else if (ofMojang(EntityLiving.class, "isDamageSourceBlocked", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.BOOLEAN_TYPE, Type.getType(DamageSource.class)))) {
                                log("   Modify method: boolean isDamageSourceBlocked(DamageSource source)");
                                MethodVisitor visitor = writer.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, null) {
                                    @Override public void visitCode() {
                                        visitor.visitCode();

                                        visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                        visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                        visitor.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                "net/minecraft/world/entity/DamageSourceBlockEvent",
                                                "execute",
                                                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(EntityLiving.class), Type.getType(DamageSource.class)),
                                                false
                                        );

                                        visitor.visitInsn(Opcodes.IRETURN);
                                        visitor.visitMaxs(0, 0);
                                        visitor.visitEnd();
                                    }
                                };
                            }
                            else if (ofMojang(EntityLiving.class, "canDisableShield", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.BOOLEAN_TYPE))) {
                                log("   Modify method: boolean canDisableShield()");
                                MethodVisitor visitor = writer.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, null) {
                                    @Override public void visitCode() {
                                        visitor.visitCode();
                                        visitor.visitInsn(Opcodes.ICONST_0);
                                        visitor.visitInsn(Opcodes.IRETURN);
                                        visitor.visitMaxs(0, 0);
                                        visitor.visitEnd();
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchEntityHuman(JarArchive version) {
        String name = classFile(EntityHuman.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch EntityHuman...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        int index = 0;
                        int var_flag = 0;
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(EntityHuman.class, "attack", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE, Type.getType(Entity.class)))) {
                                log("   Modify method: void attack(Entity target) ");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitEnd() {
                                        log("VisitEnd");
                                        //super.visitMaxs(0, 0);
                                        super.visitEnd();
                                    }

                                    @Override public void visitLineNumber(int line, Label start) {
                                        super.visitLineNumber(line, start);
                                        if (index == 2) {
                                            index++;
                                            super.visitVarInsn(Opcodes.ALOAD, 0);
                                            super.visitVarInsn(Opcodes.ILOAD, var_flag);
                                            super.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    "net/minecraft/world/entity/EntityAttackSweepEvent",
                                                    "execute",
                                                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(EntityHuman.class), Type.BOOLEAN_TYPE),
                                                    false
                                            );
                                            super.visitVarInsn(Opcodes.ISTORE, var_flag);
                                            log("Patch EntityHuman...OK!");
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
                                        if (opcode == Opcodes.INSTANCEOF && index == 0 && type.equals("net/minecraft/world/item/ItemSword")) index++;
                                        super.visitTypeInsn(opcode, type);
                                    }
                                };
                            }
                            else if (ofMojang(EntityHuman.class, "tick", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE))) {
                                log("   Modify method: void tick() ");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    private boolean isFound(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                        //log("    - TryCompare");
                                        //log("       - " + opcode + " with " + Opcodes.INVOKESTATIC);
                                        if (opcode != Opcodes.INVOKESTATIC) return false;
                                        //log("       - " + owner + " with " + "net/minecraft/world/item/ItemStack");
                                        if (!owner.equals("net/minecraft/world/item/ItemStack")) return false;
                                        //log("       - " + ofMojang(net.minecraft.world.item.ItemStack.class, "isSame", descriptor, true) + " with " + name);
                                        if (!ofMojang(net.minecraft.world.item.ItemStack.class, "isSame", descriptor, true).equals(name)) return false;
                                        //log("       - " + Type.getType(descriptor) + " with " + Type.getMethodType(Type.BOOLEAN_TYPE, Type.getType(net.minecraft.world.item.ItemStack.class), Type.getType(net.minecraft.world.item.ItemStack.class)));
                                        if (!Type.getType(descriptor).equals(Type.getMethodType(Type.BOOLEAN_TYPE, Type.getType(net.minecraft.world.item.ItemStack.class), Type.getType(net.minecraft.world.item.ItemStack.class)))) return false;
                                        return true;
                                    }
                                    @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                        if (isFound(opcode, owner, name, descriptor, isInterface)) {
                                            log("    - Swap ItemStack.isSame(ItemStack,ItemStack)Z to false");
                                            super.visitInsn(Opcodes.POP2);
                                            super.visitInsn(Opcodes.ICONST_0);
                                        } else {
                                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                        }
                                    }
                                };
                            }
                            else if (ofMojang(EntityHuman.class, "resetAttackStrengthTicker", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE))) {
                                log("   Modify method: void resetAttackStrengthTicker() ");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitInsn(int opcode) {
                                        if (opcode == Opcodes.RETURN) {
                                            super.visitVarInsn(Opcodes.ALOAD, 0);
                                            super.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    "net/minecraft/world/entity/player/PlayerAttackStrengthResetEvent",
                                                    "execute",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(EntityHuman.class)),
                                                    false
                                            );
                                        }
                                        super.visitInsn(opcode);
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchEntityCaveSpider(JarArchive version) {
        String name = classFile(EntityCaveSpider.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch EntityCaveSpider...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(EntityCaveSpider.class, "doHurtTarget", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.BOOLEAN_TYPE, Type.getType(Entity.class)))) {
                                log("   Modify method: boolean doHurtTarget(Entity target) ");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitIntInsn(int opcode, int operand) {
                                        if (opcode == Opcodes.BIPUSH) {
                                            log("Convert '"+operand+"' to 0");
                                            operand = 0;
                                        }
                                        super.visitIntInsn(opcode, operand);
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchRecipeRepair(JarArchive version) {
        String name = classFile(RecipeRepair.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch RecipeRepair...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        int index = 0;
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(RecipeRepair.class, "assemble", descriptor, true).equals(name)
                                    && Type.getType(descriptor).equals(Type.getMethodType(Type.getType(net.minecraft.world.item.ItemStack.class), Type.getType(InventoryCrafting.class)))) {
                                log("   Modify method: ItemStack assemble(InventoryCrafting inventory)");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                        if (opcode == Opcodes.INVOKEVIRTUAL
                                                && owner.equals(className(Item.class))
                                                && ofMojang(net.minecraft.world.item.Item.class, "canBeDepleted", descriptor, true).equals(name)
                                                && Type.getType(descriptor).equals(Type.getMethodType(Type.BOOLEAN_TYPE))) {
                                            //boolean canBeDepleted()
                                            index++;
                                        } else if (opcode == Opcodes.INVOKEVIRTUAL
                                                && index == 2
                                                && owner.equals(className(net.minecraft.world.item.ItemStack.class))
                                                && ofMojang(net.minecraft.world.item.ItemStack.class, "getItem", descriptor, true).equals(name)
                                                && Type.getType(descriptor).equals(Type.getMethodType(Type.getType(Item.class)))) {
                                            index++;
                                            super.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    "net/minecraft/world/item/ItemMaxDamageEvent",
                                                    "call_getMaxDamageItem",
                                                    Type.getMethodDescriptor(Type.getType(Item.class), Type.getType(net.minecraft.world.item.ItemStack.class)),
                                                    false
                                            );
                                            log("Patch RecipeRepair...OK!");
                                            return;
                                        }
                                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchContainerGrindstone(JarArchive version) {
        String name = classFile(ContainerGrindstone.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch ContainerGrindstone...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        int index = 0;
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(ContainerGrindstone.class, "createResult", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE))) {
                                log("   Modify method: void createResult()");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                        if (opcode == Opcodes.INVOKEVIRTUAL
                                                && owner.equals(className(ContainerGrindstone.class))
                                                && ofMojang(ContainerGrindstone.class, "broadcastChanges", descriptor, true).equals(name)
                                                && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE))) {
                                            //void broadcastChanges()
                                            index++;
                                        } else if (opcode == Opcodes.INVOKEVIRTUAL
                                                && index == 2
                                                && owner.equals(className(net.minecraft.world.item.ItemStack.class))
                                                && ofMojang(net.minecraft.world.item.ItemStack.class, "getItem", descriptor, true).equals(name)
                                                && Type.getType(descriptor).equals(Type.getMethodType(Type.getType(Item.class)))) {
                                            index++;
                                            super.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    "net/minecraft/world/item/ItemMaxDamageEvent",
                                                    "call_getMaxDamageItem",
                                                    Type.getMethodDescriptor(Type.getType(Item.class), Type.getType(net.minecraft.world.item.ItemStack.class)),
                                                    false
                                            );
                                            log("Patch ContainerGrindstone...OK!");
                                            return;
                                        }
                                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchBiomeSettings(JarArchive version) {
        String name = classFile(BiomeSettings.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch BiomeSettings...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(BiomeSettings.class, "addDefaultMonsterRoom", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE, Type.getType(BiomeSettingsGeneration.a.class)))) {
                                log("   Modify method: void addDefaultMonsterRoom(BiomeSettingsGeneration.a builder)");
                                MethodVisitor visitor = writer.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, null) {
                                    @Override public void visitCode() {
                                        visitor.visitCode();
                                        visitor.visitInsn(Opcodes.RETURN);
                                        visitor.visitMaxs(0, 0);
                                        visitor.visitEnd();
                                        log("Patch BiomeSettings...OK!");
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchFoodMetaData(JarArchive version) {
        String name = classFile(FoodMetaData.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch FoodMetaData...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                            List<String> _interfaces = Lists.newArrayList(interfaces);
                            _interfaces.add("net/minecraft/world/food/IFoodNative");
                            super.visit(version, access, name, signature, superName, _interfaces.toArray(new String[0]));
                        }

                        @Override public void visitEnd() {
                            super.visitField(Opcodes.ACC_PRIVATE, "nativeData", Type.getDescriptor(NBTTagCompound.class).toString(), "", null).visitEnd();
                            new MethodVisitor(Opcodes.ASM9, super.visitMethod(Opcodes.ACC_PUBLIC, "nativeData", Type.getMethodDescriptor(Type.getType(NBTTagCompound.class)), "", new String[0])) {
                                @Override public void visitCode() {
                                    super.visitCode();
                                    super.visitIntInsn(Opcodes.ALOAD, 0);
                                    super.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/world/food/FoodMetaData", "nativeData", Type.getDescriptor(NBTTagCompound.class));
                                    super.visitInsn(Opcodes.ARETURN);
                                    super.visitMaxs(0, 0);
                                    super.visitEnd();
                                    log("Patch FoodMetaData...GETTER nativeData...OK!");
                                }
                            }.visitCode();
                            new MethodVisitor(Opcodes.ASM9, super.visitMethod(Opcodes.ACC_PUBLIC, "nativeData", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(NBTTagCompound.class)), "", new String[0])) {
                                @Override public void visitCode() {
                                    super.visitCode();
                                    super.visitIntInsn(Opcodes.ALOAD, 0);
                                    super.visitIntInsn(Opcodes.ALOAD, 1);
                                    super.visitFieldInsn(Opcodes.PUTFIELD, "net/minecraft/world/food/FoodMetaData", "nativeData", Type.getDescriptor(NBTTagCompound.class));
                                    super.visitInsn(Opcodes.RETURN);
                                    super.visitMaxs(0, 0);
                                    super.visitEnd();
                                    log("Patch FoodMetaData...SETTER nativeData...OK!");
                                }
                            }.visitCode();

                            super.visitEnd();
                        }

                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            //log("   Founded method '" + name + "' with descriptor '" + descriptor + "'");
                            //log("   Try compare '"+Type.getType(descriptor)+"' with '" + Type.getMethodType(Type.VOID_TYPE, Type.getType(NBTTagCompound.class)) + "'");
                            if (ofMojang(FoodMetaData.class, "readAdditionalSaveData", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE, Type.getType(NBTTagCompound.class)))) {
                                log("   Modify method: void readAdditionalSaveData(NBTTagCompound)");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    int index = 0;
                                    @Override public void visitLineNumber(int line, Label start) {
                                        super.visitLineNumber(line, start);
                                        if (index == 0) {
                                            index++;
                                            super.visitVarInsn(Opcodes.ALOAD, 0);
                                            super.visitVarInsn(Opcodes.ALOAD, 1);
                                            super.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    "net/minecraft/world/food/IFoodNative",
                                                    "readNativeSaveData",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, replaceDescriptor(Type.getType(FoodMetaData.class), "FoodMetaData", "IFoodNative"), Type.getType(NBTTagCompound.class)),
                                                    true
                                            );
                                            log("Patch FoodMetaData...ReadAdditional...OK!");
                                        }
                                    }
                                };
                            }
                            else if (ofMojang(FoodMetaData.class, "addAdditionalSaveData", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE, Type.getType(NBTTagCompound.class)))) {
                                log("   Modify method: void addAdditionalSaveData(NBTTagCompound)");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    int index = 0;
                                    @Override public void visitLineNumber(int line, Label start) {
                                        super.visitLineNumber(line, start);
                                        if (index == 0) {
                                            index++;
                                            super.visitVarInsn(Opcodes.ALOAD, 0);
                                            super.visitVarInsn(Opcodes.ALOAD, 1);
                                            super.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    "net/minecraft/world/food/IFoodNative",
                                                    "addNativeSaveData",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, replaceDescriptor(Type.getType(FoodMetaData.class), "FoodMetaData", "IFoodNative"), Type.getType(NBTTagCompound.class)),
                                                    true
                                            );
                                            log("Patch FoodMetaData...AddAdditional...OK!");
                                        }
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchBlockSnow(JarArchive version) {
        String name = classFile(BlockSnow.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch BlockSnow...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(BlockSnow.class, "randomTick", descriptor, true).equals(name)
                                    && Type.getType(descriptor).equals(
                                    Type.getMethodType(Type.VOID_TYPE,
                                            Type.getType(IBlockData.class),
                                            Type.getType(WorldServer.class),
                                            Type.getType(BlockPosition.class),
                                            Type.getType(RandomSource.class)
                                    ))) {
                                log("   Modify method: void randomTick(IBlockData state, WorldServer world, BlockPosition pos, RandomSource random)");
                                MethodVisitor visitor = writer.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, null) {
                                    @Override public void visitCode() {
                                        visitor.visitCode();
                                        visitor.visitIntInsn(Opcodes.ALOAD, 0);
                                        visitor.visitIntInsn(Opcodes.ALOAD, 1);
                                        visitor.visitIntInsn(Opcodes.ALOAD, 2);
                                        visitor.visitIntInsn(Opcodes.ALOAD, 3);
                                        visitor.visitIntInsn(Opcodes.ALOAD, 4);
                                        visitor.visitMethodInsn(
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
                                        );
                                        visitor.visitInsn(Opcodes.RETURN);
                                        visitor.visitMaxs(0, 0);
                                        visitor.visitEnd();
                                        log("Patch BlockSnow...OK!");
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchWorldServer(JarArchive version) {
        String name = classFile(WorldServer.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch WorldServer...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(WorldServer.class, "tickChunk", descriptor, true).equals(name)
                                    && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE, Type.getType(Chunk.class), Type.INT_TYPE))) {
                                log("   Modify method: void tickChunk(Chunk chunk, int randomTickSpeed)");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                        if (opcode == Opcodes.GETSTATIC
                                                && owner.equals(className(Blocks.class))
                                                && ofMojang(Blocks.class, "ICE", descriptor, false).equals(name)
                                                && Type.getType(descriptor).equals(Type.getType(Block.class))) {
                                            super.visitFieldInsn(opcode, owner, ofMojang(Blocks.class, "FROSTED_ICE", descriptor, false), descriptor);
                                            log("Patch WorldServer...OK!");
                                            return;
                                        }
                                        super.visitFieldInsn(opcode, owner, name, descriptor);
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchBiomeBase(JarArchive version) {
        String name = classFile(BiomeBase.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch BiomeBase...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(BiomeBase.class, "getTemperature", descriptor, true).equals(name)
                                    && Type.getType(descriptor).equals(Type.getMethodType(Type.FLOAT_TYPE, Type.getType(BlockPosition.class)))) {
                                log("   Modify method: float getTemperature(BlockPosition)");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitInsn(int opcode) {
                                        if (opcode == Opcodes.FRETURN) {
                                            super.visitIntInsn(Opcodes.ALOAD, 0);
                                            super.visitIntInsn(Opcodes.ALOAD, 1);
                                            super.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    "net/minecraft/world/level/biome/BiomeTemperatureEvent",
                                                    "execute",
                                                    Type.getMethodDescriptor(Type.FLOAT_TYPE,
                                                            Type.FLOAT_TYPE,
                                                            Type.getType(BiomeBase.class),
                                                            Type.getType(BlockPosition.class)
                                                    ),
                                                    false
                                            );
                                            log("Patch BiomeBase...OK!");
                                        }
                                        super.visitInsn(opcode);
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchItems(JarArchive version) {
        String name = classFile(Items.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresent(bytes -> {
                    log("Patch Items...");

                    String from = InstrumentItem.class.getName().replace('.', '/');
                    String to = from.replace("InstrumentItem", "InstrumentSoundItem");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (name.equals("<clinit>")) return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                @Override public void visitTypeInsn(int opcode, String type) {
                                    if (opcode == Opcodes.NEW && type.equals(from)) {
                                        type = to;
                                        log("Patch Items... NEW!");
                                    }
                                    super.visitTypeInsn(opcode, type);
                                }
                                @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                    if (opcode == Opcodes.INVOKESPECIAL
                                            && owner.equals(from)
                                            && name.equals("<init>")
                                            && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE, Type.getType(Item.Info.class), Type.getType(TagKey.class)))
                                    ) {
                                        owner = to;
                                        log("Patch Items... <init>!");
                                    }
                                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                }
                            };
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                });
    }
    private static void patchEntityThrownTrident(JarArchive version) {
        String name = classFile(EntityThrownTrident.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch EntityThrownTrident...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(EntityThrownTrident.class, "onHitEntity", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.VOID_TYPE, Type.getType(MovingObjectPositionEntity.class)))) {
                                log("   Modify method: void onHitEntity(MovingObjectPositionEntity entityHitResult) ");
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitLdcInsn(Object value) {
                                        if (value instanceof Float raw && raw == 8.0) {
                                            super.visitIntInsn(Opcodes.ALOAD, 0);
                                            super.visitIntInsn(Opcodes.ALOAD, 1);
                                            super.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    "net/minecraft/world/entity/projectile/EntityTridentBaseDamageEvent",
                                                    "execute",
                                                    Type.getMethodDescriptor(Type.FLOAT_TYPE,
                                                            Type.getType(EntityThrownTrident.class),
                                                            Type.getType(MovingObjectPositionEntity.class)
                                                    ),
                                                    false
                                            );
                                            log("Patch EntityThrownTrident...OK!");
                                            return;
                                        }
                                        super.visitLdcInsn(value);
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    private static void patchEntityEntityStrider(JarArchive version) {
        String name = classFile(EntityStrider.class);
        Optional.ofNullable(version.entries.get(name))
                .ifPresentOrElse(bytes -> {
                    log("Patch EntityStrider...");

                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (ofMojang(EntityStrider.class, "getControllingPassenger", descriptor, true).equals(name) && Type.getType(descriptor).equals(Type.getMethodType(Type.getType(EntityLiving.class)))) {
                                log("   Modify method: EntityLiving getControllingPassenger() ");
                                MethodVisitor visitor = writer.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, null) {
                                    @Override public void visitCode() {
                                        visitor.visitCode();
                                        visitor.visitInsn(Opcodes.ACONST_NULL);
                                        visitor.visitInsn(Opcodes.ARETURN);
                                        visitor.visitMaxs(0, 0);
                                        visitor.visitEnd();
                                        log("Patch EntityStrider...OK!");
                                    }
                                };
                            }
                            else return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    version.entries.put(name, writer.toByteArray());
                }, () -> log("File '"+name+"' not founded in version"));
    }
    //Paper: Paths.get(ManagementFactory.getRuntimeMXBean().getClassPath())
    //Version: new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath()
    //Plugin: new File(patch.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath()
    private static Path of(Class<?> tClass) throws Throwable {
        return new File(tClass.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath();
    }
    private static Path loadBaseFile(Path base) throws Throwable {
        Path dir = base.getParent();
        Path orig = Paths.get(
                dir.toString(),
                FilenameUtils.getBaseName(base.toString()) + "-orig." + FilenameUtils.getExtension(base.toString())
        );
        if (!Files.exists(orig)) Files.copy(base, orig);
        return orig;
    }
    private static void throwablePatch(byte[] resource) throws Throwable {
        log("Check patch...");
        Path paper_path = Paths.get(ManagementFactory.getRuntimeMXBean().getClassPath());
        JarArchive paper_archive = JarArchive.of(paper_path);

        JsonObject patch_data = system.json.parse(new String(resource)).getAsJsonObject();

        String current_version = Optional.ofNullable(paper_archive.entries.get("lime-patch.json"))
                .map(String::new)
                .map(system.json::parse)
                .map(JsonElement::getAsJsonObject)
                .map(v -> v.get("version"))
                .map(JsonElement::getAsString)
                .orElse(null);
        String patch_version = patch_data.get("version").getAsString();

        log("Current patch version: " + (current_version == null ? "Not patched" : current_version));
        log("Patch version: " + patch_version);

        if (patch_version.equals(current_version)) return;

        log("Patch...");
        paper_archive.entries.put("lime-patch.json", system.toFormat(system.json.object().add("version", patch_version).build()).getBytes());

        Path version_base = of(Main.class);
        String sha256_old = sha256(Files.readAllBytes(version_base));
        Path bukkit_base = of(Bukkit.class);
        String sha256_bukkit_old = sha256(Files.readAllBytes(bukkit_base));

        log("Getting original version jar...");
        Path version_orig = loadBaseFile(version_base);
        Path bukkit_orig = loadBaseFile(bukkit_base);

        log("Read version jar...");
        JarArchive version_archive = JarArchive.of(version_orig);
        log("Read bukkit jar...");
        JarArchive bukkit_archive = JarArchive.of(bukkit_orig);
        log("Read plugin jar...");
        JarArchive plugin_archive = JarArchive.of(of(patch.class));
        log("Append...");
        patch_data.getAsJsonArray("append").forEach(append -> {
            String name = append.getAsString();
            log("Append '"+name+"'...");
            version_archive.entries.put(name, plugin_archive.entries.get(name));
        });
        patch_data.getAsJsonArray("append_regex").forEach(append_regex -> {
            String regex = append_regex.getAsString();
            log("Append group '"+regex+"':");
            plugin_archive.entries
                    .entrySet()
                    .stream()
                    .filter(v -> system.compareRegex(v.getKey(), regex))
                    .forEach(kv -> {
                        log(" - '"+kv.getKey()+"'...");
                        version_archive.entries.put(kv.getKey(), kv.getValue());
                    });
        });

        log("Read deobf file...");
        try (Closeable ignored = loadDeobf()) {
            log("Modify version jar...");
            patchLootTable(version_archive);
            patchBlocks(version_archive);
            patchTileEntities(version_archive);
            patchEntities(version_archive);

            patchBukkitItemStack(bukkit_archive);
            patchCraftItemStack(version_archive);
            patchNMSItemStack(version_archive);
            patchRecipeRepair(version_archive);
            patchContainerGrindstone(version_archive);

            patchEntityLiving(version_archive);
            patchEntityHuman(version_archive);
            patchEntityCaveSpider(version_archive);
            patchBiomeSettings(version_archive);

            patchFoodMetaData(version_archive);
            patchBlockSnow(version_archive);
            patchWorldServer(version_archive);
            patchBiomeBase(version_archive);
            patchItems(version_archive);
            patchEntityThrownTrident(version_archive);
            patchEntityEntityStrider(version_archive);
        }

        log("Save version jar...");
        version_archive.toFile(version_base);
        bukkit_archive.toFile(bukkit_base);

        String sha256_base = sha256(Files.readAllBytes(version_base));
        String sha256_orig = sha256(Files.readAllBytes(version_orig));
        String sha256_bukkit_base = sha256(Files.readAllBytes(bukkit_base));
        String sha256_bukkit_orig = sha256(Files.readAllBytes(bukkit_orig));

        log("Apply paper jar...");
        paper_archive.entries.put("META-INF/versions.list", new String(paper_archive.entries.get("META-INF/versions.list"))
                .replace(sha256_old, sha256_base)
                .replace(sha256_orig, sha256_base)
                .replace(sha256_bukkit_old, sha256_bukkit_base)
                .replace(sha256_bukkit_orig, sha256_bukkit_base)
                .getBytes());
        paper_archive.entries.put("META-INF/patches.list", new String(paper_archive.entries.get("META-INF/patches.list"))
                .replace(sha256_old, sha256_base)
                .replace(sha256_orig, sha256_base)
                .replace(sha256_bukkit_old, sha256_bukkit_base)
                .replace(sha256_bukkit_orig, sha256_bukkit_base)
                .getBytes());

        log("Save paper jar...");
        paper_archive.toFile(paper_path);

        log("Patch status: OK");
        log("Exit...");
        Runtime.getRuntime().halt(0);
    }
    */
}






























