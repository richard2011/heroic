package com.spotify.heroic.filter.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.spotify.heroic.filter.AndFilter;
import com.spotify.heroic.filter.Filter;
import com.spotify.heroic.filter.HasTagFilter;
import com.spotify.heroic.filter.MatchKeyFilter;
import com.spotify.heroic.filter.MatchTagFilter;
import com.spotify.heroic.filter.OrFilter;
import com.spotify.heroic.filter.RegexFilter;
import com.spotify.heroic.filter.StartsWithFilter;

public class FilterJsonDeserializer extends JsonDeserializer<Filter> {
    private static final Map<String, FilterSerialization<? extends Filter>> IMPL = new HashMap<>();

    static {
        IMPL.put(MatchTagFilter.OPERATOR, Filter.MATCH_TAG);
        IMPL.put(StartsWithFilter.OPERATOR, Filter.STARTS_WITH);
        IMPL.put(RegexFilter.OPERATOR, Filter.REGEX);
        IMPL.put(HasTagFilter.OPERATOR, Filter.HAS_TAG);
        IMPL.put(MatchKeyFilter.OPERATOR, Filter.MATCH_KEY);
        IMPL.put(AndFilter.OPERATOR, Filter.AND);
        IMPL.put(OrFilter.OPERATOR, Filter.OR);
    }

    @Override
    public Filter deserialize(JsonParser p, DeserializationContext c)
            throws IOException, JsonProcessingException {

        if (p.getCurrentToken() != JsonToken.START_ARRAY)
            throw c.mappingException("Expected start of array");

        final String operator;

        {
            if (p.nextToken() != JsonToken.VALUE_STRING)
                throw c.mappingException("Expected string (operation)");

            operator = p.readValueAs(String.class);
        }

        final FilterSerialization<? extends Filter> deserializer = IMPL
                .get(operator);

        if (deserializer == null)
            throw c.mappingException("No such operator: " + operator);

        return deserializer.deserialize(p, c).optimize();
    }
}