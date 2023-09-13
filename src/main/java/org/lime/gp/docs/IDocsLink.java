package org.lime.gp.docs;

import org.lime.ToDoException;
import org.lime.docs.IIndexDocs;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.item.settings.ItemSetting;

public interface IDocsLink {
    IIndexDocs vanillaMaterial();
    IIndexDocs formattedChat();
    IIndexDocs formattedText();
    IIndexDocs formattedJson();
    IIndexDocs potionEffect();
    IIndexDocs itemFlag();
    IIndexDocs enchantment();
    IIndexDocs regexItem();
    IIndexDocs parseItem();
    IIndexDocs setBlock();
    IIndexDocs attribute();
    IIndexDocs menu();
    IIndexDocs setting();
    IIndexDocs component();
    IIndexDocs instrument();
    IIndexDocs rotation();
    IIndexDocs blockVariables();
    IIndexDocs bulletAction();
    IIndexDocs soundMaterial();
    IIndexDocs sound();
    IIndexDocs loot();
    IIndexDocs range();
    IIndexDocs elemental();
    IIndexDocs vector();
    IIndexDocs js();
    IIndexDocs json();
    IIndexDocs dynamicNbt();
    IIndexDocs model();
    IIndexDocs location();
    /*
                JProperty.require(IName.raw("arm"), IJElement.link(docs.handType()), IComment.empty()
                        .append(IComment.text("Тип руки, в которой происходит взаимодействие. Возможные значения: "))
                        .append(IComment.or(
                                IComment.raw(EquipmentSlot.HAND),
                                IComment.raw(EquipmentSlot.OFF_HAND)
                        ))),
    */
    IIndexDocs handType();
    IIndexDocs transform();

    default IIndexDocs settingsLink(Class<? extends ItemSetting<?>> tClass) { throw new ToDoException("SETTINGS LINK: " + tClass); } //return IIndexDocs.raw(ItemSetting.getName(tClass)); }
    default IIndexDocs componentsLink(Class<? extends ComponentStatic<?>> tClass) { throw new ToDoException("COMPONENTS LINK: " + tClass); } //return IIndexDocs.raw(ComponentStatic.getName(tClass)); }

    IIndexDocs menuInsert();
    IIndexDocs menuBase();

    static IDocsLink test() {
        return new IDocsLink() {
            @Override public IIndexDocs vanillaMaterial() { return IIndexDocs.url("vanillaItem", "https://example.com"); }
            @Override public IIndexDocs formattedChat() { return IIndexDocs.url("formattedChat", "https://example.com"); }
            @Override public IIndexDocs formattedText() { return IIndexDocs.url("formattedText", "https://example.com"); }
            @Override public IIndexDocs formattedJson() { return IIndexDocs.url("formattedJson", "https://example.com"); }
            @Override public IIndexDocs potionEffect() { return IIndexDocs.url("potionEffect", "https://example.com"); }
            @Override public IIndexDocs itemFlag() { return IIndexDocs.url("itemFlag", "https://example.com"); }
            @Override public IIndexDocs enchantment() { return IIndexDocs.url("enchantment", "https://example.com"); }
            @Override public IIndexDocs regexItem() { return IIndexDocs.url("giveItem", "https://example.com"); }

            @Override
            public IIndexDocs parseItem() {
                return null;
            }

            @Override
            public IIndexDocs setBlock() {
                return null;
            }

            @Override public IIndexDocs attribute() { return IIndexDocs.url("attribute", "https://example.com"); }

            @Override
            public IIndexDocs menu() {
                return null;
            }

            @Override public IIndexDocs setting() { return IIndexDocs.url("setting", "https://example.com"); }

            @Override
            public IIndexDocs component() {
                return null;
            }

            @Override public IIndexDocs instrument() { return IIndexDocs.url("instrument", "https://example.com"); }

            @Override
            public IIndexDocs rotation() {
                return null;
            }

            @Override
            public IIndexDocs blockVariables() {
                return null;
            }

            @Override public IIndexDocs bulletAction() { return IIndexDocs.url("bulletAction", "https://example.com"); }
            @Override public IIndexDocs soundMaterial() { return IIndexDocs.url("soundMaterial", "https://example.com"); }
            @Override public IIndexDocs sound() { return IIndexDocs.url("sound", "https://example.com"); }
            @Override public IIndexDocs loot() { return IIndexDocs.url("loot", "https://example.com"); }
            @Override public IIndexDocs range() { return IIndexDocs.url("range", "https://example.com"); }
            @Override public IIndexDocs elemental() { return IIndexDocs.url("elemental", "https://example.com"); }
            @Override public IIndexDocs vector() { return IIndexDocs.url("vector", "https://example.com"); }
            @Override public IIndexDocs js() { return IIndexDocs.url("js", "https://example.com"); }

            @Override
            public IIndexDocs json() {
                return null;
            }

            @Override
            public IIndexDocs dynamicNbt() {
                return null;
            }

            @Override
            public IIndexDocs model() {
                return null;
            }

            @Override
            public IIndexDocs location() {
                return null;
            }

            @Override
            public IIndexDocs handType() {
                return null;
            }

            @Override
            public IIndexDocs transform() {
                return null;
            }

            //@Override public IIndexDocs settingsMaxStack() { return IIndexDocs.url("settingsMaxStack", "https://example.com"); }
            //@Override public IIndexDocs settingsNext() { return IIndexDocs.url("settingsNext", "https://example.com"); }
            //@Override public IIndexDocs settingsDrugs() { return IIndexDocs.url("settingsDrugs", "https://example.com"); }
            //@Override public IIndexDocs componentsLimit() { return IIndexDocs.url("componentsLimit", "https://example.com"); }
            //@Override public IIndexDocs componentsClicker() { return IIndexDocs.url("componentsClicker", "https://example.com"); }
            //@Override public IIndexDocs componentsCrops() { return IIndexDocs.url("componentsCrops", "https://example.com"); }
            //@Override public IIndexDocs componentsDecay() { return IIndexDocs.url("componentsDecay", "https://example.com"); }
            @Override public IIndexDocs menuInsert() { return IIndexDocs.url("menuInsert", "https://example.com"); }
            @Override public IIndexDocs menuBase() { return IIndexDocs.url("menuBase", "https://example.com"); }
        };
    }
}
