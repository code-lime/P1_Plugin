package org.lime.gp.extension;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

public interface LimePersistentDataType<T, Z> extends PersistentDataType<T, Z> {
    LimePersistentDataType<String, java.util.UUID> UUID = LimePersistentDataType.simple(String.class, java.util.UUID.class, java.util.UUID::fromString, java.util.UUID::toString);
    LimePersistentDataType<String, JsonElement> JSON = LimePersistentDataType.simple(String.class, JsonElement.class, JsonParser::parseString, JsonElement::toString);
    LimePersistentDataType<String, JsonObject> JSON_OBJECT = JSON.cast(JsonObject.class);
    LimePersistentDataType<String, JsonArray> JSON_ARRAY = JSON.cast(JsonArray.class);

    default <OUT> LimePersistentDataType<T, OUT> map(Class<OUT> zClass, Func1<Z, OUT> parse, Func1<OUT, Z> format) {
        return map(this, zClass, parse, format);
    }

    @SuppressWarnings("unchecked")
    default <OUT extends Z> LimePersistentDataType<T, OUT> cast(Class<OUT> zClass) {
        return map(this, zClass, v -> (OUT) v, v -> v);
    }

    private static <T, Z> LimePersistentDataType<T, Z> simple(Class<T> tClass, Class<Z> zClass, Func1<T, Z> parse, Func1<Z, T> format) {
        return new LimePersistentDataType<>() {
            @Override
            public Class<T> getPrimitiveType() {
                return tClass;
            }

            @Override
            public Class<Z> getComplexType() {
                return zClass;
            }

            @Override
            public T toPrimitive(Z object, PersistentDataAdapterContext persistentDataAdapterContext) {
                return format.invoke(object);
            }

            @Override
            public Z fromPrimitive(T object, PersistentDataAdapterContext persistentDataAdapterContext) {
                return parse.invoke(object);
            }
        };
    }

    private static <T, IN, OUT> LimePersistentDataType<T, OUT> map(PersistentDataType<T, IN> other, Class<OUT> zClass, Func1<IN, OUT> parse, Func1<OUT, IN> format) {
        return new LimePersistentDataType<>() {
            @Override
            public Class<T> getPrimitiveType() {
                return other.getPrimitiveType();
            }

            @Override
            public Class<OUT> getComplexType() {
                return zClass;
            }

            @Override
            public T toPrimitive(OUT object, PersistentDataAdapterContext persistentDataAdapterContext) {
                return other.toPrimitive(format.invoke(object), persistentDataAdapterContext);
            }

            @Override
            public OUT fromPrimitive(T object, PersistentDataAdapterContext persistentDataAdapterContext) {
                return parse.invoke(other.fromPrimitive(object, persistentDataAdapterContext));
            }
        };
    }
}
