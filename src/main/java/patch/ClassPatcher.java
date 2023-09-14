package patch;

import org.objectweb.asm.*;
import com.google.common.collect.Lists;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;

public final class ClassPatcher<T> {
    private final JarArchive archive;
    private final Class<T> tClass;

    private final List<String> appendInterfaces = new ArrayList<>();
    private final List<Toast5<Integer, String, String, String, Object>> appendFields = new ArrayList<>();
    private final List<Toast6<MethodPatcher, Integer, String, String, String, String[]>> appendMethods = new ArrayList<>();
    private final Map<IMethodFilter<T>, MethodPatcher> patchMethods = new HashMap<>();
    //int access, String name, String descriptor, String signature, Object value

    public ClassPatcher(JarArchive archive, Class<T> tClass) {
        this.archive = archive;
        this.tClass = tClass;
    }

    public ClassPatcher<T> addInterface(String interfaceClass) {
        this.appendInterfaces.add(interfaceClass);
        return this;
    }
    public ClassPatcher<T> addField(int access, String name, String descriptor, String signature, Object value) {
        appendFields.add(Toast.of(access, name, descriptor, signature, value));
        return this;
    }
    public ClassPatcher<T> addMethod(MethodPatcher patcher, int access, String name, String descriptor, String signature, String[] exceptions) {
        appendMethods.add(Toast.of(patcher, access, name, descriptor, signature, exceptions));
        return this;
    }
    public ClassPatcher<T> patchMethod(IMethodFilter<T> filter, MethodPatcher patcher) {
        patchMethods.put(filter, patcher);
        return this;
    }

    public JarArchive patch() {
        return archive.patch(tClass, writer -> new ClassVisitor(Opcodes.ASM9, writer) {
            private final Map<IMethodFilter<T>, MethodPatcher> filters = new HashMap<>(patchMethods);
            @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if (!appendInterfaces.isEmpty()) {
                    List<String> _interfaces = Lists.newArrayList(interfaces);
                    _interfaces.addAll(appendInterfaces);
                    Native.log("Append interfaces: " + String.join(", ", appendInterfaces) + "...");
                    interfaces = _interfaces.toArray(new String[0]);
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
            @Override public void visitEnd() {
                appendFields.forEach(field -> {
                    Native.log("Append field: " + field.val1 + field.val2 + "...");
                    field.invokeGet(super::visitField).visitEnd();
                });
                appendMethods.forEach(method -> {
                    Native.log("Append method: " + method.val2 + method.val3 + "...");
                    method.val0.patch(this, method.val1, method.val2, method.val3, method.val4, method.val5).visitCode();
                });
                super.visitEnd();
                filters.forEach((filter, patcher) -> Native.log("!!!WARNING!!! METHOD '"+filter.toInfo()+"' NOT FOUNDED FOR PATCH!"));
            }
            private boolean isSuper = false;
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (!isSuper) {
                    for (Map.Entry<IMethodFilter<T>, MethodPatcher> kv : patchMethods.entrySet()) {
                        IMethodFilter<T> filter = kv.getKey();
                        filters.remove(filter);
                        if (filter.test(access, name, descriptor, signature, exceptions)) {
                            Native.log("Patch method " + filter.toInfo() + "...");
                            isSuper = true;
                            MethodVisitor visitor = kv.getValue().patch(this, access, name, descriptor, signature, exceptions);
                            isSuper = false;
                            return visitor;
                        }
                    }
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        });
        /*new ClassVisitor(Opcodes.ASM9, writer) {
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
        }*/
    }
}







