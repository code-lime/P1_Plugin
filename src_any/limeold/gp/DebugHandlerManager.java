package org.limeold.gp;

import org.lime.system;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Method;

public class DebugHandlerManager {
    public static class DebugRegisteredListener extends RegisteredListener
    {
        String text;
        RegisteredListener listener;
        public DebugRegisteredListener(RegisteredListener listener) {
            super(listener.getListener(), null, listener.getPriority(), listener.getPlugin(), listener.isIgnoringCancelled());
            this.listener = listener;
            this.text = listener.getListener().getClass().getName();
        }

        @Override public void callEvent(Event event) throws EventException {
            listener.callEvent(event);
        }
    }

    private static system.Func1<Class<? extends Event>, HandlerList> getEventListeners;
    private static system.Func1<Class<? extends Event>, Class<? extends Event>> getRegistrationClass;
    private static SimplePluginManager manager = (SimplePluginManager)Bukkit.getServer().getPluginManager();
    static {
        for (Method method : SimplePluginManager.class.getDeclaredMethods())
        {
            switch (method.getName())
            {
                case "getEventListeners":
                {
                    method.setAccessible(true);
                    getEventListeners = (type) -> {
                        try {
                            return (HandlerList) method.invoke(manager, type);
                        } catch (Exception e) {
                            throw new Error(e);
                        }
                    };
                    break;
                }
                case "getRegistrationClass":
                {
                    method.setAccessible(true);
                    getRegistrationClass = (type) -> {
                        try {
                            return (Class<? extends Event>) method.invoke(manager, type);
                        } catch (Exception e) {
                            throw new Error(e);
                        }
                    };
                    break;
                }
            }
        }
    }

    //this.getServer().getPluginManager()
    public static void registerEvents(SimplePluginManager manager, Listener listener, Plugin plugin) {
        manager.registerEvents(listener, plugin);
        /*if (!plugin.isEnabled())
            throw new IllegalPluginAccessException("Plugin attempted to register " + listener + " while not enabled");
        for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : plugin.getPluginLoader().createRegisteredListeners(listener, plugin).entrySet())
        {
            Set<RegisteredListener> listeners = new HashSet<>();
            entry.getValue().forEach(v -> listeners.add(new DebugRegisteredListener(v)));
            getEventListeners.invoke(getRegistrationClass.invoke(entry.getKey())).registerAll(listeners);
        }*/
    }


    public static void registerEvents(Listener listener, Plugin plugin) {
        registerEvents(manager, listener, plugin);
    }
}
