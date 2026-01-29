package me.santio.lens.model;

import io.micrometer.core.instrument.MeterRegistry;import me.santio.lens.Lens;

/**
 * A metric in Lens, implementations of Lens will automatically find services from this interface
 */
public interface Metric {
    
    /**
     * Called on initialization of lens, you may register meters here
     * @param lens The associated lens instance
     * @param registry The micrometer meter registry used to manually create metrics
     */
    default void initialize(Lens lens, MeterRegistry registry) {};
    
}
