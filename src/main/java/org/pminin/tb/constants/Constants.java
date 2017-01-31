package org.pminin.tb.constants;

public interface Constants {

	static final String ACTOR_PATH_HEAD = "akka://TradingSystem/user/";

	static final String COLLECTOR = "collector";
	static final String ANALYZER = "analyzer";
	static final String STRATEGY = "strategy";

	static final String PIVOT = "pivot";
	static final String TRADE_CHECK = "tradecheck";
	static final String ORDER_CHECK = "ordercheck";

	static final String CONFIRM_FRACTAL = "confirmfractal";
	static final String BREAK_FRACTAL = "breakfractal";

	static final int DIRECTION_UP = 1;
	static final int DIRECTION_DOWN = -1;

	static final String SELL = "sell";
	static final String BUY = "buy";
	static final String TYPE_MARKET = "market";
	static final String TYPE_MARKETIFTOUCHED = "marketIfTouched";
	static final String TYPE_LIMIT = "stop";

}
