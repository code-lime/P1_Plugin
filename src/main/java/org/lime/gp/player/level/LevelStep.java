package org.lime.gp.player.level;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.module.loot.LootModifyAction;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.LevelRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.item.loot.ILoot;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.perm.Perms.CanData;
import org.lime.gp.player.perm.Perms.ICanData;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class LevelStep {

    public final int level;
    public final LevelData data;
    public final double total;

    public final HashMap<ExperienceAction<?, ?>, List<ExperienceGetter<?, ?>>> variable = new HashMap<>();
    public final LinkedHashMap<String, system.Toast2<ILoot, LootModifyAction>> modifyLootTable = new LinkedHashMap<>();
    public final ICanData canData;

    public LevelStep(int level, LevelData data, JsonObject json) {
        this.level = level;
        this.data = data;
        this.total = json.get("total").getAsDouble();
        if (json.has("actions")) json.get("actions").getAsJsonObject().entrySet().forEach(kv -> {
            try {
                ExperienceAction<?, ?> action = ExperienceAction.getByName(kv.getKey());
                this.variable.put(action, action.createVariable(kv.getValue()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Error in work " + data.work + " in level " + level + " in action " + kv.getKey() + " with json " + kv.getValue(), e);
            }
        });
        if (json.has("loot")) json.get("loot")
                .getAsJsonObject()
                .entrySet()
                .forEach(kv -> LootModifyAction.parse(kv.getKey(), kv.getValue())
                        .invoke((key, loot, action) -> this.modifyLootTable
                                .put(key, system.toast(loot, action))
                        )
                );
        if (json.has("perm")) canData = new CanData(json.get("perm").getAsJsonObject());
        else canData = ICanData.getNothing();
    }

    public boolean tryModifyLoot(PopulateLootEvent e) {
        String key = e.getKey().getPath();
        system.Toast2<ILoot, LootModifyAction> loot = null;
        for (var kv : modifyLootTable.entrySet()) {
            if (!system.compareRegex(key, kv.getKey())) continue;
            loot = kv.getValue();
            break;
        }
        if (loot == null) return false;
        loot.val1.modifyLoot(e, loot.val0);
        return true;
    }
    public Optional<ILoot> tryChangeLoot(String key, ILoot base, IPopulateLoot variable) {
        system.Toast2<ILoot, LootModifyAction> loot = null;
        for (var kv : modifyLootTable.entrySet()) {
            if (!system.compareRegex(key, kv.getKey())) continue;
            loot = kv.getValue();
            break;
        }
        return loot == null ? Optional.empty() : Optional.of(loot.val1.changeLoot(base, loot.val0));
    }
    
    @SuppressWarnings("unchecked")
    public <TValue, TCompare>Optional<Double> executeExpValue(UUID uuid, ExperienceAction<TValue, TCompare> type, TValue value) {
        List<ExperienceGetter<?, ?>> list = variable.getOrDefault(type, null);
        if (list == null) return Optional.empty();
        boolean isDuplicate = false;
        double exp = 0;
        for (ExperienceGetter<?, ?> _getter : list) {
            ExperienceGetter<TValue, TCompare> getter = (ExperienceGetter<TValue, TCompare>)_getter;
            if (isDuplicate) {
                if (getter.duplicate() && type.compare(value, getter.compare())) {
                    exp += getter.execute(uuid, value, total);
                }
            } else if (type.compare(value, getter.compare())) {
                isDuplicate = true;
                exp = getter.execute(uuid, value, total);
            }
        }
        return isDuplicate ? Optional.of(exp) : Optional.empty();
    }

    public <TValue, TCompare>void deltaExp(UUID uuid, ExperienceAction<TValue, TCompare> type, TValue value) {
        executeExpValue(uuid, type, value).ifPresent(exp -> UserRow.getBy(uuid).ifPresent(user -> {
            double mutate = LevelModule.levelMutate(uuid);
            double mutate_exp = exp * mutate;
            if (LevelModule.DEBUG) {
                String current = LevelRow.getBy(user.id, data.work).map(v -> system.getDouble(v.exp * total, 4) + "["+v.level+"]").orElse("0[0]");
                String debugValue = type.debug(value);
                if (!debugValue.isEmpty()) debugValue = " | " + debugValue;
                lime.logOP("Exp " + Optional.ofNullable(Bukkit.getPlayer(uuid))
                        .map(Player::getName)
                        .orElse(uuid.toString()) + ": " + current + " / " + total
                        + " ("
                            + (exp >= 0 ? "+" : "-") + system.getDouble(Math.abs(exp), 2)
                            + (mutate != 1 ? (" * " + mutate + " = " + (mutate_exp >= 0 ? "+" : "-") + system.getDouble(Math.abs(mutate_exp), 2)) : "")
                        + ")"
                        + debugValue
                );
            }
            double delta = mutate_exp / total;
            if (delta >= 0) Methods.appendDeltaLevel(user.id, data.work, delta);
            else Methods.removeDeltaLevel(user.id, data.work, -delta);
        }));
    }

    /*public static void addExp(Player player, String type, double value) {
        TeamManager.TeamData data = TeamManager.GetTeamData(player);
        if (data == null) return;
        LevelStep.TeamData.ExpData expData = data.expData;
        if (expData == null || !expData.type.equals(type)) return;
        JsonObject exp = JManager.FromContainer(JsonObject.class, player.getPersistentDataContainer(), "exp_role", null);
        if (exp == null) {
            exp = new JsonObject();
            exp.addProperty("exp_type", expData.type);
            exp.add("exp_list", new JsonObject());
        }
        JsonObject expList = exp.getAsJsonObject("exp_list");
        HashMap<String, Double> exp_multiply_list = data.HasKey("exp_multiply") ? data.GetMap("exp_multiply") : new HashMap<>();
        value *= exp_multiply_list.getOrDefault(type, 1.0);
        expList.addProperty(type, (expList.has(type) ? expList.get(type).getAsDouble() : 0) + value);
        JManager.ToContainer(player.getPersistentDataContainer(), "exp_role", exp);
    }
    private static String getCurrentType(OfflinePlayer player) {
        TeamManager.TeamData data = TeamManager.GetTeamData(player);
        if (data == null) return null;
        LevelStep.TeamData.ExpData expData = data.expData;
        if (expData == null) return null;
        return expData.type;
    }

    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    private static CraftPersistentDataContainer getOfflinePersistent(OfflinePlayer player) {
        CraftPersistentDataContainer container = new CraftPersistentDataContainer(DATA_TYPE_REGISTRY);
        try {
            NBTTagCompound nbt = ((CraftServer)Bukkit.getServer()).getServer().worldNBTStorage.getPlayerData(player.getUniqueId().toString());
            container.putAll(nbt.getCompound("BukkitValues"));
        } catch (Exception ignored) {

        }
        return container;
    }

    public static List<system.Toast3<String, Double, Boolean>> GetExp(OfflinePlayer player) {
        PersistentDataContainer container = player.isOnline() ? ((Player)player).getPersistentDataContainer() : getOfflinePersistent(player);
        JsonObject exp = JManager.FromContainer(JsonObject.class, container, "exp_role", null);
        List<system.Toast3<String, Double, Boolean>> expList = new ArrayList<>();
        if (exp == null) return expList;
        String current = getCurrentType(player);
        exp.getAsJsonObject("exp_list").entrySet().forEach(kv -> expList.add(system.toast(kv.getKey(), kv.getValue().getAsDouble(), kv.getKey().equals(current))));
        return expList;
    }
    public static void SetExp(Player player, String type, double value) {
        JsonObject exp = JManager.FromContainer(JsonObject.class, player.getPersistentDataContainer(), "exp_role", null);
        if (exp == null) return;
        JsonObject expList = exp.getAsJsonObject("exp_list");
        expList.addProperty(type, value);
        JManager.ToContainer(player.getPersistentDataContainer(), "exp_role", exp);
    }
    public static void SetExp(Player player, String type, system.Func1<Double, Double> value) {
        JsonObject exp = JManager.FromContainer(JsonObject.class, player.getPersistentDataContainer(), "exp_role", null);
        if (exp == null) return;
        JsonObject expList = exp.getAsJsonObject("exp_list");
        expList.addProperty(type, value.Invoke(expList.has(type) ? expList.get(type).getAsDouble() : 0));
        JManager.ToContainer(player.getPersistentDataContainer(), "exp_role", exp);
    }*/
}