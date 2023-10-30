package patch;

public interface IMethodInfo {
    String toInfo();

    static IMethodInfo raw(String className, String methodName, String methodDescriptor) {
        return () -> className + "." + methodName + methodDescriptor;
    }
    static IMethodInfo raw(Class<?> classType, String methodName, String methodDescriptor) {
        return () -> classType.getSimpleName() + "." + methodName + methodDescriptor;
    }
}
