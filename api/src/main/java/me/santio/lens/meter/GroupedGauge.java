package me.santio.lens.meter;

import me.santio.lens.Lens;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A gauge that tracks the tags of each member, exposing one gauge per unique set of tags with the amount
 * of members currently holding them. Values are derived from the tracked members rather than incremented
 * and decremented, so removing a member that was never added can not cause the gauge to drift.
 * Only one instance should exist per metric name, as the meter registry will keep the first registered gauge.
 * @param <K> The type used to identify members
 * @author santio
 */
@SuppressWarnings("WeakerAccess")
public class GroupedGauge<K> {

    private final Map<K, Map<String, String>> members = new ConcurrentHashMap<>();
    private final Set<Map<String, String>> groups = ConcurrentHashMap.newKeySet();

    private final String name;

    public GroupedGauge(String name) {
        this.name = name;
    }

    /**
     * Track a member under the provided tags, replacing any tags it was previously tracked under
     * @param member The member to track
     * @param tags The tags the member should be counted under
     */
    public void put(K member, Map<String, String> tags) {
        this.members.put(member, tags);

        if (this.groups.add(tags)) {
            Lens.instance().gauge(this.name, tags, () -> this.count(tags));
        }
    }

    /**
     * Stop tracking a member, nothing happens if the member is not tracked
     * @param member The member to remove
     */
    public void remove(K member) {
        this.members.remove(member);
    }

    private double count(Map<String, String> tags) {
        return this.members.values().stream()
            .filter(tags::equals)
            .count();
    }

}
