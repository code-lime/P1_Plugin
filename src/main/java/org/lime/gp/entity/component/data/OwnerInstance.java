package org.lime.gp.entity.component.data;

import net.minecraft.world.EnumInteractionResult;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInstance;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.lime;
import org.lime.gp.module.EntityOwner;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.execute.Execute;
import org.lime.system.json;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OwnerInstance extends EntityInstance implements
        CustomEntityMetadata.Interactable
{
    public OwnerInstance(ComponentDynamic<?, ?> component, CustomEntityMetadata metadata) {
        super(component, metadata);
    }

    private @Nullable EntityOwner.UserInfo owner = null;
    private Set<EntityOwner.UserInfo> subOwners = new HashSet<>();

    @Override public void read(JsonObjectOptional json) {
        owner = json.getAsJsonObject("owner")
                .map(v -> EntityOwner.UserInfo.parse(v.base()))
                .orElse(null);
        subOwners = json.getAsJsonArray("sub_owners")
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsJsonObject)
                .flatMap(Optional::stream)
                .map(v -> EntityOwner.UserInfo.parse(v.base()))
                .collect(Collectors.toSet());
    }
    @Override public json.builder.object write() {
        return json.object()
                .add("owner", owner == null ? null : owner.save())
                .addArray("sub_owners", v -> v.add(subOwners, EntityOwner.UserInfo::save));
    }

    @Override public EnumInteractionResult onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event) {
        return EntityOwner.canInteract(metadata.marker.getBukkitEntity(), event.getPlayer().getUniqueId())
            ? EnumInteractionResult.PASS
            : EnumInteractionResult.CONSUME;
    }

    public Optional<EntityOwner.UserInfo> getOwner() { return Optional.ofNullable(this.owner); }
    public void setOwner(@Nullable EntityOwner.UserInfo owner) {
        this.owner = owner;
        saveData();
    }
    public Stream<EntityOwner.UserInfo> getSubs() { return this.subOwners.stream(); }
    public void clearSubs() {
        this.subOwners.clear();
        saveData();
    }
    public void changeSubs(EntityOwner.UserInfo userInfo, boolean isAppend) {
        if (Execute.func(isAppend ? subOwners::add : subOwners::remove).invoke(userInfo))
            saveData();
    }
}




















