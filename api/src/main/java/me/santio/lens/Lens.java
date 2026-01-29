package me.santio.lens;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.santio.lens.event.EventBus;
import me.santio.lens.event.impl.LensPreScrapeEvent;
import me.santio.lens.event.impl.LensScrapedEvent;
import me.santio.lens.meter.SimpleGauge;
import me.santio.lens.model.Metric;
import me.santio.lens.model.ResponsiveMetric;
import me.santio.lens.prometheus.PrometheusHttpServer;
import me.santio.lens.prometheus.PrometheusRegistry;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Entrypoint for managing lens' observability metrics
 */
@SuppressWarnings("WeakerAccess")
@Getter
@Accessors(fluent = true, chain = true)
public class Lens {
    
    @Getter
    private static final Lens instance = new Lens();
    
    private final EventBus eventBus = new EventBus();
    private final Set<ResponsiveMetric> responsiveMetrics = new HashSet<>();
    private final Map<String, SimpleGauge> simpleGauges = new ConcurrentHashMap<>();
    
    /**
     * Register a new metric to lens
     * @param metrics The metrics to register
     */
    public void register(Metric... metrics) {
        for (Metric metric : metrics) {
            metric.initialize(this, PrometheusRegistry.meterRegistry());
            if (metric instanceof ResponsiveMetric responsiveMetric) {
                this.responsiveMetrics.add(responsiveMetric);
            }
        }
    }
    
    /**
     * Register micrometer binders into lens, the recommended usage however is to use {@link #register(Metric...)}
     * @param binders The binders to register
     */
    public void register(MeterBinder... binders) {
        for (MeterBinder binder : binders) {
            binder.bindTo(PrometheusRegistry.meterRegistry());
        }
    }
    
    private @Nullable Tags parseTags(@Nullable Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        
        final String[] values = new String[(tags.size() << 1)];
        int index = 0;
        
        for (var entry : tags.entrySet()) {
            values[index++] = entry.getKey();
            values[index++] = entry.getValue();
        }
        
        return Tags.of(values);
    }
    
    /**
     * Get or register a counter metric
     * @param name The name of the counter
     * @param tags The tags associated with this entry
     * @return The associated counter metric
     */
    @SuppressWarnings("DataFlowIssue") // Acceptable usage of null
    public Counter counter(String name, @Nullable Map<String, String> tags) {
        return Counter.builder(name)
            .tags(this.parseTags(tags))
            .register(PrometheusRegistry.meterRegistry());
    }
    
    /**
     * Get or register a counter metric
     * @param name The name of the counter
     * @return The associated counter metric
     */
    public Counter counter(String name) {
        return this.counter(name, null);
    }
    
    /**
     * Get or register a gauge metric
     * @param name The name of the gauge
     * @param supplier The supplier for the gauge value
     * @param tags The tags associated with this entry
     * @return The associated gauge metric
     */
    @SuppressWarnings("DataFlowIssue") // Acceptable usage of null
    public Gauge gauge(String name, @Nullable Map<String, String> tags, Supplier<Double> supplier) {
        return Gauge.builder(name, supplier)
            .tags(this.parseTags(tags))
            .register(PrometheusRegistry.meterRegistry());
    }
    
    /**
     * Get or register a gauge metric
     * @param name The name of the gauge
     * @param tags The tags associated with this entry
     * @return The associated gauge metric
     */
    @SuppressWarnings("DataFlowIssue")
    public SimpleGauge gauge(String name, @Nullable Map<String, String> tags) {
        return PrometheusRegistry.meterRegistry().gauge(
            name,
            this.parseTags(tags),
            new SimpleGauge(),
            SimpleGauge::value
        );
    }
    
    /**
     * Get or register a gauge metric
     * @param name The name of the gauge
     * @param supplier The supplier for the gauge value
     * @return The associated gauge metric
     */
    public Gauge gauge(String name, Supplier<Double> supplier) {
        return this.gauge(name, null, supplier);
    }
    
    /**
     * Get or register a gauge metric
     * @param name The name of the gauge
     * @return The associated gauge metric
     */
    public SimpleGauge gauge(String name) {
        return this.gauge(name, Collections.emptyMap());
    }
    
    /**
     * Get or register a summary metric
     * @param name The name of the summary
     * @param tags The tags associated with this entry
     * @return The associated summary metric
     */
    @SuppressWarnings("DataFlowIssue") // Acceptable usage of null
    public DistributionSummary summary(String name, @Nullable Map<String, String> tags) {
        return DistributionSummary.builder(name)
            .tags(this.parseTags(tags))
            .publishPercentileHistogram()
            .register(PrometheusRegistry.meterRegistry());
    }
    
    /**
     * Get or register a summary metric
     * @param name The name of the summary
     * @return The associated summary metric
     */
    public DistributionSummary summary(String name) {
        return this.summary(name, null);
    }
    
    /**
     * Scrapes all the data currently in the registry
     * @return The text-value scraped data
     * @apiNote this call could be expensive
     */
    @Blocking
    public String scrape() {
        eventBus.fire(LensPreScrapeEvent.instance());
        
        this.responsiveMetrics.forEach(ResponsiveMetric::onUpdate);
        final String data = PrometheusRegistry.meterRegistry().scrape();
        
        eventBus.fire(new LensScrapedEvent(data));
        return data;
    }
    
    /**
     * Starts the HTTP server required to get scraped
     * @param address The bind address for the http server
     * @param port The port to bind to
     * @param classLoader The class loader used to find services
     */
    public void start(String address, int port, @Nullable ClassLoader classLoader) {
        // Add built-in metrics
        this.register(
            new ClassLoaderMetrics(),
            new JvmMemoryMetrics(),
            new JvmGcMetrics(),
            new ProcessorMetrics(),
            new JvmThreadMetrics(),
            new JvmThreadDeadlockMetrics()
        );
        
        // Find and load in custom metrics
        final ClassLoader loader = classLoader == null ? Lens.class.getClassLoader() : classLoader;
        ServiceLoader.load(Metric.class, loader).forEach(this::register);
        
        // Start http server
        PrometheusHttpServer.start(address, port);
    }
    
    /**
     * Starts the HTTP server required to get scraped
     * @param address The bind address for the http server
     * @param port The port to bind to
     */
    public void start(String address, int port) {
        this.start(address, port, null);
    }
    
}
