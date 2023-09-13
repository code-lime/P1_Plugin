package org.lime.gp.item.data;

import java.util.Set;

public interface IUpdate {
    boolean is(UpdateType type);
    default boolean any(UpdateType... types) {
        for (UpdateType type : types)
            if (is(type))
                return true;
        return false;
    }

    static IUpdate all() { return type -> true; }
    static IUpdate of(Set<UpdateType> list) { return list::contains; }
    static IUpdate of(UpdateType type) { return type::equals; }
    static IUpdate of(UpdateType... types) { return of(Set.of(types)); }
}
