package org.lime.gp.block.component.data;

import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.display.IDisplayVariable;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.module.RandomTickSpeed;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;

import java.util.Map;

public abstract class BaseAgeableInstance<T extends ComponentDynamic<?, ?>> extends BlockComponentInstance<T> implements CustomTileMetadata.Tickable, IDisplayVariable {
    public interface AgeableData {
        double tickAgeModify();
        int limitAge();
    }

    private double ageValue = 0;

    public BaseAgeableInstance(T component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    public int age() {
        return (int)ageValue;
    }
    public void age(int value) {
        ageValue = value;
        saveData();
        syncDisplayVariable();
        onAgeUpdated();
    }
    public abstract AgeableData ageableData();
    protected void onAgeUpdated() { }
    @Override
    public void read(JsonObjectOptional json) {
        age(json.getAsInt("age").orElse(0));
    }

    @Override
    public json.builder.object write() {
        return json.object()
                .add("age", age());
    }

    @Override
    public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        AgeableData ageable = ageableData();
        int limitAge = ageable.limitAge();
        int oldAge = age();
        double value = ageValue + ageable.tickAgeModify() * (RandomTickSpeed.finalValue() / 3);
        if (value > limitAge) value = limitAge;
        ageValue = value;
        int newAge = age();
        if (oldAge != newAge) {
            saveData();
            syncDisplayVariable();
            onAgeUpdated();
        }
    }

    protected boolean modifyDisplayVariable(Map<String, String> map) {
        map.put("age", String.valueOf(age()));
        return true;
    }

    @Override public final void syncDisplayVariable() {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> {
            display.modify(this::modifyDisplayVariable);
        });
    }
}









