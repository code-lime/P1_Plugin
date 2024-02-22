package org.lime.gp.player.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.entity.EnumItemSlot;
import org.bukkit.craftbukkit.v1_20_R1.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.joml.Math;
import org.lime.gp.admin.Administrator;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.DeathRow;
import org.lime.gp.database.rows.QuentaRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.KeyedTable;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.gp.module.ConfirmCommand;
import org.lime.gp.module.biome.time.DateTime;
import org.lime.gp.module.biome.time.DayManager;
import org.lime.gp.module.npc.EPlayerModule;
import org.lime.gp.module.npc.eplayer.Pose;
import org.lime.gp.module.npc.eplayer.RawEPlayer;
import org.lime.gp.player.module.needs.INeedEffect;
import org.lime.gp.player.module.needs.NeedSystem;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action1;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;
import org.lime.system.utils.ItemUtils;

import java.util.*;
import java.util.stream.Stream;

public class DeathGame {
    public static CoreElement create() {
        return CoreElement.create(DeathGame.class)
                .withInit(DeathGame::init)
                .addCommand("death.user", v -> ConfirmCommand.setup(v, "смерть", DeathGame::deathUser));
    }
    private static void init() {
        NeedSystem.register(DeathGame::getDeathNeeds);

        lime.repeat(DeathGame::update, 10);
        EPlayerModule.registry(() -> deathEPlayers.values().stream());
        sync();
    }
    private static void update() {
        DateTime now = DayManager.now();
        double nowTotalSeconds = now.getTotalSeconds();
        lime.MainWorld.getPlayers().forEach(player -> UserRow.getBy(player).ifPresent(user -> user.dieDate.ifPresent(dieDate -> {
            double totalSeconds = dieDate.getTotalSeconds();
            if (totalSeconds > nowTotalSeconds) return;
            Set<String> tags = player.getScoreboardTags();
            if (tags.contains("death")) return;
            tags.add("death");
            executeDeath(player, user, dieDate);
        })));
    }
    private static void executeDeath(Player player, UserRow user, DateTime dieDate) {
        Administrator.aban(player.getUniqueId(), "Вы мертвы! Дата смерти: " + dieDate.toFormat("dd.yyyy"), null);
        Skins.Property property = Skins.getProperty(player);

        PlayerInventory inventory = player.getInventory();
        HashMap<EquipmentSlot, ItemStack> display = new HashMap<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            display.put(slot, item.clone());
        }
        List<ItemStack> items = Death.extractInventoryDrop(player, true);
        Methods.addDeath(user.id, dieDate, player.getLocation(), property.toJson().toString(), saveEquipment(display, items), () -> {});
    }
    public static void onUpdate(DeathRow row, KeyedTable.Event event) { sync(); }
    private static HashMap<Integer, RawEPlayer> deathEPlayers = new HashMap<>();
    private static void onClick(DeathRow row, Player player, boolean isShift) {
        if (!isShift) return;
        if (!QuentaRow.hasQuenta(player.getUniqueId())) {
            LangMessages.Message.Action_Error.sendMessage(player);
            return;
        }
        UserRow.getBy(row.userID).ifPresent(userRow -> Methods.lootDeath(row.id, equip -> {
            if (Objects.equals(equip, "ERROR")) return;
            Items.getItemCreator("Tool.Custom.DeathLog")
                    .map(v -> v.createItem(Apply.of()
                            .add(userRow)
                            .add("die_date", row.dieDate.toString())
                    ))
                    .ifPresent(item -> Items.dropGiveItem(player, item, true));
            if (equip == null) return;
            loadEquipment(equip).val1
                    .forEach(item -> Items.dropGiveItem(player, item, true));
        }));
    }
    private static String saveEquipment(Map<EquipmentSlot, ItemStack> display, List<ItemStack> items) {
        return json.object()
                .addObject("display", v -> v.add(display, Enum::name, ItemUtils::saveItem))
                .addArray("items", v -> v.add(items, ItemUtils::saveItem))
                .build()
                .toString();
    }
    private static Toast2<Map<EnumItemSlot, net.minecraft.world.item.ItemStack>, List<ItemStack>> loadEquipment(String equipment) {
        if (equipment == null || equipment.isBlank()) return Toast.of(Collections.emptyMap(), Collections.emptyList());
        JsonObject _equipment = json.parse(equipment).getAsJsonObject();
        JsonObject _display = _equipment.getAsJsonObject("display");
        JsonArray _items = _equipment.getAsJsonArray("items");
        Map<EnumItemSlot, net.minecraft.world.item.ItemStack> display = new HashMap<>();
        List<ItemStack> items = new ArrayList<>();
        _display.entrySet()
                .forEach(kv -> display.put(
                        CraftEquipmentSlot.getNMS(EquipmentSlot.valueOf(kv.getKey())),
                        CraftItemStack.asNMSCopy(ItemUtils.loadItem(kv.getValue().getAsString()))
                ));
        _items.forEach(v -> items.add(ItemUtils.loadItem(v.getAsString())));
        return Toast.of(display, items);
    }

    private static void sync() {
        lime.once(() -> Tables.DEATH_TABLE.forEach(row -> {
            String deathKey = "death#" + row.uniqueRowKey;
            deathEPlayers.compute(row.id, (k,v) -> {
                if (row.status == DeathRow.Status.HIDE) return null;
                if (v != null && Objects.equals(deathKey, v.key())) return v;
                return new RawEPlayer(
                        deathKey,
                        row.location.clone().add(0, -0.2, 0),
                        null,
                        Pose.CRAWL,
                        (p,s) -> onClick(row, p, s),
                        List.of(Component.text("Труп")),
                        loadEquipment(row.equipment).val0,
                        row.skin == null ? null : new Skins.Property(json.parse(row.skin).getAsJsonObject())
                );
            });
        }), 1);
    }
    private static Stream<INeedEffect<?>> getDeathNeeds(Player player) {
        return UserRow.getBy(player)
                .flatMap(v -> v.dieDate)
                .map(DateTime::getTotalSeconds)
                .map(totalSeconds -> {
                    DateTime now = DayManager.now();
                    DateTime deathEffect = now.addYears(1);
                    double deathTotalSeconds = deathEffect.getTotalSeconds();
                    if (totalSeconds > deathTotalSeconds) return Stream.<INeedEffect<?>>empty();
                    double nowTotalSeconds = now.getTotalSeconds();
                    double totalPercent = deathTotalSeconds - nowTotalSeconds;
                    double diePercent = Math.clamp(0, 1, (deathTotalSeconds - totalSeconds) / totalPercent);
                    double value = 1 + diePercent * diePercent * 3;
                    return Stream.<INeedEffect<?>>of(INeedEffect.Mutate.of(INeedEffect.Type.SLEEP, value));
                })
                .orElseGet(Stream::empty);
    }
    public static float getDelta(Player player) {
        return UserRow.getBy(player)
                .flatMap(v -> v.dieDate)
                .map(DateTime::getTotalSeconds)
                .map(totalSeconds -> {
                    DateTime now = DayManager.now();
                    DateTime deathEffect = now.addYears(1);
                    double deathTotalSeconds = deathEffect.getTotalSeconds();
                    if (totalSeconds > deathTotalSeconds) return 0f;
                    double nowTotalSeconds = now.getTotalSeconds();
                    double totalPercent = deathTotalSeconds - nowTotalSeconds;
                    float diePercent = (float)Math.clamp(0, 1, (deathTotalSeconds - totalSeconds) / totalPercent);
                    return diePercent * diePercent;
                })
                .orElse(0f);
    }

    private static void deathUser(UUID uuid, Action1<Component> callback) {
        UserRow.getBy(uuid).ifPresentOrElse(row -> Methods.forceDeath(row.id, DayManager.now(), () -> {
                    callback.invoke(Component.empty()
                            .color(NamedTextColor.GREEN)
                            .append(Component.text("Дата смерти игрока ")
                                    .append(Component.text(uuid.toString()).color(NamedTextColor.GOLD))
                                    .append(Component.text(" была успешно изменена!"))
                            ));
        }), () -> callback.invoke(Component.empty()
                .color(NamedTextColor.RED)
                .append(Component.text("Игрок ")
                        .append(Component.text(uuid.toString()).color(NamedTextColor.GOLD))
                        .append(Component.text(" не найден!"))
                ))
        );
    }
}










