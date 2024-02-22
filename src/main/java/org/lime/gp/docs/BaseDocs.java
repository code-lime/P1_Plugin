package org.lime.gp.docs;
/*
import org.lime.ToDoException;
import org.lime.display.ItemParser;
import org.lime.docs.IGroup;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.system.toast.Toast2;

import java.util.stream.Stream;

public class BaseDocs implements IDocsLink {
    private static IIndexGroup remoteGroup(String title, String url) {
        return IIndexGroup.raw(title, null, new String[]{"Список возможных значений: [LINK]("+url+")"});
    }

    private final IIndexGroup formatted = IIndexGroup.raw("FORMATTED", null, new String[] {
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
            "- `{JS.EXE:arg0}` - вызывает JavaScript код ввиде `arg0` и возвращает строку",
    });
    private final IIndexGroup formatted_js = IIndexGroup.raw("FORMATTED_JS", null, new String[]{
            "Форматируется по правилам [FORMATTED](#formatted)",
            "",
            "После форматирования вызывается как **JavaScript** код и возвращает js объект",
    });
    private final IIndexGroup formatted_text = IIndexGroup.raw("FORMATTED_TEXT", null, new String[]{
            "Форматируется по правилам [FORMATTED](#formatted)",
            "",
            "После форматирования проверяется на начало с текста `\"!js \"` и если обнаруживает данный текст вызывается как **JavaScript** код и возвращает строку",
    });
    private final IIndexGroup formatted_json = JsonGroup.of("FORMATTED_JSON", IJElement.or(
            IJElement.link(formatted_text),
            IJElement.anyList(IJElement.linkCurrent()),
            IJElement.anyObject(JProperty.require(IName.link(formatted_text), IJElement.linkCurrent()))
    ));
    private final IIndexGroup command_text = IIndexGroup.raw("COMMAND_TEXT", null, new String[]{
            "Форматируется по правилам [FORMATTED](#formatted)",
            "",
            "Если начинается с `!` то вызывает команду от *консоли*. В противном случае - от *игрока*.",
    });
    private final IIndexGroup formatted_page = IIndexGroup.raw("FORMATTED_PAGE", null, new String[]{
            "Форматируется по правилам [FORMATTED](#formatted)",
            "",
            "После форматирования если содержится символ `:` разделяется на части по этому символу. Если нет, то Часть #2 будет равна `0`",
            "- Часть #1 - открываемая страница",
            "- Часть #2 - число, содержащее `page` открываемой страницы (счет идет от `0`)",
    });
    private final IIndexGroup formatted_sql_query = IIndexGroup.raw("FORMATTED_SQL_QUERY", null, new String[]{
            "Форматируется по правилам [FORMATTED](#formatted)",
            "",
            "Итоговый результат должен являтся SQL запросом",
    });
    private final IIndexGroup formatted_sql_data = IIndexGroup.raw("FORMATTED_SQL_DATA", null, new String[]{
            "Форматируется по правилам [FORMATTED](#formatted)",
            "",
            "Возможно 2 состояния:",
            "- Текст начинающийся с `\"!sql \"` и продолжающийся как [FORMATTED_SQL_QUERY](#formatted_sql_query). Вызывает полный SQL запрос",
            "- Именная таблица, передаваемая через использование [SQL_TABLE_INFO](#sql_table_info)",
    });
    private final IIndexGroup formatted_chat = IIndexGroup.raw("FORMATTED_CHAT", null, new String[]{
            "Форматируется по правилам [FORMATTED](#formatted)",
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
            "  - `<JS:value>` - Вызов `value` как **JavaScript** код и возвращает строку. Возвращенная строка форматируется как [FORMATTED_CHAT](#formatted_chat)",
            "  - `<NICK:value>`- Отображает USER_NAME игрока по его uuid. `value` - это UUID",
            "- Взаимодействие с текстом",
            "  - `<HOVER_ITEM:value>` - *Не используется",
            "  - `<HOVER_ENTITY:value>` - *Не используется",
            "  - `<HOVER_TEXT:value>` - Отображает блок текста при наведении. `value` - это [FORMATTED_CHAT](#formatted_chat)",
            "  - `</HOVER>` - Окончание отображения при наведении",
            "  - `<CLICK_URL:value>` - Взаимодействие при нажатии - предложение на переход по ссылке `value`",
            "  - `<CLICK_FILE:value>` - Взаимодействие при нажатии - предложение на открытие файла `value`",
            "  - `<CLICK_EXECUTE:value>` - Взаимодействие при нажатии - выполнение команды `value` со стороны клиента",
            "  - `<CLICK_SUGGEST:value>` - Взаимодействие при нажатии - подставка текста `value` в поле ввода команды",
            "  - `<CLICK_PAGE:value>` - Взаимодействие при нажатии (только внутри книги) - переход на страницу `value`",
            "  - `<CLICK_COPY:value>` - Взаимодействие при нажатии - копирование текста `value`",
            "  - `</CLICK>` - Окончание взаимодействие при нажатии",
    });
    private final IIndexGroup formatted_lines_chat = JsonGroup.of("FORMATTED_LINES_CHAT", IJElement.or(
            IJElement.link(formatted_chat),
            IJElement.anyList(IJElement.link(formatted_chat))
    ));
    private final IIndexGroup click_type = IIndexGroup.raw("CLICK_TYPE", null, new String[]{
            "Указывает список вариантов действий игрока с предметом в инвертаре через разделитель <string>|</string>.",
            "",
            "Список возможных значений:",
            "- <string>ALL</string> - Любое взаимодействие с предметом",
            "- <string>CONTROL_DROP</string>",
            "- <string>CREATIVE</string>",
            "- <string>DOUBLE_CLICK</string>",
            "- <string>DROP</string>",
            "- <string>LEFT</string>",
            "- <string>MIDDLE</string>",
            "- <string>NUMBER_KEY</string>",
            "- <string>RIGHT</string>",
            "- <string>SHIFT_LEFT</string>",
            "- <string>SHIFT_RIGHT</string>",
            "- <string>SWAP_OFFHAND</string>",
            "- <string>UNKNOWN</string>",
            "- <string>WINDOW_BORDER_LEFT</string>",
            "- <string>WINDOW_BORDER_RIGHT</string>",
    });
    private final IIndexGroup vanilla_item = remoteGroup("VANILLA_ITEM", "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
    private final IIndexGroup item_flag = remoteGroup("ITEM_FLAG", "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/inventory/ItemFlag.html");
    private final IIndexGroup give_item = JsonGroup.of("GIVE_ITEM", IJElement.or(
            IJElement.link(vanilla_item),
            IJElement.text("Название предмета из <b>items.json</b>")
    ));
    private final IIndexGroup menu_name = JsonGroup.of("MENU_NAME", IJElement.text("Название меню из <b>menu.json</b>"));
    private final IIndexGroup vanilla_sound = JsonGroup.of("VANILLA_SOUND", IJElement.text("Внутриигровой звук (ванильный или из ресурспака)"));
    private final IIndexGroup instrument = JsonGroup.of("INSTRUMENT", ItemCreator.Instrument.docs(vanilla_sound));
    private final IIndexGroup slot = remoteGroup("SLOT", "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/EquipmentSlot.html");
    private final IIndexGroup regex_item = JsonGroup.of("REGEX_ITEM", IJElement.link(give_item), "Является `regex` выражением");
    private final IIndexGroup parser = ItemParser.docs("RAW_ITEM", "raw_item", vanilla_item);
    private final IIndexGroup setBlock = JsonGroup.of("SET_BLOCK", IJElement.or(
            IJElement.link(vanilla_item),
            IJElement.text("Название блока из <b>blocks.json</b>"))
    );
    private final IIndexGroup attributeName = IIndexGroup.raw("ATTRIBUTE_NAME", null, new String[]{
            "Список возможных значений:",
            "- <string>max_health</string>",
            "- <string>follow_range</string>",
            "- <string>knockback_resistance</string>",
            "- <string>movement_speed</string>",
            "- <string>flying_speed</string>",
            "- <string>attack_damage</string>",
            "- <string>attack_knockback</string>",
            "- <string>attack_speed</string>",
            "- <string>armor</string>",
            "- <string>armor_toughness</string>",
            "- <string>luck</string>",
            "- <string>jump_strength</string>",
            "- <string>spawn_reinforcements</string>",
    });
    private final IIndexGroup attributeOperator = IIndexGroup.raw("ATTRIBUTE_OPERATOR", null, new String[]{
            "Список возможных значений:",
            "- <string>+</string> <comment>//Добавляет указанное значение к базовому значению</comment>",
            "- <string>-</string> <comment>//Вычитает указанное значение к базовому значению</comment>",
            "- <string>*</string> <comment>//Добавляет указанное скалярное значение суммы к базовому значению</comment>",
            "- <string>x</string> <comment>//Умножает сумму на это значение, прибавив к нему 1</comment>",
    });
    private final IIndexGroup attribute = JsonGroup.of("ATTRIBUTE", IJElement.concat(
            "",
            IJElement.link(attributeName),
            IJElement.text(":"),
            IJElement.link(slot),
            IJElement.text(","),
            IJElement.link(slot),
            IJElement.any(),
            IJElement.link(slot),
            IJElement.text(":"),
            IJElement.link(attributeOperator),
            IJElement.raw(1.0)
    )).withChilds(attributeName, attributeOperator);
    private final IIndexGroup potion_effects = JsonGroup.of("POTION_EFFECTS", JObject.of(
            JProperty.require(IName.raw("type"), IJElement.raw("slow_falling"), IComment.text("Название эффекта из ").append(IComment.field("minecraft:slow_falling"))),
            JProperty.require(IName.raw("duration"), IJElement.raw(5), IComment.text("Время действия эффекта в тиках")),
            JProperty.require(IName.raw("amplifier"), IJElement.raw(0), IComment.text("Уровень эффекта")),
            JProperty.optional(IName.raw("ambient"), IJElement.bool(), IComment.text("Увеличить количество частиц")),
            JProperty.optional(IName.raw("icon"), IJElement.bool(), IComment.text("Отображать иконку справа сверху (на экране игрока)")),
            JProperty.optional(IName.raw("particles"), IJElement.bool(), IComment.text("Отображать частицы"))
    ));

    private final IIndexGroup action_slot = JsonGroup.of("ACTION_SLOT", JObject.of(
            JProperty.optional(IName.raw("wait"), IJElement.raw(10), IComment.text("Время в секундах. Задерживает вызывание на время")),
            JProperty.optional(IName.raw("args"), IJElement.anyObject(JProperty.require(IName.raw("KEY"), IJElement.link(formatted_text)))),
            JProperty.optional(IName.raw("command"), IJElement.anyList(IJElement.link(command_text)), IComment.text("Вызывает список команд последовательно")),
            JProperty.optional(IName.raw("close"), IJElement.bool(), IComment.empty()
                    .append(IComment.text("Закрыть ли меню после выполнения? (по умолчанию "))
                    .append(IComment.raw(false))),
            JProperty.optional(IName.raw("messages"), IJElement.anyList(IJElement.link(formatted_chat)), IComment.text("Отображает текст в чат")),
            JProperty.optional(IName.raw("logs"), IJElement.anyList(IJElement.link(formatted_chat)), IComment.text("Отображает логи в чат всем админам")),
            JProperty.optional(IName.raw("sql"), IJElement.anyList(IJElement.link(formatted_sql_query)), IComment.text("Вызывает список запросов в БД")),
            JProperty.optional(IName.raw("page"), IJElement.link(formatted_page), IComment.text("Открывает меню")),
            JProperty.optional(IName.raw("page_args"), IJElement.anyObjectWithPrefix(
                    JProperty.require(IName.raw("KEY"), IJElement.link(formatted_text)),
                    JProperty.require(IName.raw("*"), IJElement.text("REGEX"), IComment.empty()
                            .append(IComment.text("Копирует параметры из текущих "))
                            .append(IComment.field("args"))
                            .append(IComment.text(" в открываемое меню по "))
                            .append(IComment.field("REGEX")))
            ), IComment.text("Передает аргументы в открываемое меню")),
            JProperty.optional(IName.raw("actions"), IJElement.anyList(IJElement.linkCurrent()), IComment.empty()
                    .append(IComment.text("Вызывает "))
                    .append(IComment.linkCurrent())
                    .append(IComment.text(" последовательно"))),
            JProperty.optional(IName.raw("sounds"), IJElement.anyList(IJElement.link(formatted_text)), IComment.text("Проигрывает музыку из ").append(IComment.field("sounds.json")).append(IComment.text(" текущему игроку"))),
            JProperty.optional(IName.raw("global_sounds"), IJElement.anyList(IJElement.link(formatted_text)), IComment.text("Проигрывает музыку из ").append(IComment.field("sounds.json")).append(IComment.text(" всем игрокам от текущего игрока"))),
            JProperty.optional(IName.raw("online"), IJElement.bool(), IComment.empty()
                    .append(IComment.text("Разделяет вызов если игрок не в сети, при "))
                    .append(IComment.raw(true))
                    .append(IComment.text(" и если игрок не в сети вызывает "))
                    .append(IComment.field("offline_actions"))
                    .append(IComment.text(" (по умолчанию "))
                    .append(IComment.raw(false))
                    .append(IComment.text(")"))),
            JProperty.optional(IName.raw("offline_actions"), IJElement.anyList(IJElement.linkCurrent()), IComment.empty()
                    .append(IComment.text("Вызывается если "))
                    .append(IComment.field("online"))
                    .append(IComment.text(" - "))
                    .append(IComment.raw(true))
                    .append(IComment.text(" и игрок не в сети"))),
            JProperty.optional(IName.raw("check_actions"), IJElement.anyObject(
                    JProperty.require(IName.link(formatted_js), IJElement.linkCurrent())
            ), IComment.empty()
                    .append(IComment.text("Проверяет все "))
                    .append(IComment.link(formatted_js))
                    .append(IComment.text(" пока не найдет "))
                    .append(IComment.link(formatted_js))
                    .append(IComment.text(" который возвращает"))
                    .append(IComment.raw(true))
                    .append(IComment.text(" и только у него вызывает "))
                    .append(IComment.linkCurrent()))
    ));
    private final IIndexGroup enchantment = remoteGroup("ENCHANTMENT", "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html");
    private final IIndexGroup item_slot = JsonGroup.of("ITEM_SLOT", IJElement.or(
            IJElement.link(give_item),
            IJElement.text("Предмет, в полном формате как и в `items.json`, но с добавлениями:").join(JObject.of(
                    JProperty.optional(IName.raw("count"), IJElement.raw(1), IComment.text("Количество в слоте")),
                    JProperty.optional(IName.raw("isShow"), IJElement.link(formatted_js), IComment.empty()
                            .append(IComment.text("Если возвращаемое значение - "))
                            .append(IComment.raw(false))
                            .append(IComment.text(", то не отображает данный предмет"))),
                    JProperty.optional(IName.raw("isAction"), IJElement.any(), IComment.text("*Не используется")),
                    JProperty.optional(IName.raw("action"), IJElement.anyObject(
                            JProperty.optional(IName.link(click_type), IJElement.link(action_slot))
                    ), IComment.text("Действия для каждого взаимодействия со слотом"))
            ))
    ));
    private final IIndexGroup range_key = IIndexGroup.raw("RANGE_KEY", null, new String[]{
            "Состоит из списка <a href=\"#range_part\">RANGE_PART</a> разделенных символом `,`.",
            "",
            "Является списком порядковых номеров слотов",
            "#### RANGE_PART",
            "> Может являтся одним из нижеперечисленного",
            "> - Число - порядковый номер слота",
            "> - Текст в формате `FROM..TO`. *Пример: `3..6` - порядковые номера слотов `3`, `4`, `5` и `6`*",
    });
    private final IIndexGroup array_range_part = JsonGroup.of("ARRAY_RANGE_PART", IJElement.or(
            IJElement.raw(1).comment(IComment.text("Порядковый номер слота")),
            IJElement.list(
                    IJElement.text("FROM"),
                    IJElement.text("TO")
            ).comment(IComment.empty()
                    .append(IComment.text("Пример: "))
                    .append(IComment.field("[3,6]"))
                    .append(IComment.text(" - порядковые номера слотов "))
                    .append(IComment.raw(3)).append(IComment.text(", "))
                    .append(IComment.raw(4)).append(IComment.text(", "))
                    .append(IComment.raw(5)).append(IComment.text(" и "))
                    .append(IComment.raw(6)))
    ));
    private final IIndexGroup array_range_key = JsonGroup.of("ARRAY_RANGE_KEY", IJElement.anyList(IJElement.link(array_range_part)), "Является представлением, содержащее список порядковых номеров слотов")
            .withChild(array_range_part);
    private final IIndexGroup roll_slots = JsonGroup.of("ROLL_SLOTS", JObject.of(
            JProperty.optional(IName.raw(""))
    ), "Является представлением настройки запуска рулетки");

    private final IIndexGroup roll_slots = IIndexGroup.raw("ROLL_SLOTS", null, new String[]{
            "Является представлением настройки запуска рулетки",
            "<pre><code>{",
            "\t<name>\"slots\"</name>: <a href=\"#array_range_key\">ARRAY_RANGE_KEY</a>, <comment>//Список используемых слотов, в рулетке будут двигаться <b>ПОСЛЕДОВАТЕЛЬНО</b> от <i>первого</i> до <i>последнего</i> элемента</comment>",
            "\t<name>\"format\"</name>: <a href=\"#item_slot\">ITEM_SLOT</a>, <comment>//паттерн предмета, ктоторый будет отображатся в каждом слоте рулетки. В каждый слот передаются <string>args</string> из <name>\"data\"</name></comment>",
            "\t<name>\"data\"</name>: <a href=\"#roll_slots_data\">ROLL_SLOTS_DATA</a>, <comment>//<b>JavaScript</b> код возвращающий JS объект</comment>",
            "\t<name>\"?generate\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается после запуска рулетки с передачей в <string>args</string> данных из <a href=\"#roll_slots_data_element\">ROLL_SLOTS_DATA_ELEMENT</a> выпавшего элемента</comment>",
            "\t<name>\"?end\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается после окончания рулетки с передачей в <string>args</string> данных из <a href=\"#roll_slots_data_element\">ROLL_SLOTS_DATA_ELEMENT</a> выпавшего элемента, а также параметр <string>closed</string> со значением <bool>false</bool> если игрок дождался окончания рулетки, и <bool>true</bool> если игрок просто закрыл меню рулети</comment>",
            "\t<name>\"?tick\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a> <comment>//Вызывается после запуска каждый сдвиг рулетки</comment>",
            "}</code></pre>",
            "> #### ROLL_SLOTS_DATA",
            "> Набор возможных слотов данных рулетки, который может в ней выпасть",
            "> Является <a href=\"#formatted_js\">FORMATTED_JS</a> возвращающий JS объект ввиде:",
            "> <pre><code>[",
            "> \t<a href=\"#roll_slots_data_element\">ROLL_SLOTS_DATA_ELEMENT</a>,",
            "> \t<a href=\"#roll_slots_data_element\">ROLL_SLOTS_DATA_ELEMENT</a>,",
            "> \t<any>...</any>",
            "> ]</code></pre>",
            "#### ROLL_SLOTS_DATA_ELEMENT",
            "> Слот данных рулетки",
            "> Является JS объектом ввиде:",
            "> <pre><code>{",
            "> \t<name>\"scale\"</name>: <string>1</string>, <comment>//Вес слота данных рулетки. Целое число. Чем больше - тем больше шансов на выпадение</comment>",
            "> \t<name>\"KEY\"</name>: <string>\"VALUE\"</string>, <comment>//Значение, предающееся в <string>args</string> для слота данных рулетки</comment>",
            "> \t<name>\"KEY\"</name>: <string>\"VALUE\"</string>, <comment>//Значение, предающееся в <string>args</string> для слота данных рулетки</comment>",
            "> \t<any>...</any>",
            "> }</code></pre>",
            "",
    });
    private final IIndexGroup table_slots = IIndexGroup.raw("TABLE_SLOTS", null, new String[]{
            "Является отображением списка строк",
            "<pre><code>{",
            "\t<name>\"slots\"</name>: <a href=\"#array_range_key\">ARRAY_RANGE_KEY</a>, <comment>//Указываются слоты в которых будут отображатся таблица</comment>",
            "\t<name>\"format\"</name>: <a href=\"#item_slot\">ITEM_SLOT</a>, <comment>//паттерн предмета, ктоторый будет отображатся в каждом слоте таблицы. В каждый слот передаются <string>args</string> из <string>SQL-запроса</string></comment>",
            "}</code></pre>",
            "",
    });
    private final IIndexGroup invoke_action = IIndexGroup.raw("INVOKE_ACTION", null, new String[]{
            "<pre><code>{",
            "\t<name>\"?owner\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается игроку-ожидающему если он в сети</comment>",
            "\t<name>\"?other\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается отигроку-выполняющему если он в сети</comment>",
            "\t<name>\"?call\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается при любом случае всегда для игрока-ожидающему даже если он не в сети</comment>",
            "}</code></pre>",
            "",
    });
    private final IIndexGroup sql_table_info = IIndexGroup.raw("SQL_TABLE_INFO", null, new String[]{
            "Если начинается с `!` - добавление таблицы для использования",
            "",
            "Остальное - добавление первого элемента таблицы в `args` в формате `{SQL_TABLE_INFO.ROW_NAME}`",
            "",
    });
    private final IIndexGroup selector_type = IIndexGroup.raw("SELECTOR_TYPE", null, new String[]{
            "Список возможных значений:",
            "- `\"main\"` - Позволяет выделить определенный блок. При выделении передает в `output`.`args`:",
            "  - `pos` - координаты блока через пробел",
            "\t- `pos_x` - координата `x` блока",
            "\t- `pos_y` - --//--",
            "\t- `pos_z` - --//--",
            "\t- `pos_face_x` - координата `x` блока, который находится у стороны выделения блока",
            "\t- `pos_face_y` - --//--",
            "\t- `pos_face_z` - --//--",
            "\t- `face` - сторона блока выделения",
            "- `\"zone_main\"` - Позволяет выделять територию и точку выхода. При выделении передает в `output`.`args`:",
            "\t- `pos1` - координаты 1 точки через пробел",
            "\t- `pos1_x` - координату `x` 1 точки",
            "\t- `pos1_y` - --//--",
            "\t- `pos1_z` - --//--",
            "\t- `pos2` - координаты 2 точки через пробел",
            "\t- `pos2_x` - координату `x` 2 точки",
            "\t- `pos2_y` - --//--",
            "\t- `pos2_z` - --//--",
            "\t- `posMain` - координаты точки окончания выделения через пробел",
            "\t- `posMain_x` - координату `x` точки окончания выделения",
            "\t- `posMain_y` - --//--",
            "\t- `posMain_z` - --//--",
            "\t- `faceMain` - сторона блока на которой было окончание выделения",
            "- `\"zone_readonly\"` - Позволяет просматривать территорию, требуется передать `args`.`pos1` и `args`.`pos2`.",
            "",
            "<comment>P.S. Команда <name>/other.event UUID user.selector</name> удаляет текущее выделение</comment>",
            "",
    });
    private IDocsLink createDocs() {
        return new IDocsLink() {
            @Override public IIndexDocs vanillaMaterial() { return vanilla_item; }
            @Override public IIndexDocs formattedChat() { return formatted_chat; }
            @Override public IIndexDocs formattedText() { return formatted_text; }
            @Override public IIndexDocs formattedJson() { return formatted_json; }
            @Override public IIndexDocs potionEffect() { return potion_effects; }
            @Override public IIndexDocs itemFlag() { return item_flag; }
            @Override public IIndexDocs enchantment() { return enchantment; }
            @Override public IIndexDocs regexItem() { return regex_item; }
            @Override public IIndexDocs parseItem() { return parser; }
            @Override public IIndexDocs setBlock() { return setBlock; }
            @Override public IIndexDocs attribute() { return attribute; }
            @Override public IIndexDocs menuName() { return menu_name; }
            @Override public IIndexDocs instrument() { return instrument; }
            @Override public IIndexDocs rotation() { return null; }

            @Override
            public IIndexDocs blockVariables() {
                return null;
            }

            @Override
            public IIndexDocs bulletAction() {
                return null;
            }

            @Override
            public IIndexDocs soundMaterial() {
                return null;
            }

            @Override
            public IIndexDocs sound() {
                return null;
            }

            @Override
            public IIndexDocs loot() {
                return null;
            }

            @Override
            public IIndexDocs range() {
                return null;
            }

            @Override
            public IIndexDocs elemental() {
                return null;
            }

            @Override
            public IIndexDocs vector() {
                return null;
            }

            @Override
            public IIndexDocs js() {
                return null;
            }

            @Override
            public IIndexDocs json() {
                return null;
            }

            @Override
            public IIndexDocs dynamicNbt() {
                return null;
            }

            @Override
            public IIndexDocs model() {
                return null;
            }

            @Override
            public IIndexDocs location() {
                return null;
            }

            @Override
            public IIndexDocs handType() {
                return null;
            }

            @Override
            public IIndexDocs transform() {
                return null;
            }

            @Override
            public IIndexDocs menuInsert() {
                return null;
            }

            @Override
            public IIndexDocs menuBase() {
                return null;
            }

            @Override
            public IIndexDocs settingsLink(Class<? extends ItemSetting<?>> tClass) { throw new ToDoException("SETTINGS LINK: " + tClass); } //return IIndexDocs.raw(ItemSetting.getName(tClass)); }
            @Override
            public IIndexDocs componentsLink(Class<? extends ComponentStatic<?>> tClass) { throw new ToDoException("COMPONENTS LINK: " + tClass); } //return IIndexDocs.raw(ComponentStatic.getName(tClass)); }
        };
    }

    public static Toast2<Stream<IGroup>, IDocsLink> allDocs() {
        return
    }
    private static final String[] RAW_BASE = new String[] {
            "### FORMATTED_PAGE",
            "Форматируется по правилам [FORMATTED](#formatted)",
            "",
            "После форматирования если содержится символ `:` разделяется на части по этому символу. Если нет, то Часть #2 будет равна `0`",
            "- Часть #1 - открываемая страница",
            "- Часть #2 - число, содержащее `page` открываемой страницы (счет идет от `0`)",
            "",
            "### FORMATTED_SQL_QUERY",
            "Форматируется по правилам [FORMATTED](#formatted)",
            "",
            "Итоговый результат должен являтся SQL запросом",
            "",
            "### FORMATTED_SQL_DATA",
            "Форматируется по правилам [FORMATTED](#formatted)",
            "",
            "Возможно 2 состояния:",
            "- Текст начинающийся с `\"!sql \"` и продолжающийся как [FORMATTED_SQL_QUERY](#formatted_sql_query). Вызывает полный SQL запрос",
            "- Именная таблица, передаваемая через использование [SQL_TABLE_INFO](#sql_table_info)",
            "",
            "### FORMATTED_CHAT",
            "Форматируется по правилам [FORMATTED](#formatted)",
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
            "  - `<JS:value>` - Вызов `value` как **JavaScript** код и возвращает строку. Возвращенная строка форматируется как [FORMATTED_CHAT](#formatted_chat)",
            "  - `<NICK:value>`- Отображает USER_NAME игрока по его uuid. `value` - это UUID",
            "- Взаимодействие с текстом",
            "  - `<HOVER_ITEM:value>` - *Не используется",
            "  - `<HOVER_ENTITY:value>` - *Не используется",
            "  - `<HOVER_TEXT:value>` - Отображает блок текста при наведении. `value` - это [FORMATTED_CHAT](#formatted_chat)",
            "  - `</HOVER>` - Окончание отображения при наведении",
            "  - `<CLICK_URL:value>` - Взаимодействие при нажатии - предложение на переход по ссылке `value`",
            "  - `<CLICK_FILE:value>` - Взаимодействие при нажатии - предложение на открытие файла `value`",
            "  - `<CLICK_EXECUTE:value>` - Взаимодействие при нажатии - выполнение команды `value` со стороны клиента",
            "  - `<CLICK_SUGGEST:value>` - Взаимодействие при нажатии - подставка текста `value` в поле ввода команды",
            "  - `<CLICK_PAGE:value>` - Взаимодействие при нажатии (только внутри книги) - переход на страницу `value`",
            "  - `<CLICK_COPY:value>` - Взаимодействие при нажатии - копирование текста `value`",
            "  - `</CLICK>` - Окончание взаимодействие при нажатии",
            "",
            "### FORMATTED_LINES_CHAT",
            "Может являтся одним из нижеперечисленного",
            "- <a href=\"#formatted_chat\">FORMATTED_CHAT</a>",
            "- <pre><code>[",
            "\t\t<a href=\"#formatted_chat\">FORMATTED_CHAT</a>,",
            "\t\t<a href=\"#formatted_chat\">FORMATTED_CHAT</a>,",
            "\t\t<any>...</any>",
            "\t]</code></pre>",
            "",
            "",
            "### CLICK_TYPE",
            "Указывает список вариантов действий игрока с предметом в инвертаре через разделитель <string>|</string>.",
            "",
            "Список возможных значений:",
            "- <string>ALL</string> - Любое взаимодействие с предметом",
            "- <string>CONTROL_DROP</string>",
            "- <string>CREATIVE</string>",
            "- <string>DOUBLE_CLICK</string>",
            "- <string>DROP</string>",
            "- <string>LEFT</string>",
            "- <string>MIDDLE</string>",
            "- <string>NUMBER_KEY</string>",
            "- <string>RIGHT</string>",
            "- <string>SHIFT_LEFT</string>",
            "- <string>SHIFT_RIGHT</string>",
            "- <string>SWAP_OFFHAND</string>",
            "- <string>UNKNOWN</string>",
            "- <string>WINDOW_BORDER_LEFT</string>",
            "- <string>WINDOW_BORDER_RIGHT</string>",
            "",
            "### ITEM_SLOT",
            "Может являтся одним из нижеперечисленного",
            "- [FORMATTED_TEXT](#formatted_text) в котором содержится [GIVE_ITEM](#give_item)",
            "- Предмет, в полном формате как и в `items.json`, но с добавлениями:",
            "<pre><code>{",
            "\t<name>\"?count\"</name>: 1, <comment>//Количество в слоте</comment>",
            "\t<name>\"?isShow\"</name>: <a href=\"#formatted_js\">FORMATTED_JS</a>, <comment>//Если возвращаемое значение - <bool>false</bool>, то не отображает данный предмет</comment>",
            "\t<name>\"?isAction\"</name>: <any>...</any>, <comment>//*Не используется</comment>",
            "\t<name>\"?action\"</name>: {",
            "\t\t<a href=\"#click_type\">CLICK_TYPE</a>: [",
            "\t\t\t<a href=\"#action_slot\">ACTION_SLOT</a>,",
            "\t\t\t<a href=\"#action_slot\">ACTION_SLOT</a>,",
            "\t\t\t<any>...</any>",
            "\t\t]",
            "\t}",
            "}",
            "</code></pre>",
            "",
            "### RANGE_KEY",
            "Состоит из списка <a href=\"#range_part\">RANGE_PART</a> разделенных символом `,`.",
            "",
            "Является списком порядковых номеров слотов",
            "#### RANGE_PART",
            "> Может являтся одним из нижеперечисленного",
            "> - Число - порядковый номер слота",
            "> - Текст в формате `FROM..TO`. *Пример: `3..6` - порядковые номера слотов `3`, `4`, `5` и `6`*",
            "",
            "### ARRAY_RANGE_KEY",
            "Является представлением, содержащее список порядковых номеров слотов:",
            "<pre><code>[",
            "\t<a href=\"#array_range_part\">ARRAY_RANGE_PART</a>,",
            "\t<a href=\"#array_range_part\">ARRAY_RANGE_PART</a>,",
            "\t<any>...</any>",
            "]</code></pre>",
            "",
            "#### ARRAY_RANGE_PART",
            "> Может являтся одним из нижеперечисленного",
            "> - Число - порядковый номер слота",
            "> - Массив в формате [`FROM`,`TO`]. *Пример: `[3,6]` - порядковые номера слотов `3`, `4`, `5` и `6`*",
            "",
            "### ROLL_SLOTS",
            "Является представлением настройки запуска рулетки",
            "<pre><code>{",
            "\t<name>\"slots\"</name>: <a href=\"#array_range_key\">ARRAY_RANGE_KEY</a>, <comment>//Список используемых слотов, в рулетке будут двигаться <b>ПОСЛЕДОВАТЕЛЬНО</b> от <i>первого</i> до <i>последнего</i> элемента</comment>",
            "\t<name>\"format\"</name>: <a href=\"#item_slot\">ITEM_SLOT</a>, <comment>//паттерн предмета, ктоторый будет отображатся в каждом слоте рулетки. В каждый слот передаются <string>args</string> из <name>\"data\"</name></comment>",
            "\t<name>\"data\"</name>: <a href=\"#roll_slots_data\">ROLL_SLOTS_DATA</a>, <comment>//<b>JavaScript</b> код возвращающий JS объект</comment>",
            "\t<name>\"?generate\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается после запуска рулетки с передачей в <string>args</string> данных из <a href=\"#roll_slots_data_element\">ROLL_SLOTS_DATA_ELEMENT</a> выпавшего элемента</comment>",
            "\t<name>\"?end\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается после окончания рулетки с передачей в <string>args</string> данных из <a href=\"#roll_slots_data_element\">ROLL_SLOTS_DATA_ELEMENT</a> выпавшего элемента, а также параметр <string>closed</string> со значением <bool>false</bool> если игрок дождался окончания рулетки, и <bool>true</bool> если игрок просто закрыл меню рулети</comment>",
            "\t<name>\"?tick\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a> <comment>//Вызывается после запуска каждый сдвиг рулетки</comment>",
            "}</code></pre>",
            "> #### ROLL_SLOTS_DATA",
            "> Набор возможных слотов данных рулетки, который может в ней выпасть",
            "> Является <a href=\"#formatted_js\">FORMATTED_JS</a> возвращающий JS объект ввиде:",
            "> <pre><code>[",
            "> \t<a href=\"#roll_slots_data_element\">ROLL_SLOTS_DATA_ELEMENT</a>,",
            "> \t<a href=\"#roll_slots_data_element\">ROLL_SLOTS_DATA_ELEMENT</a>,",
            "> \t<any>...</any>",
            "> ]</code></pre>",
            "#### ROLL_SLOTS_DATA_ELEMENT",
            "> Слот данных рулетки",
            "> Является JS объектом ввиде:",
            "> <pre><code>{",
            "> \t<name>\"scale\"</name>: <string>1</string>, <comment>//Вес слота данных рулетки. Целое число. Чем больше - тем больше шансов на выпадение</comment>",
            "> \t<name>\"KEY\"</name>: <string>\"VALUE\"</string>, <comment>//Значение, предающееся в <string>args</string> для слота данных рулетки</comment>",
            "> \t<name>\"KEY\"</name>: <string>\"VALUE\"</string>, <comment>//Значение, предающееся в <string>args</string> для слота данных рулетки</comment>",
            "> \t<any>...</any>",
            "> }</code></pre>",
            "",
            "### TABLE_SLOTS",
            "Является отображением списка строк",
            "<pre><code>{",
            "\t<name>\"slots\"</name>: <a href=\"#array_range_key\">ARRAY_RANGE_KEY</a>, <comment>//Указываются слоты в которых будут отображатся таблица</comment>",
            "\t<name>\"format\"</name>: <a href=\"#item_slot\">ITEM_SLOT</a>, <comment>//паттерн предмета, ктоторый будет отображатся в каждом слоте таблицы. В каждый слот передаются <string>args</string> из <string>SQL-запроса</string></comment>",
            "}</code></pre>",
            "",
            "### INVOKE_ACTION",
            "<pre><code>{",
            "\t<name>\"?owner\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается игроку-ожидающему если он в сети</comment>",
            "\t<name>\"?other\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается отигроку-выполняющему если он в сети</comment>",
            "\t<name>\"?call\"</name>: <a href=\"#action_slot\">ACTION_SLOT</a>, <comment>//Вызывается при любом случае всегда для игрока-ожидающему даже если он не в сети</comment>",
            "}</code></pre>",
            "",
            "### ACTION_SLOT",
            "<pre><code>{",
            "\t<name>\"?wait\"</name>: <string>10</string>, <comment>//Время в секундах. Задерживает вызывание на время</comment>",
            "\t<name>\"?args\"</name>: {",
            "\t\t<string>\"KEY\"</string>: <a href=\"#formatted_text\">FORMATTED_TEXT</a>,",
            "\t\t<string>\"KEY\"</string>: <a href=\"#formatted_text\">FORMATTED_TEXT</a>,",
            "\t\t<any>...</any>",
            "\t},",
            "\t<name>\"?command\"</name>: [ <comment>//Вызывает список команд последовательно</comment>",
            "\t\t<a href=\"#command_text\">COMMAND_TEXT</a>,",
            "\t\t<a href=\"#command_text\">COMMAND_TEXT</a>,",
            "\t\t<any>...</any>",
            "\t],",
            "\t<name>\"?close\"</name>: <bool>true</bool>/<bool>false</bool>, <comment>//Закрыть ли меню после выполнения? (по умолчанию <bool>false</bool>)</comment>",
            "\t<name>\"?messages\"</name>: [ <comment>//Отображает текст в чат</comment>",
            "\t\t<a href=\"#formatted_chat\">FORMATTED_CHAT</a>,",
            "\t\t<a href=\"#formatted_chat\">FORMATTED_CHAT</a>,",
            "\t\t<any>...</any>",
            "\t],",
            "\t<name>\"?logs\"</name>: [ <comment>//Отображает логи в чат всем админам</comment>",
            "\t\t<a href=\"#formatted_chat\">FORMATTED_CHAT</a>,",
            "\t\t<a href=\"#formatted_chat\">FORMATTED_CHAT</a>,",
            "\t\t<any>...</any>",
            "\t],",
            "\t<name>\"?sql\"</name>: [ <comment>//Вызывает список запросов в БД</comment>",
            "\t\t<a href=\"#formatted_sql_query\">FORMATTED_SQL_QUERY</a>,",
            "\t\t<a href=\"#formatted_sql_query\">FORMATTED_SQL_QUERY</a>,",
            "\t\t<any>...</any>",
            "\t],",
            "\t<name>\"?page\"</name>: <a href=\"#formatted_page\">FORMATTED_PAGE</a>, <comment>//Открывает меню</comment>",
            "\t<name>\"?page_args\"</name>: { <comment>//Передает аргументы в открываемое меню</comment>",
            "\t\t<string>\"*\"</string>: <string>\"REGEX\"</string>, <comment>//Копирует параметры из текущих <string>args</string> в открываемое меню по <string>REGEX</string></comment>",
            "\t\t<string>\"KEY\"</string>: <a href=\"#formatted_text\">FORMATTED_TEXT</a>,",
            "\t\t<string>\"KEY\"</string>: <a href=\"#formatted_text\">FORMATTED_TEXT</a>,",
            "\t\t<any>...</any>",
            "\t},",
            "\t<name>\"?actions\"</name>: [ <comment>//Вызывает <a href=\"#action_slot\">ACTION_SLOT</a> последовательно</comment>",
            "\t\t<a href=\"#action_slot\">ACTION_SLOT</a>,",
            "\t\t<a href=\"#action_slot\">ACTION_SLOT</a>,",
            "\t\t<any>...</any>",
            "\t],",
            "\t<name>\"?sounds\"</name>: [ <comment>//Проигрывает музыку текущему игроку</comment>",
            "\t\t<a href=\"#formatted_text\">FORMATTED_TEXT</a>, <comment>//Музыка из <string>sounds.json</string></comment>",
            "\t\t<a href=\"#formatted_text\">FORMATTED_TEXT</a>,",
            "\t\t<any>...</any>",
            "\t],",
            "\t<name>\"?global_sounds\"</name>: [ <comment>//Проигрывает музыку всем игрокам от текущего игрока</comment>",
            "\t\t<a href=\"#formatted_text\">FORMATTED_TEXT</a>, <comment>//Музыка из <string>sounds.json</string></comment>",
            "\t\t<a href=\"#formatted_text\">FORMATTED_TEXT</a>,",
            "\t\t<any>...</any>",
            "\t],",
            "\t<name>\"?online\"</name>: <bool>true</bool>/<bool>false</bool>, <comment>//Разделяет вызов если игрок не в сети, при <bool>true</bool> и если игрок не в сети вызывает <name>\"?offline_actions\"</name> (по умолчанию <bool>false</bool>)</comment>",
            "\t<name>\"?offline_actions\"</name>: [ <comment>//Вызывается если <name>\"?online\"</name> - <bool>true</bool> и игрок не в сети",
            "\t\t<a href=\"#action_slot\">ACTION_SLOT</a>,",
            "\t\t<a href=\"#action_slot\">ACTION_SLOT</a>,",
            "\t\t<any>...</any>",
            "\t],",
            "\t<name>\"?check_actions\"</name>: { <comment>//Проверяет все <a href=\"#formatted_js\">FORMATTED_JS</a> пока не найдет <a href=\"#formatted_js\">FORMATTED_JS</a> который возвращает <bool>true</bool> и только у него вызывает <a href=\"#action_slot\">ACTION_SLOT</a></comment>",
            "\t\t<a href=\"#formatted_js\">FORMATTED_JS</a>: <a href=\"#action_slot\">ACTION_SLOT</a>,",
            "\t\t<a href=\"#formatted_js\">FORMATTED_JS</a>: <a href=\"#action_slot\">ACTION_SLOT</a>,",
            "\t\t<any>...</any>",
            "\t}",
            "}</code></pre>",
            "",
            "### SQL_TABLE_INFO",
            "Если начинается с `!` - добавление таблицы для использования",
            "",
            "Остальное - добавление первого элемента таблицы в `args` в формате `{SQL_TABLE_INFO.ROW_NAME}`",
            "",
            "### SELECTOR_TYPE:",
            "Список возможных значений:",
            "- `\"main\"` - Позволяет выделить определенный блок. При выделении передает в `output`.`args`:",
            "  - `pos` - координаты блока через пробел",
            "\t- `pos_x` - координата `x` блока",
            "\t- `pos_y` - --//--",
            "\t- `pos_z` - --//--",
            "\t- `pos_face_x` - координата `x` блока, который находится у стороны выделения блока",
            "\t- `pos_face_y` - --//--",
            "\t- `pos_face_z` - --//--",
            "\t- `face` - сторона блока выделения",
            "- `\"zone_main\"` - Позволяет выделять територию и точку выхода. При выделении передает в `output`.`args`:",
            "\t- `pos1` - координаты 1 точки через пробел",
            "\t- `pos1_x` - координату `x` 1 точки",
            "\t- `pos1_y` - --//--",
            "\t- `pos1_z` - --//--",
            "\t- `pos2` - координаты 2 точки через пробел",
            "\t- `pos2_x` - координату `x` 2 точки",
            "\t- `pos2_y` - --//--",
            "\t- `pos2_z` - --//--",
            "\t- `posMain` - координаты точки окончания выделения через пробел",
            "\t- `posMain_x` - координату `x` точки окончания выделения",
            "\t- `posMain_y` - --//--",
            "\t- `posMain_z` - --//--",
            "\t- `faceMain` - сторона блока на которой было окончание выделения",
            "- `\"zone_readonly\"` - Позволяет просматривать территорию, требуется передать `args`.`pos1` и `args`.`pos2`.",
            "",
            "<comment>P.S. Команда <name>/other.event UUID user.selector</name> удаляет текущее выделение</comment>",
            "",
            "### VANILLA_ITEM",
            "Список возможных значений: [LINK](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html)",
            "",
            "### GIVE_ITEM",
            "Является [FORMATTED_TEXT](#formatted_text)",
            "",
            "Список возможных значений:",
            "- [VANILLA_ITEM](#vanilla_item)",
            "- Название предмета из *items.json*"
    };
}
*/