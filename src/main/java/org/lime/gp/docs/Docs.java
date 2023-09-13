package org.lime.gp.docs;

import org.lime.docs.DocsRoot;
import org.lime.docs.IDocs;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;

import java.util.stream.Collectors;

public class Docs {
    public static CoreElement create() {
        return CoreElement.create(Docs.class)
                .disable()
                .withInit(Docs::init);
    }

    private static void init() {
        IDocsLink link = IDocsLink.test();
        IIndexGroup setting = IIndexGroup.empty("Setting", "setting", null);
        DocsRoot items = DocsRoot.of(
                IDocs.style(),
                ItemCreator.docs("Базовое представление", link),
                setting.withChilds(ItemSetting.allDocs(link).toList())
        );
        IIndexGroup component = IIndexGroup.empty("Component", "component", null);
        DocsRoot blocks = DocsRoot.of(
                IDocs.style(),
                BlockInfo.docs("Базовое представление", link),
                component.withChilds(ComponentStatic.allDocs(link).toList())
        );
        lime.writeAllConfig("items", ".md", items.lines().collect(Collectors.joining("\n")));
    }
}
