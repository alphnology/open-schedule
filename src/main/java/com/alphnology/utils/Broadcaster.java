package com.alphnology.utils;

import com.vaadin.flow.shared.Registration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author me@fredpena.dev
 * @created 17/11/2024  - 20:16
 */
public final class Broadcaster {

    public static final String RATE_SESSION = "RATE_SESSION";

    private Broadcaster() {
    }

    private static final Executor executor = Executors.newFixedThreadPool(4);

    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<Object>>> channels = new ConcurrentHashMap<>();

    public static synchronized Registration register(String channel, Consumer<Object> listener) {

        channels.computeIfAbsent(channel, key -> new CopyOnWriteArrayList<>()).add(listener);

        return () -> {
            CopyOnWriteArrayList<Consumer<Object>> listeners = channels.get(channel);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    channels.remove(channel);
                }
            }
        };
    }

    public static synchronized void broadcast(String channel) {
        broadcast(channel, null);
    }

    public static synchronized void broadcast(String channel, Object message) {
        CopyOnWriteArrayList<Consumer<Object>> listeners = channels.get(channel);
        if (listeners != null) {
            for (Consumer<Object> listener : listeners) {
                executor.execute(() -> listener.accept(message));
            }
        }
    }
}