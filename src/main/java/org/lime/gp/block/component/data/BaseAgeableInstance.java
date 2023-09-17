package org.lime.gp.block.component.data;

import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.display.IDisplayVariable;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.lime;
import org.lime.gp.module.RandomTickSpeed;
import org.lime.json.JsonObjectOptional;
import org.lime.system.execute.Func0;
import org.lime.system.json;

import java.util.List;
import java.util.Map;

public abstract class BaseAgeableInstance<T extends ComponentDynamic<?, ?>> extends BlockComponentInstance<T> implements CustomTileMetadata.Tickable, IDisplayVariable {
    public interface AgeableData {
        double tickAgeModify();
        int limitAge();
    }

    protected abstract String debugKey();
    protected void writeDebug(String line) { writeDebug(() -> line); }
    protected void writeDebug(List<String> lines) {
        String save = debugKey() + "/" + this.metadata().position().toSave();
        lines.forEach(line -> lime.logToFile(save, "[{time}] " + line));
    }
    protected void writeDebug(Func0<String> line) {
        String save = debugKey() + "/" + this.metadata().position().toSave();
        lime.logToFile(save, "[{time}] " + line.invoke());
    }

    private double ageValue = 0;

    public BaseAgeableInstance(T component, CustomTileMetadata metadata) {
        super(component, metadata);
        writeDebug("ctor");
    }

    public int age() {
        return (int)ageValue;
    }
    public void age(int value) {
        writeDebug("Change age: " + ageValue + " -> " + value);
        ageValue = value;
        saveData();
        syncDisplayVariable();
        onAgeUpdated();
    }
    public abstract AgeableData ageableData();
    protected void onAgeUpdated() { }
    @Override public void read(JsonObjectOptional json) {
        writeDebug("Read: " + json);
        age(json.getAsInt("age").orElse(0));
    }

    @Override public json.builder.object write() {
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
            writeDebug("Change age: " + oldAge + " -> " + newAge);
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
        writeDebug("syncDisplayVariable");
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> {
            display.modify(this::modifyDisplayVariable);
        });
    }
}









