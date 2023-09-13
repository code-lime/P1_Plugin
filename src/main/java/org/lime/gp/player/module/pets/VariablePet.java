package org.lime.gp.player.module.pets;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ambient.EntityBat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.horse.EntityHorse;
import net.minecraft.world.entity.animal.horse.EntityLlama;
import net.minecraft.world.entity.animal.horse.HorseColor;
import net.minecraft.world.entity.animal.horse.HorseStyle;
import net.minecraft.world.entity.monster.EntitySlime;
import net.minecraft.world.entity.monster.EntityVex;
import net.minecraft.world.item.EnumColor;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPanda;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftTropicalFish;
import org.bukkit.entity.*;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.display.models.shadow.Builder;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.lime;
import org.lime.system;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VariablePet extends AbstractPet {
    public final EntityTypes<? extends EntityLiving> type;
    public final system.Action1<Entity> variable;
    public final boolean baby;
    public final IBuilder model;

    private static final ConcurrentHashMap<EntityTypes<? extends Entity>, Class<? extends Entity>> entityTypes = new ConcurrentHashMap<>();

    @Override public IBuilder model() { return model; }
    @Override public void tick(BaseChildDisplay<?, ?, ?> model, Map<String, Object> data) { }

    @SuppressWarnings("all")
    protected VariablePet(String key, JsonObject json) {
        super(key, json);
        this.type = (EntityTypes<? extends EntityLiving>) EntityTypes.byString(json.get("type").getAsString()).get();
        this.baby = json.has("baby") && json.get("baby").getAsBoolean();

        Class<? extends Entity> tClass = entityTypes.computeIfAbsent(this.type, _type -> _type.create(lime.MainWorld.getHandle()).getClass());
        this.variable = variableApply(tClass, json.has("variable") && !json.get("variable").isJsonNull() ? json.get("variable").getAsString() : null);

        this.model = lime.models.builder().entity().entity(this.type).nbt(createEntity()).noCollision(true);
    }

    private static MinecraftKey of(NamespacedKey key) {
        return new MinecraftKey(key.getNamespace(), key.getKey());
    }

    @SuppressWarnings("deprecation")
    private static system.Action1<Entity> variableApply(Class<? extends Entity> type, String variable) {
        system.Action1<Entity> apply = v -> {
        };
        if (variable == null) return apply;
        if (Axolotl.class.isAssignableFrom(type))
            apply = apply.andThen(variableApply(Axolotl::setVariant, Axolotl.Variant.byId(org.bukkit.entity.Axolotl.Variant.valueOf(variable).ordinal())));
        if (EntityCat.class.isAssignableFrom(type))
            apply = apply.andThen(variableApply(EntityCat::setVariant, BuiltInRegistries.CAT_VARIANT.get(of(Cat.Type.valueOf(variable).getKey()))));
        if (EntityFox.class.isAssignableFrom(type))
            apply = apply.andThen(variableApply(EntityFox::setVariant, EntityFox.Type.values()[Fox.Type.valueOf(variable).ordinal()]));
        if (EntityHorse.class.isAssignableFrom(type)) {
            String[] vars = variable.split("&");
            apply = apply.andThen(variableApply(EntityHorse::setVariantAndMarkings, HorseColor.byId(Horse.Color.valueOf(vars[1]).ordinal()), HorseStyle.byId(Horse.Style.valueOf(vars[0]).ordinal())));
        }
        if (EntityLlama.class.isAssignableFrom(type))
            apply = apply.andThen(variableApply(EntityLlama::setVariant, EntityLlama.Variant.byId(Llama.Color.valueOf(variable).ordinal())));
        if (EntityPanda.class.isAssignableFrom(type))
            apply = apply.andThen(variableApply(EntityPanda::setMainGene, CraftPanda.toNms(Panda.Gene.valueOf(variable))));
        if (EntityParrot.class.isAssignableFrom(type))
            apply = apply.andThen(variableApply(EntityParrot::setVariant, EntityParrot.Variant.byId(Parrot.Variant.valueOf(variable).ordinal())));
        if (EntityRabbit.class.isAssignableFrom(type))
            apply = apply.andThen(variableApply(EntityRabbit::setVariant, EntityRabbit.Variant.byId(Rabbit.Type.valueOf(variable).ordinal())));
        if (EntitySheep.class.isAssignableFrom(type)) {
            if ("RANDOM".equals(variable)) variable = DyeColor.WHITE.name();
            apply = apply.andThen(variableApply(EntitySheep::setColor, EnumColor.byId(DyeColor.valueOf(variable).getWoolData())));
        }
        if (EntitySlime.class.isAssignableFrom(type))
            apply = apply.andThen(variableApply(EntitySlime::setSize, Integer.valueOf(variable), false));
        if (EntityTropicalFish.class.isAssignableFrom(type)) {
            String[] vars = variable.split("&");
            apply = apply.andThen(variableApply(EntityTropicalFish::setPackedVariant, DyeColor.valueOf(vars[0]).getWoolData() << 24 | DyeColor.valueOf(vars[1]).getWoolData() << 16 | CraftTropicalFish.CraftPattern.values()[TropicalFish.Pattern.valueOf(vars[2]).ordinal()].getDataValue()));
        }
        if (EntityBee.class.isAssignableFrom(type)) {
            String[] vars = variable.split("&");

            boolean nectar = switch (vars[0]) {
                case "NECTAR" -> true;
                case "NONE" -> false;
                default -> throw new IllegalArgumentException("Type '" + vars[0] + "' not supported");
            };
            boolean stung = switch (vars[1]) {
                case "STUNG" -> true;
                case "NONE" -> false;
                default -> throw new IllegalArgumentException("Type '" + vars[1] + "' not supported");
            };
            boolean anger = switch (vars[2]) {
                case "ANGER" -> true;
                case "NONE" -> false;
                default -> throw new IllegalArgumentException("Type '" + vars[2] + "' not supported");
            };

            apply = apply.andThen(variableApply(EntityBee::setHasNectar, nectar));
            apply = apply.andThen(variableApply(EntityBee::setHasStung, stung));
            apply = apply.andThen(variableApply(EntityBee::setRemainingPersistentAngerTime, anger ? 1000 : -1));
        }
        if (EntityVex.class.isAssignableFrom(type)) {
            boolean charging = switch (variable) {
                case "CHARGING" -> true;
                case "NONE" -> false;
                default -> throw new IllegalArgumentException("Type '" + variable + "' not supported");
            };
            apply = apply.andThen(variableApply(EntityVex::setIsCharging, charging));
        }
        return apply;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity, V> system.Action1<Entity> variableApply(system.Action2<T, V> apply, V value) {
        return e -> apply.invoke((T) e, value);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity, V1, V2> system.Action1<Entity> variableApply(system.Action3<T, V1, V2> apply, V1 value1, V2 value2) {
        return e -> apply.invoke((T) e, value1, value2);
    }

    private EntityLiving createEntity() {
        EntityLiving entity = type.create(lime.MainWorld.getHandle());
        if (entity instanceof EntityBat bat) bat.setResting(false);
        if (baby && entity instanceof EntityAgeable age) age.setAge(-1);
        variable.invoke(entity);
        return entity;
    }
}
