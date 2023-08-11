package org.lime.gp.item.elemental.step.group;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.elemental.step.action.NoneStep;

import java.util.List;
import java.util.Map;

public record PaletteStep(IStep[] matrix, int sizeX, int sizeY, int sizeZ) implements IStep {
    private static IStep[] createMatrix(Map<String, IStep> palette, List<List<String>> map, int sizeX, int sizeY, int sizeZ) {
        IStep[] matrix = new IStep[sizeX * sizeY * sizeZ];
        for (int y = 0; y < sizeY; y++) {
            List<String> layer = map.get(y);
            for (int z = 0; z < sizeZ; z++) {
                String line = layer.get(z);
                for (int x = 0; x < sizeX; x++) {
                    String key = String.valueOf(line.charAt(x));
                    matrix[(z + x * sizeZ) * sizeY + y] = palette.getOrDefault(key, NoneStep.Instance);
                }
            }
        }
        return matrix;
    }

    private PaletteStep(Map<String, IStep> palette, List<List<String>> map, int sizeX, int sizeY, int sizeZ) {
        this(createMatrix(palette, map, sizeX, sizeY, sizeZ), sizeX, sizeY, sizeZ);
    }
    private PaletteStep(Map<String, IStep> palette, List<List<String>> map, int sizeY, int sizeZ) {
        this(palette, map, sizeZ > 0 ? map.get(0).get(0).length() : 0, sizeY, sizeZ);
    }
    private PaletteStep(Map<String, IStep> palette, List<List<String>> map, int sizeY) {
        this(palette, map, sizeY, sizeY > 0 ? map.get(0).size() : 0);
    }
    public PaletteStep(Map<String, IStep> palette, List<List<String>> map) {
        this(palette, map, map.size());
    }

    @Override public void execute(Player player, LocalLocation location) {
        double offsetX = -sizeX / 2.0 + 0.5;
        double offsetY = -sizeY / 2.0 + 0.5;
        double offsetZ = -sizeZ / 2.0 + 0.5;
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    matrix[(z + x * sizeZ) * sizeY + y].execute(player, location.add(x + offsetX, y + offsetY, z + offsetZ));
                }
            }
        }
    }
}
