package org.lime.gp.entity;

import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.Optional;
import java.util.UUID;

public abstract class EntityInstance implements CustomEntityMetadata.Element {
    private final CustomEntityMetadata metadata;
    private final ComponentDynamic<?, ?> component;
    private final UUID unique = UUID.randomUUID();

    public EntityInstance(ComponentDynamic<?, ?> component, CustomEntityMetadata metadata) {
        this.metadata = metadata;
        this.component = component;
    }

    public abstract void read(JsonObjectOptional json);
    public abstract system.json.builder.object write();

    public void saveData() {
        metadata.marker.persistentDataContainer.set(Entities.ofKey(component.name()), LimePersistentDataType.JSON_OBJECT, write().build());
    }
    public EntityInstance loadData() {
        Optional.ofNullable(metadata.marker.persistentDataContainer.get(Entities.ofKey(component.name()), LimePersistentDataType.JSON_OBJECT))
                .map(JsonObjectOptional::of)
                .ifPresent(this::read);
        return this;
    }

    public EntityInfo info() { return component.info(); }
    public UUID unique() { return unique; }
    public CustomEntityMetadata metadata() { return metadata; }
    public ComponentDynamic<?, ?> component() { return component; }
}






