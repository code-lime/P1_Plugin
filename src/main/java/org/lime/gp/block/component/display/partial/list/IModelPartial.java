package org.lime.gp.block.component.display.partial.list;

import org.lime.display.models.shadow.IBuilder;
import org.lime.system;

import java.util.Optional;

public interface IModelPartial {
    Optional<system.Toast2<IBuilder, Double>> model();
}
