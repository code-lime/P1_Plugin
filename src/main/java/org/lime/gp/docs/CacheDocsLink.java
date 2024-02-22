package org.lime.gp.docs;

import org.lime.display.models.shadow.serialize.IDocsTypes;
import org.lime.docs.IIndexDocs;
import org.lime.system.execute.Func0;
import org.lime.system.execute.Func1;
import org.lime.system.toast.IToast;
import org.lime.system.toast.Toast;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheDocsLink extends KeyedDocsLink {
    public CacheDocsLink(IDocsLink base) {
        super(base);
    }

    private final ConcurrentHashMap<IToast, IIndexDocs> cache = new ConcurrentHashMap<>();

    @Override protected IIndexDocs map(String key, Func0<IIndexDocs> builder) {
        return cache.computeIfAbsent(Toast.of(key), v -> builder.invoke());
    }
    @Override protected <T> IIndexDocs map(String key, T arg, Func1<T, IIndexDocs> builder) {
        return cache.computeIfAbsent(Toast.of(key, arg), v -> builder.invoke(arg));
    }
    private @Nullable IDocsTypes cacheBuilderTypes = null;
    @Override protected IDocsTypes mapTypes(Func0<IDocsTypes> builder) {
        return cacheBuilderTypes != null ? cacheBuilderTypes : (cacheBuilderTypes = builder.invoke());
    }
}
