package org.lime.gp.docs;

import com.google.common.collect.ImmutableList;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.lime.display.ItemParser;
import org.lime.display.ext.JsonNBT;
import org.lime.display.models.shadow.serialize.IDocsTypes;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.player.module.needs.INeedEffect;
import org.lime.gp.sound.SoundMaterial;
import org.lime.system.range.IRange;
import org.lime.system.utils.MathUtils;
import org.lime.web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class SourceDocsLink implements IDocsLink {
    private final IDocsLink base;
    private final boolean cache;

    private SourceDocsLink(boolean cache) {
        this.cache = cache;
        this.base = cache ? new CacheDocsLink(this) : this;
    }
    private IDocsLink base() {
        return base;
    }
    public static IDocsLink source(boolean cache) {
        return new SourceDocsLink(cache).base();
    }

    private static final ConcurrentHashMap<Class<?>, Boolean> checked = new ConcurrentHashMap<>();
    private static IIndexGroup remoteGroup(String title, String url) {
        return IIndexGroup.raw(title, null, () -> Stream.of("Список возможных значений: <a href=\""+url+"\" target=\"_blank\">LINK</a>"));
    }
    private static IIndexGroup remoteGroup(String title, Class<?> tClass) {
        String url = "https://hub.spigotmc.org/javadocs/bukkit/" + tClass.getName().replace('.', '/') + ".html";
        if (checked.computeIfAbsent(tClass, v -> web.method.GET.create(url).none().execute().val1 == 200))
            return remoteGroup(title, url);

        throw new IllegalArgumentException("Error load remote docs from '"+tClass.getName()+"'. Url '"+url+"' is 404!");
    }

    @Override public IDocsTypes builderTypes() { return SourceDocsTypes.source(cache, base()); }

    @Override public IIndexGroup vanillaMaterial() { return remoteGroup("VANILLA_MATERIAL", Material.class); }
    @Override public IIndexDocs particleType() { return remoteGroup("PARTICLE_TYPE", Particle.class); }
    @Override public IIndexGroup formatted() {
        return IIndexGroup.raw("FORMATTED", null, () -> Stream.of(
                "Текст, в котором происходит замена `args` элементов.",
                "",
                "Пример:",
                "- `args`:",
                "  - `\"arg0\": \"123456\"`",
                "  - `\"arg1\": \"12222\"`",
                "  - `\"arg2\": \"123\"`",
                "- Текст:",
                "  - `\"Тестовый текст: {arg0}, и тому подобное как и {arg1}, так и {arg2} но не {arg3}\"`",
                "- Результат:",
                "  - `\"Тестовый текст: 123456, и тому подобное как и 12222, так и 123 но не {arg3}\"`",
                "",
                "Существует также возможность защить информации. **ОБЯЗАТЕЛЬНОЕ ИСПОЛЬЗОВАНИЕ ПРИ ИСПОЛЬЗОВАНИИ ТЕКСТА УКАЗЫВАЕМОГО ИГРОКОМ!**",
                "- `{JS:arg0}` - позволяет обезопасить передачу текста `arg0` в **JavaScript** код",
                "- `{TXT:arg0}` - позволяет обезопасить передачу текста `arg0` в чат",
                "- `{SQL:arg0}` - позволяет обезопасить передачу текста `arg0` в SQL пареметр",
                "- `{JS.SQL:arg0}` - вначале вызовет `{SQL:arg0}`, далее вызовет `{JS:out0}` по отношению к вернувшему значению",
                "",
                "Дополнительное взаимодействие",
                "- `{JS.EXE:arg0}` - вызывает JavaScript код ввиде `arg0` и возвращает строку"
        ));
    }
    @Override public IIndexGroup formattedChat() {
        return IIndexGroup.raw("FORMATTED_CHAT", null, current -> Stream.of(
                "Форматируется по правилам " + base().formatted().link(current),
                "",
                "После форматирования форматируется по правилам:",
                "- Вставка данных",
                "  - `<R>` - Символ стрелки вправо `>`",
                "  - `<L>` - Символ стрелки влево `<`",
                "  - `<RA>` - Символ фигурной скобки вправо `}`",
                "  - `<LA>` - Символ фигурной скобки влево `{`",
                "  - `<+10>` - Сдвигает следующий символ на определенное количество пикселей вправо",
                "  - `<-10>` - Сдвигает следующий символ на определенное количество пикселей влево",
                "  - `<uFFFF>` - Вставка определенного UNICODE символа по его 0xFFFF коду",
                "- Отображение",
                "  - `</>` или `<RESET>` - Сбрасывает отображение по умолчанию",
                "  - `<BOLD>` - Включает **жирное** отображение текста",
                "  - `</BOLD>` - Отключает **жирное** отображение текста",
                "  - `<ITALIC>` - Включает *курсивное* отображение текста",
                "  - `</ITALIC>` - Отключает *курсивное* отображение текста",
                "  - `<MAGIC>` - Включает `переливающееся` отображение текста",
                "  - `</MAGIC>` - Отключает `переливающееся` отображение текста",
                "  - `<UNDERLINE>` - Включает <ins>подчеркиваемое</ins> отображение текста",
                "  - `</UNDERLINE>` - Отключает <ins>подчеркиваемое</ins> отображение текста",
                "  - `<STRIKETHROUGH>` - Включает ~~зачеркиваемое~~ отображение текста",
                "  - `</STRIKETHROUGH>` - Отключает ~~зачеркиваемое~~ отображение текста",
                "  - `<FONT:value>` - Указание шрифта `value`",
                "  - `</FONT>` - Окончание шрифта",
                "  - `<INSERTION:value>` - *Что-то",
                "  - `</INSERTION>` - *Окончание что-то",
                "- Цвета",
                "  - `<#FFFFFF>` - Установка цвета в HEX-формате",
                "  - `<AQUA>` - Установка цвета по названию",
                "  - `<BLACK>` - Установка цвета по названию",
                "  - `<BLUE>` - Установка цвета по названию",
                "  - `<DARK_AQUA>` - Установка цвета по названию",
                "  - `<DARK_BLUE>` - Установка цвета по названию",
                "  - `<DARK_GRAY>` - Установка цвета по названию",
                "  - `<DARK_GREEN>` - Установка цвета по названию",
                "  - `<DARK_PURPLE>` - Установка цвета по названию",
                "  - `<DARK_RED>` - Установка цвета по названию",
                "  - `<GOLD>` - Установка цвета по названию",
                "  - `<GRAY>` - Установка цвета по названию",
                "  - `<GREEN>` - Установка цвета по названию",
                "  - `<LIGHT_PURPLE>` - Установка цвета по названию",
                "  - `<RED>` - Установка цвета по названию",
                "  - `<WHITE>` - Установка цвета по названию",
                "  - `<YELLOW>` - Установка цвета по названию",
                "- Динамический текст",
                "  - `<?translate.key>` - Отображение перевода `translate.key` в зависимости от ресурспака и языка на стороне клиента",
                "  - `<@selector>` - Отображение определенного энтити по `selector`. *Пример: <@@p> - отобразит текущего игрока*",
                "  - `<$key.name>` - Отображение определенной клавиши `key.name`. *Пример: <$key.jump> - отобразит пробел*",
                "  - `<'SIZE:OFFSET:TEXT>` - Отображение определенного текста с указанием его длины в пикселях (`SIZE` - число) и сдвига в пикселях (`OFFSET` - число)",
                "  - `<L10>` - Отображение цельной горизонтальной линии определенной длины в пикселях",
                "  - `<%objective.name>` - Отображение числового значения из `scoreboard`",
                "  - `<JS:value>` - Вызов `value` как **JavaScript** код и возвращает строку. Возвращенная строка форматируется как " + current.link(current),
                "  - `<NICK:value>`- Отображает USER_NAME игрока по его uuid. `value` - это UUID",
                "- Взаимодействие с текстом",
                "  - `<HOVER_ITEM:value>` - *Не используется",
                "  - `<HOVER_ENTITY:value>` - *Не используется",
                "  - `<HOVER_TEXT:value>` - Отображает блок текста при наведении. `value` - это " + current.link(current),
                "  - `</HOVER>` - Окончание отображения при наведении",
                "  - `<CLICK_URL:value>` - Взаимодействие при нажатии - предложение на переход по ссылке `value`",
                "  - `<CLICK_FILE:value>` - Взаимодействие при нажатии - предложение на открытие файла `value`",
                "  - `<CLICK_EXECUTE:value>` - Взаимодействие при нажатии - выполнение команды `value` со стороны клиента",
                "  - `<CLICK_SUGGEST:value>` - Взаимодействие при нажатии - подставка текста `value` в поле ввода команды",
                "  - `<CLICK_PAGE:value>` - Взаимодействие при нажатии (только внутри книги) - переход на страницу `value`",
                "  - `<CLICK_COPY:value>` - Взаимодействие при нажатии - копирование текста `value`",
                "  - `</CLICK>` - Окончание взаимодействие при нажатии"
        ));
    }
    @Override public IIndexGroup formattedText() {
        return IIndexGroup.raw("FORMATTED_TEXT", null, current -> Stream.of(
                "Форматируется по правилам " + base().formatted().link(current),
                "",
                "После форматирования проверяется на начало с текста `\"!js \"` и если обнаруживает данный текст вызывается как **JavaScript** код и возвращает строку"
        ));
    }
    @Override public IIndexGroup formattedJson() {
        return JsonEnumInfo.of("FORMATTED_JSON")
                .add(IJElement.link(base().formattedText()))
                .add(IJElement.anyList(IJElement.linkCurrent()))
                .add(IJElement.anyObject(JProperty.require(IName.link(base().formattedText()), IJElement.linkCurrent())));
    }
    @Override public IIndexGroup potionEffect() {
        return JsonGroup.of("POTION_EFFECT", JObject.of(
                JProperty.require(IName.raw("type"), IJElement.raw("slow_falling"), IComment.text("Название эффекта из ").append(IComment.field("minecraft:slow_falling"))),
                JProperty.require(IName.raw("duration"), IJElement.raw(5), IComment.text("Время действия эффекта в тиках")),
                JProperty.require(IName.raw("amplifier"), IJElement.raw(0), IComment.text("Уровень эффекта")),
                JProperty.optional(IName.raw("ambient"), IJElement.bool(), IComment.text("Увеличить количество частиц")),
                JProperty.optional(IName.raw("icon"), IJElement.bool(), IComment.text("Отображать иконку справа сверху (на экране игрока)")),
                JProperty.optional(IName.raw("particles"), IJElement.bool(), IComment.text("Отображать частицы"))
        ));
    }
    @Override public IIndexGroup itemFlag() { return remoteGroup("ITEM_FLAG", ItemFlag.class); }
    @Override public IIndexGroup enchantment() { return remoteGroup("ENCHANTMENT", Enchantment.class); }
    @Override public IIndexGroup giveItem() {
        return JsonEnumInfo.of("GIVE_ITEM")
                .add(IJElement.text("Minecraft.").join(IJElement.link(base().vanillaMaterial())))
                .add(IJElement.text("Название предмета из <b>items.json</b>"));
    }
    @Override public IIndexGroup regexItem() {
        return JsonGroup.of("REGEX_ITEM", IJElement.link(base().giveItem()), IComment.text("Является `regex` выражением"));
    }
    @Override public IIndexGroup parseItem() {
        return ItemParser.docs("RAW_ITEM", "raw_item", base().vanillaMaterial());
    }
    @Override public IIndexGroup setBlock() {
        return JsonEnumInfo.of("SET_BLOCK")
                .add(IJElement.text("Minecraft.").join(IJElement.link(base().vanillaMaterial())))
                .add(IJElement.text("Название блока из <b>blocks.json</b>"));
    }
    @Override public IIndexGroup equipmentSlot() {
        return remoteGroup("EQUIPMENT_SLOT", EquipmentSlot.class);
    }
    @Override public IIndexGroup attribute() {
        IIndexGroup attributeName = JsonEnumInfo.of("ATTRIBUTE_NAME", ItemCreator.ATTRIBUTE_NAMES.keySet().stream().map(IJElement::raw).collect(ImmutableList.toImmutableList()));
        IIndexGroup attributeOperator = JsonEnumInfo.of("ATTRIBUTE_OPERATOR")
                .add(IJElement.raw("+"), IComment.text("Добавляет указанное значение к базовому значению"))
                .add(IJElement.raw("-"), IComment.text("Вычитает указанное значение к базовому значению"))
                .add(IJElement.raw("*"), IComment.text("Добавляет указанное скалярное значение суммы к базовому значению"))
                .add(IJElement.raw("x"), IComment.text("Умножает сумму на это значение, прибавив к нему 1"));
        IIndexDocs equipmentSlot = base().equipmentSlot();
        return JsonGroup.of("ATTRIBUTE", IJElement.concat(
                "",
                IJElement.link(attributeName),
                IJElement.text(":"),
                IJElement.link(equipmentSlot),
                IJElement.text(","),
                IJElement.link(equipmentSlot),
                IJElement.any(),
                IJElement.link(equipmentSlot),
                IJElement.text(":"),
                IJElement.link(attributeOperator),
                IJElement.raw(1.0)
        )).withChilds(attributeName, attributeOperator);
    }
    @Override public IIndexGroup menuName() {
        return JsonGroup.of("MENU_NAME", IJElement.text("Название меню из <b>menu.json</b>"));
    }
    @Override public IIndexGroup modelName() {
        return JsonGroup.of("MODEL_NAME", IJElement.text("Название модели из <b>models.json</b>"));
    }
    @Override public IIndexGroup vanillaSound() {
        return JsonGroup.of("VANILLA_SOUND", IJElement.text("Внутриигровой звук (ванильный или из ресурспака)"));
    }
    @Override public IIndexGroup instrument() {
        return JsonGroup.of("INSTRUMENT", ItemCreator.Instrument.docs(base().vanillaSound()));
    }
    @Override public IIndexGroup rotation() {
        return JsonEnumInfo.of("ROTATION", InfoComponent.Rotation.Value.class, IComment.text("Значение угла поворота"));
    }
    @Override public IIndexGroup soundMaterial() {
        return JsonEnumInfo.of("SOUND_MATERIAL", SoundMaterial.class);
    }
    @Override public IIndexGroup sound() {
        return JsonGroup.of("SOUND", IJElement.text("Название звука из <b>sounds.json</b>"));
    }
    @Override public IIndexGroup range() {
        return IRange.docs("RANGE");
    }
    @Override public IIndexGroup elementalName() {
        return JsonGroup.of("ELEMENTAL_NAME", IJElement.text("Название элементаля из <b>elemental.json</b>"));
    }
    @Override public IIndexGroup vector() {
        return MathUtils.docsVector("VECTOR");
    }
    @Override public IIndexGroup location() {
        return MathUtils.docsLocation("LOCATION", base().vector());
    }
    @Override public IIndexGroup quaternion() {
        return MathUtils.docsQuaternion("QUATERNION", base().vector());
    }
    @Override public IIndexGroup transform() {
        return MathUtils.docsTransformation("TRANSFORMATION", base().vector(), base().quaternion(), base().location());
    }
    @Override public IIndexGroup vectorInt() {
        return MathUtils.docsVectorInt("VECTOR_INT");
    }
    @Override public IIndexGroup need() {
        return INeedEffect.docs("NEED", base());
    }
    @Override public IIndexGroup js() {
        return JsonEnumInfo.of("JAVASCRIPT")
                .add(IJElement.raw("JS CODE"), IComment.text("Код на языке JavaScript, который будет прочитан и выполнен"))
                .add(IJElement.join(
                        IJElement.text("FUNCTION "),
                        IJElement.field("NAME"),
                        IJElement.text("("),
                        IJElement.field("ARG"),
                        IJElement.text(","),
                        IJElement.field("ARG"),
                        IJElement.text(","),
                        IJElement.any(),
                        IJElement.text(","),
                        IJElement.field("ARG"),
                        IJElement.text(")")
                ), IComment.join(
                        IComment.text("Вызов уже существующего метода "),
                        IComment.field("NAME"),
                        IComment.text(" с набором аргументов "),
                        IComment.field("ARG"),
                        IComment.text(". "),
                        IComment.warning("ВНИМАНИЕ! Данный способ вызова JavaScript является самым быстрым")
                ));
    }
    @Override public IIndexGroup json() {
        return JsonEnumInfo.of("JSON")
                .add(IJElement.raw("VALUE"))
                .add(IJElement.raw(1.0))
                .add(IJElement.bool())
                .add(IJElement.nullable())
                .add(IJElement.anyList(IJElement.linkCurrent()))
                .add(IJElement.anyObject(JProperty.require(IName.raw("KEY"), IJElement.linkCurrent())));
    }
    @Override public IIndexGroup jsonNbt() {
        return JsonNBT.docsJson("NBT");
    }
    @Override public IIndexGroup dynamicNbt() {
        return JsonNBT.docsDynamic("NBT_DYNAMIC");
    }

    @Override public IIndexGroup handType() {
        return JsonEnumInfo.of("HAND_TYPE", Stream.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND), IComment.text("Тип руки"));
    }
    @Override public IIndexGroup jsonText() {
        return remoteGroup("JSON_TEXT", "https://minecraft.wiki/w/Raw_JSON_text_format");
    }

    @Override public IIndexDocs settingsLink(Class<? extends ItemSetting<?>> tClass) {
        String index = ItemSetting.docsKey(tClass);
        return IIndexDocs.raw(index, index, null);
    }
    @Override public IIndexDocs componentsLink(Class<? extends ComponentStatic<?>> tClass) {
        String index = ComponentStatic.docsKey(tClass);
        return IIndexDocs.raw(index, index, null);
    }
    @Override public IIndexDocs model() {
        return Docs.modelDocs().href();
    }
    @Override public IIndexDocs loot() {
        return Docs.lootDocs().href();
    }
}
