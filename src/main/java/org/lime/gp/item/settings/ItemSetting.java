package org.lime.gp.item.settings;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.docs.IGroup;
import org.lime.gp.docs.IDocsLink;
import org.lime.reflection;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.data.ItemCreator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.unsafe;

public abstract class ItemSetting<T extends JsonElement> implements IItemSetting {
    private final ItemCreator _creator;
    public ItemCreator creator() { return this._creator; }

    private String _name;
    public String name() { return _name; }
    public void apply(ItemMeta meta, Apply apply) { }
    public void appendArgs(ItemMeta meta, Apply apply) { }

    private interface SettingLink {
        ItemSetting<?> create(ItemCreator creator, JsonElement json);
        Class<?> tClass();

        String key();
        default IGroup docs(IDocsLink docs) {
            ItemSetting<?> setting = (ItemSetting<?>)unsafe.createInstance(tClass());
            return setting.docs(key(), docs);
        }
    }

    private static final Map<String, SettingLink> settings;
    private static Optional<Constructor<?>> constructor(Class<?> tClass, Class<?>... args) {
        try { return Optional.of(reflection.access(tClass.getDeclaredConstructor(args))); }
        catch (Exception e) { return Optional.empty(); }
    }
    static {
        try {
            String packageFilter = ItemSetting.class.getPackage().getName() + ".list.";
            settings = lime._plugin.getJarClassesNames()
                    .stream()
                    .filter(v -> v.startsWith(packageFilter))
                    .map(system.<String, Class<?>>funcEx(Class::forName).throwable())
                    .filter(ItemSetting.class::isAssignableFrom)
                    .flatMap(v -> constructor(v, ItemCreator.class, JsonElement.class)
                            .or(() -> constructor(v, ItemCreator.class, JsonArray.class))
                            .or(() -> constructor(v, ItemCreator.class, JsonObject.class))
                            .or(() -> constructor(v, ItemCreator.class, JsonPrimitive.class))
                            .or(() -> constructor(v, ItemCreator.class, JsonNull.class))
                            .or(() -> constructor(v, ItemCreator.class))
                            .stream()
                            .flatMap(c -> Arrays.stream(v.getAnnotationsByType(Setting.class)).map(_c -> system.toast(_c, c, v)))
                    )
                    .collect(Collectors.toMap(kv -> kv.val0.name(), kv -> new SettingLink() {
                        private final Constructor<?> constructor = kv.val1;
                        private final Class<?> tClass = kv.val2;
                        private final String key = kv.val0.name();
                        @Override public ItemSetting<?> create(ItemCreator creator, JsonElement json) {
                            try {
                                return (ItemSetting<?>)(constructor.getParameterCount() == 2 ? constructor.newInstance(creator, json) : constructor.newInstance(creator));
                            } catch (InvocationTargetException e) {
                                lime.logStackTrace(e.getCause());
                                throw new IllegalArgumentException(e.getCause());
                            } catch (Exception e) {
                                lime.logStackTrace(e);
                                throw new IllegalArgumentException(e);
                            }
                        }
                        @Override public String key() { return key; }
                        @Override public Class<?> tClass() { return tClass; }
                    }));
        } catch (Exception e) {
            lime.logStackTrace(e);
            throw e;
        }
        settings.keySet().forEach(k -> lime.logOP("Setting: " + k));
    }
    public ItemSetting(ItemCreator creator) { this._creator = creator; }
    public ItemSetting(ItemCreator creator, T json) { this(creator); }
    public static ItemSetting<?> parse(String key, ItemCreator creator, JsonElement json) {
        SettingLink link = settings.get(key);
        if (link == null) throw new IllegalArgumentException("Item setting '"+key+"' not founded!");
        ItemSetting<?> setting = link.create(creator, json);
        setting._name = key;
        return setting;
    }

    public static String getName(Class<? extends ItemSetting<?>> tClass) {
        return Arrays.stream(tClass.getAnnotationsByType(Setting.class)).map(Setting::name).findFirst().orElseThrow();
    }

    public static Stream<IGroup> allDocs(IDocsLink docs) {
        return settings.values().stream().sorted(Comparator.comparing(SettingLink::key)).map(link -> link.docs(docs));
    }
}