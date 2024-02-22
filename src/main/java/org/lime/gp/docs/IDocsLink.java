package org.lime.gp.docs;

import org.lime.display.models.shadow.serialize.IDocsTypes;
import org.lime.docs.DocsReader;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.item.settings.ItemSetting;

import java.util.stream.Stream;

public interface IDocsLink {
    static IDocsLink source(boolean cache) { return SourceDocsLink.source(cache); }
    static IDocsLink temp() { return TempDocsLink.temp(); }

    IDocsTypes builderTypes();

    IIndexDocs vanillaMaterial();

    IIndexDocs formatted();
    IIndexDocs formattedChat();
    IIndexDocs formattedText();
    IIndexDocs formattedJson();
    IIndexDocs potionEffect();
    IIndexDocs itemFlag();
    IIndexDocs enchantment();
    IIndexDocs giveItem();
    IIndexDocs regexItem();
    IIndexDocs parseItem();
    IIndexDocs setBlock();

    IIndexDocs equipmentSlot();

    IIndexDocs attribute();
    IIndexDocs menuName();

    IIndexDocs vanillaSound();

    IIndexDocs instrument();
    IIndexDocs rotation();
    IIndexDocs soundMaterial();
    IIndexDocs sound();
    IIndexDocs loot();
    IIndexDocs range();
    IIndexDocs elementalName();
    //IIndexDocs elementalStep();
    //IIndexDocs spawn();

    IIndexDocs vector();
    IIndexDocs location();
    IIndexDocs quaternion();
    IIndexDocs transform();
    IIndexDocs vectorInt();

    IIndexDocs need();

    IIndexDocs js();
    IIndexDocs json();

    IIndexDocs jsonNbt();

    IIndexDocs dynamicNbt();
    IIndexDocs model();
    IIndexDocs handType();

    IIndexDocs settingsLink(Class<? extends ItemSetting<?>> tClass);// { throw new ToDoException("SETTINGS LINK: " + tClass); } //return IIndexDocs.raw(ItemSetting.getName(tClass)); }
    IIndexDocs componentsLink(Class<? extends ComponentStatic<?>> tClass);// { throw new ToDoException("COMPONENTS LINK: " + tClass); } //return IIndexDocs.raw(ComponentStatic.getName(tClass)); }

    IIndexDocs jsonText();
    IIndexDocs modelName();

    default Stream<IIndexGroup> child() {
        return DocsReader.groups(IDocsLink.class, this, true).stream();
    }
}
