package org.lime.gp.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Material;
import org.lime.system.map;

import java.util.*;

public enum SoundMaterial {
    AIR(Material.AIR, Material.STRUCTURAL_AIR, Material.FIRE, Material.EXPLOSIVE, Material.BARRIER),
    STONE(Material.STONE, Material.PORTAL, Material.PISTON),
    WOOL(Material.WOOL, Material.CLOTH_DECORATION, Material.DECORATION, Material.WEB, Material.SPONGE, Material.SHULKER_SHELL, Material.CAKE),
    GRASS(Material.GRASS, Material.DIRT, Material.CLAY, Material.PLANT, Material.REPLACEABLE_PLANT, Material.WATER_PLANT, Material.REPLACEABLE_FIREPROOF_PLANT, Material.REPLACEABLE_WATER_PLANT, Material.SCULK, Material.SAND, Material.MOSS, Material.VEGETABLE),
    WATER(Material.WATER, Material.BUBBLE_COLUMN, Material.LAVA),
    SNOW(Material.SNOW, Material.TOP_SNOW, Material.POWDER_SNOW),
    GLASS(Material.GLASS, Material.ICE, Material.ICE_SOLID, Material.BUILDABLE_GLASS, Material.EGG, Material.AMETHYST),
    WOOD(Material.WOOD, Material.NETHER_WOOD, Material.BAMBOO_SAPLING, Material.BAMBOO, Material.LEAVES, Material.CACTUS),
    METAL(Material.METAL, Material.HEAVY_METAL);

    private final List<net.minecraft.world.level.material.Material> materials;

    private static final HashMap<org.bukkit.Material, net.minecraft.world.level.material.Material> typeList = map.<org.bukkit.Material, net.minecraft.world.level.material.Material>of()
            .add(Arrays.asList(org.bukkit.Material.values()), k -> k, v -> net.minecraft.world.level.material.Material.STONE)
            .add(BuiltInRegistries.BLOCK.stream().map(Block::defaultBlockState).toList(), IBlockData::getBukkitMaterial, BlockBase.BlockData::getMaterial)
            .build();
    private static final HashMap<net.minecraft.world.level.material.Material, SoundMaterial> soundTypes = new HashMap<>();
    static {
        for (SoundMaterial material : SoundMaterial.values())
            material.materials.forEach(v -> soundTypes.put(v, material));
    }

    SoundMaterial(net.minecraft.world.level.material.Material... materials) {
        this.materials = Arrays.asList(materials);
    }

    public static SoundMaterial of(org.bukkit.Material material) {
        return Optional.ofNullable(material)
                .map(typeList::get)
                .map(soundTypes::get)
                .orElse(SoundMaterial.GRASS);
    }
    public static SoundMaterial of(IBlockData state) {
        return Optional.ofNullable(state)
                .map(BlockBase.BlockData::getMaterial)
                .map(soundTypes::get)
                .orElse(SoundMaterial.GRASS);
    }
}
























