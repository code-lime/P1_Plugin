package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.game.CheckerInstance;
import org.lime.gp.block.component.data.game.ChessInstance;
import org.lime.gp.block.component.data.game.ITableGameInstance;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import javax.annotation.Nullable;

@InfoComponent.Component(name = "game")
public final class MapGameComponent extends ComponentDynamic<JsonObject, ITableGameInstance<?>> {
    public enum GameType {
        Checkers(CheckerInstance.class, CheckerInstance::new),
        Chess(ChessInstance.class, ChessInstance::new);

        public final Func2<MapGameComponent, CustomTileMetadata, ? extends ITableGameInstance<?>> createInstance;
        public final Class<? extends ITableGameInstance<?>> classInstance;

        <T extends ITableGameInstance<?>>GameType(Class<T> classInstance, Func2<MapGameComponent, CustomTileMetadata, T> createInstance) {
            this.classInstance = classInstance;
            this.createInstance = createInstance;
        }
    }

    public final GameType type;

    public MapGameComponent(BlockInfo info, GameType type) {
        super(info);
        this.type = type;
    }
    public MapGameComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        type = GameType.valueOf(json.get("type").getAsString());
    }

    @Override public ITableGameInstance<?> createInstance(CustomTileMetadata metadata) { return type.createInstance.invoke(this, metadata); }
    @Override public @Nullable Class<? extends ITableGameInstance<?>> classInstance() { return null; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup mapGameType = JsonEnumInfo.of("MapGameType", GameType.class);
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("type"), IJElement.link(mapGameType), IComment.text("Тип настольной игры"))
        )).withChild(mapGameType);
    }
}