package org.lime.gp.block;

import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.json.JsonObjectOptional;
import org.lime.system.Time;
import org.lime.system.json;

import java.util.Optional;
import java.util.UUID;

public abstract class BlockInstance implements CustomTileMetadata.Element {
    private final CustomTileMetadata metadata;
    private final ComponentDynamic<?, ?> component;
    private final UUID unique = UUID.randomUUID();

    public BlockInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        this.metadata = metadata;
        this.component = component;
    }

    public abstract void read(JsonObjectOptional json);
    public abstract json.builder.object write();

    private int changeIndex = 0;
    public void saveData() {
        metadata.skull
                .persistentDataContainer
                .set(Blocks.ofKey(component.name()), LimePersistentDataType.JSON_OBJECT, write()
                        .add("_debug.change.time", Time.formatCalendar(Time.moscowNow(), true))
                        .add("_debug.change.index", changeIndex++)
                        .build());
    }
    public BlockInstance loadData() {
        Optional.ofNullable(metadata.skull
                .persistentDataContainer
                .get(Blocks.ofKey(component.name()), LimePersistentDataType.JSON_OBJECT)
        )
                .map(JsonObjectOptional::of)
                .ifPresent(v -> {
                    changeIndex = v.getAsInt("_debug.change.index").orElse(0);
                    read(v);
                });
        return this;
    }

    public BlockInfo info() { return component.info(); }
    public UUID unique() { return unique; }
    public CustomTileMetadata metadata() { return metadata; }
    public ComponentDynamic<?, ?> component() { return component; }
}






