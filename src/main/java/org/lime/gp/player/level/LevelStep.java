package org.lime.gp.player.level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.lime.system;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.item.loot.ILoot;
import org.lime.gp.module.PopulateLootEvent;

import com.google.gson.JsonObject;

public class LevelStep {
    public enum LootModifyAction {
        NONE("n"),
        APPEND("a"),
        REPLACE("r"),
        APPEND_IF_NOT_EMPTY("ane"),
        REPLACE_IF_NOT_EMPTY("rne");

        public final String fastPrefix;

        LootModifyAction(String fp) {
            this.fastPrefix = fp;
        }

        private boolean isPostfix(String postfix) {
            return postfix.equalsIgnoreCase(fastPrefix) ||
                postfix.equalsIgnoreCase(name());
        }

        public static LootModifyAction byPostfix(String postfix) {
            for (LootModifyAction action : values()) {
                if (action.isPostfix(postfix))
                    return action;
            }
            return LootModifyAction.NONE;
        }
    }

    public final int level;
    public final LevelData data;
    public final double total;

    public final HashMap<ExperienceAction<?, ?>, HashMap<?, Double>> variable = new HashMap<>();
    public final HashMap<String, system.Toast2<ILoot, LootModifyAction>> modifyLootTable = new HashMap<>();

    public LevelStep(int level, LevelData data, JsonObject json) {
        this.level = level;
        this.data = data;
        this.total = json.get("total").getAsDouble();
        json.get("actions").getAsJsonObject().entrySet().forEach(kv -> {
            ExperienceAction<?, ?> action = ExperienceAction.getByName(kv.getKey());
            this.variable.put(action, createVariable(action, kv.getValue().getAsJsonObject()));
        });
        json.get("loot").getAsJsonObject().entrySet().forEach(kv -> {
            String key = kv.getKey();
            String[] keys = key.split("#", 2);
            this.modifyLootTable.put(keys[0], system.toast(ILoot.parse(kv.getValue()), LootModifyAction.byPostfix(keys[1])));
        });
    }


    public void tryModifyLoot(PopulateLootEvent e) {
        system.Toast2<ILoot, LootModifyAction> loot = modifyLootTable.get(e.getKey().getPath());
        if (loot == null) return;
        List<ItemStack> items = loot.val0.generateFilter(e);
        switch (loot.val1) {
            case APPEND -> e.addItems(items);
            case APPEND_IF_NOT_EMPTY -> {
                if (!items.isEmpty()) e.addItems(items);
            }
            case REPLACE -> e.setItems(items);
            case REPLACE_IF_NOT_EMPTY -> {
                if (!items.isEmpty()) e.setItems(items);
            }
            default -> {}
        }
    }

    private static <TValue, TCompare>HashMap<TCompare, Double> createVariable(ExperienceAction<TValue, TCompare> action, JsonObject values) {
        HashMap<TCompare, Double> list = new HashMap<>();
        values.entrySet().forEach(kv -> list.put(action.parse(kv.getKey()), kv.getValue().getAsDouble()));
        return list;
    }
    
    @SuppressWarnings("unchecked")
    public <TValue, TCompare>Optional<Double> getExpValue(ExperienceAction<TValue, TCompare> type, TValue value) {
        HashMap<?, Double> list = variable.getOrDefault(type, null);
        if (list == null) return Optional.empty();
        for (Map.Entry<TCompare, Double> item : ((HashMap<TCompare, Double>)list).entrySet()) {
            if (type.compare(value, item.getKey()))
                return Optional.of(item.getValue());
        }
        return Optional.empty();
    }

    public <TValue, TCompare>void appendExp(UUID uuid, ExperienceAction<TValue, TCompare> type, TValue value) {
        getExpValue(type, value).ifPresent(exp -> UserRow.getBy(uuid).ifPresent(user -> {
            Methods.appendDeltaLevel(user.id, data.work, exp / total);
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