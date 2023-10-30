package patch;

import org.lime.system.execute.Execute;
import org.lime.system.execute.ICallable;
import org.objectweb.asm.Type;

import java.lang.invoke.SerializedLambda;

public interface IMethodFilter<T> extends IMethodInfo {
    Class<T> tClass();
    boolean test(int access, String name, String descriptor, String signature, String[] exceptions);
    String toInfo();

    static <T>IMethodFilter<T> of(ICallable callable) {
        SerializedLambda lambda = Native.infoFromLambda(callable);
        Class<T> methodClass = Execute.funcEx(() -> (Class<T>)Class.forName(lambda.getImplClass().replace('/','.'))).throwable().invoke();
        String methodName = lambda.getImplMethodName();
        String methodDescriptor = lambda.getImplMethodSignature();
        return new IMethodFilter<T>() {
            @Override public Class<T> tClass() { return methodClass; }
            @Override public boolean test(int access, String name, String descriptor, String signature, String[] exceptions) {
                return name.equals(methodName) && descriptor.equals(methodDescriptor);
            }
            @Override public String toInfo() { return methodClass.getSimpleName() + "." + methodName + methodDescriptor; }
        };
    }
    static <T>IMethodFilter<T> of(Class<T> tClass, String methodName, Type methodDescriptor, boolean isMojang) {
        return new IMethodFilter<T>() {
            @Override public Class<T> tClass() { return tClass; }
            @Override public boolean test(int access, String name, String descriptor, String signature, String[] exceptions) {
                return (isMojang
                        ? Native.getMojangName(tClass, methodName, descriptor, true)
                        : methodName
                ).equals(name) && Type.getType(descriptor).equals(methodDescriptor);
            }
            @Override public String toInfo() { return tClass.getSimpleName() + "." + methodName + methodDescriptor; }
        };
    }
    static <T>IMethodFilter<T> of(Class<T> tClass, String methodName, boolean isMojang) {
        return new IMethodFilter<T>() {
            @Override public Class<T> tClass() { return tClass; }
            @Override public boolean test(int access, String name, String descriptor, String signature, String[] exceptions) {
                return (isMojang
                        ? Native.getMojangName(tClass, methodName, descriptor, true)
                        : methodName
                ).equals(name);
            }
            @Override public String toInfo() { return tClass.getSimpleName() + "." + methodName + "(*)*"; }
        };
    }
}
















