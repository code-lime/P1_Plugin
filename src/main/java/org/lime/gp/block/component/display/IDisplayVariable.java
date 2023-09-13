package org.lime.gp.block.component.display;

import org.lime.ToDoException;
import org.lime.docs.json.IJElement;

public interface IDisplayVariable {
    void syncDisplayVariable();
    default IJElement docsDisplayVariable() { throw new ToDoException("DOCS DISPLAY VARIABLE"); }
}
