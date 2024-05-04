package org.lime.gp.block.component.data;

import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.BlockSkullDestroyInfo;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.FakeUserComponent;
import org.lime.gp.player.module.FakeUsers;
import org.lime.json.JsonObjectOptional;
import org.lime.plugin.CoreElement;
import org.lime.system.json;

import java.util.Objects;
import java.util.UUID;

public class FakeUserInstance extends BlockComponentInstance<FakeUserComponent> implements CustomTileMetadata.Tickable, CustomTileMetadata.Destroyable, CustomTileMetadata.Interactable {
    public static CoreElement create() {
        return CoreElement.create(FakeUserInstance.class)
                .withInit(FakeUserInstance::init);
    }

    private static void init() {
        AnyEvent.addEvent("fake_user.block.set", AnyEvent.type.other, v -> v
                        .createParam(UUID::fromString, "[block_uuid:uuid]")
                        .createParam(Integer::parseInt, "[x:int]")
                        .createParam(Integer::parseInt, "[y:int]")
                        .createParam(Integer::parseInt, "[z:int]")
                        .createParam(UUID::fromString, "[owner_uuid:uuid]"),
                (p, block_uuid, x, y, z, owner_uuid) -> org.lime.gp.block.Blocks.of(p.getWorld().getBlockAt(x,y,z))
                        .flatMap(org.lime.gp.block.Blocks::customOf)
                        .filter(v -> v.key.uuid().equals(block_uuid))
                        .flatMap(v -> v.list(FakeUserInstance.class).findAny())
                        .ifPresent(instance -> instance.setOwner(owner_uuid, 1))
        );
    }

    private UUID lastOwnerUid = UUID.randomUUID();
    private UUID ownerUid = null;

    private long checkAfter = 0;

    public FakeUserInstance(FakeUserComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (checkAfter != 0) {
            long now = System.currentTimeMillis();
            if (now > checkAfter) checkAfter = 0;
            else return;
        }
        if (ownerUid != null && !FakeUsers.hasFakeUser(ownerUid))
            ownerUid = null;
        if (Objects.equals(lastOwnerUid, ownerUid))
            return;
        lastOwnerUid = ownerUid;
        syncDisplayVariable();
        saveData();
    }
    public void setOwner(UUID owner_uuid, int checkAfterSec) {
        if (checkAfterSec <= 0) {
            ownerUid = owner_uuid;
            lastOwnerUid = owner_uuid;
            syncDisplayVariable();
            saveData();
        } else {
            ownerUid = owner_uuid;
            checkAfter = System.currentTimeMillis() +  checkAfterSec * 1000L;
        }
    }

    @Override public void read(JsonObjectOptional json) {
        ownerUid = json.getAsString("owner_uuid").map(UUID::fromString).orElse(null);
        syncDisplayVariable();
    }
    @Override public json.builder.object write() {
        return json.object()
                .add("owner_uuid", ownerUid == null ? null : ownerUid.toString());
    }

    private void syncDisplayVariable() {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> display.modify(map -> {
            map.put("fake_user.exist", ownerUid != null && FakeUsers.hasFakeUser(ownerUid) ? "true" : "false");
            return true;
        }));
    }

    @Override public void onDestroy(CustomTileMetadata metadata, BlockSkullDestroyInfo event) {
        if (ownerUid == null) return;
        FakeUsers.setRemoteStatus(ownerUid, "DISCONNECT");
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        return ownerUid != null && component().isInteractLock ? EnumInteractionResult.SUCCESS : EnumInteractionResult.PASS;
    }
}
