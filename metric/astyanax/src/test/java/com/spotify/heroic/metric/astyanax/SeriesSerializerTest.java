package com.spotify.heroic.metric.astyanax;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.spotify.heroic.common.Series;

public class SeriesSerializerTest {
    private static final SeriesSerializer serializer = SeriesSerializer.get();

    private Series roundTrip(Series series) {
        final ByteBuffer bb = serializer.toByteBuffer(series);
        final Series after = serializer.fromByteBuffer(bb);
        bb.rewind();
        Assert.assertEquals(bb, serializer.toByteBuffer(after));
        return after;
    }

    @Test
    public void testEmpty() throws Exception {
        final Series series = Series.of(null, new HashMap<String, String>());
        Assert.assertEquals(series, roundTrip(series));
    }

    @Test
    public void testTagsWithNull() throws Exception {
        final Map<String, String> tags = new HashMap<String, String>();
        tags.put(null, null);
        final Series series = Series.of(null, tags);
        Assert.assertEquals(series, roundTrip(series));
    }

    @Test
    public void testTagsWithMixed() throws Exception {
        final Map<String, String> tags = new HashMap<String, String>();
        tags.put(null, null);
        tags.put("foo", "bar");
        tags.put("bar", null);
        final Series series = Series.of(null, tags);
        Assert.assertEquals(series, roundTrip(series));
    }

    @Test
    public void testStoreSomeValues() throws Exception {
        final Map<String, String> tags = new HashMap<String, String>();
        tags.put("a", "b");
        tags.put("b", "c");
        final Series series = Series.of("baz", tags);
        Assert.assertEquals(series, roundTrip(series));
    }
}