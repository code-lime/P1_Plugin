package org.lime.gp.docs;

import org.lime.display.models.shadow.serialize.IDocsTypes;
import org.lime.docs.IIndexDocs;
import org.lime.system.execute.Func0;
import org.lime.system.execute.Func1;
import org.lime.system.toast.IToast;
import org.lime.system.toast.Toast;

import java.util.concurrent.ConcurrentHashMap;

public final class CacheDocsTypes implements IDocsTypes {
    private final IDocsTypes base;
    public CacheDocsTypes(IDocsTypes base) {
        this.base = base;
    }

    private final ConcurrentHashMap<IToast, IIndexDocs> cache = new ConcurrentHashMap<>();
    private IIndexDocs cache(String key, Func0<IIndexDocs> builder) {
        return cache.computeIfAbsent(Toast.of(key), v -> builder.invoke());
    }
    private <T>IIndexDocs cache(String key, T arg, Func1<T, IIndexDocs> builder) {
        return cache.computeIfAbsent(Toast.of(key, arg), v -> builder.invoke(arg));
    }

    @Override public IIndexDocs vector() { return cache("vector", base::vector); }
    @Override public IIndexDocs transformation() { return cache("transformation", base::transformation); }
    @Override public IIndexDocs material() { return cache("material", base::material); }
    @Override public IIndexDocs nbt() { return cache("nbt", base::nbt); }
    @Override public IIndexDocs item() { return cache("item", base::item); }
    @Override public IIndexDocs text() { return cache("text", base::text); }
    @Override public IIndexDocs modelKey() { return cache("modelKey", base::modelKey); }
    @Override public IIndexDocs smoothMove() { return cache("smoothMove", base::smoothMove); }
    @Override public IIndexDocs billboard() { return cache("billboard", base::billboard); }
    @Override public IIndexDocs color() { return cache("color", base::color); }
    @Override public IIndexDocs animation() { return cache("animation", base::animation); }
    @Override public IIndexDocs entity() { return cache("entity", base::entity); }
    @Override public IIndexDocs itemDisplay() { return cache("itemDisplay", base::itemDisplay); }
    @Override public IIndexDocs itemSlot() { return cache("itemSlot", base::itemSlot); }
    @Override public IIndexDocs align() { return cache("align", base::align); }
}
