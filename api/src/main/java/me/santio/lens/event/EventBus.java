package me.santio.lens.event;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A class-based event system used to listen for events
 */
public class EventBus {
    
    private final Map<Class<? extends LensEvent>, Set<Subscription<? extends LensEvent>>> subscriptions = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    public <T extends LensEvent> T fire(T event) {
        final @Nullable Set<Subscription<? extends LensEvent>> subscriptions = this.subscriptions.get(event.getClass());
        
        if (subscriptions != null) {
            for (var subscription : subscriptions) {
                ((Subscription<T>) subscription).consumer().accept(event);
            }
        }
        
        return event;
    }
    
    public <T extends LensEvent> Subscription<T> subscribe(Class<T> clazz, Consumer<T> consumer) {
        final Subscription<T> subscription = new Subscription<>(clazz, consumer);
        this.subscriptions.computeIfAbsent(clazz, key -> new HashSet<>()).add(subscription);
        return subscription;
    }
    
}
