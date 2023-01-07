package org.lime.gp.block.component.data.cauldron;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.item.Items;
import org.lime.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LoadedStep {
    private final JsonArray items;
    public final Step.IItem iitem;

    private LoadedStep(JsonArray items, Step.IItem iitem) {
        this.iitem = iitem;
        this.items = items;
    }
    public LoadedStep(Step.IItem iitem, ItemStack item) {
        this(new JsonArray(), iitem);
        addItem(item);
    }

    public boolean compare(Step step) { return step.item.compare(iitem) && step.count == getAmount(); }
    public int getAmount() { return getItems().stream().mapToInt(ItemStack::getAmount).sum(); }
    public static LoadedStep parse(JsonObject json) { return new LoadedStep(json.getAsJsonArray("items"), Step.IItem.parse(json.get("format"))); }
    public JsonObject toJson() { return system.json.object().add("items", items).add("format", iitem.toJson()).build(); }
    public void addItem(ItemStack item) { addItems(Collections.singletonList(item)); }
    public void addItems(List<ItemStack> items) { items.forEach(item -> this.items.add(system.saveItem(item))); }
    public void setItems(List<ItemStack> items) {
        system.clear(this.items);
        addItems(items);
    }
    public List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();
        this.items.forEach(item -> items.add(system.loadItem(item.getAsString())));
        return items;
    }
    @Override public String toString() { return "LSTEP:" + iitem + ":" + getItems().stream().map(ItemStack::toString).collect(Collectors.joining("&")); }
    public void drop(Location location) { Items.dropItem(location, getItems()); }
}

