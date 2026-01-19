package me.santio.lens.event.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import me.santio.lens.event.LensEvent;

/**
 * Called before lens is scraped for data
 */
@Accessors(fluent = true)
@RequiredArgsConstructor
@Getter
public class LensScrapedEvent implements LensEvent {
    private final String data;
}
