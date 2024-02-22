package org.lime.gp.player.module.needs;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import org.bukkit.potion.PotionEffect;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.Items;
import org.lime.system.execute.Func2;

public interface INeedEffect<T extends INeedEffect<T>> {
    class Type<T extends INeedEffect<T>> {
        public static final Type<Mutate> SLEEP = new Type<>(Mutate.class, Mutate::parse);
        public static final Type<Mutate> THIRST = new Type<>(Mutate.class, Mutate::parse);
        public static final Type<Mutate> FOOD = new Type<>(Mutate.class, Mutate::parse);

        public static final Type<Effect> EFFECT = new Type<>(Effect.class, Effect::parse);

        private final Class<T> tClass;
        private final Func2<Type<T>, JsonObject, T> ctor;

        private Type(Class<T> tClass, Func2<Type<T>, JsonObject, T> ctor) {
            this.tClass = tClass;
            this.ctor = ctor;
        }

        public T create(JsonObject json) { return ctor.invoke(this, json); }

        @SuppressWarnings("unchecked")
        public T nullCast(INeedEffect<?> effect) { return tClass.isInstance(effect) ? (T)effect : null; }
    }
    interface Mutate extends INeedEffect<Mutate> {
        double value();
        static Mutate of(Type<Mutate> type, double value) {
            return new Mutate() {
                @Override public Type<Mutate> type() { return type; }
                @Override public double value() { return value; }
            };
        }

        static Mutate parse(Type<Mutate> type, JsonObject json) {
            return of(type, json.get("mutate").getAsDouble());
        }
        static IJElement docs(String type, String info) {
            return JObject.of(
                    JProperty.require(IName.raw("type"), IJElement.raw(type), IComment.text(info)),
                    JProperty.require(IName.raw("mutate"), IJElement.raw(1.2), IComment.text("Модификатор (умножение)"))
            );
        }
    }
    interface Effect extends INeedEffect<Effect> {
        int delay();
        PotionEffect effect();

        static Effect parse(Type<Effect> type, JsonObject json) {
            int delay = json.get("delay").getAsInt();
            PotionEffect effect = Items.parseEffect(json.get("effect").getAsJsonObject());
            return new Effect() {
                @Override public Type<Effect> type() { return type; }

                @Override public int delay() { return delay; }
                @Override public PotionEffect effect() { return effect; }
            };
        }
        static IJElement docs(String type, String info, IIndexDocs effect) {
            return JObject.of(
                    JProperty.require(IName.raw("type"), IJElement.raw(type), IComment.text(info)),
                    JProperty.require(IName.raw("delay"), IJElement.raw(10), IComment.text("Время в тиках, раз в которое вызывается добавление эффекта")),
                    JProperty.require(IName.raw("effect"), IJElement.link(effect), IComment.text("Эффект"))
            );
        }
    }
    Type<T> type();

    static INeedEffect<?> parse(JsonObject json) {
        return switch (json.get("type").getAsString()) {
            case "sleep" -> Type.SLEEP.create(json);
            case "thirst" -> Type.THIRST.create(json);
            case "food" -> Type.FOOD.create(json);
            case "effect" -> Type.EFFECT.create(json);
            default -> throw new IllegalArgumentException("Type '"+json.get("type").getAsString()+"' not supported");
        };
    }

    static IIndexGroup docs(String index, IDocsLink docs) {
        return JsonEnumInfo.of(index, ImmutableList.of(
                Mutate.docs("sleep", "Уменьшение времени сна"),
                Mutate.docs("thirst", "Ускорение траты жидкости"),
                Mutate.docs("food", "Ускорение траты еди"),
                Effect.docs("effect", "Выдача эффекта", docs.potionEffect())
        ));
    }
}