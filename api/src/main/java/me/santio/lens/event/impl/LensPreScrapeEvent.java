package me.santio.lens.event.impl;

import lombok.Getter;
import lombok.experimental.Accessors;
import me.santio.lens.event.LensEvent;

/**
 * Called before lens is scraped for data
 */
@Accessors(fluent = true)
public class LensPreScrapeEvent implements LensEvent {
    @Getter
    private static final LensPreScrapeEvent instance = new LensPreScrapeEvent();
}
