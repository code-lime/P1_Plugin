package patch;

import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.objectweb.asm.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

//public float getTemperature(BlockPosition blockPos) {
public class JarArchive {
    public final String name;
    public final Manifest manifest;
    public final HashMap<String, byte[]> entries = new HashMap<>();

    private JarArchive(String name, Manifest manifest) {
        this.name = name;
        this.manifest = manifest;
    }

    public static JarArchive of(String name, Path path) throws Throwable { return of(name, Files.readAllBytes(path)); }
    public static JarArchive of(String name, byte[] bytes) throws Throwable {
        try (JarInputStream zis = new JarInputStream(new ByteArrayInputStream(bytes))) {
            JarArchive archive = new JarArchive(name, zis.getManifest());
            JarEntry jarEntry;
            while (true) {
                jarEntry = zis.getNextJarEntry();
                if (jarEntry == null) break;
                String entryName = jarEntry.getName();
                byte[] entryBytes;
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    for (int c = zis.read(); c != -1; c = zis.read()) stream.write(c);
                    entryBytes = stream.toByteArray();
                }
                zis.closeEntry();
                archive.entries.put(entryName, entryBytes);
            }
            return archive;
        }
    }

    public byte[] toByteArray() throws Throwable {
        try (ByteArrayOutputStream array = new ByteArrayOutputStream(); JarOutputStream zos = new JarOutputStream(array, manifest)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new JarEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
            zos.close();
            return array.toByteArray();
        }
    }
    public void toFile(Path path) throws Throwable {
        Files.write(path, toByteArray());
    }

    public <T>ClassPatcher<T> of(Class<T> tClass) {
        return new ClassPatcher<T>(this, tClass);
    }

    public <T>JarArchive patchMethod(IMethodFilter<T> filter, MethodPatcher patcher) {
        return of(filter.tClass()).patchMethod(filter, patcher).patch();
    }

    public <T>JarArchive patch(Class<T> tClass, Func1<ClassWriter, ClassVisitor> visitor) {
        String className = Native.classFile(tClass);
        Optional.ofNullable(this.entries.get(className))
                .ifPresentOrElse(bytes -> {
                    Native.log("Patch " + className + ":");
                    Native.subLog(() -> {
                        ClassReader reader = new ClassReader(bytes);
                        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                        reader.accept(visitor.invoke(writer), 0);
                        this.entries.put(className, writer.toByteArray());
                    });
                    Native.log("Patch " + className + " saved!");
                }, () -> Native.log("File '" + className + "' not founded in '"+this.name+"'"));
        return this;
    }
}













