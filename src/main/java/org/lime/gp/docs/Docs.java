package org.lime.gp.docs;

import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.path.DocsFolder;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.item.loot.ILoot;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.lime;
import org.lime.gp.player.menu.page.Menu;
import org.lime.plugin.CoreElement;

import java.util.Comparator;

public class Docs {
    public static final IDocsLink link = IDocsLink.source(true);

    public static CoreElement create() {
        return CoreElement.create(Docs.class)
                //.disable()
                .withInit(Docs::init);
    }

    private static IIndexGroup model;
    private static IIndexGroup loot;

    public static IIndexDocs modelDocs() { return model; }
    public static IIndexDocs lootDocs() { return loot; }

    private static void init() {
        model = lime.models.builder().docs("MODEL");
        loot = ILoot.docs("LOOT", link);

        DocsFolder root = DocsFolder.root("docs")
                .file("base", v -> v
                        .add(IIndexGroup.empty("Базовые элементы", "base", null)
                                .withChilds(link.child().sorted(Comparator.comparing(IIndexDocs::index)).toList())))
                .file("model", _v -> _v
                        .add(IIndexGroup.empty("Модель", "model", null)
                                .withChilds(model)))
                .file("loot", _v -> _v
                        .add(IIndexGroup.empty("Генератор лута", "loot", null)
                                .withChilds(loot)))
                .file("settings", _v -> _v
                        .add(IIndexGroup.empty("Настройки предметов", "settings", null)
                                .withChilds(ItemSetting.allDocs(link).sorted(Comparator.comparing(IIndexDocs::index)).toList())))
                .file("components", _v -> _v
                        .add(IIndexGroup.empty("Настройки блоков", "components", null)
                                .withChilds(ComponentStatic.allDocs(link).sorted(Comparator.comparing(IIndexDocs::index)).toList())))
        ;

        if (root.save(lime.getConfigFile("").toPath())) {
            lime.logOP("[DOCS] Status: SAVED");
        } else {
            lime.logOP("[DOCS] Status: ERROR");
        }
/*
        DocsRoot raw = DocsRoot.style()
                .add(IIndexGroup.empty("Базовые элементы", "base_types", null)
                        .withChilds(list));

        lime.writeAllConfig(".docs.raw", ".md", raw.save() + "\n" + String.join("\n", new String[] {
                "-----",
                "Создано: " + Time.formatCalendar(Time.moscowNow(), true)
        }));
*/
        /*
        DocsRoot base = DocsRoot.of(
                IDocs.style(),
                IIndexGroup.empty("Base", "base", null)
                        .withChilds(),
                IIndexGroup.raw("Base", "base", null, RAW_BASE)
        );
        DocsRoot items = DocsRoot.of(
                IDocs.style(),
                IIndexGroup.empty("Setting", "setting", null)
                        .withChilds(ItemSetting.allDocs(link).toList())
        );
        DocsRoot blocks = DocsRoot.of(
                IDocs.style(),
                IIndexGroup.empty("Component", "component", null)
                        .withChilds(ComponentStatic.allDocs(link).toList())
        );
        lime.writeAllConfig("docs/items", ".md", items.lines().collect(Collectors.joining("\n")));
        lime.writeAllConfig("docs/blocks", ".md", blocks.lines().collect(Collectors.joining("\n")));
        */
    }
}
