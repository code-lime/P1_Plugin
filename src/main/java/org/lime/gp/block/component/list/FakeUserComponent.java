package org.lime.gp.block.component.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.FakeUserInstance;
import org.lime.gp.docs.IDocsLink;

@InfoComponent.Component(name = "fake_user")
public final class FakeUserComponent extends ComponentDynamic<JsonElement, FakeUserInstance> {
    public final boolean isInteractLock;

    public FakeUserComponent(BlockInfo info, JsonObject json) {
        super(info, json);

        isInteractLock = json.get("interact_lock").getAsBoolean();
    }

    @Override public FakeUserInstance createInstance(CustomTileMetadata metadata) { return new FakeUserInstance(this, metadata); }
    @Override public Class<FakeUserInstance> classInstance() { return FakeUserInstance.class; }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("interact_lock"), IJElement.bool(), IComment.text("Запрещает взаимодействие при нахождении fake_user"))
        ), IComment.text("Добавляет возможность детектить fake_users")).withChilds();
    }
}
