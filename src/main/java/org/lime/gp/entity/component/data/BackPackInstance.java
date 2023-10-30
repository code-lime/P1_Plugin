package org.lime.gp.entity.component.data;

import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.minecraft.world.EnumInteractionResult;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.plugin.CoreElement;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.rows.UserFlagsRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.Entities;
import org.lime.gp.entity.EntityInstance;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.component.display.instance.DisplayInstance;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.item.Items;
import org.lime.gp.module.DrawText;
import org.lime.gp.player.module.TabManager;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.Time;
import org.lime.system.json;
import org.lime.system.utils.ItemUtils;

import java.util.*;

public class BackPackInstance extends EntityInstance implements CustomEntityMetadata.Interactable, CustomEntityMetadata.Tickable {
    public static double LOCK_TIME = 10 * 60;
    public static CoreElement create() {
        return CoreElement.create(BackPackInstance.class)
                .<JsonPrimitive>addConfig("config", v -> v.withParent("backpack_lock_time").withDefault(new JsonPrimitive(LOCK_TIME)).withInvoke(_v -> LOCK_TIME = _v.getAsDouble()));
    }

    public UUID owner() {
        return this.owner_uuid;
    }
    public int owner_id() {
        return this.owner_id;
    }
    public String displayName() {
        if (owner_uuid == null) return "#???";
        Integer id = TabManager.getPayerIDorNull(owner_uuid);
        if (id != null) return "<" + id + ">";
        return "#" + (owner_id == -1 ? "???" : (owner_id+""));
    }
    public BackPackInstance setup(int id, UUID uuid, List<ItemStack> items) {
        this.owner_id = id;
        this.owner_uuid = uuid;
        this.items.clear();
        this.items.addAll(items);
        saveData();
        return this;
    }

    private UUID owner_uuid = null;
    private int owner_id = -1;

    private long create_time = System.currentTimeMillis();
    public final List<ItemStack> items = new ArrayList<>();
    public BackPackInstance(ComponentDynamic<?, ?> component, CustomEntityMetadata metadata) {
        super(component, metadata);
    }

    @Override public void read(JsonObjectOptional json) {
        items.clear();
        owner_uuid = null;
        owner_id = -1;

        json.getAsJsonArray("items")
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsString)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ItemUtils::loadItem)
                .forEach(items::add);
        json.getAsJsonObject("owner").ifPresent(owner -> {
            owner.get("uuid").flatMap(JsonElementOptional::getAsString).map(UUID::fromString).ifPresent(uuid -> owner_uuid = uuid);
            owner.get("id").flatMap(JsonElementOptional::getAsInt).ifPresent(id -> owner_id = id);
        });
        create_time = json.get("create_time").flatMap(JsonElementOptional::getAsLong).orElseGet(System::currentTimeMillis);
    }
    @Override public json.builder.object write() {
        return json.object()
                .addArray("items", v -> v.add(items, ItemUtils::saveItem))
                .addObject("owner", v -> v
                        .add("uuid", owner_uuid == null ? null : owner_uuid.toString())
                        .add("id", owner_id)
                )
                .add("create_time", create_time);
    }
    @Override public void onTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        int total_sec = (int)((create_time + (LOCK_TIME * 1000) - System.currentTimeMillis()) / 1000);
        List<Component> lines = (total_sec <= 0 ? LangMessages.Message.Entity_BackPack_Unlock : LangMessages.Message.Entity_BackPack_Lock).getMessages(Apply.of()
                .add("name", displayName())
                .add("time", total_sec <= 0 ? "00:00" : Time.formatTotalTime(total_sec, Time.Format.MINUTE_TIME))
        );
        int size = lines.size();
        String prefix = unique().toString();
        for (int i = 0; i < size; i++) {
            int index = i;
            double y = 0.5 + index * 0.2;
            DrawText.show(new DrawText.IShowTimed(0.5) {
                @Override public String getID() { return prefix + "." + index; }
                @Override public boolean filter(Player player) { return true; }
                @Override public Component text(Player player) { return lines.get(index); }
                @Override public Location location() { return metadata.location(0, y, 0); }
                @Override public double distance() { return 5; }
            });
        }
    }
    @Override public EnumInteractionResult onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event) {
        if (!event.isPlayerSneaking())
            return EnumInteractionResult.PASS;
        if (event.getHand() != EquipmentSlot.HAND)
            return EnumInteractionResult.PASS;
        if (!event.getPlayer().getUniqueId().equals(owner_uuid) && create_time + (LOCK_TIME * 1000) > System.currentTimeMillis())
            return EnumInteractionResult.PASS;
        Items.dropItem(metadata.location(), items);
        items.clear();
        metadata.destroy();
        return EnumInteractionResult.CONSUME;
    }

    public static void dropItems(Player player, Location location, List<ItemStack> items) {
        UUID uuid = player.getUniqueId();
        Optional<Integer> backPackID = UserFlagsRow.backPackID(uuid);
        Entities.creator("backpack")
                .map(v -> v.spawn(location))
                .flatMap(Entities::customOf)
                .filter(v -> {
                    backPackID.ifPresent(id -> v.list(DisplayInstance.class).findAny().ifPresent(_v -> _v.set("id", id + "")));
                    return true;
                })
                .flatMap(v -> v.list(BackPackInstance.class).findAny())
                .ifPresentOrElse(
                        v -> v.setup(UserRow.getBy(player).map(user -> user.id).orElse(-1), uuid, items),
                        () -> Items.dropItem(location, items)
                );
    }
}






















