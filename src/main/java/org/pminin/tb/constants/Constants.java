package org.pminin.tb.constants;

public interface Constants {

    String ACTOR_PATH_HEAD = "akka://TradingSystem/user/";

    String COLLECTOR = "collector";
    String ANALYZER = "analyzer";
    String STRATEGY = "strategy";
    String NEWSCHECK = "newscheck";

    String PIVOT = "pivot";
    String TRADE_CHECK = "tradecheck";
    String ORDER_CHECK = "ordercheck";

    String CONFIRM_FRACTAL = "confirmfractal";
    String BREAK_FRACTAL = "breakfractal";

    int DIRECTION_UP = 1;
    int DIRECTION_DOWN = -1;

    String SELL = "sell";
    String BUY = "buy";
    String TYPE_MARKET = "market";
    String TYPE_MARKETIFTOUCHED = "marketIfTouched";
    String TYPE_LIMIT = "stop";
    String STOP_LOSS = "STOP_LOSS";

}
