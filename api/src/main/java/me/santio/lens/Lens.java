package me.santio.lens;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.AccessLevel;
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
import java.util.concurrent.CopyOnWriteArrayList;
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

    @Getter(AccessLevel.NONE)
    private final List<AutoCloseable> closeables = new CopyOnWriteArrayList<>();

    private final EventBus eventBus = new EventBus();
    private final Set<ResponsiveMetric> responsiveMetrics = ConcurrentHashMap.newKeySet();
    private final Map<Meter.Id, SimpleGauge> simpleGauges = new ConcurrentHashMap<>();

    /**
     * Register a new metric to lens, metrics implementing {@link AutoCloseable} will be closed on {@link #stop()}
     * @param metrics The metrics to register
     */
    public void register(Metric... metrics) {
        for (Metric metric : metrics) {
            metric.initialize(this, PrometheusRegistry.meterRegistry());

            if (metric instanceof ResponsiveMetric responsiveMetric) {
                this.responsiveMetrics.add(responsiveMetric);
            }

            if (metric instanceof AutoCloseable closeable) {
                this.closeables.add(closeable);
            }
        }
    }

    /**
     * Register micrometer binders into lens, the recommended usage however is to use {@link #register(Metric...)}.
     * Binders implementing {@link AutoCloseable} will be closed on {@link #stop()}
     * @param binders The binders to register
     */
    public void register(MeterBinder... binders) {
        for (MeterBinder binder : binders) {
            binder.bindTo(PrometheusRegistry.meterRegistry());

            if (binder instanceof AutoCloseable closeable) {
                this.closeables.add(closeable);
            }
        }
    }

    private Tags parseTags(@Nullable Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) return Tags.empty();

        final String[] values = new String[(tags.size() << 1)];
        int index = 0;

        for (var entry : tags.entrySet()) {
            values[index++] = entry.getKey();
            values[index++] = entry.getValue();
        }

        return Tags.of(values);
    }

    private Meter.Id meterId(String name, @Nullable Map<String, String> tags, Meter.Type type) {
        return new Meter.Id(name, this.parseTags(tags), null, null, type);
    }

    /**
     * Get or register a counter metric
     * @param name The name of the counter
     * @param tags The tags associated with this entry
     * @return The associated counter metric
     */
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
    public Gauge gauge(String name, @Nullable Map<String, String> tags, Supplier<Double> supplier) {
        return Gauge.builder(name, supplier)
            .tags(this.parseTags(tags))
            .register(PrometheusRegistry.meterRegistry());
    }

    /**
     * Get or register a gauge metric, the same instance is returned for the same name and tags
     * @param name The name of the gauge
     * @param tags The tags associated with this entry
     * @return The associated gauge metric
     */
    public SimpleGauge gauge(String name, @Nullable Map<String, String> tags) {
        final Meter.Id id = this.meterId(name, tags, Meter.Type.GAUGE);

        return this.simpleGauges.computeIfAbsent(id, (key) -> {
            final SimpleGauge gauge = new SimpleGauge();

            Gauge.builder(name, gauge, SimpleGauge::value)
                .tags(key.getTagsAsIterable())
                .strongReference(true)
                .register(PrometheusRegistry.meterRegistry());

            return gauge;
        });
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
     * Removes a meter from the registry, this should be used for meters tagged with
     * short-lived values (such as players) to prevent them from being kept forever
     * @param name The name of the meter
     * @param tags The exact tags the meter was registered with
     */
    public void remove(String name, @Nullable Map<String, String> tags) {
        final Meter.Id id = this.meterId(name, tags, Meter.Type.OTHER);

        this.simpleGauges.remove(id);
        PrometheusRegistry.meterRegistry().remove(id);
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

    /**
     * Stops the HTTP server and releases every resource held by lens, this must be called on shutdown
     * to prevent JVM-wide listeners (such as GC notifications) from keeping lens loaded
     */
    public void stop() {
        PrometheusHttpServer.stop();

        for (AutoCloseable closeable : this.closeables) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.closeables.clear();
        this.responsiveMetrics.clear();
        this.simpleGauges.clear();
        this.eventBus.clear();
        PrometheusRegistry.meterRegistry().clear();
    }

}
