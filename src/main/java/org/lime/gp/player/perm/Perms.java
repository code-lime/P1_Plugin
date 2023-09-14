package org.lime.gp.player.perm;

import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.projectile.EntityFishingHook;
import net.minecraft.world.item.crafting.IRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.lime.Position;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.chat.Apply;
import org.lime.gp.craft.Crafts;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.UserCraftsRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.KeyedTable;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.BlockLimitSetting;
import org.lime.gp.item.settings.list.BlockSetting;
import org.lime.gp.lime;
import org.lime.gp.module.loot.Parameters;
import org.lime.system.Regex;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.gp.extension.JManager;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.player.level.LevelModule;
import org.lime.gp.player.module.Death;
import org.lime.gp.player.module.Knock;
import org.lime.gp.player.module.HandCuffs;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.system.utils.ParseUtils;
import org.lime.system.utils.RandomUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Perms implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Perms.class)
                .withInstance();
    }

    private static final long canBreakCooldown = 60 * 1000;
    private static final HashMap<Position, Toast3<UUID, Long, String>> owners = new HashMap<>();

    public interface ICanData {
        String unique();
        Optional<Integer> role();
        Optional<Integer> work();

        boolean isCanBreak(String material);
        boolean isCanPlace(String material);
        boolean isCanPlace(String from_material, String to_material);
        boolean isCanCraft(String craft);
        Stream<String> getCanCrafts();
        boolean isCanUse(String item);
        Stream<String> getCanUse();
        boolean isCanDamage(EntityType entity);
        boolean isCanFarm(EntityType entity);
        boolean isCanFishing();
        double getBreakFarmReplace(Material material);

        default ICanData with(ICanData data) { return combine(this, data); }

        ICanData nothing = combine();
        static ICanData getNothing() { return nothing; }

        static ICanData combine(List<ICanData> datas) { return combine(datas.toArray(new ICanData[0])); }
        static ICanData combine(ICanData... datas) {
            String unique = "[" + Arrays.stream(datas).map(ICanData::unique).collect(Collectors.joining(",")) + "]";
            return new ICanData() {
                @Override public String unique() { return unique; }
                @Override public Optional<Integer> role() { return Arrays.stream(datas).map(ICanData::role).flatMap(Optional::stream).findFirst(); }
                @Override public Optional<Integer> work() { return Arrays.stream(datas).map(ICanData::work).flatMap(Optional::stream).findFirst(); }

                @Override public boolean isCanBreak(String material) { return Arrays.stream(datas).anyMatch(v -> v.isCanBreak(material)); }
                @Override public boolean isCanPlace(String material) { return Arrays.stream(datas).anyMatch(v -> v.isCanPlace(material)); }
                @Override public boolean isCanPlace(String from_material, String to_material) { return Arrays.stream(datas).anyMatch(v -> v.isCanPlace(from_material, to_material)); }
                @Override public boolean isCanCraft(String craft) { return Arrays.stream(datas).anyMatch(v -> v.isCanCraft(craft)); }
                @Override public Stream<String> getCanCrafts() { return Arrays.stream(datas).flatMap(ICanData::getCanCrafts); }
                @Override public boolean isCanUse(String item) { return Arrays.stream(datas).anyMatch(v -> v.isCanUse(item)); }
                @Override public Stream<String> getCanUse() { return Arrays.stream(datas).flatMap(ICanData::getCanUse); }
                @Override public boolean isCanDamage(EntityType entity) { return Arrays.stream(datas).anyMatch(v -> v.isCanDamage(entity)); }
                @Override public boolean isCanFarm(EntityType entity) { return Arrays.stream(datas).anyMatch(v -> v.isCanFarm(entity)); }
                @Override public boolean isCanFishing() { return Arrays.stream(datas).anyMatch(ICanData::isCanFishing); }
                @Override public double getBreakFarmReplace(Material material) { return Arrays.stream(datas).mapToDouble(v -> v.getBreakFarmReplace(material)).max().orElse(0.0); }
            };
        }

        static ICanData of(int role, int work) {
            return new ICanData() {
                @Override public String unique() { return role+"#"+work; }
                @Override public Optional<Integer> role() { return Optional.of(role); }
                @Override public Optional<Integer> work() { return Optional.of(work); }

                @Override public boolean isCanBreak(String material) { return false; }
                @Override public boolean isCanPlace(String material) { return false; }
                @Override public boolean isCanPlace(String from_material, String to_material) { return false; }
                @Override public boolean isCanCraft(String craft) { return false; }
                @Override public Stream<String> getCanCrafts() { return Stream.empty(); }
                @Override public boolean isCanUse(String item) { return false; }
                @Override public Stream<String> getCanUse() { return Stream.empty(); }
                @Override public boolean isCanDamage(EntityType entity) { return false; }
                @Override public boolean isCanFarm(EntityType entity) { return false; }
                @Override public boolean isCanFishing() { return false; }
                @Override public double getBreakFarmReplace(Material material) { return 0.0; }
            };
        }
    }
    public static class CanData implements ICanData {
        @Override public Optional<Integer> role() { return Optional.empty(); }
        @Override public Optional<Integer> work() { return Optional.empty(); }

        public final Set<String> canBreak = new HashSet<>();
        public final Set<String> canPlace = new HashSet<>();
        public final Set<String> canCraft = new HashSet<>();
        public final Set<String> canUse = new HashSet<>();
        public final Set<EntityType> canDamage = new HashSet<>();
        public final Set<EntityType> canFarm = new HashSet<>();
        public final boolean canFishing;
        public final HashMap<Material, Double> breakFarmReplace = new HashMap<>();
        public final String unique;
        private static int uniqueIterator = 0;

        public CanData(JsonObject json) {
            unique = String.valueOf(uniqueIterator++);
            canFishing = json.has("fishing") && json.get("fishing").getAsBoolean();
            ParseUtils.parseAdd(json, "break", canBreak, Stream.concat(Blocks.creators.keySet().stream(), Arrays.stream(Material.values()).map(Enum::name)).collect(Collectors.toSet()), v -> v);
            ParseUtils.parseAdd(json, "place", canPlace, Stream.concat(Blocks.creators.keySet().stream(), Arrays.stream(Material.values()).map(Enum::name)).collect(Collectors.toSet()), v -> v);
            ParseUtils.parseAdd(json, "craft", canCraft, Streams.stream(Bukkit.getServer().recipeIterator()).map(v -> ((Keyed)v).getKey().getKey()).toList(), v -> v);
            ParseUtils.parseAdd(json, "use", canUse, Items.creatorIDs.keySet(), v -> v);
            ParseUtils.parseAdd(json, "damage", canDamage, Arrays.asList(EntityType.values()), Enum::name);
            ParseUtils.parseAdd(json, "farm", canFarm, Arrays.asList(EntityType.values()), Enum::name);

            if (json.has("break_farm")) json.get("break_farm").getAsJsonObject().entrySet().forEach(kv -> {
                double chance = kv.getValue().getAsDouble();
                for (Material material : Material.values())
                    if (Regex.compareRegex(material.name(), kv.getKey()))
                        breakFarmReplace.put(material, chance);
            });
        }

        @Override public String unique() { return unique; }

        @Override public boolean isCanBreak(String material) { return canBreak.contains(material); }
        @Override public boolean isCanPlace(String material) { return canPlace.contains(material); }
        @Override public boolean isCanPlace(String from_material, String to_material) { return canPlace.contains(to_material); }
        @Override public boolean isCanCraft(String craft) { return canCraft.contains(craft); }
        @Override public Stream<String> getCanCrafts() { return canCraft.stream(); }
        @Override public boolean isCanUse(String use) { return canUse.contains(use); }
        @Override public Stream<String> getCanUse() { return canUse.stream(); }
        @Override public boolean isCanDamage(EntityType entity) { return canDamage.contains(entity); }
        @Override public boolean isCanFarm(EntityType entity) { return canFarm.contains(entity); }
        @Override public boolean isCanFishing() { return canFishing; }
        @Override public double getBreakFarmReplace(Material material) { return breakFarmReplace.getOrDefault(material, 0.0); }
    }
    public static ICanData getCanData(Player player) {
        return getCanData(player.getUniqueId());
    }
    public static ICanData getCanData(UUID uuid) {
        if (uuid == null) return ICanData.getNothing();
        Optional<UserRow> row = UserRow.getBy(uuid);

        int role = row.map(v -> v.role).orElse(0);
        Optional<Integer> work = row.map(v -> v.work);

        List<ICanData> canData = new ArrayList<>();
        canData.add(ICanData.of(role, work.orElse(0)));

        work.map(Works.works::get).map(v -> v.canData).ifPresent(canData::add);
        work.flatMap(workID -> row.flatMap(v -> LevelModule.getLevelStep(v.id, workID))).map(v -> v.canData).ifPresent(canData::add);
        if (role != 0) row.flatMap(Grants::getGrantData).ifPresent(grantData -> {
            canData.add(grantData.canData);
            work.map(grantData.worksCans::get).ifPresent(canData::add);
        });
        row.ifPresent(user -> {
            UUID _uuid = user.uuid;
            int workID = work.orElse(0);
            Tables.USERCRAFTS_TABLE.forEach(craftRow -> {
                if (!craftRow.uuid.equals(_uuid)) return;
                if (craftRow.craftWorks != null && !craftRow.craftWorks.contains(workID)) return;
                Integer useCount = userCraftUsages.get(craftRow.id);
                if (useCount == null) useCount = craftRow.useCount;
                if (useCount != null && useCount <= 0) return;
                canData.add(Crafts.canDataByRegex(craftRow.craftRegex));
            });
        });
        return ICanData.combine(canData);
    }

    private static final ConcurrentHashMap<Integer, Integer> userCraftUsages = new ConcurrentHashMap<>();
    public static void onUserCraftUpdate(UserCraftsRow row, KeyedTable.Event event) {
        if (event.removed || row.useCount == null) {
            userCraftUsages.remove(row.id);
            return;
        }
        userCraftUsages.compute(row.id, (id, usage) -> {
            if (usage == null) return null;
            if (usage.equals(row.useCount)) return null;
            Methods.syncUserCrafts(row.id, usage);
            return usage;
        });
    }
    public static void onRecipeUse(IRecipe<?> recipe, UUID uuid, ICanData data) {
        onRecipeUse(recipe.getId().getPath(), uuid, data);
    }
    public static void onRecipeUse(Recipe recipe, UUID uuid, ICanData data) {
        if (recipe instanceof Keyed keyed) onRecipeUse(keyed.getKey().getKey(), uuid, data);
    }
    private static void onRecipeUse(String recipePath, UUID uuid, ICanData data) {
        if (uuid == null) return;
        Toast1<Boolean> use = Toast.of(false);
        data.work().ifPresent(work -> UserRow.getBy(uuid).ifPresent(user -> {
            UUID _uuid = user.uuid;
            Tables.USERCRAFTS_TABLE.forEach(craftRow -> {
                if (use.val0 || !craftRow.uuid.equals(_uuid) || craftRow.useCount == null) return;
                if (craftRow.craftWorks != null && !craftRow.craftWorks.contains(work)) return;
                if (!Crafts.canDataByRegex(craftRow.craftRegex).isCanCraft(recipePath)) return;
                userCraftUsages.compute(craftRow.id, (id, usage) -> (usage == null ? craftRow.useCount : usage) - 1);
                use.val0 = true;
                onUserCraftUpdate(craftRow, KeyedTable.Event.Updated);
            });
        }));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST) public static void on(EntityChangeBlockEvent e) {
        if (e.getEntityType() == EntityType.FALLING_BLOCK) {
            if (e.getTo().isAir()) {
                Optional<Toast3<UUID, Long, String>> owner = Optional.ofNullable(owners.remove(new Position(e.getBlock())));
                if (owner.isEmpty()) return;
                e.getEntity().getPersistentDataContainer().set(
                        JManager.key("place_owner"),
                        PersistentDataType.STRING,
                        owner.map(v -> v.val0 + " " + v.val1 + " " + v.val2).get()
                );
            } else {
                Optional<Toast3<UUID, Long, String>> owner = Optional.ofNullable(e.getEntity().getPersistentDataContainer().get(JManager.key("place_owner"), PersistentDataType.STRING))
                        .map(v -> v.split(" "))
                        .map(v -> Toast.of(UUID.fromString(v[0]), Long.parseLong(v[1]), v[2]));
                if (owner.isEmpty()) return;
                owners.put(new Position(e.getBlock()), owner.get());
            }
        }
    }
    @EventHandler public static void on(BlockBreakEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (Death.isDamageLay(uuid) || Knock.isKnock(uuid) || HandCuffs.isMove(uuid)) {
            e.setCancelled(true);
            return;
        }
        Block block = e.getBlock();
        Material material = block.getType();
        ICanData data = getCanData(uuid);
        Position pos = new Position(block);
        String block_key = Blocks.getBlockKey(block);
        if (data.isCanBreak(block_key) || Optional.ofNullable(owners.get(pos)).filter(v -> uuid.equals(v.val0)).filter(v -> block_key.equals(v.val2)).isPresent()) {
            owners.remove(pos);
            Block farmland = block.getLocation().add(0, -1, 0).getBlock();
            if (farmland.getType() == Material.FARMLAND) {
                double chance = data.getBreakFarmReplace(material);
                if (chance != 0 && RandomUtils.rand_is(chance)) {
                    farmland.setType(Material.COARSE_DIRT);
                }
            }
            return;
        }
        e.setCancelled(true);
        LangMessages.Message.Work_Error_Use.sendMessage(e.getPlayer());
    }
    @EventHandler public static void on(PlayerBucketFillEvent e) {
        ICanData data = getCanData(e.getPlayer().getUniqueId());
        if (data.isCanBreak(e.getItemStack().getType().name())) return;
        e.setCancelled(true);
        LangMessages.Message.Work_Error_Use.sendMessage(e.getPlayer());
    }
    @EventHandler public static void on(PlayerBucketEmptyEvent e) {
        ICanData data = getCanData(e.getPlayer().getUniqueId());
        if (data.isCanPlace(e.getBucket().name())) return;
        e.setCancelled(true);
        LangMessages.Message.Work_Error_Use.sendMessage(e.getPlayer());
    }
    @EventHandler public static void on(BlockPlaceEvent e) {
        Block block = e.getBlock();
        UUID uuid = e.getPlayer().getUniqueId();
        ICanData data = getCanData(uuid);
        Material from = e.getBlockReplacedState().getType();
        Material to = block.getType();
        if (from == Material.ROOTED_DIRT && to == Material.DIRT) {
            block.setType(Material.COARSE_DIRT);
            return;
        }
        Items.getOptional(BlockLimitSetting.class, e.getItemInHand())
                .flatMap(v -> v.isLimitWithGet(e.getBlock().getLocation()).map(_v -> Toast.of(_v, v.limit)))
                .ifPresent(dat -> dat.invoke((count, limit) -> {
                    e.setCancelled(true);
                    LangMessages.Message.Block_Error_Limit.sendMessage(e.getPlayer(), Apply.of()
                            .add("limit", String.valueOf(limit))
                            .add("count", String.valueOf(count))
                    );
                }));
        if (e.isCancelled()) return;
        if (data.isCanPlace(Items.getOptional(BlockSetting.class, e.getItemInHand())
                .flatMap(setting -> {
                    InfoComponent.Rotation.Value rotation = InfoComponent.Rotation.of(e.getPlayer().getLocation().getDirection(), setting.rotation.keySet());
                    return Optional.ofNullable(setting.rotation.get(rotation));
                })
                .orElseGet(() -> Blocks.getBlockKey(block))
        )) {
            if (from == Material.DIRT_PATH && to == Material.FARMLAND) block.setType(Material.DIRT);
            lime.nextTick(() -> owners.put(new Position(block), Toast.of(uuid, System.currentTimeMillis() + canBreakCooldown, Blocks.getBlockKey(block))));
            return;
        }
        e.setCancelled(true);
        LangMessages.Message.Work_Error_Use.sendMessage(e.getPlayer());
    }
    @EventHandler public static void on(PopulateLootEvent e) {
        net.minecraft.world.entity.Entity entity = e.getOrDefault(Parameters.ThisEntity, null);
        if (entity == null || entity instanceof EntityPlayer || entity instanceof EntityFishingHook) return;
        if (!(e.getOrDefault(Parameters.KillerEntity, null) instanceof EntityPlayer killer)) {
            e.setCancelled(true);
            return;
        }
        ICanData data = getCanData(killer.getUUID());
        if (data.isCanDamage(entity.getBukkitEntity().getType())) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(EntityDamageByEntityEvent e) {
        ExtMethods.damagerPlayer(e).ifPresent(player -> {
            ICanData data = getCanData(player.getUniqueId());
            if (data.isCanDamage(e.getEntityType())) return;
            e.setDamage(0.01D);
        });
    }
    @EventHandler(ignoreCancelled = true) public static void on(CraftItemEvent e) {
        Recipe recipe = e.getRecipe();
        if (!(recipe instanceof Keyed keyed)) return;
        Player player = (Player)e.getWhoClicked();
        ICanData data = getCanData(player.getUniqueId());
        if (data.isCanCraft(keyed.getKey().getKey())) {
            Perms.onRecipeUse(e.getRecipe(), player.getUniqueId(), data);
            return;
        }
        e.setCancelled(true);
        LangMessages.Message.Work_Error_Use.sendMessage(player);
    }
    @EventHandler public static void on(EntityEnterLoveModeEvent e) {
        Player player = (Player)e.getHumanEntity();
        if (player == null) return;
        ICanData data = getCanData(player.getUniqueId());
        if (data.isCanFarm(e.getEntityType())) return;
        e.setCancelled(true);
        LangMessages.Message.Work_Error_Use.sendMessage(player);
    }
    @EventHandler public static void on(PlayerFishEvent e) {
        Player player = e.getPlayer();
        ICanData data = getCanData(player.getUniqueId());
        if (data.isCanFishing()) return;
        e.setCancelled(true);
        LangMessages.Message.Work_Error_Use.sendMessage(player);
    }
}


















