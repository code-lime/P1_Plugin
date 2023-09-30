package org.lime.gp.player.module.cinv;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.EntityHuman;
import org.lime.gp.extension.JManager;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;

public class ViewData {
    public CreatorElement groups;
    public ViewContainer container;

    private final EntityHuman human;
    private ViewData(EntityHuman human) { this.human = human; }

    private int groupIndex = 0;
    private int groupOffset = 0;
    private int itemsOffset = 0;

    public int groupIndex() { return this.groupIndex; }
    public int groupOffset() { return this.groupOffset; }
    public int itemsOffset() { return this.itemsOffset; }

    public void groupIndex(int groupIndex) {
        if (this.groupIndex == groupIndex) return;
        this.groupIndex = groupIndex;
        save(this.human);
    }
    public void groupOffset(int groupOffset) {
        if (this.groupOffset == groupOffset) return;
        this.groupOffset = groupOffset;
        save(this.human);
    }
    public void itemsOffset(int itemsOffset) {
        if (this.itemsOffset == itemsOffset) return;
        this.itemsOffset = itemsOffset;
        save(this.human);
    }

    private int itemsStepLength = 0;
    private int itemsShowLength = 0;
    private int groupsShowLength = 0;

    public void itemsOffsetMove(int delta, boolean fullPage) {
        itemsOffset += delta * (fullPage ? itemsShowLength() : itemsStepLength());
    }
    public void groupOffsetMove(int delta, boolean fullPage) {
        groupOffset += delta * (fullPage ? groupsShowLength() : 1);
    }


    public int itemsStepLength() { return itemsStepLength; }
    public int itemsShowLength() { return itemsShowLength; }
    public int groupsShowLength() { return groupsShowLength; }

    public void itemsStepLength(int value) { itemsStepLength = value; }
    public void itemsShowLength(int value) { itemsShowLength = value; }
    public void groupsShowLength(int value) { groupsShowLength = value; }

    private JsonObject save() {
        return json.object()
                .add("groupIndex", groupIndex)
                .add("groupOffset", groupOffset)
                .add("itemsOffset", itemsOffset)
                .build();
    }
    private static ViewData load(EntityHuman human, JsonObjectOptional raw) {
        ViewData data = new ViewData(human);
        raw.getAsInt("groupIndex").ifPresent(v -> data.groupIndex = v);
        raw.getAsInt("groupOffset").ifPresent(v -> data.groupOffset = v);
        raw.getAsInt("itemsOffset").ifPresent(v -> data.itemsOffset = v);
        return data;
    }


    public void save(EntityHuman human) {
        JManager.set(human.getBukkitEntity().getPersistentDataContainer(), "cinv.data", save());
    }
    public static ViewData load(EntityHuman human) {
        return ViewData.load(human, JsonObjectOptional.of(JManager.get(JsonObject.class, human.getBukkitEntity().getPersistentDataContainer(), "cinv.data", new JsonObject())));
    }
}
