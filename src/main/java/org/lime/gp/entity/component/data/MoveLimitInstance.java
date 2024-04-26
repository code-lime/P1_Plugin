package org.lime.gp.entity.component.data;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.craftbukkit.v1_20_R1.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityComponentInstance;
import org.lime.gp.entity.component.display.instance.DisplayInstance;
import org.lime.gp.entity.component.list.MoveLimitComponent;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.settings.use.UseSetting;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.range.IRange;

import java.util.Map;

public class MoveLimitInstance extends EntityComponentInstance<MoveLimitComponent> implements
        CustomEntityMetadata.Interactable,
        CustomEntityMetadata.Tickable,
        CustomEntityMetadata.FirstTickable
{
    public MoveLimitInstance(MoveLimitComponent component, CustomEntityMetadata metadata) {
        super(component, metadata);
    }

    private double value = 1;

    public boolean isActive() {
        return value > 0;
    }

    @Override public void read(JsonObjectOptional json) {
        value = Math.max(1 / component().total, Math.min(1, json.getAsString("value").flatMap(ExtMethods::parseDouble).orElse(1.0)));
    }
    @Override public json.builder.object write() {
        return json.object().add("value", value);
    }
    private void setValue(double value) {
        value = Math.max(0, Math.min(1, value));
        if (this.value == value) return;
        this.value = value;
        saveData();
        syncDisplayVariable();

        if (this.value > 0) return;
        if (!component().destroy) return;

        EntityLimeMarker marker = metadata().marker;
        metadata().destroyWithLoot(v -> v
                .withParameter(LootContextParameters.DAMAGE_SOURCE, marker.damageSources().starve())
                .withParameter(LootContextParameters.KILLER_ENTITY, marker)
        );
    }
    private void changeValue(double delta) {
        setValue(this.value + delta);
    }

    @Override public EnumInteractionResult onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event) {
        EnumHand hand = CraftEquipmentSlot.getHand(event.getHand());
        if (hand != EnumHand.MAIN_HAND || value >= 1 || !(event.getPlayer() instanceof CraftPlayer player) || Cooldown.hasOrSetCooldown(player.getUniqueId(), "move_limit", 0.1))
            return EnumInteractionResult.PASS;
        return event.getClickDisplay()
                .singleOfKey("move_limit.repair")
                .map(v -> {
                    EntityPlayer handle = player.getHandle();
                    MoveLimitComponent component = component();
                    ItemStack item = handle.getItemInHand(hand);
                    for (Map.Entry<Checker, IRange> kv : component.repair.entrySet()) {
                        if (kv.getKey().check(item)) {
                            UseSetting.modifyUseItem(handle, item, false);
                            changeValue(kv.getValue().getValue(component.total) / component.total);
                            return EnumInteractionResult.CONSUME;
                        }
                    }
                    return EnumInteractionResult.PASS;
                })
                .orElse(EnumInteractionResult.PASS);
    }

    private Vec3D lastPosition = null;
    private double moveDelta = 0;
    @Override public void onTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        Vec3D position = metadata.marker.position();
        if (lastPosition == null) lastPosition = position;
        moveDelta += position.subtract(lastPosition).length();
        MoveLimitComponent component = component();
        if (moveDelta > 5) {
            changeValue(-moveDelta / component.total);
            moveDelta = 0;
        }
        lastPosition = position;
    }
    public final void syncDisplayVariable() {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> {
            MoveLimitComponent component = component();
            display.set("move_limit.percent", String.valueOf(value));
            display.set("move_limit.value", String.valueOf((int)Math.round(value * component.total)));
            display.set("move_limit.total", String.valueOf(component.total));
        });
    }

    @Override public void onFirstTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        syncDisplayVariable();
    }
}
