package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.game.CheckerInstance;
import org.lime.gp.block.component.data.game.ChessInstance;
import org.lime.gp.block.component.data.game.ITableGameInstance;
import org.lime.gp.docs.IDocsLink;
import org.lime.system;

@InfoComponent.Component(name = "game")
public final class MapGameComponent extends ComponentDynamic<JsonObject, ITableGameInstance<?>> {
    public enum GameType {
        Checkers(CheckerInstance::new),
        Chess(ChessInstance::new);

        public final system.Func2<MapGameComponent, CustomTileMetadata, ITableGameInstance<?>> createInstance;

        GameType(system.Func2<MapGameComponent, CustomTileMetadata, ITableGameInstance<?>> createInstance) {
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
    @Override public Class<ITableGameInstance<?>> classInstance() { throw new ToDoException("BLOCK CLASS INSTANCE: " + ITableGameInstance.class); }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
