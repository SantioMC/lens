package me.santio.lens.ext;

import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * A responsive meter binder is a meter that has its {@link #onUpdate()} called right before
 * data is scraped from lens.
 */
public interface ResponsiveMeterBinder extends MeterBinder {
    
    /**
     * Called right before the meter is scraped, the delay between scrapes is unknown
     */
    void onUpdate();

}
