package org.lime.gp.block.component.list;

import com.badlogic.gdx.utils.Json;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.EnumDirection;
import org.lime.Position;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.MenuInstance;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.docs.IDocsLink;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@InfoComponent.Component(name = "menu")
public final class MenuComponent extends ComponentDynamic<JsonElement, MenuInstance> {
    public static Optional<Apply> argsOf(Position position) {
        return Blocks.of(position.getBlock())
                .flatMap(Blocks::customOf)
                .map(MenuComponent::argsOf);
    }

    public static Apply argsOf(CustomTileMetadata metadata) {
        Position position = metadata.position();
        return Apply.of()
                .add(metadata.list(DisplayInstance.class).findAny().map(DisplayInstance::getAll).orElseGet(Collections::emptyMap))
                .add("block_uuid", metadata.key.uuid().toString())
                .add("block_pos", position.toSave())
                .add("block_pos_x", String.valueOf(position.x))
                .add("block_pos_y", String.valueOf(position.y))
                .add("block_pos_z", String.valueOf(position.z));
    }

    public static class MenuData {
        public final String menu;
        public final String shift_menu;

        public String sound_open_any;
        public String sound_close_any;
        public String sound_open_once;
        public String sound_close_once;

        public final int open_timeout;
        public final HashMap<String, String> args = new HashMap<>();

        public MenuData(JsonObject json) {
            this.menu = json.get("menu").getAsString();
            this.shift_menu = json.has("shift_menu") ? json.get("shift_menu").isJsonNull() ? null : json.get("shift_menu").getAsString() : menu;
            this.open_timeout = json.has("open_timeout") ? json.get("open_timeout").getAsInt() : 10;

            this.sound_open_any = json.has("sound_open_any") ? json.get("sound_open_any").getAsString() : null;
            this.sound_close_any = json.has("sound_close_any") ? json.get("sound_close_any").getAsString() : null;
            this.sound_open_once = json.has("sound_open_once") ? json.get("sound_open_once").getAsString() : null;
            this.sound_close_once = json.has("sound_close_once") ? json.get("sound_close_once").getAsString() : null;

            if (json.has("args"))
                json.get("args").getAsJsonObject().entrySet().forEach(kv -> args.put(kv.getKey(), kv.getValue().getAsString()));
        }

        public static IIndexGroup docs(IDocsLink docs) {
            return JsonGroup.of("MenuData", JObject.of(
                    JProperty.require(IName.raw("menu"), IJElement.link(docs.menuName()), IComment.text("Меню, которое будет открыто")),
                    JProperty.optional(IName.raw("menu"), IJElement.link(docs.menuName()), IComment.text("Меню, которое будет открыто при нажатии с шифтом. Если не указано то срабатывает вызов ").append(IComment.field("menu"))),
                    JProperty.optional(IName.raw("args"), IJElement.anyObject(
                            JProperty.require(IName.raw("ARG_NAME"), IJElement.link(docs.formattedText()))
                    ), IComment.empty()
                            .append(IComment.text("Параметры, передаваемые в открываемое меню. Происходит дополнительная передача параметров блока."))),
                    JProperty.optional(IName.raw("open_timeout"), IJElement.raw(10), IComment.text("Время в тиках через которое произойдет срабатвания закрытия")),
                    JProperty.optional(IName.raw("sound_open_any"), IJElement.link(docs.sound()), IComment.text("Звук открытия (даже если уже открыто)")),
                    JProperty.optional(IName.raw("sound_close_any"), IJElement.link(docs.sound()), IComment.text("Звук закрытия (даже если еще открыто)")),
                    JProperty.optional(IName.raw("sound_open_once"), IJElement.link(docs.sound()), IComment.text("Звук открытия (если не было открыто)")),
                    JProperty.optional(IName.raw("sound_close_once"), IJElement.link(docs.sound()), IComment.text("Звук закрытия (если все закрыли)"))
            ));
        }
    }

    public final HashMap<EnumDirection, MenuData> data = new HashMap<>();

    public MenuComponent(BlockInfo info, JsonElement json) {
        super(info, json);
        if (json instanceof JsonObject obj) {
            MenuData data = new MenuData(obj);
            for (EnumDirection item : EnumDirection.values())
                this.data.put(item, data);
        } else {
            json.getAsJsonArray().forEach(item -> {
                JsonObject _item = item.getAsJsonObject();
                MenuData data = new MenuData(_item);
                for (String str : _item.get("direction").getAsString().split(","))
                    this.data.put(EnumDirection.byName(str.toLowerCase()), data);
            });
        }
    }

    @Override public MenuInstance createInstance(CustomTileMetadata metadata) { return new MenuInstance(this, metadata); }
    @Override public Class<MenuInstance> classInstance() { return MenuInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup direction = JsonEnumInfo.of("DIRECTION",
                EnumDirection.stream().map(v -> IJElement.raw(v.getSerializedName())).collect(ImmutableList.toImmutableList()),
                IComment.text("Сторона блока"));

        IIndexGroup directionList = JsonGroup.of("DIRECTION_LIST", IJElement.concat(",",
                IJElement.link(direction),
                IJElement.link(direction),
                IJElement.any(),
                IJElement.link(direction)));

        IIndexGroup data = MenuData.docs(docs);
        return JsonGroup.of(index, IJElement.or(
                IJElement.anyObject(JProperty.require(IName.link(direction), IJElement.link(data))),
                IJElement.anyList(JObject.of(IJProperty.base(data), JProperty.require(IName.raw("direction"), IJElement.link(directionList))))
        )).withChilds(direction, data);
    }
}
