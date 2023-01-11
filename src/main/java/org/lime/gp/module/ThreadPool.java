package org.lime.gp.module;

import com.google.gson.JsonObject;
import org.lime.core;
import org.lime.gp.TestData;
import org.lime.gp.lime;
import org.lime.system;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ThreadPool {
    public static core.element create() {
        return core.element.create(ThreadPool.class)
                .sortType(core.element.SortType.First)
                .<JsonObject>addConfig("thread_pool", v -> v
                        .withDefault(system.json.object()
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

        /*public void execute(system.Action0 func) {
            if (TestData.ENABLE_TYPE.get0().isSQL()) return;
            pool.execute(func);
        }
        public <T>void execute(T instance, system.Action1<T> func) {
            if (TestData.ENABLE_TYPE.get0().isSQL()) return;
            pool.execute(() -> func.invoke(instance));
        }*/
        public void executeRepeat(system.Action0 func) {
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
        public void executeRepeat(system.Action0 func, system.LockToast2<Long, Long> loggerTimes) {
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
                        long last = loggerTimes.get0();
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
    public static void config(JsonObject json) {
        boolean modify = false;
        for (Type type : Type.values()) {
            if (!json.has(type.name())) {
                modify = true;
                json.addProperty(type.name(), 5);
            }
            int THREAD_COUNT = json.get(type.name()).getAsInt();

            ThreadPoolExecutor pool = type.pool;
            List<Runnable> runnableList = new ArrayList<>();
            if (pool != null) runnableList.addAll(pool.shutdownNow());
            //pool = new ScheduledThreadPoolExecutor(THREAD_COUNT);
            pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(THREAD_COUNT);
            runnableList.forEach(pool::execute);
            type.pool = pool;
        }
        if (modify) lime.writeAllConfig("thread_pool", system.toFormat(json));
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
    private static system.LockToast1<Boolean> DISABLE_TOKEN = system.toast(false).lock();
    public static void uninit() {
        for (Type type : Type.values())
            if (type.pool != null)
                type.pool.shutdown();
        DISABLE_TOKEN.set0(true);
    }
}



















