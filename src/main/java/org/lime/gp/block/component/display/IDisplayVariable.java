package org.lime.gp.block.component.display;

import org.lime.ToDoException;
import org.lime.docs.json.IJElement;
import org.lime.gp.block.CustomTileMetadata;

public interface IDisplayVariable {
    void syncDisplayVariable(CustomTileMetadata metadata);
    default IJElement docsDisplayVariable() {
        throw new ToDoException("DISPLAY VARIABLES");
    }
}
