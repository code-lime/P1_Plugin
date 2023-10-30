package org.lime.gp.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.entity.Entities;
import org.lime.gp.entity.component.data.OwnerInstance;
import org.lime.gp.extension.JManager;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Execute;
import org.lime.system.json;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityOwner {
    public record UserInfo(int id) {
        public static UserInfo parse(JsonObject json) {
            JsonObject obj = json.getAsJsonObject();
            return new UserInfo(obj.get("id").getAsInt());
        }
        private static Optional<UserInfo> parseOld(JsonElement json) {
            return json.isJsonObject()
                    ? Optional.of(parse(json.getAsJsonObject()))
                    : UserRow.getBy(UUID.fromString(json.getAsString())).map(v -> new UserInfo(v.id));
        }
        public static Optional<UserInfo> tryLoad(UUID uuid) { return UserRow.getBy(uuid).map(v -> new UserInfo(v.id)); }

        public json.builder.object save() { return json.object().add("id", id); }

        public boolean is(UserRow row) { return row.id == id; }
        public Optional<UserRow> row() { return UserRow.getBy(id); }
    }

    private final Entity entity;
    public EntityOwner(Tameable tameable) { this.entity = tameable; }
    public EntityOwner(Marker marker) { this.entity = marker; }

    public void setOwner(@Nullable UserRow row) { setOwner(entity, row); }
    public void changeSubs(UserRow row, boolean isAppend) { changeSubs(entity, row, isAppend); }
    public Optional<UserInfo> getOwner() { return getOwner(entity); }
    public Stream<UserInfo> getSubs() { return getSubs(entity); }
    public boolean canInteract(UUID uuid) { return canInteract(entity, uuid); }

    public Entity entity() { return entity; }


    public static CoreElement create() {
        return CoreElement.create(EntityOwner.class)
                .withInit(EntityOwner::init);
    }

    private static void init() {
        AnyEvent.addEvent("animal.owner.set", AnyEvent.type.other, builder -> builder.createParam(UUID::fromString, "[animal_uuid]").createParam(Integer::parseInt, "[owner_id]"), (player, animal_uuid, owner_id) -> {
            Entity entity = Bukkit.getEntity(animal_uuid);
            if (entity == null) return;
            if (!getOwner(entity).flatMap(v -> UserRow.getBy(player).map(v::is)).orElse(false)) return;
            UserRow.getBy(owner_id).ifPresent(row -> setOwner(entity, row));
        });
        AnyEvent.addEvent("animal.sub", AnyEvent.type.other, builder -> builder.createParam("add","del").createParam(UUID::fromString, "[animal_uuid]").createParam(Integer::parseInt, "[sub_id]"), (player, state, animal_uuid, sub_id) -> {
            Entity entity = Bukkit.getEntity(animal_uuid);
            if (entity == null) return;
            if (!getOwner(entity).flatMap(v -> UserRow.getBy(player).map(v::is)).orElse(false)) return;
            boolean isAppend;
            switch (state) {
                case "add" -> isAppend = true;
                case "del" -> isAppend = false;
                default -> { return; }
            }
            UserRow.getBy(sub_id).ifPresent(row -> changeSubs(entity, row, isAppend));
        });

        AnyEvent.addEvent("animal.owner.set", AnyEvent.type.owner_console, builder -> builder.createParam(UUID::fromString, "[animal_uuid]").createParam(Integer::parseInt, "[owner_id]"), (player, animal_uuid, owner_id) -> {
            Entity entity = Bukkit.getEntity(animal_uuid);
            if (entity == null) return;
            UserRow.getBy(owner_id).ifPresent(row -> setOwner(entity, row));
        });
        AnyEvent.addEvent("animal.sub", AnyEvent.type.owner_console, builder -> builder.createParam("add","del").createParam(UUID::fromString, "[animal_uuid]").createParam(Integer::parseInt, "[sub_id]"), (player, state, animal_uuid, sub_id) -> {
            Entity entity = Bukkit.getEntity(animal_uuid);
            if (entity == null) return;
            boolean isAppend;
            switch (state) {
                case "add" -> isAppend = true;
                case "del" -> isAppend = false;
                default -> { return; }
            }
            UserRow.getBy(sub_id).ifPresent(row -> changeSubs(entity, row, isAppend));
        });
    }

    private static Stream<UserInfo> readOldSubs(PersistentDataContainer container) {
        return JManager.get(JsonArray.class, container, "sub_owners", new JsonArray())
                .asList()
                .stream()
                .map(UserInfo::parseOld)
                .flatMap(Optional::stream);
    }

    private static final NamespacedKey OWNER_SUBS = new NamespacedKey(lime._plugin, "owner_subs");
    private static final NamespacedKey OWNER_ID = new NamespacedKey(lime._plugin, "owner_id");

    private static AnimalTamer createOwner(UUID uuid) {
        return new AnimalTamer() {
            @Override public String getName() { return null; }
            @Override public @NotNull UUID getUniqueId() { return uuid; }
        };
    }

    public static void setOwner(Entity entity, @Nullable UserRow row) {
        if (entity instanceof Marker marker) {
            Entities.of(marker)
                    .flatMap(Entities::customOf)
                    .flatMap(v -> v.list(OwnerInstance.class).findAny())
                    .ifPresent(owner -> {
                        owner.setOwner(row == null ? null : new UserInfo(row.id));
                        owner.clearSubs();
                    });
        }
        else if (entity instanceof Tameable tameable) {
            tameable.setOwner(row == null ? null : createOwner(row.uuid));
            PersistentDataContainer container = tameable.getPersistentDataContainer();
            if (row == null) container.remove(OWNER_ID);
            else container.set(OWNER_ID, PersistentDataType.INTEGER, row.id);
            JManager.set(container, OWNER_SUBS, new JsonArray());
        }
    }
    public static void changeSubs(Entity entity, UserRow row, boolean isAppend) {
        UserInfo info = new UserInfo(row.id);
        if (entity instanceof Marker marker) {
            Entities.of(marker)
                    .flatMap(Entities::customOf)
                    .flatMap(v -> v.list(OwnerInstance.class).findAny())
                    .ifPresent(owner -> owner.changeSubs(info, isAppend));
        }
        else if (entity instanceof Tameable tameable) {
            PersistentDataContainer container = tameable.getPersistentDataContainer();
            Set<UserInfo> subs = JManager.has(container, OWNER_SUBS)
                    ? Objects.requireNonNull(JManager.get(JsonArray.class, container, OWNER_SUBS, new JsonArray()))
                    .asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(UserInfo::parse)
                    .collect(Collectors.toSet())
                    : readOldSubs(container)
                    .collect(Collectors.toSet());
            if (Execute.func(isAppend ? subs::add : subs::remove).invoke(info)) {
                JManager.set(container, OWNER_SUBS, json.array().add(subs, UserInfo::save).build());
            }
        }
    }

    public static Optional<UserInfo> getOwner(Entity entity) {
        if (entity instanceof Marker marker)
            return Entities.of(marker)
                    .flatMap(Entities::customOf)
                    .flatMap(v -> v.list(OwnerInstance.class).findAny())
                    .flatMap(OwnerInstance::getOwner);
        if (entity instanceof Tameable tameable && tameable.isTamed()) {
            UUID uuid = tameable.getOwnerUniqueId();
            if (uuid != null) {
                PersistentDataContainer container = tameable.getPersistentDataContainer();
                if (container.has(OWNER_ID, PersistentDataType.INTEGER)) {
                    int id = Objects.requireNonNull(container.get(OWNER_ID, PersistentDataType.INTEGER));
                    UserRow.getBy(id)
                            .filter(v -> !v.uuid.equals(uuid))
                            .ifPresent(row -> tameable.setOwner(createOwner(row.uuid)));
                    return Optional.of(new UserInfo(id));
                }
                return UserInfo.tryLoad(uuid)
                        .filter(info -> {
                            container.set(OWNER_ID, PersistentDataType.INTEGER, info.id);
                            return true;
                        });
            }
        }
        return Optional.empty();
    }
    public static Stream<UserInfo> getSubs(Entity entity) {
        if (entity instanceof Marker marker)
            return Entities.of(marker)
                    .flatMap(Entities::customOf)
                    .stream()
                    .flatMap(v -> v.list(OwnerInstance.class))
                    .flatMap(OwnerInstance::getSubs);
        if (entity instanceof Tameable tameable && tameable.isTamed()) {
            PersistentDataContainer container = tameable.getPersistentDataContainer();
            if (!JManager.has(container, OWNER_SUBS))
                JManager.set(container, OWNER_SUBS, json.array().add(readOldSubs(container).iterator(), UserInfo::save).build());
            return Objects.requireNonNull(JManager.get(JsonArray.class, container, OWNER_SUBS, new JsonArray()))
                    .asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(UserInfo::parse);
        }
        return Stream.empty();
    }
    public static boolean canInteract(Entity entity, UUID uuid) {
        return UserRow.getBy(uuid)
                .map(row -> {
                    if (row.role == 9) return true;
                    Optional<UserInfo> owner = getOwner(entity);
                    if (owner.isEmpty()) return true;
                    return owner.filter(v -> v.is(row))
                            .or(() -> getSubs(entity)
                                    .filter(v -> v.is(row))
                                    .findAny())
                            .isPresent();
                })
                .orElse(false);
    }

    public static Stream<EntityOwner> getAllEntities() {
        return Bukkit.getWorlds()
                .stream()
                .flatMap(world -> world.getEntities()
                        .stream()
                        .map(v -> {
                            if (v instanceof Tameable tameable) return new EntityOwner(tameable);
                            else if (v instanceof Marker marker) return new EntityOwner(marker);
                            else return null;
                        })
                        .filter(Objects::nonNull));
    }
}




