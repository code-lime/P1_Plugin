package org.lime.gp;

import net.kyori.adventure.text.Component;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.block.BlockSweetBerryBush;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Material;
import org.lime.core;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.component.display.partial.Variable;
import org.lime.gp.block.component.display.partial.list.BlockPartial;
import org.lime.gp.block.component.list.CropsComponent;
import org.lime.gp.block.component.list.DisplayComponent;
import org.lime.gp.block.component.list.ShrubComponent;
import org.lime.gp.block.component.list.WaitingComponent;
import org.lime.gp.craft.RecipesBook;
import org.lime.gp.craft.recipe.WaitingRecipe;
import org.lime.gp.craft.slot.OutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.system;

import java.util.List;

public class test {
    public static core.element create() {
        return core.element.create(test.class)
                .withInit(test::init);
    }

    private static void enableBlocksAgeable() {
        org.lime.gp.block.Blocks.addDefaultBlocks(new BlockInfo("block.test.shrub")
                .add(info -> new DisplayComponent(info, List.of(
                        new BlockPartial(3, Blocks.DEAD_BUSH.defaultBlockState())
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.RED_WOOL
                                                .defaultBlockState()
                                        )).add("age", "0")
                                )
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.BLUE_WOOL
                                                .defaultBlockState()
                                        )).add("age", "1")
                                )
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.YELLOW_WOOL
                                                .defaultBlockState()
                                        )).add("age", "2")
                                )
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.LIME_WOOL
                                                .defaultBlockState()
                                        )).add("age", "3")
                                )
                )))
                .add(info -> new ShrubComponent(info, system.json.object()
                        .addObject("age", v -> v
                                .add("count", 3)
                                .add("step_ticks", 5 * 20)
                        )
                        .add("loot", "test.shrub*3")
                        .build())
                )
        );
        org.lime.gp.item.Items.addHardcodeItem("test.shrub",
                system.json.object()
                        .add("item", "STONE")
                        .add("id", -100)
                        .add("name", "Test Shrub")
                        .addObject("settings", v -> v
                                .addObject("block", _v -> _v
                                        .addObject("rotation", __v -> __v
                                                .add("0", "block.test.shrub")
                                        )
                                )
                        )
                        .build()
        );


        org.lime.gp.block.Blocks.addDefaultBlocks(new BlockInfo("block.test.crops")
                .add(info -> new DisplayComponent(info, List.of(
                        new BlockPartial(3, Blocks.DEAD_BUSH.defaultBlockState())
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.OAK_FENCE
                                                .defaultBlockState()
                                        )).add("has", "false")
                                )
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.OAK_FENCE
                                                .defaultBlockState()
                                        )).add("has", "true")
                                )
                )))
                .add(info -> new CropsComponent(info, system.json.object()
                        .add("offset", "0 0 0")
                        .add("filter", "test.crops")
                        .build())
                )
        );
        org.lime.gp.item.Items.addHardcodeItem("test.crops",
                system.json.object()
                        .add("item", "STONE")
                        .add("id", -101)
                        .add("name", "Test Crops")
                        .addObject("settings", v -> v
                                .addObject("block", _v -> _v
                                        .addObject("rotation", __v -> __v
                                                .add("0", "block.test.crops")
                                        )
                                )
                        )
                        .build()
        );
        org.lime.gp.item.Items.addHardcodeItem("test.seed.1",
                system.json.object()
                        .add("item", "FEATHER")
                        .add("id", -102)
                        .add("name", "Test Seed 1")
                        .addObject("settings", v -> v
                                .addObject("crops", _v -> _v
                                        .addObject("age", __v -> __v
                                                .add("count", 3)
                                                .add("step_ticks", 5 * 20)
                                        )
                                        .add("loot", "test.seed.1*3")
                                )
                                .addObject("table_display", _v -> _v
                                        .addObject("crops:0", __v -> __v
                                                .add("material", Material.RED_WOOL.toString())
                                        )
                                        .addObject("crops:1", __v -> __v
                                                .add("material", Material.BLUE_WOOL.toString())
                                        )
                                        .addObject("crops:2", __v -> __v
                                                .add("material", Material.YELLOW_WOOL.toString())
                                        )
                                        .addObject("crops:3", __v -> __v
                                                .add("material", Material.LIME_WOOL.toString())
                                        )
                                )
                        )
                        .build()
        );
        org.lime.gp.item.Items.addHardcodeItem("test.seed.2",
                system.json.object()
                        .add("item", "FEATHER")
                        .add("id", -103)
                        .add("name", "Test Seed 2")
                        .addObject("settings", v -> v
                                .addObject("crops", _v -> _v
                                        .addObject("age", __v -> __v
                                                .add("count", 3)
                                                .add("step_ticks", 5 * 20)
                                        )
                                        .add("loot", "test.seed.2*3")
                                )
                                .addObject("table_display", _v -> _v
                                        .addObject("crops:0", __v -> __v
                                                .add("material", Material.RED_CONCRETE.toString())
                                        )
                                        .addObject("crops:1", __v -> __v
                                                .add("material", Material.BLUE_CONCRETE.toString())
                                        )
                                        .addObject("crops:2", __v -> __v
                                                .add("material", Material.YELLOW_CONCRETE.toString())
                                        )
                                        .addObject("crops:3", __v -> __v
                                                .add("material", Material.LIME_CONCRETE.toString())
                                        )
                                )
                        )
                        .build()
        );
    }

    private static void enableBlockWaiting() {
        RecipesBook.addDefaultBook("waiting:test.waiting.thirst", Component.text("Ожидатель"));
        org.lime.gp.block.Blocks.addDefaultBlocks(new BlockInfo("test.waiting.block")
                .add(info -> new DisplayComponent(info, List.of(
                        new BlockPartial(3, Blocks.BARREL.defaultBlockState())
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.BARREL
                                                .defaultBlockState()
                                        ))
                                )
                )))
                .add(info -> new WaitingComponent(info, system.json.object()
                        .add("progress", 5)
                        .add("max_count", 3)
                        .add("type", "test.waiting.thirst")
                        .build())
                )
        );
        org.lime.gp.item.Items.addHardcodeItem("test.waiting.item.water",
                system.json.object()
                        .add("item", Material.POTION.name())
                        .add("id", -100)
                        .add("name", "test.waiting.item.water")
                        .addObject("settings", v -> v
                                .addObject("thirst", _v -> _v
                                        .add("type", "test.waiting.thirst")
                                        .add("color", "#FF0000")
                                )
                        )
                        .build()
        );
        org.lime.gp.item.Items.addHardcodeItem("test.waiting.item.water.ignore",
                system.json.object()
                        .add("item", Material.POTION.name())
                        .add("id", -101)
                        .add("name", "test.waiting.item.water.ignore")
                        .addObject("settings", v -> v
                                .addObject("thirst", _v -> _v
                                        .add("type", "test.waiting.thirst.ignore")
                                        .add("color", "#FF0000")
                                )
                        )
                        .build()
        );
        org.lime.gp.item.Items.addHardcodeItem("test.waiting.item.single",
                system.json.object()
                        .add("item", Material.STONE.name())
                        .add("id", -102)
                        .add("name", "test.waiting.item.single")
                        .build()
        );
        org.lime.gp.craft.Crafts.addDefaultCraft(new WaitingRecipe(
                MinecraftKey.tryBuild("lime","craft.test.waiting.water"),
                "",
                CraftingBookCategory.MISC,
                RecipeSlot.of(Checker.createCheck("test.waiting.item.water")),
                List.of(
                        RecipeSlot.of(Checker.createCheck(Items.getMaterialKey(Material.COAL))).withAmount(1)
                ),
                List.of(
                        RecipeSlot.of(Checker.createCheck(Items.getMaterialKey(Material.ACACIA_LOG))).withAmount(1),
                        RecipeSlot.of(Checker.createCheck(Items.getMaterialKey(Material.STONE))).withAmount(3)
                ),
                OutputSlot.of(Material.BEDROCK.name() + "*5"),
                30,
                "test.waiting.thirst"));
        org.lime.gp.craft.Crafts.addDefaultCraft(new WaitingRecipe(
                MinecraftKey.tryBuild("lime","craft.test.waiting.single"),
                "",
                CraftingBookCategory.MISC,
                RecipeSlot.of(Checker.createCheck("test.waiting.item.single")),
                List.of(),
                List.of(),
                OutputSlot.of(Material.RED_CONCRETE.name() + "*2"),
                10,
                "test.waiting.thirst"));
    }

    private static void init() {
        //enableBlocksAgeable();
        enableBlockWaiting();
    }
}



















