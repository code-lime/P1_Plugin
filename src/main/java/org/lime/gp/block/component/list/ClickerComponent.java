package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.lime.Position;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.ClickerInstance;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.list.ClickerSetting;
import org.lime.gp.player.menu.MenuCreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@InfoComponent.Component(name = "clicker")
public final class ClickerComponent extends ComponentDynamic<JsonObject, ClickerInstance> {
    public final List<String> types = new ArrayList<>();
    public final String sound_click;
    public final String sound_result;
    public final Replace replace;
    /*
    public final Transformation show;
    public final Transformation step_fore;
    public final Transformation step_back;
    */
    public final Material particle;
    public final boolean hand_click;

    public interface Replace {
        void invoke(Position position, InfoComponent.Rotation.Value rotation);

        int max_damage();

        Replace none = new Replace() {
            @Override
            public void invoke(Position position, InfoComponent.Rotation.Value rotation) {
            }

            @Override
            public int max_damage() {
                return -1;
            }
        };

        static Replace of(String block, int max_damage) {
            return new Replace() {
                @Override public void invoke(Position position, InfoComponent.Rotation.Value rotation) {
                    Blocks.setBlock(position, block, rotation);
                }
                @Override public int max_damage() { return max_damage; }
            };
        }

        static Replace of(Map<String, String> variable, int max_damage) {
            return new Replace() {
                @Override
                public void invoke(Position position, InfoComponent.Rotation.Value rotation) {
                    Blocks.of(position.getBlock())
                            .flatMap(Blocks::customOf)
                            .ifPresent(metadata -> metadata.list(DisplayInstance.class)
                                    .findAny()
                                    .ifPresent(display -> {
                                        Apply apply = MenuComponent.argsOf(metadata);
                                        display.set(variable.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, kv -> ChatHelper.formatText(kv.getValue(), apply))));
                                    })
                            );
                }

                @Override
                public int max_damage() {
                    return max_damage;
                }
            };
        }

        static Replace of(String page, Map<String, String> args, int max_damage) {
            return new Replace() {
                @Override
                public void invoke(Position position, InfoComponent.Rotation.Value rotation) {
                    Apply apply = MenuComponent.argsOf(position).orElseGet(Apply::of);
                    MenuCreator.show(page, apply.add(args.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, kv -> ChatHelper.formatText(kv.getValue(), apply)))));
                }

                @Override
                public int max_damage() {
                    return max_damage;
                }
            };
        }

        static Replace of(JsonObject json) {
            if (json.has("block")) return of(json.get("block").getAsString(), json.get("max_damage").getAsInt());
            else if (json.has("page"))
                return of(json.get("page").getAsString(), json.has("args") ? json.get("args").getAsJsonObject().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().getAsString())) : Collections.emptyMap(), json.get("max_damage").getAsInt());
            else if (json.has("variable"))
                return of(json.get("variable").getAsJsonObject().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().getAsString())), json.get("max_damage").getAsInt());
            return none;
        }
    }

    public ClickerComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        if (json.get("type").isJsonArray()) json.getAsJsonArray("type").forEach(type -> this.types.add(type.getAsString()));
        else types.add(json.get("type").getAsString());

        this.sound_click = json.has("sound_click") ? json.get("sound_click").getAsString() : null;
        this.sound_result = json.has("sound_result") ? json.get("sound_result").getAsString() : null;
        this.replace = json.has("replace") ? Replace.of(json.getAsJsonObject("replace")) : Replace.none;
        /*
        this.show = json.has("show") ? system.transformation(json.get("show")) : Transformation.identity();
        if (json.has("step")) {
            if (json.get("step").isJsonPrimitive()) {
                this.step_fore = this.step_back = system.transformation(json.get("step"));
            } else {
                JsonObject step = json.getAsJsonObject("step");
                if (step.has("all")) this.step_fore = this.step_back = system.transformation(step.get("all"));
                else {
                    this.step_fore = system.transformation(step.get("fore"));
                    this.step_back = system.transformation(step.get("back"));
                }
            }
        } else {
            this.step_fore = this.step_back = new Transformation(new Vector3f(0, 0.025f, 0), null, null, null);
        }
        */
        this.particle = json.has("particle") ? Material.valueOf(json.get("particle").getAsString()) : null;
        this.hand_click = json.has("hand_click") && json.get("hand_click").getAsBoolean();
    }

    @Override public ClickerInstance createInstance(CustomTileMetadata metadata) { return new ClickerInstance(this, metadata); }
    @Override public Class<ClickerInstance> classInstance() { return ClickerInstance.class; }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        JProperty max_damage_prop = JProperty.require(IName.raw("max_damage"), IJElement.raw(10), IComment.text("Колисечтво ударов, после которого произойдет замена"));
        IIndexGroup replace = JsonEnumInfo.of("REPLACE", "replace", IComment.text("Вызывает действие"))
                .add(JObject.of(
                        JProperty.require(IName.raw("block"), IJElement.link(docs.setBlock()), IComment.text("Блок, на который произойдет замена")),
                        max_damage_prop
                ))
                .add(JObject.of(
                        JProperty.require(IName.raw("page"), IJElement.link(docs.menuName()), IComment.text("Меню, которое будет открыто")),
                        JProperty.optional(IName.raw("args"), IJElement.anyObject(
                                JProperty.require(IName.raw("ARG_NAME"), IJElement.link(docs.formattedText()))
                        ), IComment.empty()
                                .append(IComment.text("Параметры, передаваемые в открываемое меню. Происходит дополнительная передача параметров блока."))),
                        max_damage_prop
                ))
                .add(JObject.of(
                        JProperty.require(IName.raw("variable"), IJElement.anyObject(
                                JProperty.require(IName.raw("VARIABLE_NAME"), IJElement.link(docs.formattedText()))
                        ), IComment.text("Изменяет указанные параметры внутри блока")),
                        max_damage_prop
                ));
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("type"), IJElement.raw("CLICKER_TYPE").or(IJElement.anyList(IJElement.raw("CLICKER_TYPE"))), IComment.empty()
                        .append(IComment.text("Пользовательский тип кликера. Требует ударять предметом с "))
                        .append(IComment.link(docs.settingsLink(ClickerSetting.class)))
                        .append(IComment.text(" у которого "))
                        .append(IComment.field("type"))
                        .append(IComment.text(" эквивалентен данному. Зависит от пареметра"))
                        .append(IComment.field("hand_click"))),
                JProperty.optional(IName.raw("replace"), IJElement.link(replace), IComment.text("Позволяет добавить прочность блоку с последующий заменой")),
                JProperty.optional(IName.raw("particle"), IJElement.link(docs.vanillaMaterial()), IComment.text("Материал, частицы которого будут отображатся при ударе")),
                JProperty.optional(IName.raw("hand_click"), IJElement.bool(), IComment.text("Позволяет ударять не имея при себе подходящего инструмента")),
                JProperty.optional(IName.raw("sound_click"), IJElement.link(docs.sound()), IComment.text("Звук удара")),
                JProperty.optional(IName.raw("sound_result"), IJElement.link(docs.sound()), IComment.text("Звук крафта"))
        ), IComment.text("Блок является кликером")).withChilds(replace);
    }
}
