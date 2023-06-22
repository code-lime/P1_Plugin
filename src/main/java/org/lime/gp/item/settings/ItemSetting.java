package org.lime.gp.item.settings;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

public abstract class ItemSetting<T extends JsonElement> implements IItemSetting {
    private final ItemCreator _creator;
    public ItemCreator creator() { return this._creator; }

    private String _name;
    public String name() { return _name; }
    public system.Toast2<ItemStack, Boolean> replace(ItemStack item) { return system.toast(item, false); }
    public void apply(ItemStack item, ItemMeta meta, Apply apply) { }
    public void appendArgs(ItemStack item, Apply apply) { }

    private static final Map<String, system.Func2<ItemCreator, JsonElement, ItemSetting<?>>> settings;
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
                            .flatMap(c -> Arrays.stream(v.getAnnotationsByType(Setting.class)).map(_c -> system.toast(_c, c)))
                    )
                    .collect(Collectors.toMap(kv -> kv.val0.name(), kv -> (system.Func2<ItemCreator, JsonElement, ItemSetting<?>>) (creator, json) -> {
                        try {
                            return (ItemSetting<?>)(kv.val1.getParameterCount() == 2 ? kv.val1.newInstance(creator, json) : kv.val1.newInstance(creator));
                        }
                        catch (InvocationTargetException e) {
                            lime.logStackTrace(e.getCause());
                            throw new IllegalArgumentException(e.getCause());
                        }
                        catch (Exception e) {
                            lime.logStackTrace(e);
                            throw new IllegalArgumentException(e);
                        }
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
        system.Func2<ItemCreator, JsonElement, ItemSetting<?>> func = settings.get(key);
        if (func == null) throw new IllegalArgumentException("Item setting '"+key+"' not founded!");
        ItemSetting<?> setting = func.invoke(creator, json);
        setting._name = key;
        return setting;
    }
}