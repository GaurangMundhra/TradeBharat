package com.finsimx.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Candlestick (OHLC) Data Transfer Object
 * Represents a single candle (candlestick) in a time interval
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandleDTO {

    @JsonProperty("asset")
    private String asset;

    @JsonProperty("open")
    private Double open;

    @JsonProperty("high")
    private Double high;

    @JsonProperty("low")
    private Double low;

    @JsonProperty("close")
    private Double close;

    @JsonProperty("volume")
    private Double volume;

    @JsonProperty("interval")
    private String interval; // e.g., "1m", "5m", "1h"

    @JsonProperty("startTime")
    private long startTime; // Unix timestamp in milliseconds

    @JsonProperty("endTime")
    private long endTime; // Unix timestamp in milliseconds
}
