package com.oanda.bot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Price {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bid {
        private double price;
        private int liquidity;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ask {
        private double price;
        private int liquidity;
    }

    private String instrument;
    @JsonDeserialize(using = StringDateTimeDeserializer.class)
    private DateTime time;
    private List<Bid> bids;
    private List<Ask> asks;
    private double closeoutAsk;
    private double closeoutBid;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prices {
        private List<Price> prices = new ArrayList<>();
    }

    public double getBid() {
        if (getBids().isEmpty()) return closeoutBid;

        return getBids().get(0).getPrice();
    }

    public double getAsk() {
        if (getAsks().isEmpty()) return closeoutAsk;

        return getAsks().get(0).getPrice();
    }

    public double getSpread() {
        return Math.abs(getAsk() - getBid());
    }

}
