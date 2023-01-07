package org.lime.gp.block.component.data;

import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.lime.Position;
import org.lime.core;
import org.lime.gp.block.BlocksOld;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.anvil.AnvilDisplay;
import org.lime.gp.block.component.data.anvil.IRecipe;
import org.lime.gp.display.Displays;
import org.lime.gp.display.MultiDisplayManager;
import org.lime.gp.lime;
import org.lime.gp.block.BlockMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class AnvilBlockData implements Listener {
    public static core.element create() {
        BlocksOld.addDefaultBlocks(new BlocksOld.InfoCreator("anvil", creator -> creator.add(InfoComponent.GenericDynamicComponent.of("anvil", creator, Info::new)))
                .addReplace(Material.ANVIL, block -> block.getBlockData() instanceof Directional directional ? directional.getFacing() : BlockFace.NORTH,
                        (face, info) -> info.instance(Info.class).ifPresent(v -> v.type(Info.AnvilType.anvil).direction(Info.Direction.of(face))))

                .addReplace(Material.CHIPPED_ANVIL, block -> block.getBlockData() instanceof Directional directional ? directional.getFacing() : BlockFace.NORTH,
                        (face, info) -> info.instance(Info.class).ifPresent(v -> v.type(Info.AnvilType.chipped_anvil).direction(Info.Direction.of(face))))

                .addReplace(Material.DAMAGED_ANVIL, block -> block.getBlockData() instanceof Directional directional ? directional.getFacing() : BlockFace.NORTH,
                        (face, info) -> info.instance(Info.class).ifPresent(v -> v.type(Info.AnvilType.damaged_anvil).direction(Info.Direction.of(face))))
        );
        return core.element.create(AnvilBlockData.class)
                .withInit(AnvilBlockData::init)
                .withInstance()
                .<JsonPrimitive>addConfig("config", v -> v.withParent("anvil_order").withDefault(new JsonPrimitive(true)).withInvoke(j -> anvil_order = j.getAsBoolean()))
                .<JsonObject>addConfig("anvil", v -> v.withInvoke(AnvilBlockData::InitFromJson).withDefault(new JsonObject()));
    }

    public static final class Info extends BlocksOld.InfoInstance implements InfoComponent.IShape, InfoComponent.ILoot, IAnvil {
        private static final VoxelShape BASE = net.minecraft.world.level.block.Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
        private static final VoxelShape X_LEG1 = net.minecraft.world.level.block.Block.box(3.0, 4.0, 4.0, 13.0, 5.0, 12.0);
        private static final VoxelShape X_LEG2 = net.minecraft.world.level.block.Block.box(4.0, 5.0, 6.0, 12.0, 10.0, 10.0);
        private static final VoxelShape X_TOP = net.minecraft.world.level.block.Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0);
        private static final VoxelShape Z_LEG1 = net.minecraft.world.level.block.Block.box(4.0, 4.0, 3.0, 12.0, 5.0, 13.0);
        private static final VoxelShape Z_LEG2 = net.minecraft.world.level.block.Block.box(6.0, 5.0, 4.0, 10.0, 10.0, 12.0);
        private static final VoxelShape Z_TOP = net.minecraft.world.level.block.Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0);
        private static final VoxelShape X_AXIS_AABB = VoxelShapes.or(BASE, X_LEG1, X_LEG2, X_TOP);
        private static final VoxelShape Z_AXIS_AABB = VoxelShapes.or(BASE, Z_LEG1, Z_LEG2, Z_TOP);

        public Info(BlocksOld.Info info) {
            super(info);
        }

        @Override public ItemStack getItem(int stack) { return items.stream().skip(stack).findFirst().orElse(null); }


    }

    public interface IAnvil {
        ItemStack getItem(int stack);
    }

    private static class AnvilManager extends MultiDisplayManager<String, AnvilManager.AnvilLocation, AnvilDisplay> {
        protected AnvilManager(int max_stack) { super(max_stack); }

        private static class AnvilLocation implements IAnvil {
            private final String key;
            private final IAnvil anvil;
            private final Location location;
            public AnvilLocation(String key, IAnvil anvil, Location location) {
                this.key = key;
                this.anvil = anvil;
                this.location = location;
            }
            public String key() { return key; }
            public ItemStack getItem(int stack) { return anvil.getItem(stack); }
            public Location location() { return location; }
        }

        @Override public Map<String, AnvilLocation> getData() {
            return Streams.concat(
                            BlockMap.instances(Info.class).map(v -> new AnvilLocation(v.val1.instanceUUID().toString(), v.val1, v.val0.getLocation()))
                            //CustomMeta.LoadedBlock.allReadOnly(AnvilBlock.class).stream().map(v -> new AnvilLocation(v.getKey(), v, v.getCenterLocation()))
                    )
                    .collect(Collectors.toMap(AnvilLocation::key, v -> v));
        }
        @Override public AnvilDisplay createOne(int id, AnvilDisplay last, String key, AnvilLocation anvil) { return AnvilDisplay.create(id, anvil); }
    }
    private static AnvilManager ANVIL_MANAGER;

    private static int max_clicks = 1;
    private static int MAX_STACK = 1;

    private static void updateDisplay() {
        lime.once(() -> {
            ANVIL_MANAGER.update();
            ANVIL_MANAGER.update();
        }, 0.1);
    }

    private static boolean anvil_order = true;

    @EventHandler public static void onBlockDamage(BlockDamageEvent e) {
        Player player = e.getPlayer();
        if (player.getAttackCooldown() != 1) return;
        Position position = Position.of(e.getBlock());
        BlockMap.byPosition(position).flatMap(info -> info.instance(Info.class)).ifPresent(v -> v.damage(position, player, player.getInventory().getItemInMainHand()));
    }
    @EventHandler public static void onAnvilEvent(PrepareAnvilEvent e) {
        ItemStack item = e.getInventory().getSecondItem();
        if (item == null || item.getType() == Material.AIR) return;
        e.setResult(null);
    }



    public static void init() {
        /*AnyEvent.AddEvent("ss", AnyEvent.type.owner, builder -> builder.CreateParam(Double::parseDouble, "x").CreateParam(Double::parseDouble, "y").CreateParam(Double::parseDouble, "z"), (player, x,y,z) -> {
            CustomMeta.LoadedBlock.allReadOnly(AnvilBlock.class).forEach(block -> {
                DrawText.show(DrawText.IShow.create(player, block.getCenterLocation(x, y, z), Component.text("✖").color(TextColor.color(0xFF0000)), 0.5));
            });
        });*/
    }
    public static void InitFromJson(JsonObject json) {
        HashMap<String, IRecipe> recipeList = new HashMap<>();
        /*json.add("tmp", system.json.object()
                .add("input", "Minecraft.TRIDENT")
                .add("repair", "Minecraft.PRISMARINE_SHARD")
                .add("range", "5:10")
                .add("clicks", 3)
                .build()
        );*/
        json.getAsJsonObject().entrySet().forEach(dat -> recipeList.put(dat.getKey(), IRecipe.parse(dat.getKey(), dat.getValue().getAsJsonObject())));

        AnvilBlockData.recipeList.clear();
        AnvilBlockData.whitelistMaterial.clear();

        AnvilBlockData.recipeList.putAll(recipeList);
        AnvilBlockData.recipeList.putAll(createDefault());

        AnvilBlockData.recipeList.values().forEach(IRecipe::addToWhitelist);
        max_clicks = 1;
        MAX_STACK = 1;
        AnvilBlockData.recipeList.forEach((k,v) -> {
            max_clicks = Math.max(v.getClicks(), max_clicks);
            MAX_STACK = Math.max(v.getItemCount(), MAX_STACK);
        });

        Set<Material> materials = new HashSet<>(whitelistMaterial);
        AnvilBlockData.whitelistMaterial.clear();
        AnvilBlockData.whitelistMaterial.addAll(materials);

        if (ANVIL_MANAGER != null) Displays.uninitDisplay(ANVIL_MANAGER);
        Displays.initDisplay(ANVIL_MANAGER = new AnvilManager(MAX_STACK));

    }
}

