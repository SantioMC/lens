package me.santio.lens.model;

/**
 * A responsive meter binder is a meter that has its {@link #onUpdate()} called right before
 * data is scraped from lens.
 * @see ResponsiveMetric
 */
@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface ResponsiveMetric extends Metric {
    
    /**
     * Called before a scape is performed, the delay between calls is unknown
     */
    void onUpdate();
    
}
