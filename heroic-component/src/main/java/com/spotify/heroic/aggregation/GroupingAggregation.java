/*
 * Copyright (c) 2015 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.heroic.aggregation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableList;
import com.spotify.heroic.model.DateRange;
import com.spotify.heroic.model.Series;
import com.spotify.heroic.model.Statistics;

@Data
@EqualsAndHashCode(of = { "of", "each" })
public abstract class GroupingAggregation implements Aggregation {
    private final List<String> of;
    private final Aggregation each;

    public GroupingAggregation(final List<String> of, final Aggregation each) {
        this.of = of;
        this.each = checkNotNull(each, "each");
    }

    /**
     * Generate a key for the specific group.
     * 
     * @param input The input tags for the group.
     * @return The keys for a specific group.
     */
    protected abstract Map<String, String> key(Map<String, String> input);

    @Override
    public AggregationTraversal session(List<AggregationState> states, DateRange range) {
        return traversal(map(states), range);
    }

    /**
     * Traverse the given input states, and map them to their corresponding keys.
     *
     * @param states Input states to map.
     * @return A mapping for the group key, to a set of series.
     */
    Map<Map<String, String>, Set<Series>> map(final List<AggregationState> states) {
        final Map<Map<String, String>, Set<Series>> output = new HashMap<>();

        for (final AggregationState state : states) {
            final Map<String, String> k = key(state.getKey());

            Set<Series> series = output.get(k);

            if (series == null) {
                series = new HashSet<Series>();
                output.put(k, series);
            }

            series.addAll(state.getSeries());
        }

        return output;
    }

    /**
     * Setup traversal and the corresponding session from the given mapping.
     *
     * @param mapping The mapping of series.
     * @param range The range to setup child sessions using.
     * @return A new traversal instance.
     */
    AggregationTraversal traversal(final Map<Map<String, String>, Set<Series>> mapping, final DateRange range) {
        final Map<Map<String, String>, AggregationSession> sessions = new HashMap<>();
        final List<AggregationState> states = new ArrayList<>();

        for (final Map.Entry<Map<String, String>, Set<Series>> e : mapping.entrySet()) {
            final Set<Series> series = new HashSet<>();

            final AggregationTraversal traversal = each.session(
                    ImmutableList.of(new AggregationState(e.getKey(), e.getValue())),
                    range);

            for (final AggregationState state : traversal.getStates())
                series.addAll(state.getSeries());

            sessions.put(e.getKey(), traversal.getSession());
            states.add(new AggregationState(e.getKey(), series));
        }

        return new AggregationTraversal(states, new GroupSession(sessions));
    }

    @Override
    public long estimate(DateRange range) {
        return each.estimate(range);
    }

    @Override
    public long extent() {
        return each.extent();
    }

    @RequiredArgsConstructor
    private final class GroupSession implements AggregationSession {
        private final Map<Map<String, String>, AggregationSession> sessions;

        @Override
        public void update(AggregationData group) {
            final Map<String, String> key = key(group.getGroup());
            AggregationSession session = sessions.get(key);

            if (session == null)
                throw new IllegalStateException(String.format("no session for %s", key));

            // update using this groups key.
            session.update(new AggregationData(key, group.getSeries(), group.getValues(), group.getOutput()));
        }

        @Override
        public AggregationResult result() {
            final List<AggregationData> groups = new ArrayList<>();

            Statistics.Aggregator statistics = Statistics.Aggregator.EMPTY;

            for (final Map.Entry<Map<String, String>, AggregationSession> e : sessions.entrySet()) {
                final AggregationResult r = e.getValue().result();
                statistics = statistics.merge(r.getStatistics());
                groups.addAll(r.getResult());
            }

            return new AggregationResult(groups, statistics);
        }
    }
}