package org.limeold.gp;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.JavaConstant;
import org.lime.reflection;
import org.lime.system;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ByteBuddyLoader {
    static {
        ByteBuddyAgent.install();
    }
    private static class Indexing {
        private static final ConcurrentHashMap<Integer, Object> indexingMap = new ConcurrentHashMap<>();
        private static final system.LockToast1<Integer> index = system.toast(0).lock();

        public static int of(Object value) {
            int _index = index.edit0(i -> i + 1);
            indexingMap.put(_index, value);
            return _index;
        }
        public static <T>T by(int index) {
            return (T)indexingMap.getOrDefault(index, null);
        }
    }
    public static Object anyInvoke(int index, Object self, Object[] args) {
        List<Object> list = new ArrayList<>(Arrays.asList(args));
        if (self != null) list.add(0, self);
        system.ICallable callable = Indexing.by(index);
        return callable.call(list.toArray());
    }
    private static final Method anyInvoke = reflection.method.of(ByteBuddyLoader.class, "anyInvoke", int.class, Object.class, Object[].class).method;

    private static final ConcurrentHashMap<Class<?>, List<system.Func1<DynamicType.Builder<?>, DynamicType.Builder<?>>>> map = new ConcurrentHashMap<>();
    public static class Builder {
        public void apply() {
            ClassReloadingStrategy strategy = ClassReloadingStrategy.fromInstalledAgent();
            map.forEach((tClass, modifies) -> {
                DynamicType.Builder<?> builder = new net.bytebuddy.ByteBuddy()
                        .redefine(tClass);
                for (system.Func1<DynamicType.Builder<?>, DynamicType.Builder<?>> modify : modifies) builder = modify.invoke(builder);
                builder.make().load(tClass.getClassLoader(), strategy);
            });
        }
        private Builder add(system.Toast2<Class<?>, system.Func1<DynamicType.Builder<?>, DynamicType.Builder<?>>> data) {
            map.compute(data.val0, (k,v) -> {
                if (v == null) v = new ArrayList<>();
                v.add(data.val1);
                return v;
            });
            return this;
        }

        public Builder replace(Method method, Method replace) { return add(_replace(method, replace)); }
        public <T extends system.ICallable> Builder replace(Method method, T replace) { return add(_replace(method, replace)); }
        public <T extends system.ICallable> Builder append(Class<?> tClass, String name, T body, Class<?> ret, Class<?>[] args, ModifierContributor.ForMethod[] flags) { return add(_append(tClass, name, body, ret, args, flags)); }

        public Builder invoke(Class<?> tClass, system.Func1<DynamicType.Builder<?>, DynamicType.Builder<?>> modify) { return add(system.toast(tClass, modify)); }
    }

    public static Implementation bodyOf(Method body) {
        return MethodCall.invoke(body).withAllArguments();
    }
    public static Implementation bodyOf(boolean isStatic, system.ICallable body) {
        MethodCall call = MethodCall
                .invoke(anyInvoke)
                .with(JavaConstant.Simple.ofLoaded(Indexing.of(body)));
        call = isStatic ? call.withReference(new Object[]{null}) : call.withThis();
        return call.withArgumentArray().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
    }

    private static system.Toast2<Class<?>, system.Func1<DynamicType.Builder<?>, DynamicType.Builder<?>>> _replace(Method method, Method replace) {
        return system.toast(method.getDeclaringClass(), v -> v.method(is(method)).intercept(bodyOf(replace)));
    }
    private static system.Toast2<Class<?>, system.Func1<DynamicType.Builder<?>, DynamicType.Builder<?>>> _replace(Method method, system.ICallable replace) {
        return system.toast(method.getDeclaringClass(), v -> v.method(is(method)).intercept(bodyOf(Modifier.isStatic(method.getModifiers()), replace)));
    }
    private static system.Toast2<Class<?>, system.Func1<DynamicType.Builder<?>, DynamicType.Builder<?>>> _append(Class<?> tClass, String name, system.ICallable body, Class<?> ret, Class<?>[] args, ModifierContributor.ForMethod[] flags) {
        return system.toast(tClass, v -> v
                .defineMethod(name, ret, flags)
                .withParameters(args)
                .intercept(bodyOf(Arrays.asList(flags).contains(Ownership.STATIC), body))
        );
    }

    private static final Builder Instance = new Builder();
    public static Builder get() { return Instance; }
}














