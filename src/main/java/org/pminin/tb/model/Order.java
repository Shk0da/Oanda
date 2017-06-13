package org.pminin.tb.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Order {

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
    private Details takeProfitOnFill = new Details();
    private Details stopLossOnFill = new Details();
    //private Details trailingStopLossOnFill;
    //private int fillingTransactionID;
    //private long filledTime;
    //private int tradeOpenedID;
    //private int tradeReducedID;
    //private int[] tradeClosedIDs;
    //private int cancellingTransactionID;
    private long cancelledTime;

    public enum OrderState {
        PENDING, FILLED, TRIGGERED, CANCELLED
    }

    public enum OrderType {
        MARKET, LIMIT, STOP, MARKET_IF_TOUCHED, TAKE_PROFIT, STOP_LOSS, TRAILING_STOP_LOSS, MARKET_ORDER, STOP_ORDER, LIMIT_ORDER
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
        private double price;
        private TimeInForce timeInForce = TimeInForce.GTC;

        public Details(double price) {
            this.price = price;
        }
        //private long gtdTime;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Orders {
        List<Order> orders = new ArrayList<>();
    }
}