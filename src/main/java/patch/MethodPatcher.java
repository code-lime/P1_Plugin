package patch;

import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import javax.annotation.Nullable;

public class MethodPatcher {
    private final Func1<MethodVisitor, MethodVisitor> mutate;

    private MethodPatcher(Func1<MethodVisitor, MethodVisitor> mutate) { this.mutate = mutate; }

    private @Nullable Func1<Integer, Integer> access = null;
    private @Nullable Func1<String, String> name = null;
    private @Nullable Func1<String, String> descriptor = null;
    private @Nullable Func1<String, String> signature = null;
    private @Nullable Func1<String[], String[]> exceptions = null;

    public MethodPatcher access(int access) { return this.access(v -> access); }
    public MethodPatcher name(String name) { return this.name(v -> name); }
    public MethodPatcher descriptor(String descriptor) { return this.descriptor(v -> descriptor); }
    public MethodPatcher signature(String signature) { return this.signature(v -> signature); }
    public MethodPatcher exceptions(String[] exceptions) { return this.exceptions(v -> exceptions); }

    public MethodPatcher access(Func1<Integer, Integer> access) { this.access = access; return this; }
    public MethodPatcher name(Func1<String, String> name) { this.name = name; return this; }
    public MethodPatcher descriptor(Func1<String, String> descriptor) { this.descriptor = descriptor; return this; }
    public MethodPatcher signature(Func1<String, String> signature) { this.signature = signature; return this; }
    public MethodPatcher exceptions(Func1<String[], String[]> exceptions) { this.exceptions = exceptions; return this; }

    public MethodVisitor patch(ClassVisitor visitor, int access, String name, String descriptor, String signature, String[] exceptions) {
        return mutate.invoke(visitor.visitMethod(
                this.access == null ? access : this.access.invoke(access),
                this.name == null ? name : this.name.invoke(name),
                this.descriptor == null ? descriptor : this.descriptor.invoke(descriptor),
                this.signature == null ? signature : this.signature.invoke(signature),
                this.exceptions == null ? exceptions : this.exceptions.invoke(exceptions)
        ));
    }

    public static MethodPatcher mutate(Func1<MethodVisitor, MethodVisitor> mutate) { return new MethodPatcher(mutate); }
    public static MethodPatcher none() { return mutate(v -> v); }
}
