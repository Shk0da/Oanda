package org.pminin.tb.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.pminin.tb.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Order {

    private String id;
    @JsonDeserialize(using = StringDateTimeDeserializer.class)
    private DateTime createTime;
    private OrderState state;
    private OrderType type = OrderType.MARKET;
    private String instrument;
    private double units;
    private double price;
    private TimeInForce timeInForce = TimeInForce.GTD;
    private String gtdTime = DateTimeUtil.rfc3339Plus2Days();
    private OrderPositionFill positionFill = OrderPositionFill.DEFAULT;
    private OrderTriggerCondition triggerCondition = OrderTriggerCondition.DEFAULT;
    private Details takeProfitOnFill = new Details();
    private Details stopLossOnFill = new Details();
    private String cancelledTime = DateTimeUtil.rfc3339Plus2Days();

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
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Orders {
        List<Order> orders = new ArrayList<>();
    }

    public void setType(OrderType type) {
        if (type.equals(OrderType.LIMIT_ORDER)) type = OrderType.LIMIT;
        if (type.equals(OrderType.STOP_ORDER)) type = OrderType.STOP;
        if (type.equals(OrderType.MARKET_ORDER)) type = OrderType.MARKET;

        this.type = type;
    }

    public double getUnits() {
        if (type.equals(OrderType.STOP)) units = units * (-1);
        if (type.equals(OrderType.LIMIT)) units = Math.abs(units);

        return units;
    }

}