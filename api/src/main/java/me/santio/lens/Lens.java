package me.santio.lens;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.santio.lens.event.EventBus;
import me.santio.lens.event.impl.LensPreScrapeEvent;
import me.santio.lens.event.impl.LensScrapedEvent;
import me.santio.lens.ext.ResponsiveMeterBinder;
import me.santio.lens.prometheus.PrometheusHttpServer;
import me.santio.lens.prometheus.PrometheusRegistry;
import org.jetbrains.annotations.Blocking;

import java.util.HashSet;
import java.util.Set;

/**
 * Entrypoint for managing lens' observability metrics
 */
@Getter
@Accessors(fluent = true, chain = true)
public class Lens {
    
    @Getter
    private static final Lens instance = new Lens();
    
    private final EventBus eventBus = new EventBus();
    private final Set<ResponsiveMeterBinder> responsiveBinders = new HashSet<>();
    
    /**
     * Register a new binder to lens
     * @param binders The binders to register
     */
    public void register(MeterBinder... binders) {
        for (MeterBinder binder : binders) {
            binder.bindTo(PrometheusRegistry.meterRegistry());
            if (binder instanceof ResponsiveMeterBinder responsiveBinder) {
                this.responsiveBinders.add(responsiveBinder);
            }
        }
    }
    
    /**
     * Scrapes all the data currently in the registry
     * @return The text-value scraped data
     * @apiNote this call could be expensive
     */
    @Blocking
    public String scrape() {
        eventBus.fire(LensPreScrapeEvent.instance());
        
        this.responsiveBinders.forEach(ResponsiveMeterBinder::onUpdate);
        final String data = PrometheusRegistry.meterRegistry().scrape();
        
        eventBus.fire(new LensScrapedEvent(data));
        return data;
    }
    
    /**
     * Starts the HTTP server required to get scraped
     * @param address The bind address for the http server
     * @param port The port to bind to
     */
    public void start(String address, int port) {
        // Add built-in metrics
        this.register(
            new ClassLoaderMetrics(),
            new JvmMemoryMetrics(),
            new JvmGcMetrics(),
            new ProcessorMetrics(),
            new JvmThreadMetrics(),
            new JvmThreadDeadlockMetrics()
        );
        
        // Start http server
        PrometheusHttpServer.start(address, port);
    }
    
}
