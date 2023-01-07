package org.lime.gp.module;

import net.minecraft.server.network.LoginListener;
import net.minecraft.server.network.PlayerConnection;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.reflection;
import org.slf4j.Logger;
import org.slf4j.Marker;

public class DisableConsoleSpam {
    public static core.element create() {
        return core.element.create(DisableConsoleSpam.class)
                .withInit(DisableConsoleSpam::init);
    }

    private static class ProxyLogger implements Logger {
        public final Logger base_of_proxy;
        public ProxyLogger(Logger base_of_proxy) {
            this.base_of_proxy = base_of_proxy;
        }
        @Override public String getName() { return base_of_proxy.getName(); }
        @Override public boolean isTraceEnabled() { return base_of_proxy.isTraceEnabled(); }
        @Override public void trace(String var1) { base_of_proxy.trace(var1); }
        @Override public void trace(String var1, Object var2) { base_of_proxy.trace(var1, var2); }
        @Override public void trace(String var1, Object var2, Object var3) { base_of_proxy.trace(var1, var2, var3); }
        @Override public void trace(String var1, Object... var2) { base_of_proxy.trace(var1, var2); }
        @Override public void trace(String var1, Throwable var2) { base_of_proxy.trace(var1, var2); }
        @Override public boolean isTraceEnabled(Marker var1) { return base_of_proxy.isTraceEnabled(var1); }
        @Override public void trace(Marker var1, String var2) { base_of_proxy.trace(var1, var2); }
        @Override public void trace(Marker var1, String var2, Object var3) { base_of_proxy.trace(var1, var2, var3); }
        @Override public void trace(Marker var1, String var2, Object var3, Object var4) { base_of_proxy.trace(var1, var2, var3, var4); }
        @Override public void trace(Marker var1, String var2, Object... var3) { base_of_proxy.trace(var1, var2, var3); }
        @Override public void trace(Marker var1, String var2, Throwable var3) { base_of_proxy.trace(var1, var2, var3); }
        @Override public boolean isDebugEnabled() { return base_of_proxy.isDebugEnabled(); }
        @Override public void debug(String var1) { base_of_proxy.debug(var1); }
        @Override public void debug(String var1, Object var2) { base_of_proxy.debug(var1, var2); }
        @Override public void debug(String var1, Object var2, Object var3) { base_of_proxy.debug(var1, var2, var3); }
        @Override public void debug(String var1, Object... var2) { base_of_proxy.debug(var1, var2); }
        @Override public void debug(String var1, Throwable var2) { base_of_proxy.debug(var1, var2); }
        @Override public boolean isDebugEnabled(Marker var1) { return base_of_proxy.isDebugEnabled(var1); }
        @Override public void debug(Marker var1, String var2) { base_of_proxy.debug(var1, var2); }
        @Override public void debug(Marker var1, String var2, Object var3) { base_of_proxy.debug(var1, var2, var3); }
        @Override public void debug(Marker var1, String var2, Object var3, Object var4) { base_of_proxy.debug(var1, var2, var3, var4); }
        @Override public void debug(Marker var1, String var2, Object... var3) { base_of_proxy.debug(var1, var2, var3); }
        @Override public void debug(Marker var1, String var2, Throwable var3) { base_of_proxy.debug(var1, var2, var3); }
        @Override public boolean isWarnEnabled() { return base_of_proxy.isWarnEnabled(); }
        @Override public void warn(String var1) { base_of_proxy.warn(var1); }
        @Override public void warn(String var1, Object var2) { base_of_proxy.warn(var1, var2); }
        @Override public void warn(String var1, Object... var2) { base_of_proxy.warn(var1, var2); }
        @Override public void warn(String var1, Object var2, Object var3) { base_of_proxy.warn(var1, var2, var3); }
        @Override public void warn(String var1, Throwable var2) { base_of_proxy.warn(var1, var2); }
        @Override public boolean isWarnEnabled(Marker var1) { return base_of_proxy.isWarnEnabled(var1); }
        @Override public void warn(Marker var1, String var2) { base_of_proxy.warn(var1, var2); }
        @Override public void warn(Marker var1, String var2, Object var3) { base_of_proxy.warn(var1, var2, var3); }
        @Override public void warn(Marker var1, String var2, Object var3, Object var4) { base_of_proxy.warn(var1, var2, var3, var4); }
        @Override public void warn(Marker var1, String var2, Object... var3) { base_of_proxy.warn(var1, var2, var3); }
        @Override public void warn(Marker var1, String var2, Throwable var3) { base_of_proxy.warn(var1, var2, var3); }
        @Override public boolean isErrorEnabled() { return base_of_proxy.isErrorEnabled(); }
        @Override public void error(String var1) { base_of_proxy.error(var1); }
        @Override public void error(String var1, Object var2) { base_of_proxy.error(var1, var2); }
        @Override public void error(String var1, Object var2, Object var3) { base_of_proxy.error(var1, var2, var3); }
        @Override public void error(String var1, Object... var2) { base_of_proxy.error(var1, var2); }
        @Override public void error(String var1, Throwable var2) { base_of_proxy.error(var1, var2); }
        @Override public boolean isErrorEnabled(Marker var1) { return base_of_proxy.isErrorEnabled(var1); }
        @Override public void error(Marker var1, String var2) { base_of_proxy.error(var1, var2); }
        @Override public void error(Marker var1, String var2, Object var3) { base_of_proxy.error(var1, var2, var3); }
        @Override public void error(Marker var1, String var2, Object var3, Object var4) { base_of_proxy.error(var1, var2, var3, var4); }
        @Override public void error(Marker var1, String var2, Object... var3) { base_of_proxy.error(var1, var2, var3); }
        @Override public void error(Marker var1, String var2, Throwable var3) { base_of_proxy.error(var1, var2, var3); }
        @Override public boolean isInfoEnabled() { return base_of_proxy.isInfoEnabled(); }
        @Override public void info(String var1) { base_of_proxy.info(var1); }
        @Override public void info(String var1, Object var2) { base_of_proxy.info(var1, var2); }
        @Override public void info(String var1, Object var2, Object var3) {
            switch (var1) {
                case "{} lost connection: {}", "Disconnecting {}: {}" -> {
                    if (!"The server is full!".equals(var3)) break;
                    return;
                }
            }
            base_of_proxy.info(var1, var2, var3);
        }
        @Override public void info(String var1, Object... var2) { base_of_proxy.info(var1, var2); }
        @Override public void info(String var1, Throwable var2) { base_of_proxy.info(var1, var2); }
        @Override public boolean isInfoEnabled(Marker var1) { return base_of_proxy.isInfoEnabled(var1); }
        @Override public void info(Marker var1, String var2) { base_of_proxy.info(var1, var2); }
        @Override public void info(Marker var1, String var2, Object var3) { base_of_proxy.info(var1, var2, var3); }
        @Override public void info(Marker var1, String var2, Object var3, Object var4) { base_of_proxy.info(var1, var2, var3, var4); }
        @Override public void info(Marker var1, String var2, Object... var3) { base_of_proxy.info(var1, var2, var3); }
        @Override public void info(Marker var1, String var2, Throwable var3) { base_of_proxy.info(var1, var2, var3); }
    }
    private static void loadProxy(Class<?> tClass) {
        reflection.field<Logger> LOGGER = reflection.field.<Logger>ofMojang(tClass, "LOGGER").nonFinal();
        Logger logger = LOGGER.get(null);
        while (org.lime.reflection.hasField(logger.getClass(), "base_of_proxy")) logger = org.lime.reflection.getField(logger.getClass(), "base_of_proxy", logger);
        lime.log("Logger '"+logger.getClass()+"' of '"+tClass+"' proxied");
        logger = new ProxyLogger(logger);
        LOGGER.set(null, logger);
    }
    public static void init() {
        loadProxy(PlayerConnection.class);
        loadProxy(LoginListener.class);
    }
}
