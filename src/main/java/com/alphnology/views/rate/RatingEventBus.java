package com.alphnology.views.rate;

import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author me@fredpena.dev
 * @created 25/06/2025  - 14:11
 */
@Component
public class RatingEventBus implements Serializable {

    // Using CopyOnWriteArrayList for thread-safe iteration while allowing modifications
    private final CopyOnWriteArrayList<Consumer<RatingSavedEvent>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Subscribes a listener to RatingSavedEvent.
     *
     * @param listener The consumer function to be called when an event is published.
     * @return A Registration object that can be used to unsubscribe.
     */
    public Registration subscribe(Consumer<RatingSavedEvent> listener) {
        listeners.add(listener);
        // Return a Registration that, when removed, unsubscribes the listener
        return () -> listeners.remove(listener);
    }

    /**
     * Publishes a RatingSavedEvent to all subscribed listeners.
     *
     * @param event The RatingSavedEvent to publish.
     */
    public void publish(RatingSavedEvent event) {
        listeners.forEach(listener -> listener.accept(event));
    }
}