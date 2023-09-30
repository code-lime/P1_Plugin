package org.lime.gp.block.component.display.display;

import org.lime.display.DisplayManager;
import org.lime.gp.block.component.display.instance.DisplayMap;
import org.lime.gp.block.component.display.instance.list.ItemFrameDisplayObject;
import org.lime.gp.module.TimeoutData;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockItemFrameManager extends DisplayManager<Toast2<UUID, UUID>, ItemFrameDisplayObject, BlockItemFrameDisplay> {
    @Override public boolean isAsync() { return true; }
    @Override public boolean isFast() { return true; }

    @Override public Map<Toast2<UUID, UUID>, ItemFrameDisplayObject> getData() {
        return TimeoutData.stream(DisplayMap.class)
                .flatMap(kv -> kv.getValue().frameMap.entrySet().stream().map(v -> Toast.of(kv.getKey(), v.getKey(), v.getValue())))
                .collect(Collectors.toMap(kv -> Toast.of(kv.val0, kv.val1), kv -> kv.val2));
    }
    @Override public BlockItemFrameDisplay create(Toast2<UUID, UUID> uuid, ItemFrameDisplayObject display) {
        return new BlockItemFrameDisplay(uuid.val0, uuid.val1, display);
    }
}
