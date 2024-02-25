package org.lime.gp.player.module.needs;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.Items;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.system.execute.Func2;

import javax.annotation.Nullable;
import java.util.Arrays;

public interface INeedEffect<T extends INeedEffect<T>> {
    class Type<T extends INeedEffect<T>> {
        public static final Type<Mutate> SLEEP = new Type<>(Mutate.class, Mutate::parse);
        public static final Type<Mutate> THIRST = new Type<>(Mutate.class, Mutate::parse);
        public static final Type<Mutate> FOOD = new Type<>(Mutate.class, Mutate::parse);

        public static final Type<Effect> EFFECT = new Type<>(Effect.class, Effect::parse);
        public static final Type<Text> TEXT = new Type<>(Text.class, Text::parse);

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

        @Override default void tick(Player player) {}

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

        @Override default void tick(Player player) {
            int currentTick = MinecraftServer.currentTick;
            if (currentTick % delay() == 0)
                effect().apply(player);
        }

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
    interface Text extends INeedEffect<Text> {
        String message();
        @Nullable Integer size();
        int offset();
        boolean shadow();
        @Nullable TextColor color();

        CustomUI.IType show();

        @Override default void tick(Player player) {
            String message = message();
            Integer size = size();
            if (size == null) size = ChatHelper.getTextSize(player, message);
            ImageBuilder image = ImageBuilder.of(message, size, shadow()).withOffset(offset());
            TextColor color = color();
            if (color != null) image = image.withColor(color);

            switch (show()) {
                case ACTIONBAR -> CustomUI.TextUI.show(player, image);
                case TITLE -> CustomUI.TitleUI.show(player, image);
                case BOSSBAR -> CustomUI.BossBarUI.show(player, image);
            }
        }

        static Text parse(Type<Text> type, JsonObject json) {
            String message = json.get("message").getAsString();
            Integer size = json.has("size") ? (Integer)json.get("size").getAsInt() : null;
            int offset = json.has("offset") ? json.get("offset").getAsInt() : 0;
            boolean shadow = !json.has("shadow") || json.get("shadow").getAsBoolean();
            TextColor color = json.has("color") ? TextColor.fromHexString(json.get("color").getAsString()) : null;
            CustomUI.IType show = CustomUI.IType.valueOf(json.get("show").getAsString());

            return new Text() {
                @Override public Type<Text> type() { return Type.TEXT; }
                @Override public String message() { return message; }
                @Override public @Nullable Integer size() { return size; }
                @Override public int offset() { return offset; }
                @Override public boolean shadow() { return shadow; }
                @Override public @Nullable TextColor color() { return color; }
                @Override public CustomUI.IType show() { return show; }
            };
        }
        static IJElement docs(String type, String info) {
            return JObject.of(
                    JProperty.require(IName.raw("type"), IJElement.raw(type), IComment.text(info)),
                    JProperty.require(IName.raw("message"), IJElement.text("MESSAGE"), IComment.text("Сообщение, которе будет отображено")),
                    JProperty.optional(IName.raw("size"), IJElement.raw(16), IComment.text("Ширина сообщения в пикселях (для отображения изображений)")),
                    JProperty.optional(IName.raw("offset"), IJElement.raw(16), IComment.text("Сдвиг сообщения по горизонтали в пикселях")),
                    JProperty.optional(IName.raw("shadow"), IJElement.bool(), IComment.text("Отображение тени сообщения")),
                    JProperty.optional(IName.raw("color"), IJElement.raw("#FFFFFF"), IComment.text("Hex цвет сообщения")),
                    JProperty.require(IName.raw("show"), IJElement.or(Arrays.stream(CustomUI.IType.values()).map(Enum::name).map(IJElement::text).toList()), IComment.text("Место отображения сообщения"))
            );
        }
    }
    Type<T> type();
    void tick(Player player);

    static INeedEffect<?> parse(JsonObject json) {
        return switch (json.get("type").getAsString()) {
            case "sleep" -> Type.SLEEP.create(json);
            case "thirst" -> Type.THIRST.create(json);
            case "food" -> Type.FOOD.create(json);
            case "effect" -> Type.EFFECT.create(json);
            case "text" -> Type.TEXT.create(json);
            default -> throw new IllegalArgumentException("Type '"+json.get("type").getAsString()+"' not supported");
        };
    }

    static IIndexGroup docs(String index, IDocsLink docs) {
        return JsonEnumInfo.of(index, ImmutableList.of(
                Mutate.docs("sleep", "Уменьшение времени сна"),
                Mutate.docs("thirst", "Ускорение траты жидкости"),
                Mutate.docs("food", "Ускорение траты еди"),
                Effect.docs("effect", "Выдача эффекта", docs.potionEffect()),
                Text.docs("text", "Отображение текста")
        ));
    }
}