package zone.pumpkinhill.discord4droid.api;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages event listeners and event logic.
 */
public class EventDispatcher {
    private final static String TAG = EventDispatcher.class.getCanonicalName();

    private ConcurrentHashMap<Class<?>, ConcurrentHashMap<Method, CopyOnWriteArrayList<Object>>> methodListeners = new ConcurrentHashMap<>();
    private DiscordClient client;

    public EventDispatcher(DiscordClient client) {
        this.client = client;
    }

    /**
     * Registers a listener using {@link EventSubscriber} method annotations.
     *
     * @param listener The listener.
     */
    public void registerListener(Object listener) {
        for (Method method : listener.getClass().getMethods()) {
            if (method.getParameterTypes().length == 1
                    && method.isAnnotationPresent(EventSubscriber.class)) {
                method.setAccessible(true);
                Class<?> eventClass = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(eventClass)) {
                    if (!methodListeners.containsKey(eventClass))
                        methodListeners.put(eventClass, new ConcurrentHashMap<Method, CopyOnWriteArrayList<Object>>());

                    if (!methodListeners.get(eventClass).containsKey(method))
                        methodListeners.get(eventClass).put(method, new CopyOnWriteArrayList<>());

                    methodListeners.get(eventClass).get(method).add(listener);
                    Log.d(TAG, "Registered method listener " + listener.getClass().getSimpleName() + method.toString());
                }
            }
        }
    }

    /**
     * Unregisters a listener using {@link EventSubscriber} method annotations.
     *
     * @param listener The listener.
     */
    public void unregisterListener(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.getParameterTypes().length == 1) {
                Class<?> eventClass = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(eventClass)) {
                    if (methodListeners.containsKey(eventClass))
                        if (methodListeners.get(eventClass).containsKey(method)) {
                            methodListeners.get(eventClass).get(method).remove(listener);
                            Log.d(TAG, "Unregistered method listener " + listener.getClass().getSimpleName() + method.toString());
                        }
                }
            }
        }
    }

    /**
     * Dispatches an event.
     *
     * @param event The event.
     */
    public void dispatch(Event event) {
        if (client.isReady()) {
            Log.d(TAG, "Dispatching event of type " + event.getClass().getSimpleName());
            event.client = client;
            for(Entry<Class<?>, ConcurrentHashMap<Method, CopyOnWriteArrayList<Object>>> en : methodListeners.entrySet()) {
                if(!en.getKey().isAssignableFrom(event.getClass())) continue;
                for(Entry<Method, CopyOnWriteArrayList<Object>> m : en.getValue().entrySet()) {
                    Method k = m.getKey();
                    for(Object o : m.getValue()) {
                        try {
                            k.invoke(o, event);
                        } catch (IllegalAccessException e) {
                            Log.e(TAG, "Error dispatching event " + event.getClass().getSimpleName() + e);
                        } catch (InvocationTargetException e) {
                            Log.e(TAG, "Error dispatching event " + event.getClass().getSimpleName() + e);
                            e.printStackTrace();
                        } catch (Exception e) {
                            Log.e(TAG, "Unhandled exception caught dispatching event " + event.getClass().getSimpleName() + e);
                        }
                    }
                }
            }
        }
    }
}
