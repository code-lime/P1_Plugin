package org.lime.gp.block.component.data;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemLiquidUtil;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.IDisplayVariable;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.BottleComponent;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.list.ThirstSetting;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.plugin.CoreElement;
import org.lime.system.json;

import java.util.Collection;
import java.util.Optional;

public class BottleInstance extends BlockInstance implements CustomTileMetadata.Interactable, IDisplayVariable {
    public static CoreElement create() {
        Blocks.addDefaultBlocks(new BlockInfo("bottle").add(v -> new BottleComponent(v, 10)));
        return CoreElement.create(BottleInstance.class);
    }

    @Override  public BottleComponent component() { return (BottleComponent)super.component(); }
    public BottleInstance(BottleComponent component, CustomTileMetadata metadata) { super(component, metadata); }

    public interface IFluid {
        ItemStack createBottle();
        Color waterColor();
        json.builder.object save();

        static Optional<IFluid> of(ItemStack item) {
            return Items.getOptional(ThirstSetting.class, item)
                    .map(ItemSetting::creator)
                    .map(ItemCreator::getID)
                    .<IFluid>map(CustomFluid::new)
                    .or(() -> PotionFluid.of(item))
                    .or(() -> Optional.ofNullable(item.getItemMeta())
                            .filter(v -> v instanceof PotionMeta meta && meta.getBasePotionData().getType() == PotionType.WATER)
                            .map(v -> new WaterFluid())
                    );
        }
        static Optional<IFluid> of(JsonObjectOptional json) {
            return json.getAsString("type")
                    .flatMap(type -> switch (type) {
                        case "custom" -> CustomFluid.load(json);
                        case "potion" -> PotionFluid.load(json);
                        case "water" -> WaterFluid.load(json);
                        default -> Optional.empty();
                    });
        }
    }

    public record CustomFluid(int id) implements IFluid {
        @Override public ItemStack createBottle() {
            return Items.createItem(id).orElseGet(WaterFluid::createWaterBottle);
        }
        @Override public Color waterColor() {
            return Optional.ofNullable(Items.creators.get(id))
                    .map(v -> v instanceof ItemCreator creator ? creator : null)
                    .flatMap(v -> v.getOptional(ThirstSetting.class))
                    .map(v -> v.color)
                    .orElse(ThirstSetting.DEFAULT_WATER_COLOR);
        }

        public static Optional<CustomFluid> load(JsonObjectOptional json) {
            return json.getAsInt("id").map(CustomFluid::new);
        }
        @Override public json.builder.object save() {
            return json.object()
                    .add("type", "custom")
                    .add("id", id);
        }
    }
    public record PotionFluid(PotionFluid.PotionType type, PotionData data, ImmutableList<PotionEffect> effects, Color color) implements IFluid {
        public enum PotionType {
            Default(Material.POTION),
            Lingering(Material.LINGERING_POTION),
            Splash(Material.SPLASH_POTION);

            public final Material material;

            PotionType(Material material) {
                this.material = material;
            }

            public ItemStack create() {
                return new ItemStack(material);
            }
        }
        @Override public ItemStack createBottle() {
            ItemStack item = type.create();
            PotionMeta meta = (PotionMeta)item.getItemMeta();
            meta.setBasePotionData(data);
            effects.forEach(effect -> meta.addCustomEffect(effect, true));
            meta.setColor(color);
            item.setItemMeta(meta);
            return item;
        }
        @Override public Color waterColor() { return Optional.ofNullable(color).orElse(ThirstSetting.DEFAULT_WATER_COLOR); }

        public static Optional<PotionFluid> of(ItemStack item) {
            return Optional.ofNullable(item.getItemMeta() instanceof PotionMeta meta ? meta : null)
                    .filter(v -> v.getBasePotionData().getType().getEffectType() != null)
                    .flatMap(v -> Optional.ofNullable(switch (item.getType()) {
                                        case POTION -> PotionType.Default;
                                        case LINGERING_POTION -> PotionType.Lingering;
                                        case SPLASH_POTION -> PotionType.Splash;
                                        default -> null;
                                    })
                                    .map(type -> new PotionFluid(type, v.getBasePotionData(), (ImmutableList<PotionEffect>)v.getCustomEffects(), v.getColor()))
                    );
        }

        public static Optional<PotionFluid> load(JsonObjectOptional json) {
            return json.getAsEnum(PotionFluid.PotionType.class, "potion")
                    .map(type -> new PotionFluid(type,
                                    json.getAsJsonObject("data")
                                            .map(data -> new PotionData(
                                                    data.getAsEnum(org.bukkit.potion.PotionType.class, "type").orElse(org.bukkit.potion.PotionType.UNCRAFTABLE),
                                                    data.getAsBoolean("extended").orElse(false),
                                                    data.getAsBoolean("upgraded").orElse(false)
                                            )).orElse(new PotionData(org.bukkit.potion.PotionType.UNCRAFTABLE)),
                                    json.getAsJsonArray("effects")
                                            .stream()
                                            .flatMap(Collection::stream)
                                            .map(JsonElementOptional::getAsJsonObject)
                                            .filter(Optional::isPresent)
                                            .map(Optional::get)
                                            .map(effect -> effect.getAsString()
                                                    .map(PotionEffectType::getByName)
                                                    //PotionEffectType type, int duration, int amplifier, boolean ambient, boolean particles, boolean icon
                                                    .map(effectType ->
                                                            new PotionEffect(effectType,
                                                                    effect.getAsInt("duration").orElse(0),
                                                                    effect.getAsInt("amplifier").orElse(0),
                                                                    effect.getAsBoolean("ambient").orElse(true),
                                                                    effect.getAsBoolean("particles").orElse(true),
                                                                    effect.getAsBoolean("icon").orElse(true)
                                                            )
                                                    )
                                            )
                                            .filter(Optional::isPresent)
                                            .map(Optional::get)
                                            .collect(ImmutableList.toImmutableList()),
                                    json.getAsInt("color").map(Color::fromRGB).orElse(null)

                            )
                    );
        }
        @Override public json.builder.object save() {
            return json.object()
                    .add("type", "potion")
                    .add("potion", type.name())
                    .addObject("data", v -> v
                            .add("type", data.getType().name())
                            .add("extended", data.isExtended())
                            .add("upgraded", data.isUpgraded())
                    )
                    .addArray("effects", _v -> _v
                            .add(effects, effect -> json.object()
                                    .add("amplifier", effect.getAmplifier())
                                    .add("duration", effect.getDuration())
                                    .add("type", effect.getType().getName())
                                    .add("ambient", effect.isAmbient())
                                    .add("particles", effect.hasParticles())
                                    .add("icon", effect.hasIcon())
                            )
                    )
                    .add("color", color == null ? null : color.asRGB());
        }
    }
    public record WaterFluid() implements IFluid {
        public static ItemStack createWaterBottle() {
            ItemStack item = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta)item.getItemMeta();
            meta.setBasePotionData(new PotionData(PotionType.WATER));
            item.setItemMeta(meta);
            return item;
        }
        @Override public ItemStack createBottle() { return createWaterBottle(); }
        @Override public Color waterColor() { return ThirstSetting.DEFAULT_WATER_COLOR; }

        public static Optional<WaterFluid> load(JsonObjectOptional json) {
            return Optional.of(new WaterFluid());
        }
        @Override public json.builder.object save() {
            return json.object()
                    .add("type", "water");
        }
    }

    public IFluid fluid = null;
    public int level = 0;

    @Override public void read(JsonObjectOptional json) {
        json.getAsJsonObject("fluid").flatMap(IFluid::of)
                .ifPresentOrElse(fluid -> {
                    this.level = json.getAsInt("level").orElse(0);
                    this.fluid = this.level <= 0 ? null : fluid;
                    syncDisplayVariable();
                }, () -> {
                    this.level = 0;
                    this.fluid = null;
                    syncDisplayVariable();
                });
    }
    @Override public json.builder.object write() {
        return json.object()
                .add("fluid", fluid == null ? null : fluid.save())
                .add("level", level);
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        EntityHuman entityhuman = event.player();
        EnumHand enumhand = event.hand();
        net.minecraft.world.item.ItemStack itemstack = entityhuman.getItemInHand(enumhand);
        World world = event.world();
        BlockPosition blockposition = event.pos();
        if (itemstack.getItem() == net.minecraft.world.item.Items.GLASS_BOTTLE) {
            if (!world.isClientSide) {
                if (level <= 0 || fluid == null) {
                    return EnumInteractionResult.SUCCESS;
                }
                net.minecraft.world.item.ItemStack potion = CraftItemStack.asNMSCopy(fluid.createBottle());
                level--;
                if (level <= 0) fluid = null;
                syncDisplayVariable();
                saveData();

                Item item = itemstack.getItem();
                potion = potion == null ? PotionUtil.setPotion(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION), Potions.WATER) : potion;

                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, potion));
                entityhuman.awardStat(StatisticList.USE_CAULDRON);
                entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
                world.playSound(null, blockposition, SoundEffects.BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PICKUP, blockposition);
            }
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
        if (level >= component().totalLevel) return EnumInteractionResult.PASS;
        return IFluid.of(itemstack.asBukkitCopy())
                .map(fluid -> {
                    if (this.fluid == null) {
                        this.fluid = fluid;
                        syncDisplayVariable();
                    }
                    if (!fluid.equals(this.fluid)) return EnumInteractionResult.SUCCESS;
                    level++;
                    syncDisplayVariable();
                    saveData();
                    entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
                    entityhuman.awardStat(StatisticList.USE_CAULDRON);
                    entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                    world.playSound(null, blockposition, SoundEffects.BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    world.gameEvent(null, GameEvent.FLUID_PLACE, blockposition);
                    return EnumInteractionResult.sidedSuccess(world.isClientSide);
                })
                .orElse(EnumInteractionResult.PASS);
    }
    @Override public final void syncDisplayVariable() {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> display.modify(map -> {
            map.put("water_color", ChatColorHex.toHex(Optional.ofNullable(fluid).map(IFluid::waterColor).orElse(ThirstSetting.DEFAULT_WATER_COLOR)).substring(1));
            map.put("water_level", String.valueOf(level));
            return true;
        }));
    }
}























