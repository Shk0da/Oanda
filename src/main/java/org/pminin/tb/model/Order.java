package org.pminin.tb.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Order {

    public enum OrderState {
        PENDING, FILLED, TRIGGERED, CANCELLED
    }

    public enum OrderType {
        MARKET, LIMIT, STOP, MARKET_IF_TOUCHED, TAKE_PROFIT, STOP_LOSS, TRAILING_STOP_LOSS, MARKET_ORDER
    }

    public enum TimeInForce {
        GTC, GTD, GFD, FOK, IOC
    }

    public enum OrderPositionFill {
        OPEN_ONLY, REDUCE_FIRST, REDUCE_ONLY, DEFAULT
    }

    public enum OrderTriggerCondition {
        DEFAULT, INVERSE, BID, ASK, MID
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Details {
        public Details(double price) {
            this.price = price;
        }

        private Double price;
        private TimeInForce timeInForce = TimeInForce.GTC;
        //private long gtdTime;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Orders {
        List<Order> orders = new ArrayList<>();
    }

    private String id;
    private long createTime;
    private OrderState state;
    private OrderType type = OrderType.MARKET;
    private String instrument;
    private double units;
    private double price;
    //private double priceBound;
    private TimeInForce timeInForce = TimeInForce.FOK;
    //@JsonDeserialize(using = UnixTimestampDeserializer.class)
    //private String gtdTime;
    private OrderPositionFill positionFill = OrderPositionFill.DEFAULT;
    //private OrderTriggerCondition triggerCondition;
    private Details takeProfitOnFill;
    private Details stopLossOnFill;
    //private Details trailingStopLossOnFill;
    //private int fillingTransactionID;
    //private long filledTime;
    //private int tradeOpenedID;
    //private int tradeReducedID;
    //private int[] tradeClosedIDs;
    //private int cancellingTransactionID;
    private long cancelledTime;
}