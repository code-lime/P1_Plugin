package patch;

public class PatchException extends RuntimeException {
    public PatchException(String message, JarArchive jar, String className) {
        super(message + " (" + jar.name + "::" + className + ")");
    }
    public PatchException(String message, JarArchive jar, IMethodInfo method) {
        super(message + " (" + jar.name + "::" + method.toInfo() + ")");
    }
}
