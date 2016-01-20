package com.konkerlabs.platform.registry.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class Event {

    private Instant timestamp;
    private String payload;

}