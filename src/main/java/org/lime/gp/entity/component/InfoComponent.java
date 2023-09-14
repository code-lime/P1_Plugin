package org.lime.gp.entity.component;

import com.google.gson.JsonObject;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.EntityInstance;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class InfoComponent {
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Component.Any.class)
    public @interface Component {
        @Retention(RetentionPolicy.RUNTIME) @interface Any {
            Component[] value();
        }

        String name();
    }
    public static final class GenericDynamicComponent<T extends EntityInstance> extends ComponentDynamic<JsonObject, T> {
        private final Func2<ComponentDynamic<?, ?>, CustomEntityMetadata, T> createInstance;
        private final String name;
        public GenericDynamicComponent(String name, EntityInfo info, Func2<ComponentDynamic<?, ?>, CustomEntityMetadata, T> createInstance) {
            super(info);
            this.createInstance = createInstance;
            this.name = name;
        }
        @Override public T createInstance(CustomEntityMetadata metadata) { return createInstance.invoke(this, metadata); }
        @Override public String name() { return getName(name); }

        public static <T extends EntityInstance>GenericDynamicComponent<T> of(String name, EntityInfo info, Func2<ComponentDynamic<?, ?>, CustomEntityMetadata, T> createInstance) {
            return new GenericDynamicComponent<>(name, info, createInstance);
        }
        public static String getName(String name) {
            return name + ".generic";
        }
    }
    public static class EmptyDynamicComponent extends EntityInstance {
        public EmptyDynamicComponent(ComponentDynamic<?, ?> component, CustomEntityMetadata metadata) { super(component, metadata); }
        @Override public void read(JsonObjectOptional json) {}
        @Override public json.builder.object write() { return json.object(); }
    }
}


















