package org.lime.gp.block.component;

import com.google.gson.*;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.BlockSkullEventInteract;
import net.minecraft.world.level.block.BlockSkullEventShape;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.lime.Position;
import org.lime.gp.block.BlocksOld;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.Items;
import org.lime.gp.block.data.RadioBlockData;
import org.lime.gp.lime;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.gp.block.BlockMap;
import org.lime.system;

import java.util.*;

public class Components {
    @InfoComponent.Component(name = "multiblock") public static final class MultiBlockComponent extends InfoComponent.StaticInfoComponent<JsonObject> {
        public final Map<system.Toast3<Integer, Integer, Integer>, IBlock> blocks = new LinkedHashMap<>();
        public interface IBlock {
            Optional<BlocksOld.BlockResult> set(UUID ownerBlock, Position position, InfoComponent.Rotation.Value rotation, system.Toast3<Integer, Integer, Integer> local);

            static IBlock create(JsonElement json) {
                if (json.isJsonObject()) return (ownerBlock, position, rotation, local) -> BlocksOld.otherCreator().setBlock(position.offset(rotation.rotate(local)), system.json.object()
                        .addObject(InfoComponent.GenericDynamicComponent.getName("other"), v -> v
                                .add(json.getAsJsonObject())
                                .add("position", position.toSave())
                                .add("rotation", rotation.toString())
                                .add("owner", ownerBlock.toString())
                        )
                        .build());
                String key = json.getAsString();
                return (ownerBlock, position, rotation, local) -> BlocksOld.creator(key).flatMap(block -> block.setBlock(position.offset(rotation.rotate(local)), system.json.object()
                        .addObject(InfoComponent.GenericDynamicComponent.getName("other"), v -> v
                                .add("position", position.toSave())
                                .add("rotation", rotation.toString())
                                .add("owner", ownerBlock.toString())
                        )
                        .build())
                );
            }
        }

        public MultiBlockComponent(BlocksOld.InfoCreator creator, JsonObject json) {
            super(creator, json);
            json.get("blocks").getAsJsonObject().entrySet().forEach(kv -> blocks.put(system.getPosToast(kv.getKey()), IBlock.create(kv.getValue())));
            blocks.remove(system.toast(0, 0, 0));
        }
        public boolean isCan(Block block, InfoComponent.Rotation.Value rotation) {
            for (system.Toast3<Integer, Integer, Integer> _p : blocks.keySet()) {
                system.Toast3<Integer, Integer, Integer> p = rotation.rotate(_p);
                if (!block.getRelative(p.val0, p.val1, p.val2).getType().isAir())
                    return false;
            }
            return true;
        }
    }
    @InfoComponent.Component(name = "material") public static final class MaterialComponent extends InfoComponent.StaticInfoComponent<JsonObject> implements InfoComponent.IShape {
        public final Material material;
        public final IBlockData blockData;

        public MaterialComponent(BlocksOld.InfoCreator creator, JsonObject json) {
            super(creator, json);
            this.material = Material.valueOf(json.get("material").getAsString());
            IBlockData blockData = CraftMagicNumbers
                    .getBlock(material)
                    .defaultBlockState();
            if (json.has("states")) {
                HashMap<String, IBlockState<?>> states = system.map.<String, IBlockState<?>>of()
                        .add(blockData.getProperties(), IBlockState::getName, v -> v)
                        .build();
                for (Map.Entry<String, JsonElement> kv : json.get("states").getAsJsonObject().entrySet()) {
                    IBlockState<?> state = states.getOrDefault(kv.getKey(), null);
                    if (state == null) continue;
                    blockData = BlocksOld.setValue(blockData, state, kv.getValue().getAsString());
                }
            }
            this.blockData = blockData;
        }

        @Override public Result replace(Input input) {
            return input.toResult(blockData);
        }
        @Override public boolean asyncShape(TileEntitySkull state, BlockSkullEventShape e) {
            e.setResult(blockData.getShape(e.getWorld(), e.getPos(), e.getContext()));
            return true;
        }
    }
    @InfoComponent.Component(name = "menu") public static final class MenuComponent extends InfoComponent.DynamicInfoComponent<JsonObject, MenuComponent.Var> {
        public final String menu;
        public final HashMap<String, String> args = new HashMap<>();

        public MenuComponent(BlocksOld.InfoCreator creator, JsonObject json) {
            super(creator, json);
            this.menu = json.get("menu").getAsString();
            if (json.has("args")) json.get("args").getAsJsonObject().entrySet().forEach(kv -> args.put(kv.getKey(), kv.getValue().getAsString()));
        }

        private static void playSound(Location location, boolean isOpen) {
            location.getWorld().playSound(
                    location,
                    isOpen ? Sound.BLOCK_CHEST_OPEN : Sound.BLOCK_CHEST_CLOSE,
                    SoundCategory.BLOCKS,
                    1F,
                    1.1F
            );
        }

        public final class Var extends InfoComponent.VarDynamicComponent {
            public final HashMap<UUID, Integer> open_list = new HashMap<>();
            public Var(BlocksOld.Info info) { super(info); }

            @Override public boolean tick(TileEntitySkull skull) {
                open_list.entrySet().forEach(kv -> {
                    Player player = Bukkit.getPlayer(kv.getKey());
                    if (player == null) kv.setValue(0);
                    else if (player.getOpenInventory().getTopInventory().getType() != InventoryType.CHEST) kv.setValue(kv.getValue() - 1);
                });
                int size = open_list.size();
                open_list.values().removeIf(v -> v <= 0);
                int nsize = open_list.size();
                if (nsize == 0 && nsize != size) {
                    net.minecraft.world.level.World world = skull.getLevel();
                    if (world != null) {
                        Position position = BlocksOld.position(world, skull.getBlockPos());
                        playSound(position.getLocation(), false);
                        BlockMap.byPosition(position).flatMap(_v -> _v.instance(VariableComponent.class)).ifPresent(_v -> _v.set("open", "false"));
                    }
                }
                return true;
            }
            @Override public void interact(TileEntitySkull state, BlockSkullEventInteract event) {
                if (event.getPlayer().getBukkitEntity() instanceof Player player) {
                    MenuCreator.show(player, menu, Apply.of().add(args));

                    UUID uuid = player.getUniqueId();
                    Position position = BlocksOld.position(player.getWorld(), state.getBlockPos());

                    if (open_list.size() == 0) {
                        playSound(position.getLocation(), true);
                        BlockMap.byPosition(position).flatMap(_v -> _v.instance(VariableComponent.class)).ifPresent(_v -> _v.set("open", "true"));
                    }
                    open_list.put(uuid, 10);

                    event.setResult(EnumInteractionResult.CONSUME);
                }
            }
        }

        @Override public Var createInstance(BlocksOld.Info info) {
            return this.new Var(info);
        }
    }
    @InfoComponent.Component(name = "loot") public static final class LootComponent extends InfoComponent.StaticInfoComponent<JsonObject> implements InfoComponent.ILoot {
        public final Map<String, Integer> items;

        public LootComponent(BlocksOld.InfoCreator creator, JsonObject json) {
            super(creator, json);
            Map<String, Integer> items = new HashMap<>();
            json.getAsJsonObject("items").entrySet().forEach(kv -> {
                items.put(kv.getKey(), kv.getValue().getAsInt());
                if (Items.getItemCreator(kv.getKey()).isPresent()) return;
                lime.logOP("[Warning] Key of item in loot '" + kv.getKey() + "' not founded!");
            });
            this.items = items;
        }

        @Override public List<ItemStack> populate(TileEntitySkull state, PopulateLootEvent e) {
            List<ItemStack> items = new ArrayList<>();
            this.items.forEach((key, count) -> Items.getItemCreator(key).ifPresentOrElse(
                    creator -> items.add(creator.createItem(count)),
                    () -> lime.logOP("[Warning] Key of item in loot '" + key + "' not founded!")
            ));
            return items;
        }
    }
    @InfoComponent.Component(name = "radio") public static final class RadioComponent extends InfoComponent.DynamicInfoComponent<JsonObject, RadioBlockData.Info> {
        public enum RadioState {
            none(false, false),
            input(true, false),
            output(false, true),
            all(true, true);

            public final boolean isInput;
            public final boolean isOutput;

            RadioState(boolean input, boolean output) {
                this.isInput = input;
                this.isOutput = output;
            }
        }

        public final int min_level;
        public final int max_level;
        public final int total_distance;
        public final short min_distance;
        public final short max_distance;
        public final RadioState state;

        public int rangeLevel(int level) {
            return Math.max(Math.min(level, max_level), min_level);
        }
        public int anyLevel() {
            return rangeLevel((max_level + min_level) / 2);
        }
        public short rangeDistance(int distance) {
            return (short)Math.max(Math.min(distance, max_distance), min_distance);
        }
        public short anyDistance() {
            return rangeDistance((max_distance + min_distance) / 2);
        }

        public RadioComponent(BlocksOld.InfoCreator creator, JsonObject json) {
            super(creator, json);
            min_level = json.get("min_level").getAsInt();
            max_level = json.get("max_level").getAsInt();
            total_distance = json.get("total_distance").getAsInt();
            min_distance = json.get("min_distance").getAsShort();
            max_distance = json.get("max_distance").getAsShort();
            state = json.has("state") ? RadioState.valueOf(json.get("state").getAsString()) : RadioState.all;
        }
        @Override public RadioBlockData.Info createInstance(BlocksOld.Info info) {
            return new RadioBlockData.Info(this, info);
        }
    }
    @InfoComponent.Component(name = "LOD") public static final class LODComponent extends InfoComponent.DynamicInfoComponent<JsonObject, VariableComponent> {
        public final List<LOD.ILOD> lodList = new LinkedList<>();
        public final Map<UUID, LOD.ILOD> lodMapUUID = new HashMap<>();
        public final double maxDistance;

        public LODComponent(BlocksOld.InfoCreator creator, JsonObject json) {
            super(creator, json);
            maxDistance = LOD.load(creator, json, lodList, lodMapUUID);
        }

        @Override public VariableComponent createInstance(BlocksOld.Info info) {
            return new VariableComponent(this, info);
        }
    }
}
