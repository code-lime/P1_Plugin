package patch;

import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.List;

public abstract class ProgressMethodVisitor extends PatchMethodVisitor {
    private final HashMap<String, Boolean> progressList = new HashMap<>();

    protected ProgressMethodVisitor(JarArchive jar, IMethodInfo method, MethodVisitor methodVisitor) {
        super(jar, method, methodVisitor);
        createProgressList().forEach(k -> progressList.put(k, false));
    }
    protected ProgressMethodVisitor(PatchMethodVisitor link, MethodVisitor methodVisitor) {
        super(link.jar, link.method, methodVisitor);
        createProgressList().forEach(k -> progressList.put(k, false));
    }

    protected abstract List<String> createProgressList();
    protected void setProgress(String name) {
        Boolean oldValue = progressList.get(name);
        if (oldValue == null)
            throw new PatchException("Progress '"+name+"' not registered", jar, method);
        if (oldValue)
            throw new PatchException("Duplicate progress '"+name+"' marked", jar, method);
        progressList.put(name, true);
    }
    protected void setProgressDuplicate(String name) {
        Boolean oldValue = progressList.get(name);
        if (oldValue == null)
            throw new PatchException("Progress '"+name+"' not registered", jar, method);
        progressList.put(name, true);
    }
    public void throwProgressCheck() {
        progressList.forEach((key, value) -> {
            if (!value)
                throw new PatchException("Progress '"+key+"' not marked", jar, method);
        });
    }

    public static ProgressMethodVisitor none(PatchMethodVisitor methodVisitor) {
        return new ProgressMethodVisitor(methodVisitor, methodVisitor) {
            @Override protected List<String> createProgressList() { return List.of(); }
        };
    }
}
