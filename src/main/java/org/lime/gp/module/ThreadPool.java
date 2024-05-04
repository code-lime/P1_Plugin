package org.lime.gp.module;

import com.google.gson.JsonObject;
import org.lime.plugin.CoreElement;
import org.lime.gp.TestData;
import org.lime.gp.lime;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ThreadPool {
    public static CoreElement create() {
        return CoreElement.create(ThreadPool.class)
                .sortType(CoreElement.SortType.First)
                .<JsonObject>addConfig("thread_pool", v -> v
                        .withDefault(json.object()
                                //.add("database", 10)
                                .add("async", 5)
                                .build())
                        .withInvoke(ThreadPool::config)
                )
                .withInit(ThreadPool::init)
                .withUninit(ThreadPool::uninit);
    }
    public enum Type {
        //DataBase,
        Async;

        private ThreadPoolExecutor pool = null;

        /*public void execute(Action0 func) {
            if (TestData.ENABLE_TYPE.get0().isSQL()) return;
            pool.execute(func);
        }
        public <T>void execute(T instance, Action1<T> func) {
            if (TestData.ENABLE_TYPE.get0().isSQL()) return;
            pool.execute(() -> func.invoke(instance));
        }*/
        public void executeRepeat(Action0 func) {
            pool.execute(() -> {
                while (true) {
                    if (DISABLE_TOKEN.get0()) return;
                    try {
                        if (TestData.ENABLE_TYPE.get0().isOther()) {
                            Thread.sleep(1000);
                            continue;
                        }

                        Thread.sleep(10);
                        func.invoke();
                    } catch (Throwable e) {
                        lime.logStackTrace(e);
                    }
                }
            });
        }
        public void executeRepeat(Action0 func, int deltaMs) {
            pool.execute(() -> {
                while (true) {
                    if (DISABLE_TOKEN.get0()) return;
                    try {
                        if (TestData.ENABLE_TYPE.get0().isOther()) {
                            Thread.sleep(1000);
                            continue;
                        }

                        Thread.sleep(deltaMs);
                        func.invoke();
                    } catch (Throwable e) {
                        lime.logStackTrace(e);
                    }
                }
            });
        }
        public void executeRepeat(Action0 func, LockToast2<Long, Long> loggerTimes) {
            loggerTimes.set0(System.currentTimeMillis());
            loggerTimes.set1(0L);
            pool.execute(() -> {
                while (true) {
                    if (DISABLE_TOKEN.get0()) return;
                    try {
                        if (TestData.ENABLE_TYPE.get0().isOther()) {
                            Thread.sleep(1000);
                            continue;
                        }

                        Thread.sleep(10);
                        long last = loggerTimes.get0() + 10;
                        long now = System.currentTimeMillis();
                        loggerTimes.set0(now);
                        loggerTimes.set1(now - last);

                        func.invoke();
                    } catch (Throwable e) {
                        lime.logStackTrace(e);
                    }
                }
            });
        }
    }

    //private static ThreadPoolExecutor executor;
    public static void config(JsonObject _json) {
        boolean modify = false;
        for (Type type : Type.values()) {
            if (!_json.has(type.name())) {
                modify = true;
                _json.addProperty(type.name(), 5);
            }
            int THREAD_COUNT = _json.get(type.name()).getAsInt();

            ThreadPoolExecutor pool = type.pool;
            List<Runnable> runnableList = new ArrayList<>();
            if (pool != null) runnableList.addAll(pool.shutdownNow());
            //pool = new ScheduledThreadPoolExecutor(THREAD_COUNT);
            pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(THREAD_COUNT);
            runnableList.forEach(pool::execute);
            type.pool = pool;
        }
        if (modify) lime.writeAllConfig("thread_pool", json.format(_json));
        /*
        List<Runnable> runnableList = new ArrayList<>();
        if (executor != null) runnableList.addAll(executor.shutdownNow());
        executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(json.get("size").getAsInt());
        runnableList.forEach(executor::execute);

        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(json.get("size").getAsInt());
        sexecutor.scheduleAtFixedRate(runnable, initialDelay, delay, TimeUnit.SECONDS);
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(json.get("size").getAsInt());
        */
    }
    public static void init() {

    }
    private static LockToast1<Boolean> DISABLE_TOKEN = Toast.lock(false);
    public static void uninit() {
        for (Type type : Type.values())
            if (type.pool != null)
                type.pool.shutdown();
        DISABLE_TOKEN.set0(true);
    }
}



















