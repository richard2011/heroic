package com.spotify.heroic.cache.model;

import java.util.List;

import lombok.Getter;
import lombok.ToString;

import com.spotify.heroic.aggregator.Aggregation;
import com.spotify.heroic.model.DataPoint;
import com.spotify.heroic.model.TimeSerieSlice;

@ToString(of = { "slice", "result", "misses" })
public class CacheQueryResult {
    @Getter
    private final TimeSerieSlice slice;

    @Getter
    private final Aggregation aggregation;

    /**
     * Collected results so far. Should be joined by the result from the above
     * cache misses.
     */
    @Getter
    private final List<DataPoint> result;

    /**
     * Cache misses that has to be queried and aggregated from raw storage.
     */
    @Getter
    private final List<TimeSerieSlice> misses;

    public CacheQueryResult(TimeSerieSlice slice, Aggregation aggregation,
            List<DataPoint> result, List<TimeSerieSlice> misses) {
        this.slice = slice;
        this.aggregation = aggregation;
        this.result = result;
        this.misses = misses;
    }
}