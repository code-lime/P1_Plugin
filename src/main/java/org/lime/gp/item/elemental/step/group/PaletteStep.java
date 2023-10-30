package org.lime.gp.item.elemental.step.group;

import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.elemental.step.action.NoneStep;
import org.lime.system.utils.MathUtils;

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

    @Override public void execute(Player player, DataContext context, Transformation location) {
        float offsetX = -sizeX / 2.0f + 0.5f;
        float offsetY = -sizeY / 2.0f + 0.5f;
        float offsetZ = -sizeZ / 2.0f + 0.5f;
        Quaternionf leftRotation = location.getLeftRotation();
        Quaternionf rightRotation = location.getRightRotation();
        Transformation nonRotation = new Transformation(location.getTranslation(), null, location.getScale(), null);
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    Transformation transformation = MathUtils.transform(nonRotation, new Transformation(new Vector3f(x + offsetX, y + offsetY, z + offsetZ), null, null, null));
                    matrix[(z + x * sizeZ) * sizeY + y].execute(player, context, new Transformation(transformation.getTranslation(), leftRotation, transformation.getScale(), rightRotation));
                }
            }
        }
    }
}
