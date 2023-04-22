package org.lime.gp.item.data;

import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;

public class Builder {
    private final IItemCreator creator;
    private Apply apply = Apply.of();
    private int count = 1;
    protected Builder(IItemCreator creator) {
        this.creator = creator;
    }
    public Builder addApply(Apply apply) {
        this.apply = this.apply.copy().join(apply);
        return this;
    }
    public Builder setCount(int count) {
        this.count = count;
        return this;
    }
    protected ItemStack create() {
        return creator.createItem(count, apply);
    }
}