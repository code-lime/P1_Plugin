package patch;

import io.papermc.paper.util.ObfHelper;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.lime.gp.TestData;
import org.lime.system;
import org.objectweb.asm.*;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class Native {
    public static String sha256(byte[] bytes) throws Throwable {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    public static String classFile(Class<?> tClass) { return className(tClass) + ".class"; }
    public static String className(Class<?> tClass) { return tClass.getName().replace('.', '/'); }

    /*public static Type replaceDescriptor(Type type, String from, String to) {
        return Type.getType(type.getDescriptor().replace(from, to));
    }*/

    private static @Nullable String prefix = null;
    public static void log(String log) {
        System.out.println(prefix == null ? log : (prefix + log));
    }
    public interface ICloseable extends Closeable {  @Override void close(); }
    public interface IAction { void execute(); }
    public static ICloseable subLog() {
        String oldPrefix = prefix;
        prefix = prefix == null ? " - " : ("   " + prefix);
        return () -> prefix = oldPrefix;
    }
    public static void subLog(IAction subLogger) {
        try (var ignored = subLog()) { subLogger.execute(); }
    }
    public static void subLog(String log, IAction subLogger) {
        log(log);
        subLog(subLogger);
    }

    public static void writeMethod(system.ICallable callable, system.Action5<Integer, String, String, String, Boolean> method) {
        SerializedLambda lambda = infoFromLambda(callable);
        String kind = MethodHandleInfo.referenceKindToString(lambda.getImplMethodKind());
        int opcode = switch (kind) {
            case "invokeVirtual" -> Opcodes.INVOKEVIRTUAL;
            case "invokeStatic" -> Opcodes.INVOKESTATIC;
            case "invokeSpecial", "newInvokeSpecial" -> Opcodes.INVOKESPECIAL;
            case "invokeInterface" -> Opcodes.INVOKEINTERFACE;
            case "getField", "getStatic", "putField", "putStatic" -> throw new IllegalArgumentException("Kind method type '"+kind+"' can be only invokable!");
            default -> throw new IllegalArgumentException("Kind method type '"+kind+"' not supported!");
        };
        boolean isInterface = opcode == Opcodes.INVOKEINTERFACE
                || system.funcEx(() -> Class.forName(lambda.getImplClass().replace('/', '.')).isInterface())
                    .optional().invoke().orElse(false);
        method.invoke(opcode, lambda.getImplClass(), lambda.getImplMethodName(), lambda.getImplMethodSignature(), isInterface);
    }
    public static void writeField(int opcode, system.ICallable callable, system.Action4<Integer, String, String, String> field) {
        FieldInfo info = infoFromField(callable);
        field.invoke(opcode, info.owner(), info.name(), info.descriptor());
    }

    private static Method getMethod(String owner, String name, String descriptor) {
        try {
            Class<?> _owner = Class.forName(owner.replace('/', '.'));
            for (Method method : _owner.getMethods()) {
                if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(descriptor))
                    return method;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        throw new RuntimeException("Method " + owner + "." + name + descriptor + " not founded");
    }
    private static Method getMethod(SerializedLambda lambda) {
        return getMethod(lambda.getImplClass(), lambda.getImplMethodName(), lambda.getImplMethodSignature());
    }

    public static boolean isMethod(system.ICallable callable, String owner, String name, String descriptor) {
        SerializedLambda lambda = infoFromLambda(callable);
        if (!lambda.getImplMethodName().equals(name) || !Type.getType(lambda.getImplMethodSignature()).equals(Type.getType(descriptor))) return false;
        if (lambda.getImplClass().equals(owner)) return true;
        return getMethod(owner, name, descriptor).equals(getMethod(lambda));
    }
    public static boolean isField(system.ICallable callable, String owner, String name, String descriptor) {
        FieldInfo field = infoFromField(callable);
        return field.owner().equals(owner)
                && field.name().equals(name)
                && Type.getType(field.descriptor()).equals(Type.getType(descriptor));
    }

    public static SerializedLambda infoFromLambda(Serializable lambda) {
        try {
            Method m = lambda.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);
            return (SerializedLambda)m.invoke(lambda);
        } catch(ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
    public record FieldInfo(String owner, String name, String descriptor) { }
    public static FieldInfo infoFromField(system.ICallable callable) {
        try {
            SerializedLambda lambda = infoFromLambda(callable);
            String classFile = lambda.getImplClass();
            String methodOwner = classFile.replace('/', '.');
            Class<?> tClass = Class.forName(methodOwner);

            String methodName = lambda.getImplMethodName();
            String methodDescriptor = lambda.getImplMethodSignature();

            //Native.log("Lambda method: " + methodOwner + "." + methodName + methodDescriptor);
            try (InputStream stream = Objects.requireNonNull(tClass.getClassLoader().getResourceAsStream(classFile + ".class"))) {
                ClassReader cr = new ClassReader(stream);
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                system.Toast1<FieldInfo> fieldInfo = system.toast(null);
                cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                    @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        //Native.log("Compare method with: " + methodOwner.replace('/', '.') + "." + name + descriptor);
                        if (name.equals(methodName) && descriptor.equals(methodDescriptor)) {
                            //Native.log("Read method fileds");
                            return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                    fieldInfo.val0 = new FieldInfo(owner, name, descriptor);
                                    //Native.log("Field: " + fieldInfo.val0);
                                    super.visitFieldInsn(opcode, owner, name, descriptor);
                                }
                            };
                        }
                        return super.visitMethod(access, name, descriptor, signature, exceptions);
                    }
                }, 0);
                return Objects.requireNonNull(fieldInfo.val0);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static MemoryMappingTree tree;
    public static Closeable loadDeobf() throws Throwable {
        InputStream mappingsInputStream = ObfHelper.class.getClassLoader().getResourceAsStream("META-INF/mappings/reobf.tiny");
        tree = new MemoryMappingTree();
        if (mappingsInputStream == null) return () -> {};
        MappingReader.read(new InputStreamReader(mappingsInputStream, StandardCharsets.UTF_8), MappingFormat.TINY_2, tree);
        for (MappingTree.ClassMapping classMapping : tree.getClasses()) {
            Map<system.Toast3<String, String, Boolean>, String> members = new HashMap<>();
            for (MappingTree.MemberMapping member : classMapping.getMethods()) {
                members.put(system.toast(member.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE), member.getDesc(ObfHelper.SPIGOT_NAMESPACE), true), member.getName(ObfHelper.SPIGOT_NAMESPACE));
            }
            for (MappingTree.MemberMapping member : classMapping.getFields()) {
                members.put(system.toast(member.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE), member.getDesc(ObfHelper.SPIGOT_NAMESPACE), false), member.getName(ObfHelper.SPIGOT_NAMESPACE));
            }
            classes.put(classMapping.getName(ObfHelper.SPIGOT_NAMESPACE).replace('/', '.'), members);
        }
        return mappingsInputStream;
    }
    private static final Map<String, Map<system.Toast3<String, String, Boolean>, String>> classes = new HashMap<>();
    private static final List<Class<?>> dat = new ArrayList<>();
    public static String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod) {
        Map<system.Toast3<String, String, Boolean>, String> mapping = classes.get(tClass.getName());
        if (mapping == null) return name;
        if (dat.contains(tClass)) {
            log("Class " + tClass.getName() + " with found " + (isMethod ? "method" : "field") + " " + name + desc + "\n" + system.json.object().add(mapping, system.IToast::toString, v -> v).build().toString());
            dat.add(tClass);
        }
        //
        String src_name = mapping.get(system.toast(name, desc, isMethod));
        boolean isFound = src_name != null;
        if (src_name == null) {
            src_name = name;
        }
        return src_name;
    }
    public static String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return ofMojang(tClass, name, desc.getDescriptor(), isMethod);
    }
}
