package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.lime.Position;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.ClickerInstance;
import org.lime.gp.block.component.display.DisplayInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.system;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@InfoComponent.Component(name = "clicker")
public final class ClickerComponent extends ComponentDynamic<JsonObject, ClickerInstance> {
    public final String type;
    public final String sound_click;
    public final String sound_result;
    public final Replace replace;
    public final LocalLocation show;
    public final Material particle;

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
                @Override
                public void invoke(Position position, InfoComponent.Rotation.Value rotation) {
                    Items.getMaterialKey(block).ifPresentOrElse(material -> position.getBlock().setType(material), () -> Blocks.creator(block).ifPresent(v -> v.setBlock(position, rotation)));
                }

                @Override
                public int max_damage() {
                    return max_damage;
                }
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
        type = json.get("type").getAsString();
        this.sound_click = json.has("sound_click") ? json.get("sound_click").getAsString() : null;
        this.sound_result = json.has("sound_result") ? json.get("sound_result").getAsString() : null;
        this.replace = json.has("replace") ? Replace.of(json.getAsJsonObject("replace")) : Replace.none;
        this.show = json.has("show") ? new LocalLocation(system.getVector(json.get("show").getAsString())) : LocalLocation.ZERO;
        this.particle = json.has("particle") ? Material.valueOf(json.get("particle").getAsString()) : null;
    }

    @Override
    public ClickerInstance createInstance(CustomTileMetadata metadata) {
        return new ClickerInstance(this, metadata);
    }
}
