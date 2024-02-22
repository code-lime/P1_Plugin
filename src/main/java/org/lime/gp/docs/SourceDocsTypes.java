package org.lime.gp.docs;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemDisplayContext;
import org.lime.display.models.ExecutorJavaScript;
import org.lime.display.models.SmoothMove;
import org.lime.display.models.shadow.serialize.IDocsTypes;
import org.lime.display.models.shadow.serialize.SerializeUtils;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonEnumInfo;

import java.util.Arrays;

public class SourceDocsTypes implements IDocsTypes {
    private final IDocsLink docs;
    private SourceDocsTypes(IDocsLink docs) {
        this.docs = docs;
    }

    public static IDocsTypes source(boolean cache, IDocsLink base) {
        IDocsTypes result = new SourceDocsTypes(base);
        return cache ? new CacheDocsTypes(result) : result;
    }

    @Override public IIndexDocs vector() { return docs.vector().href(); }
    @Override public IIndexDocs transformation() { return docs.transform().href(); }
    @Override public IIndexDocs material() { return docs.vanillaMaterial().href(); }
    @Override public IIndexDocs nbt() { return docs.jsonNbt().href(); }
    @Override public IIndexDocs item() { return docs.parseItem().href(); }
    @Override public IIndexDocs text() { return docs.jsonText().href(); }
    @Override public IIndexDocs modelKey() { return docs.modelName().href(); }

    @Override public IIndexGroup smoothMove() { return JsonEnumInfo.of("SMOOTH_MOVE", SmoothMove.class); }
    @Override public IIndexGroup billboard() { return IDocsTypes.createNameable(Display.BillboardConstraints.class); }
    @Override public IIndexGroup color() { return SerializeUtils.docsColor(); }
    @Override public IIndexGroup animation() {
        return ExecutorJavaScript.docs("ANIMATION_APPLY", "animation_apply", docs.js(), docs.json(), null);
    }
    @Override public IIndexGroup entity() {
        return JsonEnumInfo.of("ENTITY_TYPE", BuiltInRegistries.ENTITY_TYPE.keySet()
                .stream()
                .map(v -> IJElement.raw(v.toString()))
                .collect(ImmutableList.toImmutableList()));
    }
    @Override public IIndexGroup itemDisplay() { return IDocsTypes.createNameable(ItemDisplayContext.class); }
    @Override public IIndexGroup itemSlot() {
        return JsonEnumInfo.of("ITEM_SLOT", Arrays.stream(EnumItemSlot.values())
                .map(EnumItemSlot::getName)
                .map(IJElement::raw)
                .collect(ImmutableList.toImmutableList()));
    }
    @Override public IIndexGroup align() { return IDocsTypes.createNameable(Display.TextDisplay.Align.class); }
}
