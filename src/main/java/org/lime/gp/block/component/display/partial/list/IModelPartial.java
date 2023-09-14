package org.lime.gp.block.component.display.partial.list;

import org.lime.display.models.shadow.IBuilder;
import org.lime.system.toast.*;

import java.util.Optional;

public interface IModelPartial {
    Optional<Toast2<IBuilder, Double>> model();
}
