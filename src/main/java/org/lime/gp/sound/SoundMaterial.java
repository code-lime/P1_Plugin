package org.lime.gp.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import org.lime.gp.lime;
import org.lime.reflection;
import org.lime.system.map;

import java.lang.reflect.Field;
import java.util.*;

public enum SoundMaterial {
    AIR(SoundEffectType.EMPTY),
    STONE(SoundEffectType.LILY_PAD,
            SoundEffectType.STONE,
            SoundEffectType.BASALT,
            SoundEffectType.NETHERRACK,
            SoundEffectType.NETHER_BRICKS,
            SoundEffectType.NETHER_SPROUTS,
            SoundEffectType.NETHER_ORE,
            SoundEffectType.BONE_BLOCK,
            SoundEffectType.NETHERITE_BLOCK,
            SoundEffectType.ANCIENT_DEBRIS,
            SoundEffectType.LODESTONE,
            SoundEffectType.CHAIN,
            SoundEffectType.NETHER_GOLD_ORE,
            SoundEffectType.GILDED_BLACKSTONE,
            SoundEffectType.AMETHYST,
            SoundEffectType.AMETHYST_CLUSTER,
            SoundEffectType.SMALL_AMETHYST_BUD,
            SoundEffectType.MEDIUM_AMETHYST_BUD,
            SoundEffectType.LARGE_AMETHYST_BUD,
            SoundEffectType.TUFF,
            SoundEffectType.CALCITE,
            SoundEffectType.DRIPSTONE_BLOCK,
            SoundEffectType.POINTED_DRIPSTONE,
            SoundEffectType.COPPER,
            SoundEffectType.DEEPSLATE,
            SoundEffectType.DEEPSLATE_BRICKS,
            SoundEffectType.DEEPSLATE_TILES,
            SoundEffectType.POLISHED_DEEPSLATE,
            SoundEffectType.MUD_BRICKS,
            SoundEffectType.PACKED_MUD,
            SoundEffectType.DECORATED_POT,
            SoundEffectType.DECORATED_POT_CRACKED),
    WOOL(SoundEffectType.WOOL,
            SoundEffectType.SLIME_BLOCK,
            SoundEffectType.HONEY_BLOCK,
            SoundEffectType.FROGLIGHT),
    GRASS(SoundEffectType.GRAVEL,
            SoundEffectType.GRASS,
            SoundEffectType.SAND,
            SoundEffectType.WET_GRASS,
            SoundEffectType.CORAL_BLOCK,
            SoundEffectType.SWEET_BERRY_BUSH,
            SoundEffectType.CROP,
            SoundEffectType.HARD_CROP,
            SoundEffectType.VINE,
            SoundEffectType.NETHER_WART,
            SoundEffectType.STEM,
            SoundEffectType.NYLIUM,
            SoundEffectType.FUNGUS,
            SoundEffectType.ROOTS,
            SoundEffectType.SHROOMLIGHT,
            SoundEffectType.WEEPING_VINES,
            SoundEffectType.TWISTING_VINES,
            SoundEffectType.SOUL_SAND,
            SoundEffectType.SOUL_SOIL,
            SoundEffectType.WART_BLOCK,
            SoundEffectType.CANDLE,
            SoundEffectType.CAVE_VINES,
            SoundEffectType.SPORE_BLOSSOM,
            SoundEffectType.AZALEA,
            SoundEffectType.FLOWERING_AZALEA,
            SoundEffectType.MOSS_CARPET,
            SoundEffectType.PINK_PETALS,
            SoundEffectType.MOSS,
            SoundEffectType.BIG_DRIPLEAF,
            SoundEffectType.SMALL_DRIPLEAF,
            SoundEffectType.ROOTED_DIRT,
            SoundEffectType.HANGING_ROOTS,
            SoundEffectType.AZALEA_LEAVES,
            SoundEffectType.SCULK_SENSOR,
            SoundEffectType.SCULK_CATALYST,
            SoundEffectType.SCULK,
            SoundEffectType.SCULK_VEIN,
            SoundEffectType.SCULK_SHRIEKER,
            SoundEffectType.GLOW_LICHEN,
            SoundEffectType.CHERRY_SAPLING,
            SoundEffectType.CHERRY_LEAVES,
            SoundEffectType.SUSPICIOUS_SAND,
            SoundEffectType.SUSPICIOUS_GRAVEL),
    WATER(SoundEffectType.FROGSPAWN,
            SoundEffectType.MUDDY_MANGROVE_ROOTS,
            SoundEffectType.MUD),
    SNOW(SoundEffectType.SNOW, SoundEffectType.POWDER_SNOW),
    GLASS(SoundEffectType.GLASS, SoundEffectType.LANTERN),
    WOOD(SoundEffectType.WOOD,
            SoundEffectType.LADDER,
            SoundEffectType.BAMBOO,
            SoundEffectType.BAMBOO_SAPLING,
            SoundEffectType.SCAFFOLDING,
            SoundEffectType.MANGROVE_ROOTS,
            SoundEffectType.HANGING_SIGN,
            SoundEffectType.NETHER_WOOD_HANGING_SIGN,
            SoundEffectType.BAMBOO_WOOD_HANGING_SIGN,
            SoundEffectType.BAMBOO_WOOD,
            SoundEffectType.NETHER_WOOD,
            SoundEffectType.CHERRY_WOOD,
            SoundEffectType.CHERRY_WOOD_HANGING_SIGN,
            SoundEffectType.CHISELED_BOOKSHELF),
    METAL(SoundEffectType.METAL, SoundEffectType.ANVIL);

    private final List<SoundEffectType> materials;

    private static final HashMap<org.bukkit.Material, SoundEffectType> typeList = map.<org.bukkit.Material, SoundEffectType>of()
            .add(Arrays.asList(org.bukkit.Material.values()), k -> k, v -> SoundEffectType.STONE)
            .add(BuiltInRegistries.BLOCK.stream().map(Block::defaultBlockState).toList(), IBlockData::getBukkitMaterial, BlockBase.BlockData::getSoundType)
            .build();
    private static final HashMap<SoundEffectType, SoundMaterial> soundTypes = new HashMap<>();
    static {
        for (SoundMaterial material : SoundMaterial.values())
            material.materials.forEach(v -> soundTypes.put(v, material));
        for (Field field : SoundEffectType.class.getDeclaredFields()) {
            if (field.getType() == SoundMaterial.class) {
                try {
                    SoundEffectType type = (SoundEffectType)field.get(null);
                    if (soundTypes.containsKey(type)) continue;
                    lime.logOP("!!!WARNING!!! TYPE '"+reflection.name(field)+"' NOT REGISTERED IN 'SoundMaterial'");
                } catch (Exception ignored) {}
            }
        }
    }

    SoundMaterial(SoundEffectType... materials) {
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
                .map(BlockBase.BlockData::getSoundType)
                .map(soundTypes::get)
                .orElse(SoundMaterial.GRASS);
    }
}
























