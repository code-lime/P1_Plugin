package org.lime.gp.module.mobs.spawn;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.CommandTeam;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardTeam;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.lime.display.ext.JsonNBT;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.item.loot.ILoot;
import org.lime.gp.lime;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.module.loot.Parameters;
import org.lime.gp.module.mobs.DespawnData;
import org.lime.gp.module.mobs.IMobCreator;
import org.lime.gp.module.mobs.IPopulateSpawn;

import javax.annotation.Nullable;
import java.util.*;

public class EntitySpawn implements ISpawn {
    private final @Nullable EntityTypes<?> type;
    private final @Nullable String name;
    private final List<String> tags = new ArrayList<>();
    private final boolean slotsEmpty;
    private final @Nullable ILoot slots;
    private final boolean effectsEmpty;
    private final List<PotionEffect> effects = new ArrayList<>();
    private final @Nullable DespawnData despawn;
    private final Map<Attribute, Double> attributes = new HashMap<>();
    private final NBTTagCompound nbt;
    private final @Nullable String team;

    public static boolean isEntityType(String type) {
        try {
            EntityType.valueOf(type);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public EntitySpawn(JsonObject json) {
        String type = json.get("type").getAsString();
        this.type = type.equals("EMPTY") ? null : EntityTypes.byString(EntityType.valueOf(type).key().asString()).orElseThrow();
        this.name = json.has("name") ? json.get("name").getAsString() : null;
        this.team = json.has("team") ? json.get("team").getAsString() : null;
        if (json.has("tags")) json.getAsJsonArray("tags")
                .forEach(item -> tags.add(item.getAsString()));
        this.slotsEmpty = json.has("slots_empty") && json.get("slots_empty").getAsBoolean();
        this.slots = json.has("slots") ? ILoot.parse(json.get("slots")) : null;
        this.effectsEmpty = json.has("effects_empty") && json.get("effects_empty").getAsBoolean();
        if (json.has("effects")) json.getAsJsonArray("effects")
                .forEach(item -> effects.add(Items.parseEffect(item.getAsJsonObject())));
        this.despawn = json.has("despawn") ? DespawnData.parse(json.get("despawn").getAsJsonObject()) : null;
        if (json.has("attributes")) json.getAsJsonObject("attributes")
                .entrySet()
                .forEach(kv -> attributes.put(Attribute.valueOf(kv.getKey()), kv.getValue().getAsDouble()));
        this.nbt = json.has("nbt") ? JsonNBT.toNBT(json.getAsJsonObject("nbt")) : new NBTTagCompound();
    }
    @Override public Optional<IMobCreator> generateMob(IPopulateSpawn populate) {
        return Optional.of((world, pos) -> {
            if (type == null) return null;
            Entity entity = createEntity(world, type, pos, nbt, true);
            CraftEntity craftEntity = entity.getBukkitEntity();
            Component showName = name == null ? null : ChatHelper.formatComponent(name);
            craftEntity.customName(showName);
            craftEntity.setCustomNameVisible(showName != null);
            craftEntity.getScoreboardTags().addAll(tags);
            if (slotsEmpty && entity instanceof EntityLiving living) {
                for (EnumItemSlot slot : EnumItemSlot.values())
                    living.setItemSlot(slot, ItemStack.EMPTY);
            }
            if (team != null) {
                Scoreboard scoreboard = MinecraftServer.getServer().getScoreboard();
                ScoreboardTeam scoreboardTeam = scoreboard.getPlayerTeam(team);
                if (scoreboardTeam != null)
                    scoreboard.addPlayerToTeam(entity.getScoreboardName(), scoreboardTeam);
            }
            if (slots != null && entity instanceof EntityLiving living) {
                IPopulateLoot loot = IPopulateLoot.of(world, List.of(
                        IPopulateLoot.var(Parameters.ThisEntity, entity),
                        IPopulateLoot.var(Parameters.Origin, pos)
                ));
                slots.generateLoot(loot).forEach(item -> {
                    ItemStack itemStack = CraftItemStack.asNMSCopy(item);
                    EnumItemSlot slot = EntityLiving.getEquipmentSlotForItem(itemStack);
                    switch (slot) {
                        case HEAD, CHEST, LEGS, FEET -> {
                            //if (!living.getItemBySlot(slot).isEmpty()) break;
                            entity.setItemSlot(slot, itemStack);
                            return;
                        }
                    }
                    living.setItemSlot(living.getItemBySlot(EnumItemSlot.MAINHAND).isEmpty()
                            ? EnumItemSlot.MAINHAND
                            : EnumItemSlot.OFFHAND, itemStack);
                });
                if (living instanceof EntityInsentient insentient) {
                    Arrays.fill(insentient.handDropChances, 0);
                    Arrays.fill(insentient.armorDropChances, 0);
                }
            }
            if (craftEntity instanceof LivingEntity living) {
                attributes.forEach((attribute, value) -> {
                    AttributeInstance instance = living.getAttribute(attribute);
                    if (instance == null) {
                        living.registerAttribute(attribute);
                        instance = living.getAttribute(attribute);
                    }
                    instance.setBaseValue(value);
                });
                if (effectsEmpty) living.clearActivePotionEffects();
                effects.forEach(living::addPotionEffect);
            }
            if (despawn != null) despawn.setupData(craftEntity.getPersistentDataContainer());
            return entity;
        });
    }
    private static Entity createEntity(WorldServer worldserver, EntityTypes<?> entityType, Vec3D pos, NBTTagCompound nbt, boolean initialize) {
        NBTTagCompound tagCompound = nbt.copy();
        tagCompound.putString("id", EntityTypes.getKey(entityType).toString());
        Entity entity = EntityTypes.loadEntityRecursive(tagCompound, worldserver, v -> {
            v.moveTo(pos.x, pos.y, pos.z, v.getYRot(), v.getXRot());
            v.addTag("spawn#generic");
            return v;
        });
        if (entity == null) throw new IllegalArgumentException("ERROR_FAILED");
        if (initialize && entity instanceof EntityInsentient insentient)
            insentient.finalizeSpawn(worldserver, worldserver.getCurrentDifficultyAt(entity.blockPosition()), EnumMobSpawn.COMMAND, null, null);
        if (!worldserver.tryAddFreshEntityWithPassengers(entity, CreatureSpawnEvent.SpawnReason.COMMAND))
            throw new IllegalArgumentException("ERROR_DUPLICATE_UUID");
        return entity;
    }
}
