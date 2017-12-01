package com.oanda.bot.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostOrderResponse {

    public boolean active = false;
    private Order orderCreateTransaction = null;

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
        orderCreateTransaction = new Order();
    }

    public boolean hasOpenTrade() {
        return orderCreateTransaction != null && orderCreateTransaction.getId() != null && !orderCreateTransaction.getId().isEmpty();
    }
}
