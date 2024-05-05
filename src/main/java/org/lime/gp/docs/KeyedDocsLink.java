package org.lime.gp.docs;

import org.lime.display.models.shadow.serialize.IDocsTypes;
import org.lime.docs.IIndexDocs;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.system.execute.Func0;
import org.lime.system.execute.Func1;

public abstract class KeyedDocsLink implements IDocsLink {
    private final IDocsLink base;
    public KeyedDocsLink(IDocsLink base) {
        this.base = base;
    }

    protected abstract IIndexDocs map(String key, Func0<IIndexDocs> builder);
    protected abstract <T>IIndexDocs map(String key, T arg, Func1<T, IIndexDocs> builder);
    protected abstract IDocsTypes mapTypes(Func0<IDocsTypes> builder);

    @Override public IDocsTypes builderTypes() { return mapTypes(base::builderTypes); }

    @Override public IIndexDocs vanillaMaterial() { return map("vanillaMaterial", base::vanillaMaterial); }
    @Override public IIndexDocs particleType() { return map("particleType", base::particleType); }
    @Override public IIndexDocs formatted() { return map("formatted", base::formatted); }
    @Override public IIndexDocs formattedChat() { return map("formattedChat", base::formattedChat); }
    @Override public IIndexDocs formattedText() { return map("formattedText", base::formattedText); }
    @Override public IIndexDocs formattedJson() { return map("formattedJson", base::formattedJson); }
    @Override public IIndexDocs potionEffect() { return map("potionEffect", base::potionEffect); }
    @Override public IIndexDocs itemFlag() { return map("itemFlag", base::itemFlag); }
    @Override public IIndexDocs enchantment() { return map("enchantment", base::enchantment); }
    @Override public IIndexDocs giveItem() { return map("giveItem", base::giveItem); }
    @Override public IIndexDocs regexItem() { return map("regexItem", base::regexItem); }
    @Override public IIndexDocs parseItem() { return map("parseItem", base::parseItem); }
    @Override public IIndexDocs setBlock() { return map("setBlock", base::setBlock); }
    @Override public IIndexDocs equipmentSlot() { return map("equipmentSlot", base::equipmentSlot); }
    @Override public IIndexDocs attribute() { return map("attribute", base::attribute); }
    @Override public IIndexDocs menuName() { return map("menuName", base::menuName); }
    @Override public IIndexDocs vanillaSound() { return map("vanillaSound", base::vanillaSound); }
    @Override public IIndexDocs instrument() { return map("instrument", base::instrument); }
    @Override public IIndexDocs rotation() { return map("rotation", base::rotation); }
    @Override public IIndexDocs soundMaterial() { return map("soundMaterial", base::soundMaterial); }
    @Override public IIndexDocs sound() { return map("sound", base::sound); }
    @Override public IIndexDocs loot() { return map("loot", base::loot); }
    @Override public IIndexDocs range() { return map("range", base::range); }
    @Override public IIndexDocs elementalName() { return map("elemental", base::elementalName); }
    @Override public IIndexDocs vector() { return map("vector", base::vector); }
    @Override public IIndexDocs location() { return map("location", base::location); }
    @Override public IIndexDocs quaternion() { return map("quaternion", base::quaternion); }
    @Override public IIndexDocs transform() { return map("transform", base::transform); }
    @Override public IIndexDocs vectorInt() { return map("vectorInt", base::vectorInt); }
    @Override public IIndexDocs need() { return map("need", base::need); }
    @Override public IIndexDocs js() { return map("js", base::js); }
    @Override public IIndexDocs json() { return map("json", base::json); }
    @Override public IIndexDocs jsonNbt() { return map("jsonNbt", base::jsonNbt); }
    @Override public IIndexDocs dynamicNbt() { return map("dynamicNbt", base::dynamicNbt); }
    @Override public IIndexDocs model() { return map("model", base::model); }
    @Override public IIndexDocs handType() { return map("handType", base::handType); }

    @Override public IIndexDocs jsonText() { return map("jsonText", base::jsonText); }
    @Override public IIndexDocs modelName() { return map("modelName", base::modelName); }

    @Override public IIndexDocs settingsLink(Class<? extends ItemSetting<?>> tClass) { return map("settingsLink", tClass, base::settingsLink); }
    @Override public IIndexDocs componentsLink(Class<? extends ComponentStatic<?>> tClass) { return map("componentsLink", tClass, base::componentsLink); }
}
