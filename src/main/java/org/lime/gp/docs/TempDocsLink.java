package org.lime.gp.docs;

import org.lime.display.models.shadow.serialize.IDocsTypes;
import org.lime.docs.IIndexDocs;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.item.settings.ItemSetting;

public final class TempDocsLink implements IDocsLink {
    private final IDocsTypes builderTypes;
    private final IIndexDocs temp;
    private TempDocsLink(IIndexDocs temp) {
        this.temp = temp;
        this.builderTypes = new TempDocsTypes(temp);
    }
    public static IDocsLink temp() {
        return new TempDocsLink(IIndexDocs.raw("TEMP", "TEMP", null));
    }
    @Override public IDocsTypes builderTypes() { return this.builderTypes; }

    @Override public IIndexDocs vanillaMaterial() { return this.temp; }
    @Override public IIndexDocs particleType() { return this.temp; }
    @Override public IIndexDocs formatted() { return this.temp; }
    @Override public IIndexDocs formattedChat() { return this.temp; }
    @Override public IIndexDocs formattedText() { return this.temp; }
    @Override public IIndexDocs formattedJson() { return this.temp; }
    @Override public IIndexDocs potionEffect() { return this.temp; }
    @Override public IIndexDocs itemFlag() { return this.temp; }
    @Override public IIndexDocs enchantment() { return this.temp; }
    @Override public IIndexDocs giveItem() { return this.temp; }
    @Override public IIndexDocs regexItem() { return this.temp; }
    @Override public IIndexDocs parseItem() { return this.temp; }
    @Override public IIndexDocs setBlock() { return this.temp; }
    @Override public IIndexDocs equipmentSlot() { return this.temp; }
    @Override public IIndexDocs attribute() { return this.temp; }
    @Override public IIndexDocs menuName() { return this.temp; }
    @Override public IIndexDocs vanillaSound() { return this.temp; }
    @Override public IIndexDocs instrument() { return this.temp; }
    @Override public IIndexDocs rotation() { return this.temp; }
    @Override public IIndexDocs soundMaterial() { return this.temp; }
    @Override public IIndexDocs sound() { return this.temp; }
    @Override public IIndexDocs loot() { return this.temp; }
    @Override public IIndexDocs range() { return this.temp; }
    @Override public IIndexDocs elementalName() { return this.temp; }
    @Override public IIndexDocs vector() { return this.temp; }
    @Override public IIndexDocs location() { return this.temp; }
    @Override public IIndexDocs quaternion() { return this.temp; }
    @Override public IIndexDocs transform() { return this.temp; }
    @Override public IIndexDocs vectorInt() { return this.temp; }
    @Override public IIndexDocs need() { return this.temp; }
    @Override public IIndexDocs js() { return this.temp; }
    @Override public IIndexDocs json() { return this.temp; }
    @Override public IIndexDocs jsonNbt() { return this.temp; }
    @Override public IIndexDocs dynamicNbt() { return this.temp; }
    @Override public IIndexDocs model() { return this.temp; }
    @Override public IIndexDocs handType() { return this.temp; }
    @Override public IIndexDocs jsonText() { return this.temp; }
    @Override public IIndexDocs modelName() { return this.temp; }

    @Override public IIndexDocs settingsLink(Class<? extends ItemSetting<?>> tClass) { return this.temp; }
    @Override public IIndexDocs componentsLink(Class<? extends ComponentStatic<?>> tClass) { return this.temp; }
}
