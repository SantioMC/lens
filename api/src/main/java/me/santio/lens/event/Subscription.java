package me.santio.lens.event;

import java.util.function.Consumer;

/**
 * A subscription to an event
 * @see EventBus
 *
 * @param consumer The consumer to execute when the event is fired
 * @param clazz The class this subscription is registered for
 * @param <T> The type of event this subscription is registered for
 */
public record Subscription<T>(
    Class<T> clazz,
    Consumer<T> consumer
) {}