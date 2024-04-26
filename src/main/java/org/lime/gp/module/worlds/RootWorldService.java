package org.lime.gp.module.worlds;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.gp.player.module.Login;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RootWorldService implements IWorldService {
    private static final Path root = Bukkit.getWorldContainer().toPath();
    private static File getFolder(String sourceName) {
        return root.resolve(sourceName).toFile();
    }

    @Override public String load(String sourceName) {
        lime.logOP("World '"+sourceName+"' loading...");
        new WorldCreator(sourceName).createWorld();
        lime.logOP("World '"+sourceName+"' loaded");
        return sourceName;
    }
    @Override public String unload(String sourceName, boolean save) {
        World world = Bukkit.getWorld(sourceName);
        if (world == null)
            return sourceName;

        for (Player player : world.getPlayers())
            player.teleport(Login.getMainLocation(player));

        lime.logOP("World '"+sourceName+"' unloading...");
        Bukkit.getServer().unloadWorld(world, save);
        lime.logOP("World '"+sourceName+"' unloaded");
        return sourceName;
    }
    @Override public String copy(String sourceName, String targetName, Action1<String> callback) {
        delete(targetName);
        lime.logOP("World '"+sourceName+"' copy to '"+targetName+"'...");
        lime.invokeAsync(() -> copyFileStructure(getFolder(sourceName), getFolder(targetName)), () -> {
            lime.logOP("World '"+sourceName+"' copied to '"+targetName+"'");
            load(targetName);
            callback.invoke(targetName);
        });
        return targetName;
    }
    private void copyFileStructure(File source, File target){
        try {
            ArrayList<String> ignore = new ArrayList<>(Arrays.asList("uid.dat", "session.lock"));

            try { Files.delete(Path.of(target.getAbsolutePath())); }
            catch (Exception ignored) {}

            final String[] EMPTY = new String[0];

            if(!ignore.contains(source.getName())) {
                if(source.isDirectory()) {
                    if(!target.exists())
                        if (!target.mkdirs())
                            throw new IOException("Couldn't create world directory!");
                    for (String file : Objects.requireNonNullElse(source.list(), EMPTY)) {
                        File srcFile = new File(source, file);
                        File destFile = new File(target, file);
                        copyFileStructure(srcFile, destFile);
                    }
                } else {
                    InputStream in = new FileInputStream(source);
                    OutputStream out = new FileOutputStream(target);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0)
                        out.write(buffer, 0, length);
                    in.close();
                    out.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override public String delete(String sourceName) {
        unload(sourceName, false);
        lime.logOP("World '"+sourceName+"' deleting...");
        try { FileUtils.deleteDirectory(getFolder(sourceName)); }
        catch (Exception ignored) {}
        lime.logOP("World '"+sourceName+"' deleted");
        return sourceName;
    }
    @Override public World rawWorld(String sourceName) {
        return Bukkit.getWorld(sourceName);
    }

    @Override public Map<String, Boolean> list() {
        return FileUtils.listFiles(root.toFile(), new String[] { "dat" }, true)
                .stream()
                .filter(v -> v.getName().equals("uid.dat"))
                .map(v -> root.relativize(v.toPath().getParent()).toString())
                .collect(Collectors.toMap(v -> v, v -> Bukkit.getWorld(v) != null));
    }
}
