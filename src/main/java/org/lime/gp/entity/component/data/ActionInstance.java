package org.lime.gp.entity.component.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.EnumInteractionResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.gp.database.rows.QuentaRow;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.Entities;
import org.lime.gp.entity.EntityInstance;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.module.DrawText;
import org.lime.gp.player.menu.LangEnum;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.json.JsonObjectOptional;
import org.lime.plugin.CoreElement;
import org.lime.system.Time;
import org.lime.system.json;

import java.util.List;
import java.util.UUID;

public class ActionInstance extends EntityInstance implements CustomEntityMetadata.Tickable, DrawText.IShow, CustomEntityMetadata.Interactable {
    public static CoreElement create() {
        return CoreElement.create(ActionInstance.class)
                .withInit(ActionInstance::init);
    }

    private static void init() {
        AnyEvent.addEvent("action.remove", AnyEvent.type.other, v -> v.createParam("[action_uuid]"), (p, uuid) -> {
            if (Bukkit.getEntity(UUID.fromString(uuid)) instanceof Marker marker)
                Entities.of(marker)
                        .flatMap(Entities::customOf)
                        .flatMap(v -> v.list(ActionInstance.class).findAny())
                        .ifPresent(v -> {
                            CoreProtectHandle.logCommand(p, "/action.remove " + v.message);
                            v.metadata().destroy();
                        });
        });
    }

    public ActionInstance(ComponentDynamic<?, ?> component, CustomEntityMetadata metadata) {
        super(component, metadata);
    }

    private String message;
    private UUID owner;
    private long endTime;

    private static final UUID NONE_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Override public void read(JsonObjectOptional json) {
        message = json.getAsString("message").orElse("***");
        owner = json.getAsString("owner").map(UUID::fromString).orElse(NONE_OWNER);
        endTime = json.getAsLong("end_time").orElse(0L);
    }
    @Override public json.builder.object write() {
        return json.object()
                .add("message", message)
                .add("owner", owner.toString())
                .add("end_time", endTime);
    }
    private long lastUpdate = System.currentTimeMillis();
    @Override public void onTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        long now = System.currentTimeMillis();
        lastUpdate = now;
        if (endTime <= now) {
            metadata.destroy();
            return;
        }
        DrawText.show(this);
    }
    @Override public EnumInteractionResult onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (Cooldown.hasOrSetCooldown(uuid, "action.interact", 1)) return EnumInteractionResult.CONSUME;
        if (!QuentaRow.hasQuenta(uuid)) {
            LangMessages.Message.Action_Error.sendMessage(player);
            return EnumInteractionResult.CONSUME;
        }
        MenuCreator.showLang(player, LangEnum.ACTION_DELETE, Apply.of().add("action_uuid", metadata.marker.getUUID().toString()));
        return EnumInteractionResult.CONSUME;
    }

    public ActionInstance setup(String message, UUID owner, long endTime) {
        this.message = message;
        this.owner = owner;
        this.endTime = endTime;
        saveData();
        return this;
    }

    public static void createAction(Player player, Location location, List<String> message, int time) {
        UUID owner = player.getUniqueId();

        long endTime = System.currentTimeMillis() + time * 1000L;
        Entities.creator("action")
                .map(v -> v.spawn(location))
                .flatMap(Entities::customOf)
                .flatMap(v -> v.list(ActionInstance.class).findAny())
                .ifPresent(v -> v.setup(String.join("\n", message), owner, endTime));
    }

    @Override public String getID() { return "ACTION:" + this.metadata().key.uuid(); }
    @Override public boolean filter(Player player) { return true; }
    @Override public Component text(Player player) {
        long now = System.currentTimeMillis();
        long delta = endTime - now;
        return Component.text(message)
                .append(Component.text(" ("+ Time.formatTime((int)(delta / 1000)) +")").color(NamedTextColor.GRAY));
    }
    @Override public Location location() { return this.metadata().location(); }
    @Override public double distance() { return 15; }
    @Override public boolean tryRemove() { return System.currentTimeMillis() > lastUpdate + 1000; }
    @Override public Vector3f scale() { return new Vector3f(0.5f, 0.5f, 0.5f); }
    @Override public Vector3f offset() { return new Vector3f(0, 0, 0); }
}













