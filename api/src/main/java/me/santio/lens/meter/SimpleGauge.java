package me.santio.lens.meter;

import io.micrometer.core.instrument.cumulative.CumulativeCounter;

import java.util.concurrent.atomic.DoubleAdder;

/**
 * A simple wrapper over a gauge build with a {@link DoubleAdder}, its behavior is similar
 * to {@link CumulativeCounter} from micrometer, but with a way to decrement.
 * @author santio
 */
@SuppressWarnings("WeakerAccess")
public class SimpleGauge {
    
    private final DoubleAdder value = new DoubleAdder();
    
    /**
     * Increment the gauge up by a certain amount
     * @param amount The amount to increment the gauge by
     */
    public void increment(double amount) {
        this.value.add(amount);
    }
    
    /**
     * Increment the gauge by exactly one on top of its current value
     */
    public void increment() {
        this.increment(1.0d);
    }
    
    /**
     * Decrement the gauge down by a certain amount
     * @param amount The amount to decrement the gauge by
     */
    public void decrement(double amount) {
        this.increment(-amount);
    }
    
    /**
     * Decrement the gauge by exactly one on top of its current value
     */
    public void decrement() {
        this.decrement(1.0d);
    }
    
    public double value() {
        return this.value.sum();
    }
    
}
