package com.ddd_bootcamp.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EventWrapper {
    private final String type;
    private final String data;

    @JsonCreator
    public EventWrapper(
        @JsonProperty("type") String type,
        @JsonProperty("data") String data
    ) {
        this.type = type;
        this.data = data;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("data")
    public String getData() {
        return data;
    }
} 