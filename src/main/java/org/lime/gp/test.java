package org.lime.gp;

import net.kyori.adventure.text.Component;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.commands.CommandMe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.component.display.partial.Variable;
import org.lime.gp.block.component.display.partial.list.BlockPartial;
import org.lime.gp.block.component.list.*;
import org.lime.gp.craft.book.RecipesBook;
import org.lime.gp.craft.recipe.WaitingRecipe;
import org.lime.gp.craft.slot.output.IOutputSlot;
import org.lime.gp.craft.slot.RecipeSlot;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.system;

import java.util.List;

public class test {
    public static CoreElement create() {
        return CoreElement.create(test.class)
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
                true,
                IOutputSlot.ofString(Material.BEDROCK.name() + "*5"),
                30,
                "test.waiting.thirst"));
        org.lime.gp.craft.Crafts.addDefaultCraft(new WaitingRecipe(
                MinecraftKey.tryBuild("lime","craft.test.waiting.single"),
                "",
                CraftingBookCategory.MISC,
                RecipeSlot.of(Checker.createCheck("test.waiting.item.single")),
                List.of(),
                List.of(),
                true,
                IOutputSlot.ofString(Material.RED_CONCRETE.name() + "*2"),
                10,
                "test.waiting.thirst"));
    }

    private static void enableArmorTags() {
        org.lime.gp.item.Items.addHardcodeItem("test.armor.tag1",
                system.json.object()
                        .add("item", Material.GRASS_BLOCK.name())
                        .add("id", -100)
                        .add("name", "test.armor.tag1")
                        .addObject("settings", v -> v
                                .add("equip", "head")
                                .add("armor_tag", "tmp")
                        )
                        .build()
        );
        org.lime.gp.item.Items.addHardcodeItem("test.armor.tag2",
                system.json.object()
                        .add("item", Material.DIRT_PATH.name())
                        .add("id", -101)
                        .add("name", "test.armor.tag2")
                        .addObject("settings", v -> v
                                .add("equip", "legs")
                                .addArray("armor_tag", _v -> _v.add("tmp2").add("tmp33"))
                        )
                        .build()
        );
    }

    private static void enableBlockLimit() {
        org.lime.gp.block.Blocks.addDefaultBlocks(new BlockInfo("block.test.limit.3")
                .add(info -> new DisplayComponent(info, List.of(
                        new BlockPartial(3, Blocks.BEDROCK.defaultBlockState())
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.BEDROCK
                                                .defaultBlockState()
                                        ))
                                )
                )))
                .add(info -> new LimitComponent(info, system.json.object()
                        .add("type", "test_limit_3")
                        .build())
                )
        );
        org.lime.gp.block.Blocks.addDefaultBlocks(new BlockInfo("block.test.limit.3.double")
                .add(info -> new DisplayComponent(info, List.of(
                        new BlockPartial(3, Blocks.BEDROCK.defaultBlockState())
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.BEDROCK
                                                .defaultBlockState()
                                        ))
                                )
                )))
                .add(info -> new LimitComponent(info, system.json.object()
                        .add("type", "test_limit_3")
                        .build())
                )
        );
        org.lime.gp.block.Blocks.addDefaultBlocks(new BlockInfo("block.test.limit.4")
                .add(info -> new DisplayComponent(info, List.of(
                        new BlockPartial(3, Blocks.GLASS.defaultBlockState())
                                .addVariable(new Variable(
                                        new BlockPartial(3, Blocks.GLASS
                                                .defaultBlockState()
                                        ))
                                )
                )))
                .add(info -> new LimitComponent(info, system.json.object()
                        .add("type", "test_limit_4")
                        .build())
                )
        );
        org.lime.gp.item.Items.addHardcodeItem("test.limit.3",
                system.json.object()
                        .add("item", Material.STRUCTURE_BLOCK.name())
                        .add("id", -100)
                        .add("name", "Test Limit (3)")
                        .addObject("settings", v -> v
                                .addObject("block", _v -> _v
                                        .addObject("rotation", __v -> __v
                                                .add("0", "block.test.limit.3")
                                        )
                                )
                                .addObject("block_limit", _v -> _v
                                        .add("limit", 3)
                                        .add("type", "test_limit_3")
                                )
                        )
                        .build()
        );
        org.lime.gp.item.Items.addHardcodeItem("test.limit.3.double",
                system.json.object()
                        .add("item", Material.STRUCTURE_BLOCK.name())
                        .add("id", -100)
                        .add("name", "Test Limit (3) Double")
                        .addObject("settings", v -> v
                                .addObject("block", _v -> _v
                                        .addObject("rotation", __v -> __v
                                                .add("0", "block.test.limit.3.double")
                                        )
                                )
                                .addObject("block_limit", _v -> _v
                                        .add("limit", 3)
                                        .add("type", "test_limit_3")
                                )
                        )
                        .build()
        );
        org.lime.gp.item.Items.addHardcodeItem("test.limit.4",
                system.json.object()
                        .add("item", Material.STRUCTURE_BLOCK.name())
                        .add("id", -101)
                        .add("name", "Test Limit (4)")
                        .addObject("settings", v -> v
                                .addObject("block", _v -> _v
                                        .addObject("rotation", __v -> __v
                                                .add("0", "block.test.limit.4")
                                        )
                                )
                                .addObject("block_limit", _v -> _v
                                        .add("limit", 4)
                                        .add("type", "test_limit_4")
                                )
                        )
                        .build()
        );
    }
    private static void enableLevelMutate() {
        org.lime.gp.item.Items.addHardcodeItem("test.level.mutate.3",
                system.json.object()
                        .add("item", Material.CARROT.name())
                        .add("id", -100)
                        .add("name", "Test Level Mutate (3 sec)")
                        .addObject("settings", v -> v
                                .addObject("level_food_mutate", _v -> _v
                                        .add("sec", 3)
                                )
                        )
                        .build()
        );
        org.lime.gp.item.Items.addHardcodeItem("test.level.mutate.10",
                system.json.object()
                        .add("item", Material.APPLE.name())
                        .add("id", -101)
                        .add("name", "Test Level Mutate (10 sec)")
                        .addObject("settings", v -> v
                                .addObject("level_food_mutate", _v -> _v
                                        .add("sec", 10)
                                )
                        )
                        .build()
        );
    }

    public static void testMatches() {
        AnyEvent.addEvent("test.matches", AnyEvent.type.owner_console, v -> v.createParam("item_a").createParam("item_b"), (player, a, b) -> {
            ItemStack itemA = Items.createItem(a).map(CraftItemStack::asNMSCopy).orElseThrow();
            ItemStack itemB = Items.createItem(b).map(CraftItemStack::asNMSCopy).orElseThrow();

            boolean matches = net.minecraft.world.item.ItemStack.matches(itemA, itemB);
            boolean same = net.minecraft.world.item.ItemStack.isSame(itemA, itemB);

            lime.logOP(String.join("\n",
                    "Matches: " + matches,
                    "Same: " + same,
                    "IsReset: " + (!matches && !same)
            ));
        });
    }

    private static void init() {
        //enableBlocksAgeable();
        //enableBlockWaiting();
        //enableArmorTags();
        //enableBlockLimit();
        //enableLevelMutate();
        //testMatches();
    }
}



















